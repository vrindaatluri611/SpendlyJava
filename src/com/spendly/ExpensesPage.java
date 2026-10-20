
package com.spendly;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ExpensesPage {

    private final ObservableList<Expense> expenses =
            FXCollections.observableArrayList();

    private final TableView<Expense> table = new TableView<>();
    private final Label message = new Label();
    private final ExpenseDAO dao = new ExpenseDAO();
    private final ComboBox<String> categoryBox = new ComboBox<>();

    public VBox getView() {

        VBox page = new VBox(15);
        page.setPadding(new Insets(20));

        Label title = new Label("Expenses");
        Label subtitle =
                new Label("Add and manage your daily expenses.");

        TextField descriptionField = new TextField();
        descriptionField.setPromptText("Expense description");

        TextField amountField = new TextField();
        amountField.setPromptText("Amount");

        categoryBox.setPromptText("Select category");
        categoryBox.setMaxWidth(Double.MAX_VALUE);

        Button addCategoryButton = new Button("Add Category");
        Button deleteCategoryButton = new Button("Delete Category");
        Button refreshCategoriesButton = new Button("Refresh Categories");

        addCategoryButton.setOnAction(event -> addCategory());
        deleteCategoryButton.setOnAction(event -> deleteCategory());
        refreshCategoriesButton.setOnAction(event -> loadCategories());

        HBox categoryRow = new HBox(
                8,
                categoryBox,
                addCategoryButton,
                deleteCategoryButton,
                refreshCategoriesButton
        );

        HBox.setHgrow(categoryBox, Priority.ALWAYS);

        DatePicker datePicker = new DatePicker(LocalDate.now());

        HBox formRow = new HBox(
                10, descriptionField, amountField, categoryRow, datePicker);

        HBox.setHgrow(descriptionField, Priority.ALWAYS);
        HBox.setHgrow(amountField, Priority.ALWAYS);
        HBox.setHgrow(categoryRow, Priority.ALWAYS);

        Button addButton = new Button("Add Expense");
        Button deleteButton = new Button("Delete Selected");

        TableColumn<Expense, String> serialColumn =
                new TableColumn<>("Serial No.");

        serialColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        String.valueOf(
                                table.getItems().indexOf(data.getValue()) + 1
                        )
                )
        );

        TableColumn<Expense, String> descriptionColumn =
                new TableColumn<>("Description");

        descriptionColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getDescription()));

        TableColumn<Expense, String> categoryColumn =
                new TableColumn<>("Category");

        categoryColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getCategory()));

        TableColumn<Expense, String> amountColumn =
                new TableColumn<>("Amount");

        amountColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        String.format("%.2f", data.getValue().getAmount())));

        TableColumn<Expense, String> dateColumn =
                new TableColumn<>("Date");

        dateColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getDate().toString()));

        table.getColumns().addAll(
                serialColumn, descriptionColumn, categoryColumn,
                amountColumn, dateColumn);

        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        table.setItems(expenses);
        table.setPlaceholder(new Label("No expenses saved yet."));

        expenses.addListener(
                (javafx.collections.ListChangeListener<Expense>)
                        change -> table.refresh());

        loadCategories();
        loadExpenses();

        addButton.setOnAction(event -> {

            String description = descriptionField.getText().trim();
            String amountText = amountField.getText().trim();
            String category = categoryBox.getValue();
            LocalDate date = datePicker.getValue();

            if (description.isEmpty() || amountText.isEmpty()
                    || category == null || date == null) {
                message.setText("Please fill in all fields.");
                return;
            }

            try {
                double amount = Double.parseDouble(amountText);

                if (!Double.isFinite(amount) || amount <= 0) {
                    message.setText("Enter an amount greater than zero.");
                    return;
                }

                boolean saved = dao.addExpense(
                        description, category, amount, Date.valueOf(date));

                if (saved) {
                    message.setText("Expense saved successfully!");
                    descriptionField.clear();
                    amountField.clear();
                    categoryBox.setValue(null);
                    datePicker.setValue(LocalDate.now());
                    loadExpenses();
                } else {
                    message.setText("Could not save expense.");
                }

            } catch (NumberFormatException ex) {
                message.setText("Enter a valid amount.");
            }
        });

        deleteButton.setOnAction(event -> {

            Expense selected = table.getSelectionModel().getSelectedItem();

            if (selected == null) {
                message.setText("Select an expense first.");
                return;
            }

            boolean deleted = dao.deleteExpense(selected.getId());

            if (deleted) {
                message.setText("Expense deleted.");
                loadExpenses();
            } else {
                message.setText("Could not delete expense.");
            }
        });

        HBox buttonRow = new HBox(10, addButton, deleteButton);

        page.getChildren().addAll(
                title, subtitle, formRow, buttonRow,
                message, new Label("All Expenses"), table);

        VBox.setVgrow(table, Priority.ALWAYS);
        return page;
    }

    private void addCategory() {

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Category");
        dialog.setHeaderText("Create a shared expense category");
        dialog.setContentText("Category name:");

        dialog.showAndWait().ifPresent(input -> {

            String category = input.trim();

            if (category.isEmpty() || category.length() > 100) {
                showAlert(Alert.AlertType.WARNING, "Invalid Category",
                        "Enter a category name between 1 and 100 characters.");
                return;
            }

            String sql =
                    "INSERT INTO categories (category_name) VALUES (?)";

            try (Connection connection = DatabaseConnection.getConnection();
                 PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setString(1, category);
                statement.executeUpdate();

                loadCategories();
                categoryBox.setValue(findCategoryIgnoreCase(category));
                message.setText("Category added successfully.");

            } catch (SQLException ex) {
                if (ex.getErrorCode() == 1062) {
                    showAlert(Alert.AlertType.INFORMATION,
                            "Category Already Exists",
                            "That category already exists.");
                    loadCategories();
                    categoryBox.setValue(findCategoryIgnoreCase(category));
                } else {
                    ex.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Database Error",
                            "Could not save the category. Check the server log.");
                }
            }
        });
    }

    private void deleteCategory() {

        String category = categoryBox.getValue();

        if (category == null || category.isBlank()) {
            message.setText("Select a category to delete.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Category");
        confirmation.setHeaderText("Delete \"" + category + "\"?");
        confirmation.setContentText(
                "A category used by any expense or budget cannot be deleted.");

        Optional<ButtonType> answer = confirmation.showAndWait();

        if (answer.isEmpty() || answer.get() != ButtonType.OK) {
            return;
        }

        String usageSql = """
            SELECT
              (SELECT COUNT(*) FROM expenses WHERE category = ?)
              +
              (SELECT COUNT(*) FROM budgets WHERE category = ?)
            """;

        String deleteSql =
                "DELETE FROM categories WHERE category_name = ?";

        try (Connection connection = DatabaseConnection.getConnection()) {

            // Check usage and delete within one transaction.
            connection.setAutoCommit(false);

            try {
                try (PreparedStatement check =
                             connection.prepareStatement(usageSql)) {

                    check.setString(1, category);
                    check.setString(2, category);

                    try (ResultSet rs = check.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            connection.rollback();
                            message.setText(
                                    "Cannot delete: this category is used "
                                    + "by an expense or budget.");
                            return;
                        }
                    }
                }

                try (PreparedStatement delete =
                             connection.prepareStatement(deleteSql)) {

                    delete.setString(1, category);

                    if (delete.executeUpdate() == 0) {
                        connection.rollback();
                        message.setText("Category was not found.");
                        return;
                    }
                }

                connection.commit();

            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }

            categoryBox.setValue(null);
            loadCategories();
            message.setText("Category deleted successfully.");

        } catch (SQLException ex) {
            ex.printStackTrace();
            message.setText("Could not delete category. Check the database.");
        }
    }

    private void loadCategories() {

        String selected = categoryBox.getValue();
        List<String> categories = new ArrayList<>();

        String sql =
                "SELECT category_name FROM categories ORDER BY category_name";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                categories.add(rs.getString("category_name"));
            }

            categoryBox.setItems(
                    FXCollections.observableArrayList(categories));

            if (selected != null) {
                categoryBox.setValue(findCategoryIgnoreCase(selected));
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            message.setText("Could not load categories from MySQL.");
        }
    }

    private String findCategoryIgnoreCase(String name) {

        if (name == null) {
            return null;
        }

        for (String category : categoryBox.getItems()) {
            if (category.equalsIgnoreCase(name)) {
                return category;
            }
        }

        return null;
    }

    private void loadExpenses() {

        expenses.clear();

        for (String[] row : dao.getAllExpenses()) {
            expenses.add(new Expense(
                    Integer.parseInt(row[0]),
                    row[1],
                    row[2],
                    Double.parseDouble(row[3]),
                    LocalDate.parse(row[4])));
        }

        table.refresh();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private static class Expense {

        private final int id;
        private final String description;
        private final String category;
        private final double amount;
        private final LocalDate date;

        public Expense(int id, String description, String category,
                       double amount, LocalDate date) {
            this.id = id;
            this.description = description;
            this.category = category;
            this.amount = amount;
            this.date = date;
        }

        public int getId() { return id; }
        public String getDescription() { return description; }
        public String getCategory() { return category; }
        public double getAmount() { return amount; }
        public LocalDate getDate() { return date; }
    }
}
