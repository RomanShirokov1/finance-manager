package org.example.infra;

import org.example.core.model.User;

import java.util.ArrayList;
import java.util.List;

public class UserStore {
    private List<User> users = new ArrayList<>();

    public UserStore() {
    }

    public UserStore(List<User> users) {
        this.users = users;
    }

    public List<User> getUsers() {
        return users;
    }
}
