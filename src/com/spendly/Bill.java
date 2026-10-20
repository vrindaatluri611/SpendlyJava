
package com.spendly;

import java.time.LocalDate;

public class Bill {

    private int id;
    private String billName;
    private double totalAmount;
    private String splitType;
    private LocalDate billDate;

    public Bill(int id, String billName, double totalAmount,
                String splitType, LocalDate billDate) {
        this.id = id;
        this.billName = billName;
        this.totalAmount = totalAmount;
        this.splitType = splitType;
        this.billDate = billDate;
    }

    public Bill(String billName, double totalAmount,
                String splitType, LocalDate billDate) {
        this(0, billName, totalAmount, splitType, billDate);
    }

    public int getId() {
        return id;
    }

    public String getBillName() {
        return billName;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public String getSplitType() {
        return splitType;
    }

    public LocalDate getBillDate() {
        return billDate;
    }
}
