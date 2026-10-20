
package com.spendly;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class LoanDAO implements LoanOperations {

    @Override
    public boolean addLoan(Loan loan) {
        String sql = "INSERT INTO loans "
                + "(person_name, amount, loan_type, loan_date, "
                + "description, status) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, loan.getPersonName());
            statement.setDouble(2, loan.getAmount());
            statement.setString(3, loan.getLoanType());
            statement.setDate(4, Date.valueOf(loan.getLoanDate()));
            statement.setString(5, loan.getDescription());
            statement.setString(6, loan.getStatus());

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<Loan> getAllLoans() {
        List<Loan> loans = new ArrayList<>();

        String sql = "SELECT * FROM loans ORDER BY loan_date DESC, loan_id DESC";

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                Loan loan = new Loan(
                        resultSet.getInt("loan_id"),
                        resultSet.getString("person_name"),
                        resultSet.getDouble("amount"),
                        resultSet.getString("loan_type"),
                        resultSet.getDate("loan_date").toLocalDate(),
                        resultSet.getString("description"),
                        resultSet.getString("status")
                );

                loans.add(loan);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return loans;
    }

    @Override
    public boolean updateLoanStatus(int loanId, String status) {
        if (!"PENDING".equals(status) && !"SETTLED".equals(status)) {
            return false;
        }

        String sql = "UPDATE loans SET status = ? WHERE loan_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, status);
            statement.setInt(2, loanId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean deleteLoan(int loanId) {
        String sql = "DELETE FROM loans WHERE loan_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, loanId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
