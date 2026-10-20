
package com.spendly;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class BillDAO {

    public boolean saveBill(
            Bill bill,
            List<BillParticipant> participants) {

        String billSql =
                "INSERT INTO shared_bills " +
                "(bill_name, total_amount, split_type, bill_date) " +
                "VALUES (?, ?, ?, ?)";

        String participantSql =
                "INSERT INTO bill_participants " +
                "(bill_id, participant_name, share_amount) " +
                "VALUES (?, ?, ?)";

        try (Connection connection =
                     DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                int billId;

                try (PreparedStatement billStatement =
                             connection.prepareStatement(
                                     billSql,
                                     Statement.RETURN_GENERATED_KEYS)) {

                    billStatement.setString(1, bill.getBillName());
                    billStatement.setDouble(2, bill.getTotalAmount());
                    billStatement.setString(3, bill.getSplitType());
                    billStatement.setDate(
                            4, Date.valueOf(bill.getBillDate()));

                    billStatement.executeUpdate();

                    try (ResultSet keys =
                                 billStatement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException(
                                    "Could not retrieve the bill ID.");
                        }

                        billId = keys.getInt(1);
                    }
                }

                try (PreparedStatement participantStatement =
                             connection.prepareStatement(participantSql)) {

                    for (BillParticipant participant : participants) {
                        participantStatement.setInt(1, billId);
                        participantStatement.setString(
                                2, participant.getParticipantName());
                        participantStatement.setDouble(
                                3, participant.getShareAmount());

                        participantStatement.addBatch();
                    }

                    participantStatement.executeBatch();
                }

                connection.commit();
                return true;

            } catch (SQLException | RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }

                System.err.println(
                        "Could not save bill: " + e.getMessage());
                return false;
            }

        } catch (SQLException e) {
            System.err.println(
                    "Database error while saving bill: "
                            + e.getMessage());
            return false;
        }
    }

    public List<Bill> getAllBills() {

        List<Bill> bills = new ArrayList<>();

        String sql =
                "SELECT bill_id, bill_name, total_amount, " +
                "split_type, bill_date " +
                "FROM shared_bills " +
                "ORDER BY bill_date DESC, bill_id DESC";

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                Bill bill = new Bill(
                        resultSet.getInt("bill_id"),
                        resultSet.getString("bill_name"),
                        resultSet.getDouble("total_amount"),
                        resultSet.getString("split_type"),
                        resultSet.getDate("bill_date").toLocalDate()
                );

                bills.add(bill);
            }

        } catch (SQLException e) {
            System.err.println(
                    "Could not retrieve bills: " + e.getMessage());
        }

        return bills;
    }

    public List<BillParticipant> getParticipantsByBillId(int billId) {

        List<BillParticipant> participants = new ArrayList<>();

        String sql =
                "SELECT participant_id, bill_id, participant_name, " +
                "share_amount FROM bill_participants " +
                "WHERE bill_id = ? ORDER BY participant_id";

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, billId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    BillParticipant participant = new BillParticipant(
                            resultSet.getInt("participant_id"),
                            resultSet.getInt("bill_id"),
                            resultSet.getString("participant_name"),
                            resultSet.getDouble("share_amount")
                    );

                    participants.add(participant);
                }
            }

        } catch (SQLException e) {
            System.err.println(
                    "Could not retrieve participants: "
                            + e.getMessage());
        }

        return participants;
    }

    // Delete a bill; MySQL cascades the deletion to its participants
    public boolean deleteBill(int billId) {

        String sql = "DELETE FROM shared_bills WHERE bill_id = ?";

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, billId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println(
                    "Could not delete bill: " + e.getMessage());
            return false;
        }
    }
}
