package org.example.core.service;

import org.example.core.model.Transaction;
import org.example.core.model.TransactionType;
import org.example.core.model.User;
import org.example.core.model.Wallet;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class FinanceService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    public ServiceResult<Transaction> addIncome(User user, String category, double amount,
                                                String date, String description) {
        if (amount <= 0) {
            return ServiceResult.fail("Сумма должна быть больше нуля.");
        }
        String normalizedCategory = resolveCategory(user, category);
        if (normalizedCategory == null) {
            return ServiceResult.fail("Категория не может быть пустой.");
        }
        String resolvedDate = resolveDate(date);
        if (resolvedDate == null) {
            return ServiceResult.fail("Некорректная дата. Формат: ГГГГ-ММ-ДД.");
        }
        Transaction tx = new Transaction(UUID.randomUUID().toString(), TransactionType.INCOME,
                normalizedCategory, amount, safeText(description), resolvedDate, null);
        Wallet wallet = user.getWallet();
        wallet.getTransactions().add(tx);
        wallet.setBalance(wallet.getBalance() + amount);
        return ServiceResult.ok(tx, buildNotifications(user));
    }

    public ServiceResult<Transaction> addExpense(User user, String category, double amount,
                                                 String date, String description) {
        if (amount <= 0) {
            return ServiceResult.fail("Сумма должна быть больше нуля.");
        }
        String normalizedCategory = resolveCategory(user, category);
        if (normalizedCategory == null) {
            return ServiceResult.fail("Категория не может быть пустой.");
        }
        String resolvedDate = resolveDate(date);
        if (resolvedDate == null) {
            return ServiceResult.fail("Некорректная дата. Формат: ГГГГ-ММ-ДД.");
        }
        Wallet wallet = user.getWallet();
        if (wallet.getBalance() - amount < 0) {
            return ServiceResult.fail("Недостаточно средств. Баланс не может быть ниже 0.");
        }
        Transaction tx = new Transaction(UUID.randomUUID().toString(), TransactionType.EXPENSE,
                normalizedCategory, amount, safeText(description), resolvedDate, null);
        wallet.getTransactions().add(tx);
        wallet.setBalance(wallet.getBalance() - amount);
        return ServiceResult.ok(tx, buildNotifications(user));
    }

    public ServiceResult<Void> transfer(User sender, User receiver, double amount,
                                        String date, String description) {
        if (sender == null || receiver == null) {
            return ServiceResult.fail("Отправитель или получатель не найден.");
        }
        if (amount <= 0) {
            return ServiceResult.fail("Сумма должна быть больше нуля.");
        }
        String resolvedDate = resolveDate(date);
        if (resolvedDate == null) {
            return ServiceResult.fail("Некорректная дата. Формат: ГГГГ-ММ-ДД.");
        }
        Wallet senderWallet = sender.getWallet();
        if (senderWallet.getBalance() - amount < 0) {
            return ServiceResult.fail("Недостаточно средств. Перевод отменен.");
        }
        Wallet receiverWallet = receiver.getWallet();
        Transaction outTx = new Transaction(UUID.randomUUID().toString(), TransactionType.TRANSFER_OUT,
                "Перевод", amount, safeText(description), resolvedDate, receiver.getLogin());
        Transaction inTx = new Transaction(UUID.randomUUID().toString(), TransactionType.TRANSFER_IN,
                "Перевод", amount, safeText(description), resolvedDate, sender.getLogin());
        senderWallet.getTransactions().add(outTx);
        receiverWallet.getTransactions().add(inTx);
        senderWallet.setBalance(senderWallet.getBalance() - amount);
        receiverWallet.setBalance(receiverWallet.getBalance() + amount);
        return ServiceResult.ok(null, buildNotifications(sender));
    }

    public ServiceResult<Void> setBudget(User user, String category, double limit) {
        if (limit < 0) {
            return ServiceResult.fail("Лимит бюджета не может быть отрицательным.");
        }
        String normalizedCategory = resolveCategory(user, category);
        if (normalizedCategory == null) {
            return ServiceResult.fail("Категория не может быть пустой.");
        }
        user.getWallet().getBudgets().put(normalizedCategory, limit);
        return ServiceResult.ok(null, "Бюджет сохранен.");
    }

    public ServiceResult<Void> updateBudget(User user, String category, double limit) {
        if (limit < 0) {
            return ServiceResult.fail("Лимит бюджета не может быть отрицательным.");
        }
        String normalizedCategory = resolveCategory(user, category);
        if (normalizedCategory == null) {
            return ServiceResult.fail("Категория не может быть пустой.");
        }
        if (!user.getWallet().getBudgets().containsKey(normalizedCategory)) {
            return ServiceResult.fail("Бюджет по этой категории не найден.");
        }
        user.getWallet().getBudgets().put(normalizedCategory, limit);
        return ServiceResult.ok(null, "Бюджет обновлен.");
    }

    public ServiceResult<Void> removeBudget(User user, String category) {
        String normalizedCategory = resolveCategory(user, category);
        if (normalizedCategory == null) {
            return ServiceResult.fail("Категория не может быть пустой.");
        }
        Double removed = user.getWallet().getBudgets().remove(normalizedCategory);
        if (removed == null) {
            return ServiceResult.fail("Бюджет по этой категории не найден.");
        }
        return ServiceResult.ok(null, "Бюджет удален.");
    }

    public ServiceResult<Void> renameCategory(User user, String oldName, String newName) {
        String from = resolveCategory(user, oldName);
        String to = normalizeCategory(newName);
        if (from == null || to == null) {
            return ServiceResult.fail("Категории не могут быть пустыми.");
        }
        Wallet wallet = user.getWallet();
        boolean found = false;
        for (Transaction tx : wallet.getTransactions()) {
            if (from.equalsIgnoreCase(tx.getCategory())) {
                tx.setCategory(to);
                found = true;
            }
        }
        if (wallet.getBudgets().containsKey(from)) {
            double limit = wallet.getBudgets().remove(from);
            wallet.getBudgets().put(to, limit);
            found = true;
        }
        if (!found) {
            return ServiceResult.fail("Категория не найдена.");
        }
        return ServiceResult.ok(null, "Категория обновлена.");
    }

    public ServiceResult<Void> removeCategory(User user, String category) {
        String normalized = resolveCategory(user, category);
        if (normalized == null) {
            return ServiceResult.fail("Категория не может быть пустой.");
        }
        Wallet wallet = user.getWallet();
        boolean removed = false;
        List<Transaction> remaining = new ArrayList<>();
        for (Transaction tx : wallet.getTransactions()) {
            if (normalized.equalsIgnoreCase(tx.getCategory())) {
                removed = true;
            } else {
                remaining.add(tx);
            }
        }
        if (wallet.getBudgets().remove(normalized) != null) {
            removed = true;
        }
        if (!removed) {
            return ServiceResult.fail("Категория не найдена.");
        }
        wallet.getTransactions().clear();
        wallet.getTransactions().addAll(remaining);
        wallet.setBalance(recalculateBalance(wallet));
        return ServiceResult.ok(null, "Категория удалена.");
    }

    public ReportData buildReport(User user, List<String> categories, String fromDate, String toDate,
                                  List<String> warningsCollector, List<String> missingCategories) {
        Wallet wallet = user.getWallet();
        List<Transaction> filtered = filterTransactions(wallet, categories, fromDate, toDate, missingCategories);
        Map<String, Double> incomeByCategory = sumByCategory(filtered, TransactionType.INCOME, TransactionType.TRANSFER_IN);
        Map<String, Double> expenseByCategory = sumByCategory(filtered, TransactionType.EXPENSE, TransactionType.TRANSFER_OUT);
        double totalIncome = incomeByCategory.values().stream().mapToDouble(Double::doubleValue).sum();
        double totalExpense = expenseByCategory.values().stream().mapToDouble(Double::doubleValue).sum();
        Map<String, BudgetStatus> budgets = buildBudgetStatus(wallet, filtered);
        List<String> warnings = warningsCollector == null ? new ArrayList<>() : new ArrayList<>(warningsCollector);
        if (totalExpense > totalIncome) {
            warnings.add("Расходы превышают доходы.");
        }
        return new ReportData(totalIncome, totalExpense, incomeByCategory, expenseByCategory, budgets, warnings);
    }

    public Set<String> listCategories(User user) {
        Set<String> categories = user.getWallet().getTransactions().stream()
                .map(Transaction::getCategory)
                .collect(Collectors.toCollection(() -> new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
        categories.addAll(user.getWallet().getBudgets().keySet());
        return categories;
    }

    private String buildNotifications(User user) {
        Wallet wallet = user.getWallet();
        List<String> notices = new ArrayList<>();
        if (wallet.getBalance() == 0) {
            notices.add("Баланс равен 0.");
        }
        Map<String, BudgetStatus> statuses = buildBudgetStatus(wallet, wallet.getTransactions());
        for (Map.Entry<String, BudgetStatus> entry : statuses.entrySet()) {
            BudgetStatus status = entry.getValue();
            if (status.getLimit() <= 0) {
                continue;
            }
            if (status.getRemaining() < 0) {
                notices.add("Превышен лимит бюджета по категории: " + entry.getKey());
            } else if (status.getRemaining() <= status.getLimit() * 0.2) {
                notices.add("Потрачено 80% бюджета по категории: " + entry.getKey());
            }
        }
        double totalIncome = wallet.getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.INCOME || t.getType() == TransactionType.TRANSFER_IN)
                .mapToDouble(Transaction::getAmount)
                .sum();
        double totalExpense = wallet.getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE || t.getType() == TransactionType.TRANSFER_OUT)
                .mapToDouble(Transaction::getAmount)
                .sum();
        if (totalExpense > totalIncome) {
            notices.add("Расходы превышают доходы.");
        }
        return String.join(" ", notices);
    }

    private double recalculateBalance(Wallet wallet) {
        double income = wallet.getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.INCOME || t.getType() == TransactionType.TRANSFER_IN)
                .mapToDouble(Transaction::getAmount)
                .sum();
        double expense = wallet.getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE || t.getType() == TransactionType.TRANSFER_OUT)
                .mapToDouble(Transaction::getAmount)
                .sum();
        return income - expense;
    }

    private Map<String, BudgetStatus> buildBudgetStatus(Wallet wallet, List<Transaction> transactions) {
        Map<String, BudgetStatus> result = new LinkedHashMap<>();
        Map<String, Double> expensesByCategory = sumByCategory(transactions, TransactionType.EXPENSE, TransactionType.TRANSFER_OUT);
        wallet.getBudgets().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                .forEach(entry -> {
                    double spent = expensesByCategory.getOrDefault(entry.getKey(), 0.0);
                    result.put(entry.getKey(), new BudgetStatus(entry.getValue(), spent));
                });
        return result;
    }

    private List<Transaction> filterTransactions(Wallet wallet, List<String> categories,
                                                 String fromDate, String toDate, List<String> missingCategories) {
        LocalDate from = parseDate(fromDate);
        LocalDate to = parseDate(toDate);
        Set<String> normalizedCategories = null;
        if (categories != null && !categories.isEmpty()) {
            normalizedCategories = categories.stream()
                    .map(this::normalizeCategory)
                    .filter(c -> c != null)
                    .collect(Collectors.toCollection(() -> new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
        }
        Set<String> finalCategories = normalizedCategories;
        List<Transaction> filtered = wallet.getTransactions().stream()
                .filter(tx -> {
                    if (finalCategories != null && !finalCategories.contains(tx.getCategory())) {
                        return false;
                    }
                    LocalDate txDate = parseDate(tx.getDate());
                    if (from != null && txDate != null && txDate.isBefore(from)) {
                        return false;
                    }
                    if (to != null && txDate != null && txDate.isAfter(to)) {
                        return false;
                    }
                    return true;
                })
                .sorted(Comparator.comparing(Transaction::getDate))
                .collect(Collectors.toList());
        if (finalCategories != null && missingCategories != null) {
            Set<String> existing = wallet.getTransactions().stream()
                    .map(Transaction::getCategory)
                    .collect(Collectors.toCollection(() -> new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
            existing.addAll(wallet.getBudgets().keySet());
            for (String category : finalCategories) {
                if (!existing.contains(category)) {
                    missingCategories.add(category);
                }
            }
        }
        return filtered;
    }

    private Map<String, Double> sumByCategory(List<Transaction> transactions, TransactionType... types) {
        Map<String, Double> result = new LinkedHashMap<>();
        Set<TransactionType> typeSet = Set.of(types);
        transactions.stream()
                .filter(tx -> typeSet.contains(tx.getType()))
                .sorted(Comparator.comparing(Transaction::getCategory, String.CASE_INSENSITIVE_ORDER))
                .forEach(tx -> result.merge(tx.getCategory(), tx.getAmount(), Double::sum));
        return result;
    }

    private String normalizeCategory(String category) {
        if (category == null) {
            return null;
        }
        String trimmed = category.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String resolveCategory(User user, String category) {
        String normalized = normalizeCategory(category);
        if (normalized == null) {
            return null;
        }
        Wallet wallet = user.getWallet();
        String existing = findCategory(wallet, normalized);
        return existing != null ? existing : normalized;
    }

    private String findCategory(Wallet wallet, String category) {
        for (Transaction tx : wallet.getTransactions()) {
            if (tx.getCategory().equalsIgnoreCase(category)) {
                return tx.getCategory();
            }
        }
        for (String existing : wallet.getBudgets().keySet()) {
            if (existing.equalsIgnoreCase(category)) {
                return existing;
            }
        }
        return null;
    }

    private String resolveDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            return LocalDate.now().format(DATE_FORMAT);
        }
        if (parseDate(date) == null) {
            return null;
        }
        return date.trim();
    }

    private LocalDate parseDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(date.trim(), DATE_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String safeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
