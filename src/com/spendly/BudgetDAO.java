
package com.spendly;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BudgetDAO implements BudgetOperations {

    public boolean addBudget(String category, double amount, Date budgetMonth) {
        String sql = "INSERT INTO budgets (category, amount, budget_month) " +
                "VALUES (?, ?, ?)";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, category);
            s.setDouble(2, amount);
            s.setDate(3, budgetMonth);
            s.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<String[]> getAllBudgets() {
        List<String[]> budgets = new ArrayList<>();
        String sql = "SELECT budget_id, category, amount, budget_month " +
                "FROM budgets ORDER BY budget_month DESC";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql);
             ResultSet r = s.executeQuery()) {
            while (r.next()) {
                budgets.add(new String[] {
                    String.valueOf(r.getInt("budget_id")),
                    r.getString("category"),
                    r.getBigDecimal("amount").toString(),
                    r.getDate("budget_month").toString()
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return budgets;
    }

    public boolean deleteBudget(int budgetId) {
        String sql = "DELETE FROM budgets WHERE budget_id = ?";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, budgetId);
            return s.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public double getSpentAmount(String category, Date budgetMonth) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM expenses " +
                "WHERE category = ? AND expense_date >= ? " +
                "AND expense_date < DATE_ADD(?, INTERVAL 1 MONTH)";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, category);
            s.setDate(2, budgetMonth);
            s.setDate(3, budgetMonth);

            try (ResultSet r = s.executeQuery()) {
                if (r.next()) {
                    return r.getDouble(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<String[]> getBudgetsForMonth(Date month) {
        List<String[]> budgets = new ArrayList<>();
        String sql = "SELECT budget_id, category, amount, budget_month " +
                "FROM budgets WHERE budget_month = ? ORDER BY category";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setDate(1, month);

            try (ResultSet r = s.executeQuery()) {
                while (r.next()) {
                    budgets.add(new String[] {
                        String.valueOf(r.getInt("budget_id")),
                        r.getString("category"),
                        r.getBigDecimal("amount").toString(),
                        r.getDate("budget_month").toString()
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return budgets;
    }

    // Total budget assigned for the selected month.
    public double getTotalBudgetForMonth(Date month) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM budgets " +
                "WHERE budget_month = ?";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setDate(1, month);

            try (ResultSet r = s.executeQuery()) {
                if (r.next()) {
                    return r.getDouble(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
