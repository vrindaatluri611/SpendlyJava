
package com.spendly;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class ParentalControlsPage {

    private final BorderPane root = new BorderPane();
    private final VBox categoryList = new VBox(0);

    private final Label monthlyLimitLabel = new Label("Loading...");
    private final Label spentLabel = new Label("Loading...");
    private final Label remainingLabel = new Label("Loading...");
    private final Label statusLabel = new Label();

    private final String green = "#126149";
    private final String muted = "#718096";
    private final String border = "#e5e9e5";

    public ParentalControlsPage() {
        buildPage();
        refreshPage();
    }

    public BorderPane getView() {
        // Reload saved values whenever navigation returns to this page.
        refreshPage();
        return root;
    }

    private void buildPage() {
        root.setStyle("-fx-background-color: #f8f7f2;");

        VBox page = new VBox(22);
        page.setPadding(new Insets(28));

        Label dateLabel = new Label(
            LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)
            )
        );
        dateLabel.setStyle(
            "-fx-text-fill: " + muted + "; -fx-font-size: 13px;"
        );

        Label pageTitle = new Label("Parent Control");
        pageTitle.setStyle(
            "-fx-font-size: 32px; -fx-font-weight: bold;" +
            "-fx-text-fill: #182d25;"
        );

        VBox headingText = new VBox(5);

        Label sectionTitle = new Label("Parent Control");
        sectionTitle.setStyle(
            "-fx-font-size: 25px; -fx-font-weight: bold;" +
            "-fx-text-fill: #182d25;"
        );

        Label subtitle = new Label(
            "Monitor spending and set category restrictions."
        );
        subtitle.setStyle(
            "-fx-font-size: 14px; -fx-text-fill: " + muted + ";"
        );

        headingText.getChildren().addAll(sectionTitle, subtitle);

        Region headingSpacer = new Region();
        HBox.setHgrow(headingSpacer, Priority.ALWAYS);

        Label activeBadge = new Label("● Monitoring active");
        activeBadge.setStyle(
            "-fx-background-color: #e4f4eb;" +
            "-fx-text-fill: " + green + ";" +
            "-fx-padding: 10 15 10 15;" +
            "-fx-background-radius: 22;"
        );

        HBox heading = new HBox(
            12, headingText, headingSpacer, activeBadge
        );
        heading.setAlignment(Pos.CENTER_LEFT);

        // Account summary card
        VBox accountCard = createCard();

        HBox accountHeader = new HBox(15);
        accountHeader.setAlignment(Pos.CENTER_LEFT);

        Label avatar = new Label("CH");
        avatar.setMinSize(58, 58);
        avatar.setAlignment(Pos.CENTER);
        avatar.setStyle(
            "-fx-background-color: #dfb84f;" +
            "-fx-text-fill: #24352b;" +
            "-fx-font-size: 20px; -fx-font-weight: bold;" +
            "-fx-background-radius: 40;"
        );

        VBox accountInfo = new VBox(5);

        Label accountName = new Label("Child's account");
        accountName.setStyle(
            "-fx-font-size: 18px; -fx-font-weight: bold;"
        );

        Label accountSubtitle = new Label(
            "Spendly · Category restrictions"
        );
        accountSubtitle.setStyle("-fx-text-fill: " + muted + ";");

        accountInfo.getChildren().addAll(accountName, accountSubtitle);
        accountHeader.getChildren().addAll(avatar, accountInfo);

        accountCard.getChildren().addAll(
            accountHeader,
            new Separator(),
            summaryRow("Monthly limit", monthlyLimitLabel),
            new Separator(),
            summaryRow("Spent this month", spentLabel),
            new Separator(),
            summaryRow("Remaining", remainingLabel)
        );

        // Category control card
        VBox controlsCard = createCard();

        Label controlsTitle = new Label("Category controls");
        controlsTitle.setStyle(
            "-fx-font-size: 19px; -fx-font-weight: bold;"
        );

        Label controlsSubtitle = new Label(
            "Green means allowed. Grey means blocked. Changes save immediately."
        );
        controlsSubtitle.setWrapText(true);
        controlsSubtitle.setStyle("-fx-text-fill: " + muted + ";");

        controlsCard.getChildren().addAll(
            controlsTitle,
            controlsSubtitle,
            new Separator(),
            categoryList
        );

        HBox content = new HBox(20, accountCard, controlsCard);
        HBox.setHgrow(accountCard, Priority.ALWAYS);
        HBox.setHgrow(controlsCard, Priority.ALWAYS);
        accountCard.setPrefWidth(520);
        controlsCard.setPrefWidth(320);

        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-text-fill: " + muted + ";");

        page.getChildren().addAll(
            dateLabel,
            pageTitle,
            heading,
            content,
            statusLabel
        );

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setStyle(
            "-fx-background: #f8f7f2;" +
            "-fx-background-color: #f8f7f2;"
        );

        root.setCenter(scroll);
    }

    private void refreshPage() {
        loadDashboard();
        loadCategoryControls();
    }

    private VBox createCard() {
        VBox card = new VBox(15);
        card.setPadding(new Insets(24));
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-border-color: " + border + ";" +
            "-fx-border-radius: 18px;" +
            "-fx-background-radius: 18px;"
        );
        return card;
    }

    private HBox summaryRow(String name, Label value) {
        Label label = new Label(name);
        label.setStyle(
            "-fx-text-fill: " + muted + "; -fx-font-size: 14px;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        value.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        HBox row = new HBox(10, label, spacer, value);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void loadDashboard() {
        LocalDate firstDay = LocalDate.now().withDayOfMonth(1);
        LocalDate nextMonth = firstDay.plusMonths(1);

        String expenseSql = """
            SELECT COALESCE(SUM(amount), 0)
            FROM expenses
            WHERE expense_date >= ? AND expense_date < ?
            """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(expenseSql)) {

            ps.setDate(1, java.sql.Date.valueOf(firstDay));
            ps.setDate(2, java.sql.Date.valueOf(nextMonth));

            double spent = 0;

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    spent = rs.getDouble(1);
                }
            }

            double limit = getMonthlyBudget();

            monthlyLimitLabel.setText(money(limit));
            spentLabel.setText(money(spent));
            remainingLabel.setText(money(limit - spent));

        } catch (SQLException ex) {
            monthlyLimitLabel.setText("Unavailable");
            spentLabel.setText("Unavailable");
            remainingLabel.setText("Unavailable");

            statusLabel.setText(
                "Could not load spending summary: " + ex.getMessage()
            );
            ex.printStackTrace();
        }
    }

    private double getMonthlyBudget() throws SQLException {
        LocalDate firstDay = LocalDate.now().withDayOfMonth(1);

        String sql = """
            SELECT COALESCE(SUM(amount), 0)
            FROM budgets
            WHERE budget_month = ?
            """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setDate(1, java.sql.Date.valueOf(firstDay));

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        }
    }

    private void loadCategoryControls() {
        categoryList.getChildren().clear();

        String sql = """
            SELECT category_name
            FROM categories
            ORDER BY category_name
            """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<String> categories = new ArrayList<>();

            while (rs.next()) {
                categories.add(rs.getString("category_name"));
            }

            for (int i = 0; i < categories.size(); i++) {
                String category = categories.get(i);

                // Always get the current value from MySQL.
                boolean blocked = isCategoryBlocked(category);

                Label categoryLabel = new Label(category);
                categoryLabel.setStyle("-fx-font-size: 14px;");

                ToggleButton toggle = new ToggleButton();
                toggle.setSelected(!blocked);
                styleToggle(toggle);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                HBox row = new HBox(
                    12, categoryLabel, spacer, toggle
                );
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(12, 0, 12, 0));

                toggle.setOnAction(event -> {
                    boolean previousAllowed = !toggle.isSelected();
                    boolean newBlocked = !toggle.isSelected();

                    toggle.setDisable(true);

                    boolean saved = saveBlockedState(
                        category, newBlocked
                    );

                    if (saved) {
                        statusLabel.setText(
                            category + (newBlocked
                                ? " is blocked and saved."
                                : " is allowed and saved.")
                        );
                    } else {
                        // Restore the old switch state if saving failed.
                        toggle.setSelected(previousAllowed);
                    }

                    toggle.setDisable(false);
                });

                categoryList.getChildren().add(row);

                if (i < categories.size() - 1) {
                    categoryList.getChildren().add(new Separator());
                }
            }

            if (categories.isEmpty()) {
                statusLabel.setText("No categories found in MySQL.");
            }

        } catch (SQLException ex) {
            statusLabel.setText(
                "Could not load categories: " + ex.getMessage()
            );
            ex.printStackTrace();
        }
    }

    private boolean isCategoryBlocked(String category)
            throws SQLException {

        String sql = """
            SELECT is_blocked
            FROM category_controls
            WHERE category = ?
            """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, category);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getBoolean("is_blocked");
            }
        }
    }

    private boolean saveBlockedState(
            String category,
            boolean blocked) {

        String sql = """
            INSERT INTO category_controls
                (category, is_blocked, monthly_limit)
            VALUES (?, ?, NULL)
            ON DUPLICATE KEY UPDATE
                is_blocked = VALUES(is_blocked)
            """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, category);
            ps.setBoolean(2, blocked);

            ps.executeUpdate();
            return true;

        } catch (SQLException ex) {
            statusLabel.setText(
                "Could not save " + category + ": " + ex.getMessage()
            );
            ex.printStackTrace();
            return false;
        }
    }

    private void styleToggle(ToggleButton toggle) {
        toggle.setText("");
        toggle.setMinSize(50, 27);
        toggle.setMaxSize(50, 27);

        Runnable updateStyle = () -> toggle.setStyle(
            "-fx-background-radius: 20px;" +
            "-fx-background-color: " +
            (toggle.isSelected() ? green : "#dfe5e1") + ";" +
            "-fx-border-radius: 20px;" +
            "-fx-border-color: transparent;"
        );

        updateStyle.run();

        toggle.selectedProperty().addListener(
            (observable, oldValue, newValue) -> updateStyle.run()
        );
    }

    private String money(double value) {
        return String.format(Locale.US, "₹%,.2f", value);
    }
}
