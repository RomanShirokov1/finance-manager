package org.example.cli;

import org.example.core.service.BudgetStatus;

import java.util.Map;

public class TableFormatter {
    public String formatTwoColumn(Map<String, Double> data, String headerLeft, String headerRight) {
        int leftWidth = headerLeft.length();
        for (String key : data.keySet()) {
            leftWidth = Math.max(leftWidth, key.length());
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-" + leftWidth + "s | %s%n", headerLeft, headerRight));
        sb.append("-".repeat(leftWidth)).append("-+-").append("-".repeat(headerRight.length())).append(System.lineSeparator());
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            sb.append(String.format("%-" + leftWidth + "s | %.2f%n", entry.getKey(), entry.getValue()));
        }
        return sb.toString();
    }

    public String formatBudgets(Map<String, BudgetStatus> data) {
        String headerLeft = "Категория";
        String headerLimit = "Лимит";
        String headerRemaining = "Остаток";
        int leftWidth = headerLeft.length();
        for (String key : data.keySet()) {
            leftWidth = Math.max(leftWidth, key.length());
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-" + leftWidth + "s | %10s | %10s%n", headerLeft, headerLimit, headerRemaining));
        sb.append("-".repeat(leftWidth)).append("-+-")
                .append("-".repeat(10)).append("-+-")
                .append("-".repeat(10)).append(System.lineSeparator());
        for (Map.Entry<String, BudgetStatus> entry : data.entrySet()) {
            BudgetStatus status = entry.getValue();
            sb.append(String.format("%-" + leftWidth + "s | %10.2f | %10.2f%n",
                    entry.getKey(), status.getLimit(), status.getRemaining()));
        }
        return sb.toString();
    }
}
