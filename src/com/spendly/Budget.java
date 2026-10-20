package com.spendly;

import java.time.LocalDate;

public class Budget {

    private int id;
    private String category;
    private double amount;
    private LocalDate month;

    public Budget(int id, String category,
                  double amount, LocalDate month) {
        this.id = id;
        this.category = category;
        this.amount = amount;
        this.month = month;
    }

    public Budget(String category,
                  double amount, LocalDate month) {
        this(0, category, amount, month);
    }

    public int getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDate getMonth() {
        return month;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setMonth(LocalDate month) {
        this.month = month;
    }
}