package com.spendly;

import java.sql.Date;
import java.util.List;

public interface BudgetOperations {

    boolean addBudget(
            String category,
            double amount,
            Date budgetMonth
    );

    List<String[]> getAllBudgets();

    boolean deleteBudget(int budgetId);

    double getSpentAmount(
            String category,
            Date budgetMonth
    );

    List<String[]> getBudgetsForMonth(Date month);
}