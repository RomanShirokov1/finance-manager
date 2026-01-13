package org.example.core.service;

import org.example.core.model.User;
import org.example.core.port.UserRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class AuthService {
    private final UserRepository repository;
    private final Map<String, User> users = new HashMap<>();

    public AuthService(UserRepository repository) {
        this.repository = repository;
        users.putAll(repository.loadAll());
    }

    public ServiceResult<User> register(String login, String password) {
        if (login == null || login.trim().isEmpty()) {
            return ServiceResult.fail("Логин не может быть пустым.");
        }
        if (password == null || password.trim().isEmpty()) {
            return ServiceResult.fail("Пароль не может быть пустым.");
        }
        String key = login.trim().toLowerCase();
        if (users.containsKey(key)) {
            return ServiceResult.fail("Пользователь с таким логином уже существует.");
        }
        String hash = hashPassword(password);
        if (hash == null) {
            return ServiceResult.fail("Не удалось создать пароль.");
        }
        User user = new User(login.trim(), hash);
        users.put(key, user);
        repository.saveAll(users);
        return ServiceResult.ok(user, "Пользователь зарегистрирован.");
    }

    public ServiceResult<User> login(String login, String password) {
        if (login == null || login.trim().isEmpty()) {
            return ServiceResult.fail("Логин не может быть пустым.");
        }
        if (password == null || password.trim().isEmpty()) {
            return ServiceResult.fail("Пароль не может быть пустым.");
        }
        String key = login.trim().toLowerCase();
        User user = users.get(key);
        if (user == null) {
            return ServiceResult.fail("Пользователь не найден.");
        }
        String hash = hashPassword(password);
        if (hash == null || !hash.equals(user.getPasswordHash())) {
            return ServiceResult.fail("Неверный пароль.");
        }
        return ServiceResult.ok(user, "Успешный вход.");
    }

    public Map<String, User> getUsers() {
        return users;
    }

    public void saveAll() {
        repository.saveAll(users);
    }

    public void replaceAll(Map<String, User> newUsers) {
        users.clear();
        if (newUsers != null) {
            users.putAll(newUsers);
        }
        repository.saveAll(users);
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
