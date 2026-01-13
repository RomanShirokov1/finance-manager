package org.example.cli;

import org.example.core.model.User;
import org.example.core.service.AuthService;
import org.example.core.service.FinanceService;
import org.example.core.service.ReportData;
import org.example.core.service.ServiceResult;
import org.example.infra.JsonUserRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

public class ConsoleApp {
    private final Scanner scanner;
    private final AuthService authService;
    private final FinanceService financeService;
    private final InputParser inputParser = new InputParser();
    private final TableFormatter tableFormatter = new TableFormatter();
    private final Path storagePath;
    private User currentUser;
    private String lastReport;

    public ConsoleApp(Path storagePath) {
        this.scanner = new Scanner(System.in);
        this.storagePath = storagePath;
        this.authService = new AuthService(new JsonUserRepository(storagePath));
        this.financeService = new FinanceService();
    }

    public void run() {
        println("Финансовый менеджер.");
        boolean running = true;
        while (running) {
            if (currentUser == null) {
                running = authMenu();
            } else {
                running = userMenu();
            }
        }
        println("До свидания!");
    }

    private boolean authMenu() {
        println("");
        println("Меню входа:");
        println("1) Войти");
        println("2) Регистрация");
        println("3) Экспорт данных (JSON)");
        println("4) импорт данных (JSON)");
        println("5) Выход");
        String input = prompt("Выберите пункт или введите команду (help): ");
        if (input.isEmpty()) {
            return true;
        }
        if (isNumber(input)) {
            switch (input) {
                case "1" -> handleLogin();
                case "2" -> handleRegister();
                case "3" -> handleExport();
                case "4" -> handleImport();
                case "5" -> {
                    authService.saveAll();
                    return false;
                }
                default -> println("Неизвестный пункт.");
            }
            return true;
        }
        return handleAuthCommand(input);
    }

    private boolean userMenu() {
        println("");
        println("Меню пользователя: " + currentUser.getLogin());
        println("1) Доход");
        println("2) Расход");
        println("3) Бюджеты");
        println("4) Категории");
        println("5) Перевод");
        println("6) Отчеты");
        println("7) Помощь");
        println("8) Выйти из аккаунта");
        println("9) Выход из приложения");
        String input = prompt("Выберите пункт или введите команду: ");
        if (input.isEmpty()) {
            return true;
        }
        if (isNumber(input)) {
            switch (input) {
                case "1" -> handleIncomeInteractive();
                case "2" -> handleExpenseInteractive();
                case "3" -> handleBudgetsMenu();
                case "4" -> handleCategoriesMenu();
                case "5" -> handleTransferInteractive();
                case "6" -> handleReportMenu();
                case "7" -> printHelp();
                case "8" -> currentUser = null;
                case "9" -> {
                    authService.saveAll();
                    return false;
                }
                default -> println("Неизвестный пункт.");
            }
            return true;
        }
        return handleUserCommand(input);
    }

    private boolean handleAuthCommand(String input) {
        List<String> args = inputParser.splitArgs(input);
        if (args.isEmpty()) {
            return true;
        }
        String cmd = args.get(0).toLowerCase(Locale.ROOT);
        switch (cmd) {
            case "help", "помощь" -> printHelp();
            case "войти", "логин", "login" -> handleLogin();
            case "регистрация", "register" -> handleRegister();
            case "экспорт" -> handleExport();
            case "импорт" -> handleImport();
            case "выход", "exit" -> {
                authService.saveAll();
                return false;
            }
            default -> println("Команда не распознана. Введите help.");
        }
        return true;
    }

    private boolean handleUserCommand(String input) {
        List<String> args = inputParser.splitArgs(input);
        if (args.isEmpty()) {
            return true;
        }
        String cmd = args.get(0).toLowerCase(Locale.ROOT);
        switch (cmd) {
            case "help", "помощь" -> printHelp();
            case "доход" -> handleIncomeCommand(args);
            case "расход" -> handleExpenseCommand(args);
            case "бюджет" -> handleBudgetCommand(args);
            case "категория" -> handleCategoryCommand(args);
            case "перевод" -> handleTransferCommand(args);
            case "отчет", "отчёт" -> handleReportCommand(args);
            case "экспорт" -> handleExport();
            case "импорт" -> handleImport();
            case "выход", "exit" -> {
                authService.saveAll();
                return false;
            }
            case "выйти", "logout" -> currentUser = null;
            default -> println("Команда не распознана. Введите help.");
        }
        return true;
    }

