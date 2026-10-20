
package com.spendly;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class BankStatementImportPage {

    private final VBox view = new VBox(15);
    private final Label status = new Label(
        "Choose a CSV bank statement to import."
    );

    public BankStatementImportPage() {
        view.setPadding(new Insets(25));
        view.setStyle("-fx-background-color: #f8f8f5;");

        Label title = new Label("Bank Statement Import");
        title.setStyle(
            "-fx-font-size: 28px; -fx-font-weight: bold;"
        );

        Label instructions = new Label(
            "Upload a CSV bank statement.\n\n" +
            "Required columns: date, description, amount\n" +
            "Optional column: category\n\n" +
            "Date format: yyyy-MM-dd\n" +
            "Example: 2026-10-10, College lunch, 150, Food & Dining\n\n" +
            "Common alternatives: Transaction Date, Narration, " +
            "Details, Debit, Withdrawal."
        );
        instructions.setWrapText(true);

        Button uploadButton = new Button("Choose CSV and Import");
        uploadButton.setPrefHeight(42);
        uploadButton.setOnAction(e -> chooseAndImport());

        status.setWrapText(true);

        view.getChildren().addAll(
            title,
            instructions,
            new Separator(),
            uploadButton,
            status
        );
    }

    public VBox getView() {
        return view;
    }

    private void chooseAndImport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Bank Statement CSV");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        Stage stage = (Stage) view.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);

        if (file == null) {
            return;
        }

        System.out.println("Selected file: " + file.getAbsolutePath());
        System.out.println("File size: " + file.length() + " bytes");

        if (!file.isFile() || !file.canRead() || file.length() == 0) {
            status.setText(
                "File is empty or cannot be read:\n" +
                file.getAbsolutePath()
            );
            return;
        }

        int imported = 0;
        int duplicates = 0;
        int rejected = 0;
        int lineNumber = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    Files.newInputStream(file.toPath()),
                    StandardCharsets.UTF_8));
             Connection connection = DatabaseConnection.getConnection()) {

            // Skip blank lines and any UTF-8 BOM before the header.
            String header;
            do {
                header = reader.readLine();
                lineNumber++;

                if (header == null) {
                    break;
                }

                header = header.replace("\uFEFF", "").trim();

            } while (header.isEmpty());

            System.out.println("CSV header read: [" + header + "]");

            if (header == null) {
                status.setText(
                    "No header was found in the file.\n" +
                    "File: " + file.getAbsolutePath()
                );
                return;
            }

            List<String> headings = parseCsv(header);
            System.out.println("Parsed headings: " + headings);

            int dateIndex = findColumn(headings, "date");
            int descriptionIndex = findColumn(headings, "description");
            int amountIndex = findColumn(headings, "amount");
            int categoryIndex = findColumn(headings, "category");

            if (dateIndex < 0 ||
                descriptionIndex < 0 ||
                amountIndex < 0) {

                status.setText(
                    "Could not recognize the required columns.\n\n" +
                    "Found headers: " + headings + "\n\n" +
                    "Required: date, description, amount.\n" +
                    "Check the Eclipse Console for details."
                );
                return;
            }

            String checkSql =
                "SELECT 1 FROM expenses WHERE import_hash = ?";

            String insertSql = """
                INSERT INTO expenses
                    (description, category, amount, expense_date, import_hash)
                VALUES (?, ?, ?, ?, ?)
                """;

            String line;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (line.isBlank()) {
                    continue;
                }

                try {
                    List<String> row = parseCsv(line);

                    int lastRequired = Math.max(
                        dateIndex,
                        Math.max(descriptionIndex, amountIndex)
                    );

                    if (row.size() <= lastRequired ||
                        (categoryIndex >= 0 &&
                         row.size() <= categoryIndex)) {
                        rejected++;
                        continue;
                    }

                    String dateText = clean(row.get(dateIndex));
                    String description = clean(row.get(descriptionIndex));
                    String amountText = clean(row.get(amountIndex));

                    if (description.isBlank()) {
                        rejected++;
                        continue;
                    }

                    amountText = amountText
                        .replace("₹", "")
                        .replace("INR", "")
                        .replace("inr", "")
                        .replace(" ", "")
                        .replace(",", "");

                    double amount = Double.parseDouble(amountText);

                    if (!Double.isFinite(amount) || amount <= 0) {
                        rejected++;
                        continue;
                    }

                    java.time.LocalDate parsedDate =
                        java.time.LocalDate.parse(dateText);

                    String category = categoryIndex >= 0
                        ? clean(row.get(categoryIndex))
                        : "Other";

                    if (category.isBlank()) {
                        category = "Other";
                    }

                    if (!categoryExists(connection, category)) {
                        rejected++;
                        System.out.println(
                            "Line " + lineNumber +
                            ": category not found: " + category
                        );
                        continue;
                    }

                    String hash = sha256(
                        parsedDate + "|" +
                        description.toLowerCase(Locale.ROOT) + "|" +
                        amount + "|" +
                        category.toLowerCase(Locale.ROOT)
                    );

                    boolean exists;

                    try (PreparedStatement check =
                             connection.prepareStatement(checkSql)) {
                        check.setString(1, hash);

                        try (ResultSet rs = check.executeQuery()) {
                            exists = rs.next();
                        }
                    }

                    if (exists) {
                        duplicates++;
                        continue;
                    }

                    try (PreparedStatement insert =
                             connection.prepareStatement(insertSql)) {

                        insert.setString(1, description);
                        insert.setString(2, category);
                        insert.setDouble(3, amount);
                        insert.setDate(4, Date.valueOf(parsedDate));
                        insert.setString(5, hash);

                        insert.executeUpdate();
                        imported++;

                    } catch (SQLException ex) {
                        rejected++;
                        System.err.println(
                            "Database rejected line " + lineNumber +
                            ": " + ex.getMessage()
                        );
                    }

                } catch (Exception ex) {
                    rejected++;
                    System.err.println(
                        "Could not process line " + lineNumber +
                        ": " + ex.getMessage()
                    );
                }
            }

            status.setText(
                "Import finished!\n\n" +
                "Imported: " + imported + "\n" +
                "Duplicates skipped: " + duplicates + "\n" +
                "Invalid or rejected rows: " + rejected
            );

        } catch (Exception ex) {
            status.setText(
                "Import failed: " + ex.getMessage()
            );
            ex.printStackTrace();
        }
    }

    private boolean categoryExists(
            Connection connection,
            String category) throws SQLException {

        String sql =
            "SELECT 1 FROM categories WHERE category_name = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, category);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private int findColumn(List<String> headings, String type) {
        String[][] aliases = {
            {
                "date", "transaction date", "trans date", "txn date",
                "value date", "transaction date", "transactiondate",
                "posting date"
            },
            {
                "description", "transaction description", "details",
                "narration", "particulars", "merchant",
                "transaction details", "remarks", "reference"
            },
            {
                "amount", "transaction amount", "debit", "withdrawal",
                "debit amount", "spent"
            },
            {
                "category", "expense category", "type"
            }
        };

        String[] types = {"date", "description", "amount", "category"};
        int typeIndex = -1;

        for (int i = 0; i < types.length; i++) {
            if (types[i].equalsIgnoreCase(type)) {
                typeIndex = i;
                break;
            }
        }

        if (typeIndex < 0) {
            return -1;
        }

        for (int i = 0; i < headings.size(); i++) {
            String heading = clean(headings.get(i))
                .replace("_", " ")
                .toLowerCase(Locale.ROOT);

            for (String alias : aliases[typeIndex]) {
                if (heading.equals(alias)) {
                    return i;
                }
            }
        }

        return -1;
    }

    private List<String> parseCsv(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;

        char delimiter = ',';

        if (line.contains(";") && !line.contains(",")) {
            delimiter = ';';
        } else if (line.contains("\t") && !line.contains(",")) {
            delimiter = '\t';
        }

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
            } else if (ch == delimiter && !quoted) {
                fields.add(field.toString().trim());
                field.setLength(0);
            } else {
                field.append(ch);
            }
        }

        fields.add(field.toString().trim());
        return fields;
    }

    private String clean(String value) {
        return value
            .replace("\uFEFF", "")
            .replace("\"", "")
            .trim();
    }

    private String sha256(String value) throws Exception {
        byte[] bytes = MessageDigest.getInstance("SHA-256")
            .digest(value.getBytes(StandardCharsets.UTF_8));

        StringBuilder result = new StringBuilder();

        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }

        return result.toString();
    }
}
