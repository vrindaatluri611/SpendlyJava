package com.spendly;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class AnalysisService extends Service<List<String[]>> {

    private final ExpenseOperations expenseOperations;

    private LocalDate selectedMonth;

    public AnalysisService(ExpenseOperations expenseOperations) {
        this.expenseOperations = expenseOperations;
    }

    public void setSelectedMonth(LocalDate selectedMonth) {
        this.selectedMonth = selectedMonth;
    }

    @Override
    protected Task<List<String[]>> createTask() {

        LocalDate month = selectedMonth;

        return new Task<List<String[]>>() {

            @Override
            protected List<String[]> call() throws Exception {

                if (month == null) {
                    throw new IllegalArgumentException(
                            "Please select a month."
                    );
                }

                Date sqlMonth = Date.valueOf(
                        month.withDayOfMonth(1)
                );

                return expenseOperations.getCategoryTotals(sqlMonth);
            }
        };
    }
}