    private void handleLogin() {
        String login = prompt("Логин: ");
        String password = prompt("Пароль: ");
        ServiceResult<User> result = authService.login(login, password);
        println(result.getMessage());
        if (result.isSuccess()) {
            currentUser = result.getData();
        }
    }

    private void handleRegister() {
        String login = prompt("Логин: ");
        String password = prompt("Пароль: ");
        ServiceResult<User> result = authService.register(login, password);
        println(result.getMessage());
        if (result.isSuccess()) {
            currentUser = result.getData();
        }
    }

    private void handleIncomeInteractive() {
        String category = prompt("Категория: ");
        Double amount = readAmount("Сумма: ");
        if (amount == null) {
            return;
        }
        String date = prompt("Дата (ГГГГ-ММ-ДД, пусто = сегодня): ");
        String description = prompt("Описание (необязательно): ");
        handleIncome(category, amount, date, description);
    }

    private void handleIncomeCommand(List<String> args) {
        if (args.size() < 3) {
            println("Пример: доход \"Еда\" 500 2026-01-01 \"описание\"");
            return;
        }
        String category = args.get(1);
        Double amount = parseAmount(args.get(2));
        if (amount == null) {
            println("Некорректная сумма.");
            return;
        }
        String date = null;
        String description = "";
        if (args.size() >= 4 && args.get(3).matches("\\d{4}-\\d{2}-\\d{2}")) {
            date = args.get(3);
            if (args.size() > 4) {
                description = String.join(" ", args.subList(4, args.size()));
            }
        } else if (args.size() > 3) {
            description = String.join(" ", args.subList(3, args.size()));
        }
        handleIncome(category, amount, date, description);
    }

    private void handleIncome(String category, double amount, String date, String description) {
        ServiceResult<?> result = financeService.addIncome(currentUser, category, amount, date, description);
        println(messageOrDefault(result.getMessage(), "Доход добавлен."));
        if (result.isSuccess()) {
            authService.saveAll();
        }
    }

    private void handleExpenseInteractive() {
        String category = prompt("Категория: ");
        Double amount = readAmount("Сумма: ");
        if (amount == null) {
            return;
        }
        String date = prompt("Дата (ГГГГ-ММ-ДД, пусто = сегодня): ");
        String description = prompt("Описание (необязательно): ");
        handleExpense(category, amount, date, description);
    }

    private void handleExpenseCommand(List<String> args) {
        if (args.size() < 3) {
            println("Пример: расход \"Еда\" 300 2026-01-01 \"описание\"");
            return;
        }
        String category = args.get(1);
        Double amount = parseAmount(args.get(2));
        if (amount == null) {
            println("Некорректная сумма.");
            return;
        }
        String date = null;
        String description = "";
        if (args.size() >= 4 && args.get(3).matches("\\d{4}-\\d{2}-\\d{2}")) {
            date = args.get(3);
            if (args.size() > 4) {
                description = String.join(" ", args.subList(4, args.size()));
            }
        } else if (args.size() > 3) {
            description = String.join(" ", args.subList(3, args.size()));
        }
        handleExpense(category, amount, date, description);
    }

    private void handleExpense(String category, double amount, String date, String description) {
        ServiceResult<?> result = financeService.addExpense(currentUser, category, amount, date, description);
        println(messageOrDefault(result.getMessage(), "Расход добавлен."));
        if (result.isSuccess()) {
            authService.saveAll();
        }
    }

    private void handleTransferInteractive() {
        String recipient = prompt("Логин получателя: ");
        Double amount = readAmount("Сумма: ");
        if (amount == null) {
            return;
        }
        String date = prompt("Дата (ГГГГ-ММ-ДД, пусто = сегодня): ");
        String description = prompt("Описание (необязательно): ");
        handleTransfer(recipient, amount, date, description);
    }

    private void handleTransferCommand(List<String> args) {
        if (args.size() < 3) {
            println("Пример: перевод user2 1000 2026-01-01 \"описание\"");
            return;
        }
        String recipient = args.get(1);
        Double amount = parseAmount(args.get(2));
        if (amount == null) {
            println("Некорректная сумма.");
            return;
        }
        String date = null;
        String description = "";
        if (args.size() >= 4 && args.get(3).matches("\\d{4}-\\d{2}-\\d{2}")) {
            date = args.get(3);
            if (args.size() > 4) {
                description = String.join(" ", args.subList(4, args.size()));
            }
        } else if (args.size() > 3) {
            description = String.join(" ", args.subList(3, args.size()));
        }
        handleTransfer(recipient, amount, date, description);
    }

