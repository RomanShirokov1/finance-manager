package org.example.infra;

import org.example.core.model.User;
import org.example.core.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonUserRepositoryTest {

    @Test
    void saveAndLoadUsers(@TempDir Path tempDir) {
        Path file = tempDir.resolve("users.json");
        JsonUserRepository repo = new JsonUserRepository(file);
        AuthService service = new AuthService(repo);

        assertTrue(service.register("user1", "pass").isSuccess());
        assertTrue(service.register("user2", "pass2").isSuccess());

        Map<String, User> loaded = new JsonUserRepository(file).loadAll();
        assertEquals(2, loaded.size());
        assertNotNull(loaded.get("user1"));
        assertNotNull(loaded.get("user2"));
    }
}
