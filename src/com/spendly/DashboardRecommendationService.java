
package com.spendly;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class DashboardRecommendationService {

    public List<String> createRecommendations(
            List<String[]> dailyTotals,
            List<String[]> categoryTotals,
            List<String[]> budgets,
            double monthlyExpenses,
            double totalBudgetLeft) {

        List<String> recommendations = new ArrayList<>();
        LocalDate today = LocalDate.now();

        recommendations.add(
                today + " — Review your recorded expenses and check your remaining budgets."
        );

        if (categoryTotals != null && !categoryTotals.isEmpty()) {
            String category = categoryTotals.get(0)[0];
            double amount = Double.parseDouble(categoryTotals.get(0)[1]);

            recommendations.add(
                    "Top spending category this month: " + category +
                    " (" + rupees(amount) + "). Review whether upcoming purchases can wait."
            );
        } else {
            recommendations.add(
                    "No expenses are recorded for this month yet. Add expenses to unlock spending insights."
            );
        }

        if (dailyTotals != null && !dailyTotals.isEmpty()) {
            String[] latest = dailyTotals.get(dailyTotals.size() - 1);

            recommendations.add(
                    "Latest recorded spending date: " + latest[0] +
                    " — " + rupees(Double.parseDouble(latest[1])) + "."
            );
        }

        if (budgets != null && !budgets.isEmpty()) {
            recommendations.add(
                    "Month-end task: compare category spending against each budget before planning next month."
            );
        } else {
            recommendations.add(
                    "No budget is configured for this month. Set category budgets to receive budget-based guidance."
            );
        }

        if (YearMonth.from(today).lengthOfMonth() - today.getDayOfMonth() <= 5) {
            recommendations.add(
                    "Month-end is approaching. Review your expenses and prepare next month's budgets."
            );
        }

        if (totalBudgetLeft < 0) {
            recommendations.add(
                    "Budget alert: combined remaining budget is negative. Review overspent categories."
            );
        }

        return recommendations;
    }

    public List<String> createBudgetAlerts(List<String[]> budgets) {
        List<String> alerts = new ArrayList<>();

        if (budgets == null || budgets.isEmpty()) {
            alerts.add("No budgets set for this month.");
            return alerts;
        }

        for (String[] budget : budgets) {
            String category = budget[1];
            double amount = Double.parseDouble(budget[2]);
            double spent = Double.parseDouble(budget[4]);

            if (amount <= 0) {
                alerts.add(category + ": budget amount must be greater than zero.");
                continue;
            }

            double usage = spent / amount * 100.0;

            if (usage >= 100) {
                alerts.add(category + ": budget exceeded (" +
                        String.format("%.0f", usage) + "% used).");
            } else if (usage >= 80) {
                alerts.add(category + ": approaching budget limit (" +
                        String.format("%.0f", usage) + "% used).");
            }
        }

        if (alerts.isEmpty()) {
            alerts.add("No budget warnings right now. Keep tracking your expenses!");
        }

        return alerts;
    }

    private String rupees(double amount) {
        return String.format(java.util.Locale.US, "₹ %,.2f", amount);
    }
}
