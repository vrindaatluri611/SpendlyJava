package com.spendly;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class BudgetPage {

    private final BudgetDAO dao = new BudgetDAO();

    private final ObservableList<String[]> budgetData =
            FXCollections.observableArrayList();

    private final TableView<String[]> table = new TableView<>();

    private final Label message = new Label();

    public VBox getView() {

        VBox page = new VBox(12);
        page.setPadding(new Insets(20));

        Label title = new Label("Budgets");
        Label subtitle =
                new Label("Set a monthly budget and track your spending.");

        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll(
                "Food & Dining",
                "Transport",
                "Shopping",
                "Entertainment",
                "Bills",
                "Education",
                "Health",
                "Other"
        );
        categoryBox.setPromptText("Select category");

        TextField amountField = new TextField();
        amountField.setPromptText("Budget amount");

        DatePicker monthPicker =
                new DatePicker(LocalDate.now().withDayOfMonth(1));

        Button addButton = new Button("Add Budget");
        Button deleteButton = new Button("Delete Selected");

        HBox form = new HBox(
                10, categoryBox, amountField, monthPicker, addButton
        );

        // Displayed serial number (1, 2, 3...) instead of database ID.
        TableColumn<String[], String> serialColumn =
                new TableColumn<>("S.No.");
        serialColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        String.valueOf(
                                table.getItems().indexOf(data.getValue()) + 1
                        )
                ));

        TableColumn<String[], String> categoryColumn =
                new TableColumn<>("Category");
        categoryColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue()[1]));

        TableColumn<String[], String> budgetColumn =
                new TableColumn<>("Budget");
        budgetColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue()[2]));

        TableColumn<String[], String> spentColumn =
                new TableColumn<>("Spent");
        spentColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue()[3]));

        TableColumn<String[], String> remainingColumn =
                new TableColumn<>("Remaining");
        remainingColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue()[4]));

        TableColumn<String[], String> monthColumn =
                new TableColumn<>("Month");
        monthColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue()[5]));

        table.getColumns().addAll(
                serialColumn,
                categoryColumn,
                budgetColumn,
                spentColumn,
                remainingColumn,
                monthColumn
        );

        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );
        table.setItems(budgetData);

        // Refresh displayed serial numbers when the list changes.
        budgetData.addListener(
                (javafx.collections.ListChangeListener<String[]>) change ->
                        table.refresh()
        );

        addButton.setOnAction(event -> {

            String category = categoryBox.getValue();
            String amountText = amountField.getText().trim();
            LocalDate selectedMonth = monthPicker.getValue();

            if (category == null || amountText.isEmpty()
                    || selectedMonth == null) {
                showAlert(
                        "Missing Information",
                        "Please select a category, enter an amount, "
                                + "and select a month."
                );
                return;
            }

            try {
                double amount = Double.parseDouble(amountText);

                if (!Double.isFinite(amount) || amount <= 0) {
                    showAlert(
                            "Invalid Amount",
                            "Please enter a valid amount greater than zero."
                    );
                    return;
                }

                LocalDate firstDay =
                        selectedMonth.withDayOfMonth(1);

                boolean added = dao.addBudget(
                        category,
                        amount,
                        Date.valueOf(firstDay)
                );

                if (added) {
                    amountField.clear();
                    categoryBox.setValue(null);
                    loadBudgets();
                    message.setText("Budget added successfully.");
                } else {
                    showAlert(
                            "Error",
                            "Could not save the budget. Please try again."
                    );
                }

            } catch (NumberFormatException ex) {
                showAlert(
                        "Invalid Amount",
                        "Please enter the budget as a number."
                );
            }
        });

        deleteButton.setOnAction(event -> {

            String[] selected = table.getSelectionModel()
                    .getSelectedItem();

            if (selected == null) {
                showAlert(
                        "No Budget Selected",
                        "Please select a budget to delete."
                );
                return;
            }

            // The actual database ID remains in array position 0.
            boolean deleted =
                    dao.deleteBudget(Integer.parseInt(selected[0]));

            if (deleted) {
                loadBudgets();
                message.setText("Budget deleted successfully.");
            } else {
                showAlert(
                        "Error",
                        "Could not delete the selected budget."
                );
            }
        });

        page.getChildren().addAll(
                title,
                subtitle,
                form,
                deleteButton,
                message,
                table
        );

        loadBudgets();

        return page;
    }

    private void loadBudgets() {

        budgetData.clear();

        List<String[]> budgets = dao.getAllBudgets();

        for (String[] budget : budgets) {

            int budgetId = Integer.parseInt(budget[0]);
            String category = budget[1];
            double budgetAmount = Double.parseDouble(budget[2]);
            LocalDate month = Date.valueOf(budget[3])
                    .toLocalDate()
                    .withDayOfMonth(1);

            double spent = dao.getSpentAmount(
                    category,
                    Date.valueOf(month)
            );

            double remaining = budgetAmount - spent;

            String warning = "";

            if (spent > budgetAmount) {
                warning = "  OVER BUDGET!";
            }

            // Keep the real database ID internally for deletion.
            budgetData.add(new String[] {
                    String.valueOf(budgetId),
                    category,
                    String.format("%.2f", budgetAmount),
                    String.format("%.2f", spent),
                    String.format("%.2f", remaining) + warning,
                    month.toString()
            });
        }

        table.refresh();

        boolean overBudget = false;

        for (String[] budget : budgetData) {
            if (budget[4].contains("OVER BUDGET!")) {
                overBudget = true;
                break;
            }
        }

        if (overBudget) {
            message.setText(
                    "Warning: You have exceeded at least one budget!"
            );
        } else {
            message.setText("Your budgets are up to date.");
        }
    }

    private void showAlert(String title, String content) {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}