    private void handleTransfer(String recipient, double amount, String date, String description) {
        User receiver = authService.getUsers().get(recipient.toLowerCase(Locale.ROOT));
        ServiceResult<?> result = financeService.transfer(currentUser, receiver, amount, date, description);
        println(messageOrDefault(result.getMessage(), "Перевод выполнен."));
        if (result.isSuccess()) {
            authService.saveAll();
        }
    }

    private void handleBudgetsMenu() {
        boolean open = true;
        while (open) {
            println("");
        println("Бюджеты:");
        println("1) Установить/обновить бюджет");
        println("2) Обновить существующий бюджет");
        println("3) Удалить бюджет");
        println("4) Показать бюджеты");
        println("5) Назад");
        String input = prompt("Выберите пункт или команду: ");
        if (input.isEmpty()) {
            continue;
        }
        if (isNumber(input)) {
            switch (input) {
                case "1" -> budgetSetInteractive();
                case "2" -> budgetUpdateInteractive();
                case "3" -> budgetRemoveInteractive();
                case "4" -> showBudgets();
                case "5" -> open = false;
                default -> println("Неизвестный пункт.");
            }
            continue;
        }
        handleBudgetCommand(inputParser.splitArgs(input));
        }
    }

    private void budgetSetInteractive() {
        String category = prompt("Категория: ");
        Double limit = readAmount("Лимит: ");
        if (limit == null) {
            return;
        }
        ServiceResult<?> result = financeService.setBudget(currentUser, category, limit);
        println(result.getMessage());
        if (result.isSuccess()) {
            authService.saveAll();
        }
    }

    private void budgetRemoveInteractive() {
        String category = prompt("Категория: ");
        ServiceResult<?> result = financeService.removeBudget(currentUser, category);
        println(result.getMessage());
        if (result.isSuccess()) {
            authService.saveAll();
        }
    }

    private void budgetUpdateInteractive() {
        String category = prompt("Категория: ");
        Double limit = readAmount("Новый лимит: ");
        if (limit == null) {
            return;
        }
        ServiceResult<?> result = financeService.updateBudget(currentUser, category, limit);
        println(result.getMessage());
        if (result.isSuccess()) {
            authService.saveAll();
        }
    }

    private void showBudgets() {
        String table = tableFormatter.formatBudgets(financeService.buildReport(currentUser,
                null, null, null, new ArrayList<>(), new ArrayList<>()).getBudgets());
        println(table);
    }

    private void handleCategoriesMenu() {
        boolean open = true;
        while (open) {
            println("");
        println("Категории:");
        println("1) Переименовать категорию");
        println("2) Удалить категорию");
        println("3) Список категорий");
        println("4) Назад");
        String input = prompt("Выберите пункт или команду: ");
        if (input.isEmpty()) {
            continue;
        }
        if (isNumber(input)) {
            switch (input) {
                case "1" -> categoryRenameInteractive();
                case "2" -> categoryRemoveInteractive();
                case "3" -> showCategories();
                case "4" -> open = false;
                default -> println("Неизвестный пункт.");
            }
            continue;
        }
        handleCategoryCommand(inputParser.splitArgs(input));
        }
    }

    private void categoryRenameInteractive() {
        String oldName = prompt("Старая категория: ");
        String newName = prompt("Новая категория: ");
        ServiceResult<?> result = financeService.renameCategory(currentUser, oldName, newName);
        println(result.getMessage());
        if (result.isSuccess()) {
            authService.saveAll();
        }
    }

    private void categoryRemoveInteractive() {
        String name = prompt("Категория: ");
        ServiceResult<?> result = financeService.removeCategory(currentUser, name);
        println(result.getMessage());
        if (result.isSuccess()) {
            authService.saveAll();
        }
    }

    private void showCategories() {
        String categories = String.join(", ", financeService.listCategories(currentUser));
        println(categories.isEmpty() ? "Категории не найдены." : categories);
    }

