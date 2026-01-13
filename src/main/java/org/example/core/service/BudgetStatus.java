package org.example.core.service;

public class BudgetStatus {
    private final double limit;
    private final double spent;
    private final double remaining;

    public BudgetStatus(double limit, double spent) {
        this.limit = limit;
        this.spent = spent;
        this.remaining = limit - spent;
    }

    public double getLimit() {
        return limit;
    }

    public double getSpent() {
        return spent;
    }

    public double getRemaining() {
        return remaining;
    }
}
