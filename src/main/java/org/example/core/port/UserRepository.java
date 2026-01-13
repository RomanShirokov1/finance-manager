package org.example.core.port;

import org.example.core.model.User;

import java.util.Map;

public interface UserRepository {
    Map<String, User> loadAll();

    void saveAll(Map<String, User> users);
}
