
package com.spendly;

import java.time.LocalDate;

public class Loan {

    private int loanId;
    private String personName;
    private double amount;
    private String loanType;
    private LocalDate loanDate;
    private String description;
    private String status;

    public Loan(int loanId, String personName, double amount,
                String loanType, LocalDate loanDate,
                String description, String status) {
        this.loanId = loanId;
        this.personName = personName;
        this.amount = amount;
        this.loanType = loanType;
        this.loanDate = loanDate;
        this.description = description;
        this.status = status;
    }

    public Loan(String personName, double amount,
                String loanType, LocalDate loanDate,
                String description) {
        this(0, personName, amount, loanType, loanDate,
                description, "PENDING");
    }

    public int getLoanId() {
        return loanId;
    }

    public String getPersonName() {
        return personName;
    }

    public double getAmount() {
        return amount;
    }

    public String getLoanType() {
        return loanType;
    }

    public LocalDate getLoanDate() {
        return loanDate;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }
}
