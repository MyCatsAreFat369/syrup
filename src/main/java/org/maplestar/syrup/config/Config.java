package org.maplestar.syrup.config;

import io.github.cdimascio.dotenv.Dotenv;

public class Config {
    private final Dotenv dotenv;

    private Config() {
        this.dotenv = Dotenv.load();
    }

    public static Config load() {
        return new Config();
    }

    public String botToken() {
        return dotenv.get("TOKEN");
    }

    public String databaseHost() {
        return dotenv.get("DATABASE_HOST");
    }

    public String databaseName() {
        return dotenv.get("DATABASE_NAME");
    }

    public String databaseUsername() {
        return dotenv.get("DATABASE_USERNAME");
    }

    public String databasePassword() {
        return dotenv.get("DATABASE_PASSWORD");
    }
}
