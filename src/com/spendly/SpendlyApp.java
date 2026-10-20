
package com.spendly;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SpendlyApp extends Application {

    private BorderPane mainLayout;

    private final String backgroundColor = "#f8f8f5";
    private final String greenColor = "#cfe8d3";
    private final String darkGreen = "#287a52";

    private Button dashboardButton;
    private Button expensesButton;
    private Button budgetsButton;
    private Button billButton;
    private Button lendingButton;
    private Button analysisButton;

    // New independent feature buttons
    private Button bankImportButton;
    private Button parentalControlsButton;

    private final ExpenseDAO expenseDAO = new ExpenseDAO();
    private final BudgetDAO budgetDAO = new BudgetDAO();

    private final DashboardRecommendationService recommendationService =
            new DashboardRecommendationService();

    private Label totalBudgetValueLabel;
    private Label monthlyExpensesValueLabel;
    private Label budgetLeftValueLabel;

    @Override
    public void start(Stage stage) {

        mainLayout = new BorderPane();
        mainLayout.setStyle(
                "-fx-background-color: " + backgroundColor + ";"
        );

        VBox sidebar = new VBox(15);
        sidebar.setPadding(new Insets(25, 15, 25, 15));
        sidebar.setPrefWidth(260);
        sidebar.setStyle(
                "-fx-background-color: " + greenColor + ";"
        );

        Label logo = new Label("Spendly");
        logo.setStyle(
                "-fx-font-size: 34px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + darkGreen + ";"
        );

        Label menuTitle = new Label("MAIN MENU");
        menuTitle.setStyle(
                "-fx-font-size: 13px;" +
                "-fx-text-fill: #496653;"
        );

        dashboardButton = createMenuButton("Dashboard");
        expensesButton = createMenuButton("Expenses");
        budgetsButton = createMenuButton("Budgets");
        billButton = createMenuButton("Bill Splitting");
        lendingButton = createMenuButton("Lending & Borrowing");
        analysisButton = createMenuButton("Financial Analysis");

        // Separate feature buttons
        bankImportButton =
                createMenuButton("Bank Statement Import");

        parentalControlsButton =
                createMenuButton("Parental Controls");

        sidebar.getChildren().addAll(
                logo,
                menuTitle,
                dashboardButton,
                expensesButton,
                budgetsButton,
                billButton,
                lendingButton,
                analysisButton,
                bankImportButton,
                parentalControlsButton
        );

        // Original navigation
        dashboardButton.setOnAction(e -> showDashboard());

        expensesButton.setOnAction(e -> {
            setActiveButton(expensesButton);
            mainLayout.setCenter(new ExpensesPage().getView());
        });

        budgetsButton.setOnAction(e -> {
            setActiveButton(budgetsButton);
            mainLayout.setCenter(new BudgetPage().getView());
        });

        analysisButton.setOnAction(e -> {
            setActiveButton(analysisButton);
            mainLayout.setCenter(new FinancialAnalysisPage().getView());
        });

        billButton.setOnAction(e -> {
            setActiveButton(billButton);
            mainLayout.setCenter(new BillSplittingPage().getView());
        });

        lendingButton.setOnAction(e -> {
            setActiveButton(lendingButton);
            mainLayout.setCenter(new LendingBorrowingPage().getView());
        });

        // NEW FEATURE 1: Bank statement import
        bankImportButton.setOnAction(e -> {
            setActiveButton(bankImportButton);
            mainLayout.setCenter(
                    new BankStatementImportPage().getView()
            );
        });

        // NEW FEATURE 2: Parental controls
        parentalControlsButton.setOnAction(e -> {
            setActiveButton(parentalControlsButton);
            mainLayout.setCenter(
                    new ParentalControlsPage().getView()
            );
        });

        mainLayout.setLeft(sidebar);

        showDashboard();

        Scene scene = new Scene(mainLayout, 1200, 800);

        stage.setTitle("Spendly - Personal Finance Manager");
        stage.setMinWidth(900);
        stage.setMinHeight(650);
        stage.setScene(scene);
        stage.show();
    }

    private Button createMenuButton(String text) {

        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(55);
        button.setWrapText(true);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setPadding(new Insets(10, 18, 10, 18));

        button.setStyle(
                "-fx-background-color: #f5f5f5;" +
                "-fx-text-fill: #202020;" +
                "-fx-font-size: 14px;" +
                "-fx-border-color: #b7b7b7;" +
                "-fx-background-radius: 5px;" +
                "-fx-border-radius: 5px;"
        );

        return button;
    }

    private void setActiveButton(Button activeButton) {

        Button[] buttons = {
                dashboardButton,
                expensesButton,
                budgetsButton,
                billButton,
                lendingButton,
                analysisButton,
                bankImportButton,
                parentalControlsButton
        };

        for (Button button : buttons) {

            if (button == activeButton) {
                button.setStyle(
                        "-fx-background-color: " + darkGreen + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 5px;" +
                        "-fx-border-radius: 5px;"
                );
            } else {
                button.setStyle(
                        "-fx-background-color: #f5f5f5;" +
                        "-fx-text-fill: #202020;" +
                        "-fx-font-size: 14px;" +
                        "-fx-border-color: #b7b7b7;" +
                        "-fx-background-radius: 5px;" +
                        "-fx-border-radius: 5px;"
                );
            }
        }
    }

    private void showDashboard() {

        setActiveButton(dashboardButton);

        VBox page = new VBox(18);
        page.setPadding(new Insets(24, 28, 28, 28));
        page.setStyle(
                "-fx-background-color: " + backgroundColor + ";"
        );

        Label title = new Label("Dashboard");
        title.setStyle(
                "-fx-font-size: 34px;" +
                "-fx-font-weight: bold;"
        );

        Label subtitle = new Label(
                "Your financial overview and recommendations."
        );
        subtitle.setStyle("-fx-font-size: 15px;");

        HBox cards = new HBox(15);

        VBox totalBudgetCard = createCard(
                "Total Budget",
                "Loading...",
                "Total budget assigned this month",
                "#e3f3e5",
                "#287a52"
        );

        VBox expensesCard = createCard(
                "Monthly Expenses",
                "Loading...",
                "Recorded expenses this month",
                "#f9e7e5",
                "#b13b32"
        );

        VBox budgetLeftCard = createCard(
                "Budget Left",
                "Loading...",
                "Remaining across this month's budgets",
                "#fff3d5",
                "#8a5a16"
        );

        cards.getChildren().addAll(
                totalBudgetCard,
                expensesCard,
                budgetLeftCard
        );

        HBox.setHgrow(totalBudgetCard, Priority.ALWAYS);
        HBox.setHgrow(expensesCard, Priority.ALWAYS);
        HBox.setHgrow(budgetLeftCard, Priority.ALWAYS);

        Label transactionsTitle = sectionTitle("Recent Transactions");
        GridPane transactionTable = createTransactionTable();

        Label chartsTitle = sectionTitle("Spending Insights");

        HBox chartRow = new HBox(15);

        VBox dailyChartBox = new VBox(10);
        dailyChartBox.setPadding(new Insets(15));
        dailyChartBox.setStyle(cardStyle("#ffffff"));
        dailyChartBox.setPrefWidth(500);

        VBox categoryChartBox = new VBox(10);
        categoryChartBox.setPadding(new Insets(15));
        categoryChartBox.setStyle(cardStyle("#ffffff"));
        categoryChartBox.setPrefWidth(400);

        Label dailyTitle = new Label("Daily Spending This Month");
        dailyTitle.setStyle(
                "-fx-font-size: 16px;" +
                "-fx-font-weight: bold;"
        );

        Label categoryTitle = new Label("Spending by Category");
        categoryTitle.setStyle(
                "-fx-font-size: 16px;" +
                "-fx-font-weight: bold;"
        );

        dailyChartBox.getChildren().add(dailyTitle);
        categoryChartBox.getChildren().add(categoryTitle);

        chartRow.getChildren().addAll(dailyChartBox, categoryChartBox);
        HBox.setHgrow(dailyChartBox, Priority.ALWAYS);
        HBox.setHgrow(categoryChartBox, Priority.ALWAYS);

        Label progressTitle = sectionTitle("Category Budget Progress");

        VBox progressBox = new VBox(12);
        progressBox.setPadding(new Insets(15));
        progressBox.setStyle(cardStyle("#ffffff"));

        Label alertsTitle = sectionTitle("Budget Alerts");

        VBox alertsBox = new VBox(8);
        alertsBox.setPadding(new Insets(15));
        alertsBox.setStyle(cardStyle("#ffffff"));

        Label timelineTitle = sectionTitle("Your Timeline");

        VBox timelineBox = new VBox(12);
        timelineBox.setPadding(new Insets(15));
        timelineBox.setStyle(cardStyle("#ffffff"));

        page.getChildren().addAll(
                title,
                subtitle,
                cards,
                transactionsTitle,
                transactionTable,
                chartsTitle,
                chartRow,
                progressTitle,
                progressBox,
                alertsTitle,
                alertsBox,
                timelineTitle,
                timelineBox
        );

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setStyle(
                "-fx-background: " + backgroundColor + ";" +
                "-fx-background-color: " + backgroundColor + ";"
        );

        mainLayout.setCenter(scroll);

        loadDashboardData(
                transactionTable,
                dailyChartBox,
                categoryChartBox,
                progressBox,
                alertsBox,
                timelineBox
        );
    }

    private void loadDashboardData(
            GridPane transactionTable,
            VBox dailyChartBox,
            VBox categoryChartBox,
            VBox progressBox,
            VBox alertsBox,
            VBox timelineBox) {

        Task<DashboardData> task = new Task<>() {

            @Override
            protected DashboardData call() {

                Date month = Date.valueOf(
                        LocalDate.now().withDayOfMonth(1)
                );

                DashboardData data = new DashboardData();

                data.expenses = expenseDAO.getAllExpenses();
                data.monthlyTotal =
                        expenseDAO.getMonthlyExpenseTotal(month);
                data.dailyTotals =
                        expenseDAO.getDailyTotals(month);
                data.categoryTotals =
                        expenseDAO.getCategoryTotals(month);
                data.budgets =
                        budgetDAO.getBudgetsForMonth(month);

                data.totalBudget =
                        budgetDAO.getTotalBudgetForMonth(month);

                data.budgetLeft = 0;

                for (String[] budget : data.budgets) {

                    double amount = Double.parseDouble(budget[2]);

                    double spent = budgetDAO.getSpentAmount(
                            budget[1],
                            month
                    );

                    data.budgetLeft += amount - spent;

                    data.expandedBudgets.add(new String[] {
                            budget[0],
                            budget[1],
                            budget[2],
                            budget[3],
                            String.valueOf(spent)
                    });
                }

                data.recommendations =
                        recommendationService.createRecommendations(
                                data.dailyTotals,
                                data.categoryTotals,
                                data.budgets,
                                data.monthlyTotal,
                                data.budgetLeft
                        );

                data.alerts =
                        recommendationService.createBudgetAlerts(
                                data.expandedBudgets
                        );

                return data;
            }
        };

        task.setOnSucceeded(event -> {

            DashboardData data = task.getValue();

            totalBudgetValueLabel.setText(
                    rupees(data.totalBudget)
            );

            monthlyExpensesValueLabel.setText(
                    rupees(data.monthlyTotal)
            );

            budgetLeftValueLabel.setText(
                    rupees(data.budgetLeft)
            );

            populateTransactionTable(
                    transactionTable,
                    data.expenses
            );

            populateDailyChart(
                    dailyChartBox,
                    data.dailyTotals
            );

            populateCategoryChart(
                    categoryChartBox,
                    data.categoryTotals
            );

            populateBudgetProgress(
                    progressBox,
                    data.expandedBudgets
            );

            populateTextList(alertsBox, data.alerts, true);
            populateTextList(timelineBox, data.recommendations, false);
        });

        task.setOnFailed(event -> {

            totalBudgetValueLabel.setText("Unavailable");
            monthlyExpensesValueLabel.setText("Unavailable");
            budgetLeftValueLabel.setText("Unavailable");

            timelineBox.getChildren().setAll(
                    new Label(
                            "Could not load Dashboard data. " +
                            "Check the database connection."
                    )
            );

            if (task.getException() != null) {
                task.getException().printStackTrace();
            }
        });

        Thread thread = new Thread(task, "spendly-dashboard-data");
        thread.setDaemon(true);
        thread.start();
    }

    private VBox createCard(
            String heading,
            String amount,
            String description,
            String color,
            String amountColor) {

        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setMinWidth(0);
        card.setPrefHeight(165);
        card.setStyle(cardStyle(color));

        Label headingLabel = new Label(heading);
        headingLabel.setStyle(
                "-fx-font-size: 16px;" +
                "-fx-font-weight: bold;"
        );

        Label amountLabel = new Label(amount);
        amountLabel.setWrapText(true);
        amountLabel.setStyle(
                "-fx-font-size: 24px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + amountColor + ";"
        );

        Label descriptionLabel = new Label(description);
        descriptionLabel.setWrapText(true);
        descriptionLabel.setStyle("-fx-font-size: 13px;");

        if ("Total Budget".equals(heading)) {
            totalBudgetValueLabel = amountLabel;
        } else if ("Monthly Expenses".equals(heading)) {
            monthlyExpensesValueLabel = amountLabel;
        } else if ("Budget Left".equals(heading)) {
            budgetLeftValueLabel = amountLabel;
        }

        card.getChildren().addAll(
                headingLabel,
                amountLabel,
                descriptionLabel
        );

        return card;
    }

    private String cardStyle(String color) {
        return "-fx-background-color: " + color + ";" +
                "-fx-border-color: #dddddd;" +
                "-fx-border-width: 1px;" +
                "-fx-background-radius: 8px;" +
                "-fx-border-radius: 8px;";
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.setStyle(
                "-fx-font-size: 23px;" +
                "-fx-font-weight: bold;"
        );
        return label;
    }

    private String rupees(double amount) {
        return String.format(
                java.util.Locale.US,
                "₹ %,.2f",
                amount
        );
    }

    private GridPane createTransactionTable() {

        GridPane table = new GridPane();
        table.setMaxWidth(Double.MAX_VALUE);
        table.setStyle(
                "-fx-background-color: white;" +
                "-fx-border-color: #bdbdbd;"
        );

        String[] headings = {
                "No.", "Description", "Category", "Amount"
        };

        double[] widths = {0.10, 0.35, 0.30, 0.25};

        for (int col = 0; col < headings.length; col++) {

            Label header = new Label(headings[col]);
            header.setMaxWidth(Double.MAX_VALUE);
            header.setPadding(new Insets(12));
            header.setStyle(
                    "-fx-background-color: #eeeeee;" +
                    "-fx-font-weight: bold;" +
                    "-fx-border-color: #bdbdbd;"
            );

            ColumnConstraints constraints = new ColumnConstraints();
            constraints.setPercentWidth(widths[col] * 100);
            constraints.setHgrow(Priority.ALWAYS);
            table.getColumnConstraints().add(constraints);

            table.add(header, col, 0);
        }

        return table;
    }

    private void populateTransactionTable(
            GridPane table,
            List<String[]> expenses) {

        table.getChildren().removeIf(node ->
                GridPane.getRowIndex(node) != null &&
                GridPane.getRowIndex(node) > 0
        );

        if (expenses == null || expenses.isEmpty()) {
            table.add(
                    new Label("No expenses recorded yet."),
                    0, 1, 4, 1
            );
            return;
        }

        int count = Math.min(expenses.size(), 10);

        for (int i = 0; i < count; i++) {

            String[] expense = expenses.get(i);

            String[] values = {
                    String.valueOf(i + 1),
                    expense[1],
                    expense[2],
                    "- " + rupees(Double.parseDouble(expense[3]))
            };

            for (int col = 0; col < values.length; col++) {

                Label cell = new Label(values[col]);
                cell.setMaxWidth(Double.MAX_VALUE);
                cell.setWrapText(true);
                cell.setPadding(new Insets(12));
                cell.setStyle(
                        "-fx-border-color: #dddddd;" +
                        "-fx-font-size: 13px;"
                );

                table.add(cell, col, i + 1);
            }
        }
    }

    private void populateDailyChart(
            VBox container,
            List<String[]> dailyTotals) {

        container.getChildren().removeIf(
                node -> !(node instanceof Label)
        );

        if (dailyTotals == null || dailyTotals.isEmpty()) {
            container.getChildren().add(
                    new Label("No daily spending data yet.")
            );
            return;
        }

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Day of month");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Amount (₹)");

        BarChart<String, Number> chart =
                new BarChart<>(xAxis, yAxis);

        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(300);

        javafx.scene.chart.XYChart.Series<String, Number> series =
                new javafx.scene.chart.XYChart.Series<>();

        for (String[] row : dailyTotals) {
            series.getData().add(
                    new javafx.scene.chart.XYChart.Data<>(
                            row[0].substring(8),
                            Double.parseDouble(row[1])
                    )
            );
        }

        chart.getData().add(series);
        container.getChildren().add(chart);
    }

    private void populateCategoryChart(
            VBox container,
            List<String[]> categoryTotals) {

        container.getChildren().removeIf(
                node -> !(node instanceof Label)
        );

        if (categoryTotals == null || categoryTotals.isEmpty()) {
            container.getChildren().add(
                    new Label("No category data yet.")
            );
            return;
        }

        PieChart chart = new PieChart();
        chart.setLabelsVisible(true);
        chart.setLegendVisible(true);
        chart.setPrefHeight(300);

        for (String[] row : categoryTotals) {
            chart.getData().add(
                    new PieChart.Data(
                            row[0],
                            Double.parseDouble(row[1])
                    )
            );
        }

        container.getChildren().add(chart);
    }

    private void populateBudgetProgress(
            VBox container,
            List<String[]> budgets) {

        container.getChildren().clear();

        if (budgets == null || budgets.isEmpty()) {
            container.getChildren().add(
                    new Label("No budgets set for this month.")
            );
            return;
        }

        for (String[] budget : budgets) {

            String category = budget[1];
            double amount = Double.parseDouble(budget[2]);
            double spent = Double.parseDouble(budget[4]);

            double progress = amount > 0
                    ? Math.min(spent / amount, 1.0)
                    : 0;

            Label label = new Label(
                    category + " — " + rupees(spent) +
                    " spent of " + rupees(amount)
            );
            label.setWrapText(true);

            ProgressBar bar = new ProgressBar(progress);
            bar.setMaxWidth(Double.MAX_VALUE);
            bar.setPrefHeight(16);

            Label remaining = new Label(
                    amount - spent >= 0
                            ? rupees(amount - spent) + " remaining"
                            : rupees(spent - amount) + " over budget"
            );

            container.getChildren().add(
                    new VBox(5, label, bar, remaining)
            );
        }
    }

    private void populateTextList(
            VBox container,
            List<String> messages,
            boolean alertMode) {

        container.getChildren().clear();

        if (messages == null || messages.isEmpty()) {
            container.getChildren().add(
                    new Label("Nothing to show right now.")
            );
            return;
        }

        for (String message : messages) {

            Label label = new Label(
                    (alertMode ? "• " : "◆ ") + message
            );

            label.setWrapText(true);
            label.setStyle(
                    "-fx-font-size: 14px;" +
                    "-fx-padding: 6px;"
            );

            container.getChildren().add(label);
        }
    }

    private static class DashboardData {

        List<String[]> expenses = new ArrayList<>();
        List<String[]> dailyTotals = new ArrayList<>();
        List<String[]> categoryTotals = new ArrayList<>();
        List<String[]> budgets = new ArrayList<>();
        List<String[]> expandedBudgets = new ArrayList<>();

        List<String> recommendations = new ArrayList<>();
        List<String> alerts = new ArrayList<>();

        double totalBudget;
        double monthlyTotal;
        double budgetLeft;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
