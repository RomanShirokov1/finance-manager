package org.example.core.model;

public class Transaction {
    private String id;
    private TransactionType type;
    private String category;
    private double amount;
    private String description;
    private String date;
    private String counterparty;

    public Transaction() {
    }

    public Transaction(String id, TransactionType type, String category, double amount,
                       String description, String date, String counterparty) {
        this.id = id;
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.description = description;
        this.date = date;
        this.counterparty = counterparty;
    }

    public String getId() {
        return id;
    }

    public TransactionType getType() {
        return type;
    }

    public String getCategory() {
        return category;
    }

    public double getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public String getDate() {
        return date;
    }

    public String getCounterparty() {
        return counterparty;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