    private void handleReportMenu() {
        boolean open = true;
        while (open) {
            println("");
            println("Отчеты:");
            println("1) Общий отчет");
            println("2) Отчет по категориям");
            println("3) Отчет за период");
            println("4) Сохранить последний отчет в файл");
            println("5) Назад");
            String input = prompt("Выберите пункт или команду: ");
            if (input.isEmpty()) {
                continue;
            }
            if (isNumber(input)) {
                switch (input) {
                    case "1" -> buildReport(null, null, null);
                    case "2" -> buildReportByCategories();
                    case "3" -> buildReportByPeriod();
                    case "4" -> saveLastReport();
                    case "5" -> open = false;
                    default -> println("Неизвестный пункт.");
                }
                continue;
            }
            handleReportCommand(inputParser.splitArgs(input));
        }
    }

    private void buildReportByCategories() {
        String raw = prompt("Категории через запятую: ");
        List<String> categories = parseCategories(raw);
        buildReport(categories, null, null);
    }

    private void buildReportByPeriod() {
        String from = prompt("Дата с (ГГГГ-ММ-ДД): ");
        String to = prompt("Дата по (ГГГГ-ММ-ДД): ");
        buildReport(null, from, to);
    }

    private void buildReport(List<String> categories, String from, String to) {
        List<String> warnings = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        ReportData report = financeService.buildReport(currentUser, categories, from, to, warnings, missing);
        String text = formatReport(report, missing);
        println(text);
        lastReport = text;
    }

    private void handleReportCommand(List<String> args) {
        if (args.size() == 1) {
            buildReport(null, null, null);
            return;
        }
        String sub = args.get(1).toLowerCase(Locale.ROOT);
        switch (sub) {
            case "категории" -> {
                List<String> categories = args.size() > 2 ? parseCategories(String.join(" ", args.subList(2, args.size()))) : null;
                buildReport(categories, null, null);
            }
            case "период" -> {
                String from = args.size() > 2 ? args.get(2) : null;
                String to = args.size() > 3 ? args.get(3) : null;
                buildReport(null, from, to);
            }
            case "файл" -> saveLastReport();
            default -> println("Неизвестная команда отчета.");
        }
    }

    private void saveLastReport() {
        if (lastReport == null || lastReport.isEmpty()) {
            println("Нет отчета для сохранения.");
            return;
        }
        String pathInput = prompt("Путь для сохранения (например, reports/report.txt): ");
        if (pathInput.isEmpty()) {
            println("Путь не указан.");
            return;
        }
        Path target = Path.of(pathInput);
        try {
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            Files.writeString(target, lastReport);
            println("Отчет сохранен: " + target);
        } catch (Exception e) {
            println("Не удалось сохранить отчет.");
        }
    }

    private void handleBudgetCommand(List<String> args) {
        if (args.size() < 2) {
            println("Пример: бюджет установить \"Еда\" 4000");
            return;
        }
        String sub = args.get(1).toLowerCase(Locale.ROOT);
        switch (sub) {
            case "установить", "обновить" -> {
                if (args.size() < 4) {
                    println("Пример: бюджет установить \"Еда\" 4000");
                    return;
                }
                String category = args.get(2);
                Double limit = parseAmount(args.get(3));
                if (limit == null) {
                    println("Некорректный лимит.");
                    return;
                }
                ServiceResult<?> result = financeService.setBudget(currentUser, category, limit);
                println(result.getMessage());
                if (result.isSuccess()) {
                    authService.saveAll();
                }
            }
            case "изменить" -> {
                if (args.size() < 4) {
                    println("Пример: бюджет изменить \"Еда\" 5000");
                    return;
                }
                String category = args.get(2);
                Double limit = parseAmount(args.get(3));
                if (limit == null) {
                    println("Некорректный лимит.");
                    return;
                }
                ServiceResult<?> result = financeService.updateBudget(currentUser, category, limit);
                println(result.getMessage());
                if (result.isSuccess()) {
                    authService.saveAll();
                }
            }
            case "удалить" -> {
                if (args.size() < 3) {
                    println("Пример: бюджет удалить \"Еда\"");
                    return;
                }
                ServiceResult<?> result = financeService.removeBudget(currentUser, args.get(2));
                println(result.getMessage());
                if (result.isSuccess()) {
                    authService.saveAll();
                }
            }
            case "показать" -> showBudgets();
            default -> println("Команда бюджета не распознана.");
        }
    }

