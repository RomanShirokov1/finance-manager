package org.example.core.service;

import org.example.core.model.User;
import org.example.core.port.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    @Test
    void registerSuccessAndLogin() {
        InMemoryUserRepository repo = new InMemoryUserRepository();
        AuthService service = new AuthService(repo);

        ServiceResult<User> reg = service.register("user1", "pass");
        assertTrue(reg.isSuccess());
        assertNotNull(reg.getData());
        assertNotEquals("pass", reg.getData().getPasswordHash());

        ServiceResult<User> login = service.login("user1", "pass");
        assertTrue(login.isSuccess());
    }

    @Test
    void registerDuplicateFails() {
        InMemoryUserRepository repo = new InMemoryUserRepository();
        AuthService service = new AuthService(repo);

        assertTrue(service.register("user1", "pass").isSuccess());
        ServiceResult<User> second = service.register("user1", "pass2");
        assertFalse(second.isSuccess());
    }

    @Test
    void loginWrongPasswordFails() {
        InMemoryUserRepository repo = new InMemoryUserRepository();
        AuthService service = new AuthService(repo);
        service.register("user1", "pass");

        ServiceResult<User> login = service.login("user1", "wrong");
        assertFalse(login.isSuccess());
    }

    @Test
    void loginUnknownUserFails() {
        AuthService service = new AuthService(new InMemoryUserRepository());
        ServiceResult<User> login = service.login("unknown", "pass");
        assertFalse(login.isSuccess());
    }

    @Test
    void registerValidationFails() {
        AuthService service = new AuthService(new InMemoryUserRepository());
        assertFalse(service.register("", "pass").isSuccess());
        assertFalse(service.register("user", "").isSuccess());
    }

    private static class InMemoryUserRepository implements UserRepository {
        private final Map<String, User> data = new HashMap<>();

        @Override
        public Map<String, User> loadAll() {
            return new HashMap<>(data);
        }

        @Override
        public void saveAll(Map<String, User> users) {
            data.clear();
            data.putAll(users);
        }
    }
}
