package org.maplestar.syrup.config;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Provides access to static configuration options for the application, such as database credentials.
 */
public class Config {
    private final Dotenv dotenv;

    private Config() {
        this.dotenv = Dotenv.load();
    }

    /**
     * Initializes the config.
     *
     * @return a new config instance
     */
    public static Config load() {
        return new Config();
    }

    /**
     * The Discord bot token. Extremely confidential.
     *
     * @return the bot token
     */
    public String botToken() {
        return dotenv.get("TOKEN");
    }

    /**
     * The IP address or domain of the database host.
     *
     * @return the database host
     */
    public String databaseHost() {
        return dotenv.get("DATABASE_HOST");
    }

    /**
     * The name of the database in postgres, not to be confused with {@link Config#databaseHost()} or {@link Config#databaseUsername()}.
     *
     * @return the database name
     */
    public String databaseName() {
        return dotenv.get("DATABASE_NAME");
    }

    /**
     * The username for the database account.
     *
     * @return the database username
     */
    public String databaseUsername() {
        return dotenv.get("DATABASE_USERNAME");
    }

    /**
     * The unencrypted password for the database account. Highly confidential.
     *
     * @return the database password
     */
    public String databasePassword() {
        return dotenv.get("DATABASE_PASSWORD");
    }
}
