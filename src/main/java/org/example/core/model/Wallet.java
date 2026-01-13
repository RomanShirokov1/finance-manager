package org.example.core.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Wallet {
    private double balance;
    private List<Transaction> transactions = new ArrayList<>();
    private Map<String, Double> budgets = new HashMap<>();

    public Wallet() {
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public Map<String, Double> getBudgets() {
        return budgets;
    }
}
