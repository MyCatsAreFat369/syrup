package org.maplestar.syrup.data;

import com.zaxxer.hikari.HikariDataSource;
import org.maplestar.syrup.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Initializes, sets up and provides access to a postgres database.
 */
public class DatabaseManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private HikariDataSource dataSource;

    /**
     * Initializes and sets up the database based on the provided configuration.
     *
     * @param config the config
     */
    public DatabaseManager(Config config) {
        initializeConnectionPool(config);
        initializeTables();
    }

    /**
     * Establishes a connection to the database. Shuts down the bot on failure.
     *
     * @param config the config
     */
    private void initializeConnectionPool(Config config) {
        dataSource = new HikariDataSource();
        dataSource.setDataSourceClassName("com.impossibl.postgres.jdbc.PGDataSource");
        dataSource.setUsername(config.databaseUsername());
        dataSource.setPassword(config.databasePassword());
        dataSource.addDataSourceProperty("serverName", config.databaseHost());
        dataSource.addDataSourceProperty("databaseName", config.databaseName());
        dataSource.setMinimumIdle(3);
        dataSource.setAutoCommit(true);
        try {
            dataSource.getConnection();
        } catch (SQLException exception) {
            logger.error("Could not initialize connection", exception);
            System.exit(1);
        }
    }

    /**
     * Creates the tables in the postgres database, if necessary. Shuts down the bot on failure.
     */
    private void initializeTables() {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS Ranks (guild_id BIGINT, user_id BIGINT, level INTEGER, xp BIGINT, PRIMARY KEY (guild_id, user_id))");
            statement.execute("CREATE TABLE IF NOT EXISTS BlockedChannels (guild_id BIGINT, channel_id BIGINT, PRIMARY KEY (guild_id, channel_id))");
            statement.execute("CREATE TABLE IF NOT EXISTS GuildSettings (guild_id BIGINT PRIMARY KEY, remove_old_roles BOOLEAN, add_on_join BOOLEAN)");
            statement.execute("CREATE TABLE IF NOT EXISTS LevelRoles (guild_id BIGINT, role_id BIGINT, level INTEGER, PRIMARY KEY (guild_id, role_id))");
        } catch (SQLException exception) {
            logger.error("Could not create tables", exception);
            System.exit(1);
        }
    }

    /**
     * Returns a database connection from the HikariCP connection pool.
     *
     * @return a database connection
     * @throws SQLException if there's a problem communicating with the database
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Closes the database connection. Should only be invoked when the bot shuts down.
     */
    public void closeDataSource() {
        if (!dataSource.isClosed()) {
            logger.info("Database connection shutdown!");
            dataSource.close();
        }
    }
}
