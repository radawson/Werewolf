package org.clockworx.werewolf.database;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.clockworx.werewolf.WerewolfPlugin;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

/**
 * Configures and initializes Hibernate ORM for database connectivity.
 * Handles both MySQL and SQLite connection setup with appropriate dialects and connection pooling.
 * SessionFactory is initialized lazily on first request.
 */
public class HibernateConfig {
    private static volatile SessionFactory sessionFactory; // volatile for thread safety
    private static volatile boolean initialized = false; // volatile for thread safety
    private static final Object initLock = new Object(); // Lock object for synchronization

    /**
     * Initializes Hibernate with the specified database configuration.
     * This method is called internally and synchronized to ensure it only runs once.
     *
     * @param plugin The WerewolfPlugin instance
     */
    private static void initializeInternal(WerewolfPlugin plugin) {
        // Double-checked locking pattern to avoid unnecessary synchronization
        if (initialized) {
            return;
        }
        synchronized (initLock) {
            if (initialized) {
                return; // Check again inside synchronized block
            }

            try {
                plugin.getLogger().log(Level.INFO, "Initializing Hibernate SessionFactory...");

                // Get database configuration from WerewolfConfig
                String dbType = plugin.getWerewolfConfig().getDatabaseType();
                String dbUrl = plugin.getWerewolfConfig().getDatabaseUrl();
                String dbUser = plugin.getWerewolfConfig().getDatabaseUser();
                String dbPassword = plugin.getWerewolfConfig().getDatabasePassword();
                String tablePrefix = plugin.getWerewolfConfig().getDatabaseTablePrefix();

                // Configure Hibernate
                Configuration configuration = new Configuration();
                Properties settings = new Properties();

                // Common settings
                settings.put(Environment.SHOW_SQL, "false");
                settings.put(Environment.HBM2DDL_AUTO, "none");
                settings.put(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread");

                // Apply Table Prefix
                settings.put(Environment.PHYSICAL_NAMING_STRATEGY, new PrefixPhysicalNamingStrategy(tablePrefix));
                plugin.getLogger().log(Level.INFO, "Applying Hibernate table prefix: '" + tablePrefix + "'");

                // Configure Hibernate Logging
                Level sqlLogLevel = plugin.getWerewolfConfig().isDebug() ? Level.FINE : Level.INFO;
                Logger.getLogger("org.hibernate.SQL").setLevel(sqlLogLevel);
                Logger.getLogger("org.hibernate.orm.jdbc.bind").setLevel(sqlLogLevel);
                plugin.getLogger().log(Level.INFO, "Redirected Hibernate SQL logging to logger at level: " + sqlLogLevel.getName());

                // Database-specific settings
                if ("mysql".equalsIgnoreCase(dbType)) {
                    plugin.getLogger().log(Level.INFO, "Configuring Hibernate for MySQL...");
                    settings.put(Environment.DIALECT, "org.hibernate.dialect.MySQLDialect");
                    settings.put(Environment.CONNECTION_PROVIDER, "org.hibernate.hikaricp.internal.HikariCPConnectionProvider");

                    // Hikari Specific Properties
                    settings.put("hibernate.hikari.jdbcUrl", dbUrl);
                    settings.put("hibernate.hikari.username", dbUser);
                    settings.put("hibernate.hikari.password", dbPassword);
                    settings.put("hibernate.hikari.driverClassName", "com.mysql.cj.jdbc.Driver");
                    settings.put("hibernate.hikari.maximumPoolSize", "10");
                    settings.put("hibernate.hikari.minimumIdle", "5");
                    settings.put("hibernate.hikari.idleTimeout", "300000");
                    settings.put("hibernate.hikari.connectionTimeout", "10000");
                    settings.put("hibernate.hikari.autoCommit", "true");

                } else if ("sqlite".equalsIgnoreCase(dbType)) {
                    plugin.getLogger().log(Level.INFO, "Configuring Hibernate for SQLite...");
                    settings.put(Environment.DIALECT, "org.hibernate.community.dialect.SQLiteDialect");
                    settings.put("jakarta.persistence.jdbc.url", dbUrl);
                    settings.put("hibernate.connection.autocommit", "true");

                } else if ("postgres".equalsIgnoreCase(dbType) || "postgresql".equalsIgnoreCase(dbType)) {
                    plugin.getLogger().log(Level.INFO, "Configuring Hibernate for PostgreSQL...");
                    settings.put(Environment.DIALECT, "org.hibernate.dialect.PostgreSQLDialect");
                    settings.put(Environment.CONNECTION_PROVIDER, "org.hibernate.hikaricp.internal.HikariCPConnectionProvider");
                    
                    // Hikari Specific Properties for PostgreSQL
                    settings.put("hibernate.hikari.jdbcUrl", dbUrl);
                    settings.put("hibernate.hikari.username", dbUser);
                    settings.put("hibernate.hikari.password", dbPassword);
                    settings.put("hibernate.hikari.driverClassName", "org.postgresql.Driver");
                    settings.put("hibernate.hikari.maximumPoolSize", "10");
                    settings.put("hibernate.hikari.minimumIdle", "5");
                    settings.put("hibernate.hikari.idleTimeout", "300000");
                    settings.put("hibernate.hikari.connectionTimeout", "10000");
                    settings.put("hibernate.hikari.autoCommit", "true");
                } else {
                    throw new IllegalArgumentException("Unsupported database type: " + dbType);
                }

                configuration.setProperties(settings);

                // Add entity classes
                configuration.addAnnotatedClass(org.clockworx.werewolf.entity.WerewolfPlayerEntity.class);

                // Build SessionFactory
                sessionFactory = configuration.buildSessionFactory();
                initialized = true;
                plugin.getLogger().log(Level.INFO, "Hibernate SessionFactory initialized successfully.");

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to initialize Hibernate SessionFactory", e);
                throw new RuntimeException("Hibernate initialization failed", e);
            }
        }
    }

    /**
     * Gets the Hibernate SessionFactory, initializing it if necessary.
     *
     * @param plugin The WerewolfPlugin instance (required for first-time initialization)
     * @return The SessionFactory instance
     */
    public static SessionFactory getSessionFactory(WerewolfPlugin plugin) {
        if (!initialized) {
            initializeInternal(plugin);
        }
        return sessionFactory;
    }

    /**
     * Gets the Hibernate SessionFactory (for use after initialization).
     * Throws an exception if not yet initialized.
     *
     * @return The SessionFactory instance
     */
    public static SessionFactory getSessionFactory() {
        if (!initialized || sessionFactory == null) {
            throw new IllegalStateException("Hibernate SessionFactory not initialized. Call getSessionFactory(plugin) first.");
        }
        return sessionFactory;
    }

    /**
     * Shuts down the Hibernate SessionFactory.
     * This should be called during plugin shutdown.
     */
    public static void shutdown() {
        synchronized (initLock) {
            if (sessionFactory != null && !sessionFactory.isClosed()) {
                sessionFactory.close();
                sessionFactory = null;
                initialized = false;
            }
        }
    }
}

