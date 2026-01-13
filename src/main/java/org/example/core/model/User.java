package org.example.core.model;

public class User {
    private String login;
    private String passwordHash;
    private Wallet wallet = new Wallet();

    public User() {
    }

    public User(String login, String passwordHash) {
        this.login = login;
        this.passwordHash = passwordHash;
    }

    public String getLogin() {
        return login;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Wallet getWallet() {
        return wallet;
    }
}
