package org.example;

import org.example.cli.ConsoleApp;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        Path storagePath = Path.of("data", "users.json");
        new ConsoleApp(storagePath).run();
    }
}
