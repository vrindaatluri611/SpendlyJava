
package com.spendly;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ExpenseDAO implements ExpenseOperations {

    public boolean addExpense(String description, String category,
                              double amount, Date expenseDate) {
        String sql = "INSERT INTO expenses " +
                "(description, category, amount, expense_date) VALUES (?, ?, ?, ?)";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, description);
            s.setString(2, category);
            s.setDouble(3, amount);
            s.setDate(4, expenseDate);
            s.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<String[]> getAllExpenses() {
        List<String[]> expenses = new ArrayList<>();
        String sql = "SELECT expense_id, description, category, amount, expense_date " +
                "FROM expenses ORDER BY expense_date DESC, expense_id DESC";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql);
             ResultSet r = s.executeQuery()) {
            while (r.next()) {
                expenses.add(new String[] {
                    String.valueOf(r.getInt("expense_id")),
                    r.getString("description"),
                    r.getString("category"),
                    r.getBigDecimal("amount").toString(),
                    r.getDate("expense_date").toString()
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return expenses;
    }

    public boolean deleteExpense(int expenseId) {
        String sql = "DELETE FROM expenses WHERE expense_id = ?";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, expenseId);
            return s.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<String[]> getCategoryTotals(Date month) {
        List<String[]> totals = new ArrayList<>();
        String sql = "SELECT category, SUM(amount) AS total FROM expenses " +
                "WHERE expense_date >= ? " +
                "AND expense_date < DATE_ADD(?, INTERVAL 1 MONTH) " +
                "GROUP BY category ORDER BY total DESC";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setDate(1, month);
            s.setDate(2, month);

            try (ResultSet r = s.executeQuery()) {
                while (r.next()) {
                    totals.add(new String[] {
                        r.getString("category"),
                        r.getBigDecimal("total").toString()
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return totals;
    }

    public double getMonthlyExpenseTotal(Date month) {
        String sql = "SELECT COALESCE(SUM(amount), 0) AS total FROM expenses " +
                "WHERE expense_date >= ? " +
                "AND expense_date < DATE_ADD(?, INTERVAL 1 MONTH)";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setDate(1, month);
            s.setDate(2, month);

            try (ResultSet r = s.executeQuery()) {
                if (r.next()) {
                    return r.getDouble("total");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // Daily totals for the selected month: {date, total}.
    public List<String[]> getDailyTotals(Date month) {
        List<String[]> totals = new ArrayList<>();
        String sql = "SELECT expense_date, SUM(amount) AS total FROM expenses " +
                "WHERE expense_date >= ? " +
                "AND expense_date < DATE_ADD(?, INTERVAL 1 MONTH) " +
                "GROUP BY expense_date ORDER BY expense_date";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setDate(1, month);
            s.setDate(2, month);

            try (ResultSet r = s.executeQuery()) {
                while (r.next()) {
                    totals.add(new String[] {
                        r.getDate("expense_date").toString(),
                        r.getBigDecimal("total").toString()
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return totals;
    }
}
