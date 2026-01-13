package org.example.core.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReportData {
    private final double totalIncome;
    private final double totalExpense;
    private final Map<String, Double> incomeByCategory;
    private final Map<String, Double> expenseByCategory;
    private final Map<String, BudgetStatus> budgets;
    private final List<String> warnings;

    public ReportData(double totalIncome,
                      double totalExpense,
                      Map<String, Double> incomeByCategory,
                      Map<String, Double> expenseByCategory,
                      Map<String, BudgetStatus> budgets,
                      List<String> warnings) {
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
        this.incomeByCategory = new LinkedHashMap<>(incomeByCategory);
        this.expenseByCategory = new LinkedHashMap<>(expenseByCategory);
        this.budgets = new LinkedHashMap<>(budgets);
        this.warnings = warnings;
    }

    public double getTotalIncome() {
        return totalIncome;
    }

    public double getTotalExpense() {
        return totalExpense;
    }

    public Map<String, Double> getIncomeByCategory() {
        return incomeByCategory;
    }

    public Map<String, Double> getExpenseByCategory() {
        return expenseByCategory;
    }

    public Map<String, BudgetStatus> getBudgets() {
        return budgets;
    }

    public List<String> getWarnings() {
        return warnings;
    }
}
