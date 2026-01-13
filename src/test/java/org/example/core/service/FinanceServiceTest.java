package org.example.core.service;

import org.example.core.model.Transaction;
import org.example.core.model.TransactionType;
import org.example.core.model.User;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FinanceServiceTest {

    @Test
    void addIncomeUpdatesBalanceAndTransaction() {
        FinanceService service = new FinanceService();
        User user = new User("u", "hash");

        ServiceResult<Transaction> result = service.addIncome(user, "Salary", 1000, "2026-01-01", "ok");
        assertTrue(result.isSuccess());
        assertEquals(1000, user.getWallet().getBalance(), 0.001);
        assertEquals(1, user.getWallet().getTransactions().size());
        assertEquals(TransactionType.INCOME, user.getWallet().getTransactions().get(0).getType());
    }

    @Test
    void addExpenseRejectsInsufficientBalance() {
        FinanceService service = new FinanceService();
        User user = new User("u", "hash");

        ServiceResult<Transaction> result = service.addExpense(user, "Food", 500, "2026-01-01", "");
        assertFalse(result.isSuccess());
        assertEquals(0, user.getWallet().getBalance(), 0.001);
    }

    @Test
    void addExpenseUpdatesBalance() {
        FinanceService service = new FinanceService();
        User user = new User("u", "hash");
        service.addIncome(user, "Salary", 1000, "2026-01-01", "");

        ServiceResult<Transaction> result = service.addExpense(user, "Food", 200, "2026-01-02", "");
        assertTrue(result.isSuccess());
        assertEquals(800, user.getWallet().getBalance(), 0.001);
    }

    @Test
    void setBudgetAndUpdateBudget() {
        FinanceService service = new FinanceService();
        User user = new User("u", "hash");

        assertTrue(service.setBudget(user, "Food", 1000).isSuccess());
        assertTrue(service.updateBudget(user, "Food", 1500).isSuccess());
        assertEquals(1500, user.getWallet().getBudgets().get("Food"), 0.001);
    }

    @Test
    void updateBudgetFailsWhenMissing() {
        FinanceService service = new FinanceService();
        User user = new User("u", "hash");

        ServiceResult<Void> result = service.updateBudget(user, "Food", 1000);
        assertFalse(result.isSuccess());
    }

    @Test
    void removeBudgetFailsWhenMissing() {
        FinanceService service = new FinanceService();
        User user = new User("u", "hash");

        ServiceResult<Void> result = service.removeBudget(user, "Food");
        assertFalse(result.isSuccess());
    }

    @Test
    void renameCategoryUpdatesTransactionsAndBudgets() {
        FinanceService service = new FinanceService();
        User user = new User("u", "hash");
        service.addIncome(user, "Food", 1000, "2026-01-01", "");
        service.setBudget(user, "Food", 500);

        ServiceResult<Void> result = service.renameCategory(user, "Food", "Meals");
        assertTrue(result.isSuccess());
        assertEquals("Meals", user.getWallet().getTransactions().get(0).getCategory());
        assertTrue(user.getWallet().getBudgets().containsKey("Meals"));
    }

    @Test
    void renameCategoryFailsWhenMissing() {
        FinanceService service = new FinanceService();
        User user = new User("u", "hash");

        ServiceResult<Void> result = service.renameCategory(user, "Missing", "NewName");
        assertFalse(result.isSuccess());
    }

    @Test
    void removeCategoryDeletesTransactionsAndRecalculatesBalance() {
        FinanceService service = new FinanceService();
        User user = new User("u", "hash");
        service.addIncome(user, "Salary", 1000, "2026-01-01", "");
        service.addExpense(user, "Food", 200, "2026-01-02", "");
        service.setBudget(user, "Food", 300);

        ServiceResult<Void> result = service.removeCategory(user, "Food");
        assertTrue(result.isSuccess());
        assertEquals(1000, user.getWallet().getBalance(), 0.001);
        assertEquals(1, user.getWallet().getTransactions().size());
        assertFalse(user.getWallet().getBudgets().containsKey("Food"));
    }

    @Test
    void removeCategoryFailsWhenMissing() {
        FinanceService service = new FinanceService();
        User user = new User("u", "hash");

        ServiceResult<Void> result = service.removeCategory(user, "Missing");
        assertFalse(result.isSuccess());
    }

    @Test
    void transferSuccessUpdatesBothWallets() {
        FinanceService service = new FinanceService();
        User sender = new User("u1", "hash");
        User receiver = new User("u2", "hash");
        service.addIncome(sender, "Salary", 1000, "2026-01-01", "");

        ServiceResult<Void> result = service.transfer(sender, receiver, 400, "2026-01-02", "");
        assertTrue(result.isSuccess());
        assertEquals(600, sender.getWallet().getBalance(), 0.001);
        assertEquals(400, receiver.getWallet().getBalance(), 0.001);
        assertEquals(1, receiver.getWallet().getTransactions().size());
    }

    @Test
    void transferFailsWhenInsufficientBalance() {
        FinanceService service = new FinanceService();
        User sender = new User("u1", "hash");
        User receiver = new User("u2", "hash");

        ServiceResult<Void> result = service.transfer(sender, receiver, 100, "2026-01-01", "");
        assertFalse(result.isSuccess());
    }

    @Test
    void reportWarnsWhenExpensesExceedIncome() {
        FinanceService service = new FinanceService();
        User user = new User("u", "hash");
        user.getWallet().getTransactions().add(new Transaction(
                "1", TransactionType.INCOME, "Salary", 100, "", "2026-01-01", null));
        user.getWallet().getTransactions().add(new Transaction(
                "2", TransactionType.EXPENSE, "Food", 200, "", "2026-01-02", null));

        List<String> warnings = new ArrayList<>();
        ReportData report = service.buildReport(user, null, null, null, warnings, new ArrayList<>());
        assertTrue(report.getWarnings().size() > 0);
    }

    @Test
    void reportCollectsMissingCategories() {
        FinanceService service = new FinanceService();
        User user = new User("u", "hash");
        service.addIncome(user, "Salary", 100, "2026-01-01", "");

        List<String> missing = new ArrayList<>();
        service.buildReport(user, List.of("Food"), null, null, new ArrayList<>(), missing);
        assertEquals(1, missing.size());
        assertEquals("Food", missing.get(0));
    }

    @Test
    void budgetStatusRemainingIsCorrect() {
        FinanceService service = new FinanceService();
        User user = new User("u", "hash");
        service.addIncome(user, "Salary", 1000, "2026-01-01", "");
        service.addExpense(user, "Food", 200, "2026-01-02", "");
        service.setBudget(user, "Food", 500);

        ReportData report = service.buildReport(user, null, null, null, new ArrayList<>(), new ArrayList<>());
        assertEquals(300, report.getBudgets().get("Food").getRemaining(), 0.001);
    }
}
