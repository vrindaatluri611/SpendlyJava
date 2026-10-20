package com.spendly;

import java.sql.Date;
import java.util.List;

public interface ExpenseOperations {

    boolean addExpense(
            String description,
            String category,
            double amount,
            Date expenseDate
    );

    List<String[]> getAllExpenses();

    boolean deleteExpense(int expenseId);

    List<String[]> getCategoryTotals(Date month);
}
