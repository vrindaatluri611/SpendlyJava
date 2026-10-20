package com.spendly;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class BillSplittingPage {

    private final BillDAO billDAO = new BillDAO();

    private final TextField billNameField = new TextField();
    private final TextField totalAmountField = new TextField();
    private final DatePicker billDatePicker =
            new DatePicker(LocalDate.now());

    private final TextField participantNameField = new TextField();
    private final List<String> participantNames = new ArrayList<>();

    private final VBox participantListBox = new VBox(8);
    private final VBox savedBillsBox = new VBox(10);

    private final Label splitResultLabel = new Label();
    private final Label savedBillsStatus = new Label();

    private final VBox root = new VBox(18);

    private List<BigDecimal> calculatedShares = new ArrayList<>();

    public BillSplittingPage() {
        buildUI();
        loadSavedBills();
    }

    private void buildUI() {
        root.setPadding(new Insets(25));
        root.setSpacing(18);
        root.setStyle("-fx-background-color: #f7f9fc;");

        Label heading = new Label("Bill Splitting");
        heading.setStyle(
                "-fx-font-size: 26px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #243447;"
        );

        Label description = new Label(
                "Split shared expenses with friends and keep track of everyone's share."
        );
        description.setWrapText(true);

        billNameField.setPromptText("Bill name (e.g. Dinner)");
        totalAmountField.setPromptText("Total amount");
        billDatePicker.setMaxWidth(Double.MAX_VALUE);

        VBox billForm = new VBox(10);
        billForm.getChildren().addAll(
                new Label("Bill Name"),
                billNameField,
                new Label("Total Amount (₹)"),
                totalAmountField,
                new Label("Bill Date"),
                billDatePicker
        );

        Label participantHeading = new Label("Participants");
        participantHeading.setStyle(
                "-fx-font-size: 17px; -fx-font-weight: bold;"
        );

        participantNameField.setPromptText("Enter participant name");

        Button addParticipantButton = new Button("Add Participant");
        addParticipantButton.setOnAction(e -> addParticipant());

        HBox participantInputRow = new HBox(
                10, participantNameField, addParticipantButton
        );
        participantInputRow.setAlignment(Pos.CENTER_LEFT);

        participantListBox.setPadding(new Insets(5));

        Button calculateButton = new Button("Calculate Split");
        calculateButton.setStyle(
                "-fx-background-color: #287a52;" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;"
        );
        calculateButton.setOnAction(e -> calculateSplit());

        splitResultLabel.setWrapText(true);
        splitResultLabel.setStyle(
                "-fx-font-size: 14px;" +
                "-fx-text-fill: #243447;"
        );

        Button saveButton = new Button("Save Bill");
        saveButton.setStyle(
                "-fx-background-color: #243447;" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;"
        );
        saveButton.setOnAction(e -> saveBill());

        VBox formCard = new VBox(12);
        formCard.setPadding(new Insets(20));
        formCard.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: #e1e6ed;" +
                "-fx-border-radius: 12;"
        );

        formCard.getChildren().addAll(
                billForm,
                participantHeading,
                participantInputRow,
                participantListBox,
                calculateButton,
                splitResultLabel,
                saveButton
        );

        Label savedHeading = new Label("Saved Bills");
        savedHeading.setStyle(
                "-fx-font-size: 20px; -fx-font-weight: bold;"
        );

        savedBillsStatus.setWrapText(true);

        VBox savedBillsCard = new VBox(12);
        savedBillsCard.setPadding(new Insets(20));
        savedBillsCard.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: #e1e6ed;" +
                "-fx-border-radius: 12;"
        );

        savedBillsCard.getChildren().addAll(
                savedHeading,
                savedBillsStatus,
                savedBillsBox
        );

        root.getChildren().addAll(
                heading,
                description,
                formCard,
                savedBillsCard
        );
    }

    private void addParticipant() {
        String name = participantNameField.getText().trim();

        if (name.isEmpty()) {
            showAlert(
                    Alert.AlertType.WARNING,
                    "Missing Name",
                    "Please enter a participant name."
            );
            return;
        }

        for (String existingName : participantNames) {
            if (existingName.equalsIgnoreCase(name)) {
                showAlert(
                        Alert.AlertType.WARNING,
                        "Duplicate Participant",
                        "That participant has already been added."
                );
                return;
            }
        }

        participantNames.add(name);
        participantNameField.clear();

        calculatedShares.clear();
        splitResultLabel.setText("");

        refreshParticipantList();
    }

    private void refreshParticipantList() {
        participantListBox.getChildren().clear();

        for (int i = 0; i < participantNames.size(); i++) {
            String name = participantNames.get(i);
            final int index = i;

            Label nameLabel = new Label((i + 1) + ". " + name);

            Button removeButton = new Button("Remove");
            removeButton.setOnAction(e -> {
                participantNames.remove(index);
                calculatedShares.clear();
                splitResultLabel.setText("");
                refreshParticipantList();
            });

            HBox row = new HBox(10, nameLabel, removeButton);
            row.setAlignment(Pos.CENTER_LEFT);

            participantListBox.getChildren().add(row);
        }
    }

    private BigDecimal readValidTotal() {
        try {
            BigDecimal total = new BigDecimal(
                    totalAmountField.getText().trim()
            ).setScale(2, RoundingMode.HALF_UP);

            if (total.compareTo(BigDecimal.ZERO) <= 0) {
                return null;
            }

            return total;
        } catch (NumberFormatException | NullPointerException ex) {
            return null;
        }
    }

    private List<BigDecimal> calculateShares(
            BigDecimal total, int count) {

        List<BigDecimal> shares = new ArrayList<>();

        long totalPaise = total
                .movePointRight(2)
                .longValueExact();

        long basePaise = totalPaise / count;
        long remainderPaise = totalPaise % count;

        for (int i = 0; i < count; i++) {
            long sharePaise = basePaise;

            if (i < remainderPaise) {
                sharePaise++;
            }

            shares.add(
                    BigDecimal.valueOf(sharePaise, 2)
                            .setScale(2, RoundingMode.UNNECESSARY)
            );
        }

        return shares;
    }

    private void calculateSplit() {
        BigDecimal total = readValidTotal();

        if (total == null) {
            showAlert(
                    Alert.AlertType.WARNING,
                    "Invalid Amount",
                    "Enter a valid amount greater than zero."
            );
            return;
        }

        if (participantNames.isEmpty()) {
            showAlert(
                    Alert.AlertType.WARNING,
                    "No Participants",
                    "Add at least one participant before calculating."
            );
            return;
        }

        try {
            calculatedShares = calculateShares(
                    total, participantNames.size()
            );
            showShares();
        } catch (ArithmeticException ex) {
            showAlert(
                    Alert.AlertType.WARNING,
                    "Invalid Amount",
                    "Please enter a valid amount with at most two decimal places."
            );
        }
    }

    private void showShares() {
        StringBuilder result = new StringBuilder();
        result.append("Split breakdown:\n");

        for (int i = 0; i < participantNames.size(); i++) {
            result.append(participantNames.get(i))
                    .append(": ₹")
                    .append(calculatedShares.get(i).toPlainString())
                    .append("\n");
        }

        splitResultLabel.setText(result.toString());
    }

    private void saveBill() {
        String billName = billNameField.getText().trim();
        BigDecimal total = readValidTotal();
        LocalDate billDate = billDatePicker.getValue();

        if (billName.isEmpty()) {
            showAlert(
                    Alert.AlertType.WARNING,
                    "Missing Bill Name",
                    "Please enter a bill name."
            );
            return;
        }

        if (total == null) {
            showAlert(
                    Alert.AlertType.WARNING,
                    "Invalid Amount",
                    "Enter a valid amount greater than zero."
            );
            return;
        }

        if (billDate == null) {
            showAlert(
                    Alert.AlertType.WARNING,
                    "Missing Date",
                    "Please select the bill date."
            );
            return;
        }

        if (participantNames.isEmpty()) {
            showAlert(
                    Alert.AlertType.WARNING,
                    "No Participants",
                    "Add at least one participant."
            );
            return;
        }

        if (calculatedShares.size() != participantNames.size()) {
            calculatedShares = calculateShares(
                    total, participantNames.size()
            );
            showShares();
        }

        List<String> namesSnapshot =
                new ArrayList<>(participantNames);
        List<BigDecimal> sharesSnapshot =
                new ArrayList<>(calculatedShares);

        Bill bill = new Bill(
                billName,
                total.doubleValue(),
                "EQUAL",
                billDate
        );

        List<BillParticipant> participants = new ArrayList<>();

        for (int i = 0; i < namesSnapshot.size(); i++) {
            participants.add(
                    new BillParticipant(
                            namesSnapshot.get(i),
                            sharesSnapshot.get(i).doubleValue()
                    )
            );
        }

        Task<Boolean> saveTask = new Task<>() {
            @Override
            protected Boolean call() {
                return billDAO.saveBill(bill, participants);
            }
        };

        savedBillsStatus.setText("Saving bill...");
        root.setDisable(true);

        saveTask.setOnSucceeded(e -> {
            root.setDisable(false);

            if (Boolean.TRUE.equals(saveTask.getValue())) {
                showAlert(
                        Alert.AlertType.INFORMATION,
                        "Bill Saved",
                        "Your bill was saved successfully."
                );

                clearForm();
                loadSavedBills();
            } else {
                savedBillsStatus.setText("Could not save the bill.");
                showAlert(
                        Alert.AlertType.ERROR,
                        "Save Failed",
                        "The bill could not be saved. Check the Console and database connection."
                );
            }
        });

        saveTask.setOnFailed(e -> {
            root.setDisable(false);
            savedBillsStatus.setText("Could not save the bill.");

            showAlert(
                    Alert.AlertType.ERROR,
                    "Save Failed",
                    "An unexpected error occurred while saving the bill."
            );
        });

        Thread saveThread =
                new Thread(saveTask, "save-bill-thread");
        saveThread.setDaemon(true);
        saveThread.start();
    }

    private void clearForm() {
        billNameField.clear();
        totalAmountField.clear();
        billDatePicker.setValue(LocalDate.now());

        participantNames.clear();
        calculatedShares.clear();

        participantNameField.clear();
        participantListBox.getChildren().clear();
        splitResultLabel.setText("");
    }

    private void loadSavedBills() {
        savedBillsStatus.setText("Loading saved bills...");

        Task<List<Bill>> loadTask = new Task<>() {
            @Override
            protected List<Bill> call() {
                return billDAO.getAllBills();
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<Bill> bills = loadTask.getValue();

            savedBillsBox.getChildren().clear();

            if (bills == null || bills.isEmpty()) {
                savedBillsStatus.setText("No saved bills yet.");
                return;
            }

            savedBillsStatus.setText(bills.size() + " saved bill(s)");

            // Number rows by their current display position, not bill ID.
            for (int i = 0; i < bills.size(); i++) {
                Bill bill = bills.get(i);
                int serialNumber = i + 1;

                Label serialLabel = new Label(serialNumber + ".");
                serialLabel.setMinWidth(30);
                serialLabel.setStyle("-fx-font-weight: bold;");

                Label info = new Label(
                        bill.getBillName()
                        + " | " + bill.getBillDate()
                        + " | ₹"
                        + BigDecimal.valueOf(bill.getTotalAmount())
                                .setScale(2, RoundingMode.HALF_UP)
                );
                info.setWrapText(true);

                Button detailsButton = new Button("View Details");
                detailsButton.setStyle(
                        "-fx-background-color: #e8f3e9;" +
                        "-fx-text-fill: #287a52;"
                );

                detailsButton.setOnAction(detailsEvent -> {
                    detailsButton.setDisable(true);

                    Task<List<BillParticipant>> detailsTask =
                            new Task<>() {
                        @Override
                        protected List<BillParticipant> call() {
                            return billDAO.getParticipantsByBillId(
                                    bill.getId()
                            );
                        }
                    };

                    detailsTask.setOnSucceeded(detailsResult -> {
                        detailsButton.setDisable(false);

                        List<BillParticipant> participants =
                                detailsTask.getValue();

                        if (participants == null || participants.isEmpty()) {
                            showAlert(
                                    Alert.AlertType.INFORMATION,
                                    "Bill Details",
                                    "No participants were found for this bill."
                            );
                            return;
                        }

                        StringBuilder details = new StringBuilder();

                        for (BillParticipant participant : participants) {
                            details.append(
                                    participant.getParticipantName()
                            ).append(" — ₹")
                             .append(
                                    BigDecimal.valueOf(
                                            participant.getShareAmount()
                                    ).setScale(
                                            2, RoundingMode.HALF_UP
                                    ).toPlainString()
                             ).append("\n");
                        }

                        Alert alert = new Alert(
                                Alert.AlertType.INFORMATION
                        );
                        alert.setTitle("Bill Details");
                        alert.setHeaderText(
                                bill.getBillName()
                                + " — Total: ₹"
                                + BigDecimal.valueOf(
                                        bill.getTotalAmount()
                                ).setScale(
                                        2, RoundingMode.HALF_UP
                                ).toPlainString()
                        );
                        alert.setContentText(details.toString());
                        alert.showAndWait();
                    });

                    detailsTask.setOnFailed(detailsResult -> {
                        detailsButton.setDisable(false);

                        showAlert(
                                Alert.AlertType.ERROR,
                                "Loading Failed",
                                "Could not load participant details. Check the Console."
                        );
                    });

                    Thread detailsThread = new Thread(
                            detailsTask, "bill-details-thread"
                    );
                    detailsThread.setDaemon(true);
                    detailsThread.start();
                });

                Button deleteButton = new Button("Delete");
                deleteButton.setStyle(
                        "-fx-background-color: #fbe9e7;" +
                        "-fx-text-fill: #b3261e;"
                );

                deleteButton.setOnAction(deleteEvent -> {
                    Alert confirm = new Alert(
                            Alert.AlertType.CONFIRMATION
                    );
                    confirm.setTitle("Delete Bill");
                    confirm.setHeaderText(
                            "Delete \"" + bill.getBillName() + "\"?"
                    );
                    confirm.setContentText(
                            "This will also delete its participant records."
                    );

                    java.util.Optional<ButtonType> answer =
                            confirm.showAndWait();

                    if (answer.isEmpty()
                            || answer.get() != ButtonType.OK) {
                        return;
                    }

                    deleteButton.setDisable(true);

                    Task<Boolean> deleteTask = new Task<>() {
                        @Override
                        protected Boolean call() {
                            return billDAO.deleteBill(bill.getId());
                        }
                    };

                    deleteTask.setOnSucceeded(deleteResult -> {
                        if (Boolean.TRUE.equals(deleteTask.getValue())) {
                            loadSavedBills();
                        } else {
                            deleteButton.setDisable(false);
                            showAlert(
                                    Alert.AlertType.ERROR,
                                    "Delete Failed",
                                    "The bill could not be deleted. Check the Console."
                            );
                        }
                    });

                    deleteTask.setOnFailed(deleteResult -> {
                        deleteButton.setDisable(false);
                        showAlert(
                                Alert.AlertType.ERROR,
                                "Delete Failed",
                                "An unexpected error occurred while deleting the bill."
                        );
                    });

                    Thread deleteThread = new Thread(
                            deleteTask, "delete-bill-thread"
                    );
                    deleteThread.setDaemon(true);
                    deleteThread.start();
                });

                HBox row = new HBox(
                        12, serialLabel, info, detailsButton, deleteButton
                );
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(8));
                row.setStyle(
                        "-fx-border-color: #e1e6ed;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;"
                );

                savedBillsBox.getChildren().add(row);
            }
        });

        loadTask.setOnFailed(e -> {
            savedBillsStatus.setText("Could not load saved bills.");
            showAlert(
                    Alert.AlertType.ERROR,
                    "Loading Failed",
                    "Could not load saved bills. Check the Console."
            );
        });

        Thread loadThread =
                new Thread(loadTask, "load-bills-thread");
        loadThread.setDaemon(true);
        loadThread.start();
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