    private void handleCategoryCommand(List<String> args) {
        if (args.size() < 2) {
            println("Пример: категория переименовать \"Еда\" \"Питание\"");
            return;
        }
        String sub = args.get(1).toLowerCase(Locale.ROOT);
        switch (sub) {
            case "переименовать" -> {
                if (args.size() < 4) {
                    println("Пример: категория переименовать \"Еда\" \"Питание\"");
                    return;
                }
                ServiceResult<?> result = financeService.renameCategory(currentUser, args.get(2), args.get(3));
                println(result.getMessage());
                if (result.isSuccess()) {
                    authService.saveAll();
                }
            }
            case "удалить" -> {
                if (args.size() < 3) {
                    println("Пример: категория удалить \"Еда\"");
                    return;
                }
                ServiceResult<?> result = financeService.removeCategory(currentUser, args.get(2));
                println(result.getMessage());
                if (result.isSuccess()) {
                    authService.saveAll();
                }
            }
            case "список" -> showCategories();
            default -> println("Команда категории не распознана.");
        }
    }

    private void handleExport() {
        String pathInput = prompt("Файл для экспорта JSON: ");
        if (pathInput.isEmpty()) {
            println("Путь не указан.");
            return;
        }
        Path target = Path.of(pathInput);
        new JsonUserRepository(target).saveAll(authService.getUsers());
        println("Экспорт завершен.");
    }

    private void handleImport() {
        String pathInput = prompt("Файл для импорта JSON: ");
        if (pathInput.isEmpty()) {
            println("Путь не указан.");
            return;
        }
        Path source = Path.of(pathInput);
        if (!Files.exists(source)) {
            println("Файл не найден.");
            return;
        }
        Map<String, User> imported = new JsonUserRepository(source).loadAll();
        authService.replaceAll(imported);
        println( "импорт завершен. Пользователей: " + imported.size());
    }

    private String formatReport(ReportData report, List<String> missing) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Общий доход: %.2f%n", report.getTotalIncome()));
        sb.append(String.format("Общие расходы: %.2f%n", report.getTotalExpense()));
        sb.append(System.lineSeparator());
        if (!report.getIncomeByCategory().isEmpty()) {
            sb.append("Доходы по категориям:").append(System.lineSeparator());
            sb.append(tableFormatter.formatTwoColumn(report.getIncomeByCategory(), "Категория", "Сумма"));
        }
        if (!report.getExpenseByCategory().isEmpty()) {
            sb.append("Расходы по категориям:").append(System.lineSeparator());
            sb.append(tableFormatter.formatTwoColumn(report.getExpenseByCategory(), "Категория", "Сумма"));
        }
        if (!report.getBudgets().isEmpty()) {
            sb.append("Бюджет по категориям:").append(System.lineSeparator());
            sb.append(tableFormatter.formatBudgets(report.getBudgets()));
        }
        if (!missing.isEmpty()) {
            sb.append("Категории не найдены: ").append(String.join(", ", missing)).append(System.lineSeparator());
        }
        if (report.getWarnings() != null && !report.getWarnings().isEmpty()) {
            sb.append("Уведомления: ").append(String.join(" ", report.getWarnings())).append(System.lineSeparator());
        }
        return sb.toString();
    }

    private List<String> parseCategories(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toList());
    }

    private Double readAmount(String prompt) {
        String input = prompt(prompt);
        Double amount = parseAmount(input);
        if (amount == null) {
            println("Некорректная сумма.");
        }
        return amount;
    }

    private Double parseAmount(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(input.replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isNumber(String input) {
        return input.matches("\\d+");
    }

    private String prompt(String message) {
        System.out.print(message);
        return scanner.nextLine().trim();
    }

    private void println(String message) {
        System.out.println(message);
    }

    private String messageOrDefault(String message, String defaultMessage) {
        if (message == null || message.trim().isEmpty()) {
            return defaultMessage;
        }
        return message;
    }

    private void printHelp() {
        println("");
        println("Доступные команды:");
        println("- доход \"Категория\" 1000 2026-01-01 \"описание\"");
        println("- расход \"Категория\" 500 2026-01-01 \"описание\"");
        println("- бюджет установить \"Категория\" 4000");
        println("- бюджет изменить \"Категория\" 4500");
        println("- бюджет удалить \"Категория\"");
        println("- бюджет показать");
        println("- категория переименовать \"Старое\" \"Новое\"");
        println("- категория удалить \"Категория\"");
        println("- категория список");
        println("- перевод login 1000 2026-01-01 \"описание\"");
        println("- отчет");
        println("- отчет категории \"Еда, Такси\"");
        println("- отчет период 2026-01-01 2026-01-31");
        println("- отчет файл");
        println("- экспорт / импорт");
        println("- выход / выйти");
        println("Подсказка: категории с пробелами вводите в кавычках.");
    }
}
