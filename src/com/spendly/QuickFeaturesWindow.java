
package com.spendly;

import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class QuickFeaturesWindow {

    public static void show() {
        Stage stage = new Stage();
        stage.setTitle("Spendly - Bank Import & Parental Controls");

        Label status = new Label("Choose a feature.");
        ComboBox<String> categories = new ComboBox<>();
        CheckBox blocked = new CheckBox("Block spending in this category");
        TextField limit = new TextField();
        limit.setPromptText("Monthly limit (optional)");

        Button refresh = new Button("Refresh Categories");
        Button saveRule = new Button("Save Parental Rule");
        Button importCsv = new Button("Import Bank CSV");

        Runnable loadCategories = () -> {
            try (Connection c = DatabaseConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                     "SELECT category_name FROM categories ORDER BY category_name");
                 ResultSet rs = ps.executeQuery()) {

                List<String> list = new ArrayList<>();
                while (rs.next()) list.add(rs.getString(1));
                categories.setItems(FXCollections.observableArrayList(list));

            } catch (SQLException ex) {
                status.setText("Could not load categories: " + ex.getMessage());
            }
        };

        refresh.setOnAction(e -> loadCategories.run());

        saveRule.setOnAction(e -> {
            String category = categories.getValue();
            if (category == null) {
                status.setText("Select a category first.");
                return;
            }

            Double monthlyLimit = null;
            String entered = limit.getText().trim();

            if (!entered.isEmpty()) {
                try {
                    monthlyLimit = Double.parseDouble(entered);
                    if (!Double.isFinite(monthlyLimit) || monthlyLimit < 0) {
                        status.setText("Enter a valid non-negative limit.");
                        return;
                    }
                } catch (NumberFormatException ex) {
                    status.setText("Monthly limit must be a number.");
                    return;
                }
            }

            String sql = """
                INSERT INTO category_controls
                    (category, is_blocked, monthly_limit)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    is_blocked = VALUES(is_blocked),
                    monthly_limit = VALUES(monthly_limit)
                """;

            try (Connection c = DatabaseConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {

                ps.setString(1, category);
                ps.setBoolean(2, blocked.isSelected());

                if (monthlyLimit == null) ps.setNull(3, Types.DECIMAL);
                else ps.setDouble(3, monthlyLimit);

                ps.executeUpdate();
                status.setText("Parental rule saved for " + category + ".");

            } catch (SQLException ex) {
                status.setText("Could not save rule: " + ex.getMessage());
            }
        });

        importCsv.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Bank Statement CSV");
            chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV files", "*.csv"));

            File file = chooser.showOpenDialog(stage);
            if (file == null) return;

            int imported = 0;
            int duplicates = 0;
            int failed = 0;

            try (BufferedReader reader =
                     new BufferedReader(new FileReader(file, StandardCharsets.UTF_8));
                 Connection c = DatabaseConnection.getConnection()) {

                String header = reader.readLine();
                if (header == null) {
                    status.setText("The CSV file is empty.");
                    return;
                }

                List<String> columns = parseCsvLine(header);
                int dateCol = findColumn(columns, "date");
                int descCol = findColumn(columns, "description");
                int amountCol = findColumn(columns, "amount");
                int categoryCol = findColumn(columns, "category");

                if (dateCol < 0 || descCol < 0 || amountCol < 0) {
                    status.setText(
                        "CSV needs date, description and amount columns.");
                    return;
                }

                String existsSql =
                    "SELECT 1 FROM expenses WHERE import_hash = ?";
                String insertSql = """
                    INSERT INTO expenses
                        (description, category, amount, expense_date, import_hash)
                    VALUES (?, ?, ?, ?, ?)
                    """;

                String line;
                int lineNumber = 1;

                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    if (line.isBlank()) continue;

                    try {
                        List<String> row = parseCsvLine(line);
                        int max = Math.max(Math.max(dateCol, descCol),
                                           Math.max(amountCol, categoryCol));
                        if (row.size() <= max) {
                            failed++;
                            continue;
                        }

                        LocalDate date = LocalDate.parse(row.get(dateCol).trim());
                        String description = row.get(descCol).trim();
                        double amount = Double.parseDouble(
                            row.get(amountCol).trim());

                        String category = categoryCol >= 0
                            ? row.get(categoryCol).trim()
                            : "Other";

                        if (description.isBlank() ||
                            !Double.isFinite(amount) || amount <= 0) {
                            failed++;
                            continue;
                        }

                        if (category.isBlank()) category = "Other";

                        String hash = sha256(date + "|" + description + "|" +
                                             amount + "|" + category);

                        try (PreparedStatement check = c.prepareStatement(existsSql)) {
                            check.setString(1, hash);
                            try (ResultSet rs = check.executeQuery()) {
                                if (rs.next()) {
                                    duplicates++;
                                    continue;
                                }
                            }
                        }

                        try (PreparedStatement ps = c.prepareStatement(insertSql)) {
                            ps.setString(1, description);
                            ps.setString(2, category);
                            ps.setDouble(3, amount);
                            ps.setDate(4, Date.valueOf(date));
                            ps.setString(5, hash);
                            ps.executeUpdate();
                            imported++;
                        } catch (SQLException ex) {
                            // A parental rule or database constraint may reject this row.
                            failed++;
                        }

                    } catch (Exception ex) {
                        failed++;
                    }
                }

                status.setText("Import finished. Added: " + imported +
                    " | Duplicates: " + duplicates + " | Skipped/blocked: " + failed);

            } catch (Exception ex) {
                status.setText("Import failed: " + ex.getMessage());
            }
        });

        loadCategories.run();

        VBox root = new VBox(12,
            new Label("BANK STATEMENT IMPORT"),
            new Label("CSV columns: date, description, amount, category (optional)"),
            importCsv,
            new Separator(),
            new Label("PARENTAL SPENDING CONTROLS"),
            new Label("Select a category:"),
            categories,
            blocked,
            limit,
            saveRule,
            refresh,
            new Separator(),
            status
        );
        root.setStyle("-fx-padding: 20; -fx-font-size: 13px;");

        stage.setScene(new Scene(root, 540, 470));
        stage.show();
    }

    private static int findColumn(List<String> columns, String name) {
        for (int i = 0; i < columns.size(); i++) {
            String value = columns.get(i).trim()
                .replace("\uFEFF", "").replace("\"", "");
            if (value.equalsIgnoreCase(name)) return i;
        }
        return -1;
    }

    private static String sha256(String value) throws Exception {
        byte[] bytes = MessageDigest.getInstance("SHA-256")
            .digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) result.append(String.format("%02x", b));
        return result.toString();
    }

    // Supports quoted CSV fields containing commas and escaped quotes.
    private static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);

            if (ch == '"') {
                if (quoted && i + 1 < line.length()
                        && line.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                values.add(field.toString().trim());
                field.setLength(0);
            } else {
                field.append(ch);
            }
        }

        values.add(field.toString().trim());
        return values;
    }
}
