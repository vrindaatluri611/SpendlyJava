package com.spendly;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class LendingBorrowingPage {

    private final LoanDAO loanDAO = new LoanDAO();

    private final TextField personField = new TextField();
    private final TextField amountField = new TextField();
    private final TextField descriptionField = new TextField();

    private final ComboBox<String> typeComboBox = new ComboBox<>();
    private final DatePicker datePicker = new DatePicker(LocalDate.now());

    private final VBox recordsBox = new VBox(10);
    private final Label statusLabel = new Label();

    private final VBox root = new VBox(18);

    public LendingBorrowingPage() {
        buildUI();
        loadLoans();
    }

    private void buildUI() {
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: #f7f9fc;");

        Label heading = new Label("Lending & Borrowing");
        heading.setStyle(
                "-fx-font-size: 26px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #243447;"
        );

        Label subtitle = new Label(
                "Keep track of money you've lent and borrowed."
        );
        subtitle.setWrapText(true);

        personField.setPromptText("Person's name");
        amountField.setPromptText("Amount in ₹");
        descriptionField.setPromptText("Description (optional)");

        typeComboBox.getItems().addAll("LENT", "BORROWED");
        typeComboBox.setValue("LENT");
        typeComboBox.setMaxWidth(Double.MAX_VALUE);

        datePicker.setMaxWidth(Double.MAX_VALUE);

        Button addButton = new Button("Add Record");
        addButton.setStyle(
                "-fx-background-color: #287a52;" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;"
        );
        addButton.setOnAction(e -> addLoan());

        VBox form = new VBox(10);
        form.setPadding(new Insets(18));
        form.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: #e1e6ed;" +
                "-fx-border-radius: 12;"
        );

        form.getChildren().addAll(
                new Label("Person's Name"),
                personField,
                new Label("Amount (₹)"),
                amountField,
                new Label("Record Type"),
                typeComboBox,
                new Label("Date"),
                datePicker,
                new Label("Description"),
                descriptionField,
                addButton
        );

        Label recordsHeading = new Label("Your Records");
        recordsHeading.setStyle(
                "-fx-font-size: 20px;" +
                "-fx-font-weight: bold;"
        );

        VBox recordsCard = new VBox(12);
        recordsCard.setPadding(new Insets(18));
        recordsCard.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: #e1e6ed;" +
                "-fx-border-radius: 12;"
        );

        recordsCard.getChildren().addAll(
                recordsHeading,
                statusLabel,
                recordsBox
        );

        root.getChildren().addAll(
                heading,
                subtitle,
                form,
                recordsCard
        );
    }

    private void addLoan() {
        String person = personField.getText().trim();
        String amountText = amountField.getText().trim();
        String type = typeComboBox.getValue();
        LocalDate date = datePicker.getValue();
        String description = descriptionField.getText().trim();

        if (person.isEmpty()) {
            showAlert(
                    Alert.AlertType.WARNING,
                    "Missing Name",
                    "Please enter the person's name."
            );
            return;
        }

        BigDecimal amount;

        try {
            amount = new BigDecimal(amountText)
                    .setScale(2, RoundingMode.UNNECESSARY);

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException | ArithmeticException ex) {
            showAlert(
                    Alert.AlertType.WARNING,
                    "Invalid Amount",
                    "Enter a positive amount with no more than two decimal places."
            );
            return;
        }

        if (type == null || date == null) {
            showAlert(
                    Alert.AlertType.WARNING,
                    "Missing Details",
                    "Please select a record type and date."
            );
            return;
        }

        Loan loan = new Loan(
                person,
                amount.doubleValue(),
                type,
                date,
                description.isEmpty() ? null : description
        );

        setFormDisabled(true);
        statusLabel.setText("Saving record...");

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return loanDAO.addLoan(loan);
            }
        };

        task.setOnSucceeded(e -> {
            setFormDisabled(false);

            if (Boolean.TRUE.equals(task.getValue())) {
                personField.clear();
                amountField.clear();
                descriptionField.clear();
                typeComboBox.setValue("LENT");
                datePicker.setValue(LocalDate.now());

                statusLabel.setText("Record saved.");
                loadLoans();
            } else {
                statusLabel.setText("Could not save the record.");
                showAlert(
                        Alert.AlertType.ERROR,
                        "Save Failed",
                        "The record could not be saved. Check the Console."
                );
            }
        });

        task.setOnFailed(e -> {
            setFormDisabled(false);
            statusLabel.setText("Could not save the record.");
            showAlert(
                    Alert.AlertType.ERROR,
                    "Save Failed",
                    "An unexpected error occurred while saving."
            );
        });

        Thread thread = new Thread(task, "save-loan-thread");
        thread.setDaemon(true);
        thread.start();
    }

    private void loadLoans() {
        statusLabel.setText("Loading records...");

        Task<List<Loan>> task = new Task<>() {
            @Override
            protected List<Loan> call() {
                return loanDAO.getAllLoans();
            }
        };

        task.setOnSucceeded(e -> {
            recordsBox.getChildren().clear();

            List<Loan> loans = task.getValue();

            if (loans == null || loans.isEmpty()) {
                statusLabel.setText("No records yet.");
                return;
            }

            statusLabel.setText(loans.size() + " record(s)");

            // Serial numbers are based on the current display order.
            for (int i = 0; i < loans.size(); i++) {
                Loan loan = loans.get(i);
                int serialNumber = i + 1;

                Label serialLabel = new Label(serialNumber + ".");
                serialLabel.setMinWidth(30);
                serialLabel.setStyle("-fx-font-weight: bold;");

                Label info = new Label(
                        loan.getPersonName()
                        + "\n" + loan.getLoanType()
                        + " | ₹" + formatAmount(loan.getAmount())
                        + "\nDate: " + loan.getLoanDate()
                        + (loan.getDescription() == null
                                || loan.getDescription().isBlank()
                                ? ""
                                : "\nNote: " + loan.getDescription())
                );
                info.setWrapText(true);
                info.setMinWidth(220);

                Label recordStatus = new Label(loan.getStatus());
                recordStatus.setStyle(getStatusStyle(loan.getStatus()));

                Button statusButton = new Button(
                        "PENDING".equals(loan.getStatus())
                                ? "Mark Settled"
                                : "Mark Pending"
                );

                statusButton.setOnAction(event -> {
                    String newStatus =
                            "PENDING".equals(loan.getStatus())
                                    ? "SETTLED"
                                    : "PENDING";

                    statusButton.setDisable(true);
                    statusLabel.setText("Updating record...");

                    Task<Boolean> updateTask = new Task<>() {
                        @Override
                        protected Boolean call() {
                            return loanDAO.updateLoanStatus(
                                    loan.getLoanId(),
                                    newStatus
                            );
                        }
                    };

                    updateTask.setOnSucceeded(updateEvent -> {
                        if (Boolean.TRUE.equals(updateTask.getValue())) {
                            loadLoans();
                        } else {
                            statusButton.setDisable(false);
                            statusLabel.setText(
                                    "Could not update status."
                            );
                            showAlert(
                                    Alert.AlertType.ERROR,
                                    "Update Failed",
                                    "The status could not be updated."
                            );
                        }
                    });

                    updateTask.setOnFailed(updateEvent -> {
                        statusButton.setDisable(false);
                        statusLabel.setText(
                                "Could not update status."
                        );
                        showAlert(
                                Alert.AlertType.ERROR,
                                "Update Failed",
                                "An unexpected error occurred."
                        );
                    });

                    Thread updateThread = new Thread(
                            updateTask, "update-loan-thread"
                    );
                    updateThread.setDaemon(true);
                    updateThread.start();
                });

                Button deleteButton = new Button("Delete");
                deleteButton.setStyle("-fx-text-fill: #b3261e;");

                deleteButton.setOnAction(event -> {
                    Alert confirm = new Alert(
                            Alert.AlertType.CONFIRMATION
                    );
                    confirm.setTitle("Delete Record");
                    confirm.setHeaderText(
                            "Delete record for " + loan.getPersonName() + "?"
                    );
                    confirm.setContentText(
                            "This action cannot be undone."
                    );

                    Optional<ButtonType> answer = confirm.showAndWait();

                    if (answer.isEmpty()
                            || answer.get() != ButtonType.OK) {
                        return;
                    }

                    deleteButton.setDisable(true);

                    Task<Boolean> deleteTask = new Task<>() {
                        @Override
                        protected Boolean call() {
                            return loanDAO.deleteLoan(loan.getLoanId());
                        }
                    };

                    deleteTask.setOnSucceeded(deleteEvent -> {
                        if (Boolean.TRUE.equals(deleteTask.getValue())) {
                            loadLoans();
                        } else {
                            deleteButton.setDisable(false);
                            showAlert(
                                    Alert.AlertType.ERROR,
                                    "Delete Failed",
                                    "The record could not be deleted."
                            );
                        }
                    });

                    deleteTask.setOnFailed(deleteEvent -> {
                        deleteButton.setDisable(false);
                        showAlert(
                                Alert.AlertType.ERROR,
                                "Delete Failed",
                                "An unexpected error occurred."
                        );
                    });

                    Thread deleteThread = new Thread(
                            deleteTask, "delete-loan-thread"
                    );
                    deleteThread.setDaemon(true);
                    deleteThread.start();
                });

                HBox actions = new HBox(8, statusButton, deleteButton);
                actions.setAlignment(Pos.CENTER_LEFT);

                VBox content = new VBox(
                        8,
                        info,
                        recordStatus,
                        actions
                );
                content.setPadding(new Insets(12));
                content.setStyle(
                        "-fx-border-color: #e1e6ed;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;"
                );

                HBox numberedRow = new HBox(10, serialLabel, content);
                numberedRow.setAlignment(Pos.CENTER_LEFT);
                recordsBox.getChildren().add(numberedRow);
            }
        });

        task.setOnFailed(e -> {
            recordsBox.getChildren().clear();
            statusLabel.setText(
                    "Could not load records. Check the Console."
            );
        });

        Thread thread = new Thread(task, "load-loans-thread");
        thread.setDaemon(true);
        thread.start();
    }

    private String getStatusStyle(String status) {
        if ("SETTLED".equals(status)) {
            return "-fx-background-color: #d9f2e3;" +
                   "-fx-text-fill: #187747;" +
                   "-fx-font-weight: bold;" +
                   "-fx-padding: 5 10 5 10;" +
                   "-fx-background-radius: 5;";
        }

        return "-fx-background-color: #fff0cc;" +
               "-fx-text-fill: #8a5a16;" +
               "-fx-font-weight: bold;" +
               "-fx-padding: 5 10 5 10;" +
               "-fx-background-radius: 5;";
    }

    private String formatAmount(double amount) {
        return BigDecimal.valueOf(amount)
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString();
    }

    private void setFormDisabled(boolean disabled) {
        personField.setDisable(disabled);
        amountField.setDisable(disabled);
        descriptionField.setDisable(disabled);
        typeComboBox.setDisable(disabled);
        datePicker.setDisable(disabled);
    }

    private void showAlert(
            Alert.AlertType type,
            String title,
            String message) {

        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public Parent getView() {
        return root;
    }
}
