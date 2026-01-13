package org.example.infra;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.example.core.model.User;
import org.example.core.port.UserRepository;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonUserRepository implements UserRepository {
    private final Path path;
    private final Gson gson;

    public JsonUserRepository(Path path) {
        this.path = path;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @Override
    public Map<String, User> loadAll() {
        if (!Files.exists(path)) {
            return new HashMap<>();
        }
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            UserStore store = gson.fromJson(reader, UserStore.class);
            Map<String, User> result = new HashMap<>();
            if (store != null && store.getUsers() != null) {
                for (User user : store.getUsers()) {
                    result.put(user.getLogin().toLowerCase(), user);
                }
            }
            return result;
        } catch (IOException e) {
            return new HashMap<>();
        }
    }

    @Override
    public void saveAll(Map<String, User> users) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            List<User> list = new java.util.ArrayList<>(users.values());
            UserStore store = new UserStore(list);
            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                gson.toJson(store, writer);
            }
        } catch (IOException ignored) {
        }
    }
}
