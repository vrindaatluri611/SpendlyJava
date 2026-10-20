
package com.spendly;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class FinancialAnalysisPage {

    private final ExpenseDAO expenseDAO = new ExpenseDAO();
    private final BudgetDAO budgetDAO = new BudgetDAO();

    private final ObservableList<String[]> analysis =
            FXCollections.observableArrayList();

    private final TableView<String[]> table = new TableView<>();

    private final Label totalLabel = new Label();
    private final Label message = new Label();

    private AnalysisService analysisService;

    public VBox getView() {

        VBox page = new VBox(15);
        page.setPadding(new Insets(20));

        Label title = new Label("Financial Analysis");
        Label subtitle =
                new Label("Compare your budget with your spending.");

        DatePicker monthPicker =
                new DatePicker(LocalDate.now().withDayOfMonth(1));

        Button showButton = new Button("Show Analysis");

        HBox controls = new HBox(10, monthPicker, showButton);

        TableColumn<String[], String> categoryColumn =
                new TableColumn<>("Category");
        categoryColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue()[0]));

        TableColumn<String[], String> budgetColumn =
                new TableColumn<>("Budget");
        budgetColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue()[1]));

        TableColumn<String[], String> spentColumn =
                new TableColumn<>("Spent");
        spentColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue()[2]));

        TableColumn<String[], String> remainingColumn =
                new TableColumn<>("Remaining");
        remainingColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue()[3]));

        table.getColumns().addAll(
                categoryColumn,
                budgetColumn,
                spentColumn,
                remainingColumn
        );

        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );
        table.setItems(analysis);

        analysisService = new AnalysisService(expenseDAO);

        showButton.setOnAction(event -> {
            LocalDate selectedMonth = monthPicker.getValue();

            if (selectedMonth == null) {
                message.setText("Please select a month.");
                return;
            }

            loadAnalysis(selectedMonth);
        });

        page.getChildren().addAll(
                title,
                subtitle,
                controls,
                totalLabel,
                message,
                table
        );

        loadAnalysis(LocalDate.now().withDayOfMonth(1));

        return page;
    }

    private void loadAnalysis(LocalDate selectedMonth) {

        if (analysisService.isRunning()) {
            analysisService.cancel();
        }

        analysis.clear();
        totalLabel.setText("");
        message.setText("Loading analysis...");

        analysisService.reset();
        analysisService.setSelectedMonth(selectedMonth);

        analysisService.setOnSucceeded(
                (WorkerStateEvent event) -> {
                    List<String[]> categoryTotals =
                            analysisService.getValue();

                    displayAnalysis(selectedMonth, categoryTotals);
                }
        );

        analysisService.setOnFailed(
                (WorkerStateEvent event) -> {
                    Throwable error = analysisService.getException();

                    message.setText("Could not load analysis: "
                            + (error == null
                                    ? "Unknown error"
                                    : error.getMessage()));
                }
        );

        analysisService.setOnCancelled(event ->
                message.setText("Analysis cancelled.")
        );

        analysisService.start();
    }

    private void displayAnalysis(
            LocalDate selectedMonth,
            List<String[]> categoryTotals) {

        analysis.clear();

        LocalDate firstDay = selectedMonth.withDayOfMonth(1);
        Date sqlMonth = Date.valueOf(firstDay);

        List<String[]> budgets =
                budgetDAO.getBudgetsForMonth(sqlMonth);

        double totalSpent = 0;

        for (String[] budget : budgets) {

            String category = budget[1];
            double budgetAmount = Double.parseDouble(budget[2]);

            double spent = 0;

            for (String[] categoryTotal : categoryTotals) {
                if (categoryTotal[0].equals(category)) {
                    spent = Double.parseDouble(categoryTotal[1]);
                    break;
                }
            }

            double remaining = budgetAmount - spent;

            analysis.add(new String[] {
                    category,
                    String.format("%.2f", budgetAmount),
                    String.format("%.2f", spent),
                    String.format("%.2f", remaining)
            });

            totalSpent += spent;
        }

        totalLabel.setText(
                String.format(
                        "Total spending in budgeted categories: %.2f",
                        totalSpent
                )
        );

        if (budgets.isEmpty()) {
            message.setText("No budgets found for this month.");
        } else {
            message.setText("Analysis updated.");
        }
    }
}