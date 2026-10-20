
package com.spendly;

public class BillParticipant {

    private int id;
    private int billId;
    private String participantName;
    private double shareAmount;

    public BillParticipant(int id, int billId,
                           String participantName,
                           double shareAmount) {
        this.id = id;
        this.billId = billId;
        this.participantName = participantName;
        this.shareAmount = shareAmount;
    }

    public BillParticipant(String participantName,
                           double shareAmount) {
        this(0, 0, participantName, shareAmount);
    }

    public int getId() {
        return id;
    }

    public int getBillId() {
        return billId;
    }

    public String getParticipantName() {
        return participantName;
    }

    public double getShareAmount() {
        return shareAmount;
    }
}
