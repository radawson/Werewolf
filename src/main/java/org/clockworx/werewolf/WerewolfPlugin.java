package org.clockworx.werewolf;

import java.util.logging.Level;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.clockworx.werewolf.config.WerewolfConfig;
import org.clockworx.werewolf.database.DatabaseManager;
import org.clockworx.werewolf.integration.VampireIntegration;
import org.clockworx.werewolf.manager.SkinManager;
import org.clockworx.werewolf.manager.WerewolfManager;
import org.clockworx.werewolf.server.ResourcePackServer;

/**
 * Main plugin class for the Werewolf plugin.
 * This class serves as the entry point and central manager for the plugin.
 */
public final class WerewolfPlugin extends JavaPlugin {

    private static WerewolfPlugin plugin;
    private WerewolfConfig config;
    private DatabaseManager databaseManager;
    private WerewolfManager werewolfManager;
    private SkinManager skinManager;
    private VampireIntegration vampireIntegration;
    private ResourcePackServer resourcePackServer;

    @Override
    public void onEnable() {
        plugin = this; // Assign in onEnable

        // --- Configuration ---
        // Load configurations first
        if (!initializeConfigs()) { // Method returns true on success, false on failure
            getLogger().severe("Failed to initialize configurations. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // --- Database Migrations ---
        // Run Flyway migrations BEFORE initializing DatabaseManager
        if (!runDatabaseMigrations()) {
            getLogger().severe("Database migration failed. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // --- Initialize Database Manager ---
        initializeDatabaseManager();

        // --- Check for Vampire Plugin ---
        checkVampirePlugin();

        // --- Initialize Resource Pack Server ---
        initializeResourcePackServer();

        // --- Initialize Core Components ---
        initializeManagers();
        registerCommands();
        registerListeners();
        
        // --- Load existing werewolf data ---
        werewolfManager.loadWerewolves();
        
        // --- Initialize API ---
        org.clockworx.werewolf.api.WerewolfAPI.initialize(this);

        // --- Initialize bStats Metrics ---
        // Plugin ID for WereWolf Redux on bStats
        int pluginId = 28710;
        try {
            Metrics metrics = new Metrics(this, pluginId);
            
            // Add custom charts
            metrics.addCustomChart(new SimplePie("database_type", () -> {
                if (config != null) {
                    return config.getDatabaseType();
                }
                return "Unknown";
            }));
            
            metrics.addCustomChart(new SimplePie("debug_mode", () -> {
                if (config != null) {
                    return config.isDebug() ? "Enabled" : "Disabled";
                }
                return "Unknown";
            }));
            
            metrics.addCustomChart(new SimplePie("plugin_version", () -> {
                String version = getPluginMeta().getVersion();
                return version != null ? version : "Unknown";
            }));
            
            metrics.addCustomChart(new SimplePie("vampire_integration", () -> {
                if (vampireIntegration != null && vampireIntegration.isAvailable()) {
                    return "Enabled";
                }
                return "Disabled";
            }));
            
            getLogger().info("bStats metrics initialized (plugin ID: " + pluginId + ")");
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Failed to initialize bStats metrics", e);
        }

        getLogger().info("Werewolf plugin enabled successfully!");
    }

    @Override
    public void onDisable() {
        // Save data on disable
        if (werewolfManager != null) {
            try {
                werewolfManager.shutdown();
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Error during WerewolfManager shutdown in onDisable", e);
            }
        }

        // Shutdown database resources
        if (databaseManager != null) {
            try {
                databaseManager.shutdown();
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Error during DatabaseManager shutdown in onDisable", e);
            }
        }
        
        // Shutdown Hibernate SessionFactory
        try {
            org.clockworx.werewolf.database.HibernateConfig.shutdown();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error during HibernateConfig shutdown in onDisable", e);
        }

        // Shutdown Resource Pack Server
        if (resourcePackServer != null) {
            try {
                resourcePackServer.stopServer();
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Error during ResourcePackServer shutdown in onDisable", e);
            }
        }

        getLogger().info("Werewolf plugin disabled!");
    }

    /**
     * Initialize configurations.
     * @return false if initialization succeeds, true otherwise (inverted for caller convenience).
     */
    private boolean initializeConfigs() {
        try {
            config = new WerewolfConfig(this);
            getLogger().info("Configurations initialized!");
            return true; // Indicate success
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error initializing configurations", e);
            return false; // Indicate failure
        }
    }

    /**
     * Executes database migrations using Flyway.
     * @return true if migrations were successful, false otherwise.
     */
    private boolean runDatabaseMigrations() {
        getLogger().info("Starting database migration check...");
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            // Set context class loader for Flyway to find drivers/resources
            Thread.currentThread().setContextClassLoader(getClassLoader());

            // Get database details from loaded config
            String dbType = config.getDatabaseType();
            String dbUrl = config.getDatabaseUrl();
            String dbUser = config.getDatabaseUser();
            String dbPassword = config.getDatabasePassword();
            String tablePrefix = config.getDatabaseTablePrefix();

            // Load the appropriate JDBC driver explicitly
            try {
                if ("mysql".equalsIgnoreCase(dbType)) {
                    Class.forName("com.mysql.cj.jdbc.Driver", true, getClassLoader());
                } else if ("sqlite".equalsIgnoreCase(dbType)) {
                    Class.forName("org.sqlite.JDBC", true, getClassLoader());
                } else if ("postgres".equalsIgnoreCase(dbType) || "postgresql".equalsIgnoreCase(dbType)) {
                    Class.forName("org.postgresql.Driver", true, getClassLoader());
                }
            } catch (ClassNotFoundException e) {
                getLogger().log(Level.SEVERE, "Could not find JDBC driver for database type: " + dbType, e);
                return false;
            }

            org.flywaydb.core.api.configuration.FluentConfiguration flywayConfig = 
                org.flywaydb.core.Flyway.configure(getClassLoader())
                    .dataSource(dbUrl, dbUser, dbPassword)
                    .locations("classpath:db/migration")
                    .encoding("UTF-8")
                    .baselineOnMigrate(true)
                    .baselineVersion("0")
                    .placeholders(java.util.Map.of("tablePrefix", tablePrefix));

            // Set the schema history table name with the prefix
            String historyTableName = tablePrefix.isEmpty() 
                ? "flyway_schema_history" 
                : tablePrefix + "flyway_schema_history";
            flywayConfig.table(historyTableName);
            getLogger().info("Using Flyway history table: " + historyTableName);

            org.flywaydb.core.Flyway flyway = flywayConfig.load();
            flyway.migrate();

            getLogger().info("Database migration check completed successfully.");
            return true; // Success
        } catch (org.flywaydb.core.api.FlywayException e) {
            getLogger().log(Level.SEVERE, "Database migration failed!", e);
            if (e.getCause() != null) {
                getLogger().log(Level.SEVERE, "Cause: " + e.getCause().getMessage(), e.getCause());
            }
            return false; // Failure
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "An unexpected error occurred during database migration setup!", e);
            return false; // Failure
        } finally {
            // Restore original class loader
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    /**
     * Initialize database manager instance.
     */
    private void initializeDatabaseManager() {
        databaseManager = new DatabaseManager(this);
        getLogger().info("DatabaseManager initialized.");
    }

    /**
     * Check for Vampire plugin and initialize integration if available.
     */
    private void checkVampirePlugin() {
        org.bukkit.plugin.Plugin vampirePlugin = getServer().getPluginManager().getPlugin("Vampire");
        if (vampirePlugin != null && vampirePlugin.isEnabled()) {
            getLogger().info("Vampire plugin detected. Initializing integration...");
            vampireIntegration = new VampireIntegration(this);
            if (vampireIntegration.isAvailable()) {
                getLogger().info("Vampire integration initialized successfully.");
            } else {
                getLogger().warning("Vampire plugin found but integration failed. Continuing without integration.");
                vampireIntegration = null;
            }
        } else {
            getLogger().info("Vampire plugin not found. Continuing without integration.");
        }
    }

    /**
     * Initialize resource pack server if enabled in config.
     */
    private void initializeResourcePackServer() {
        if (config.isResourcePackServerEnabled()) {
            int port = config.getResourcePackServerPort();
            resourcePackServer = new ResourcePackServer(this, port);
            
            if (resourcePackServer.startServer()) {
                // Auto-populate URL and hash in config
                String host = config.getResourcePackServerHost();
                String url = resourcePackServer.getResourcePackUrl(host);
                String hash = resourcePackServer.getResourcePackHashHex();
                
                if (url != null && !url.isEmpty()) {
                    config.setResourcePackUrl(url);
                    getLogger().info("Resource pack URL auto-populated: " + url);
                }
                
                if (hash != null && !hash.isEmpty()) {
                    config.setResourcePackHash(hash);
                    getLogger().info("Resource pack hash auto-populated: " + hash);
                }
            } else {
                getLogger().warning("Resource pack server failed to start. You may need to configure a manual URL.");
                resourcePackServer = null;
            }
        } else {
            getLogger().info("Resource pack server is disabled. Using manual URL configuration.");
        }
    }

    /**
     * Initialize managers
     */
    private void initializeManagers() {
        skinManager = new SkinManager(this);
        werewolfManager = new WerewolfManager(this, skinManager, vampireIntegration);
        getLogger().info("Managers initialized!");
    }

    /**
     * Register commands using Paper's Brigadier command system
     */
    @SuppressWarnings("UnstableApiUsage")
    private void registerCommands() {
        LifecycleEventManager<Plugin> manager = getLifecycleManager();
        
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();
            
            org.clockworx.werewolf.command.WerewolfCommandTree commandTree = 
                new org.clockworx.werewolf.command.WerewolfCommandTree(this);
            
            try {
                commands.register(
                    commandTree.build().build(),
                    "Main command for the Werewolf plugin",
                    java.util.Collections.emptyList()
                );
                getLogger().info("Registered 'werewolf' command using Brigadier.");
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Failed to register 'werewolf' command", e);
            }
        });
    }

    /**
     * Register event listeners
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(
            new org.clockworx.werewolf.listeners.PlayerListener(this), this);
        if (vampireIntegration != null) {
            getServer().getPluginManager().registerEvents(
                new org.clockworx.werewolf.listeners.VampireListener(this), this);
        }
        getLogger().info("Registered event listeners.");
    }

    /**
     * Debug message
     */
    public void debug(String message) {
        if (config != null && config.isDebug()) {
            getLogger().info("[DEBUG] " + message);
        }
    }

    /**
     * Error message
     */
    public void error(String message, Throwable t) {
        getLogger().log(Level.SEVERE, message, t);
    }

    /**
     * Reloads the plugin's configuration files.
     */
    public void reload() {
        try {
            config.loadConfig();
            getLogger().info("Configuration files reloaded.");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to reload plugin configuration", e);
        }
    }

    // Getters
    public WerewolfConfig getWerewolfConfig() {
        return config;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public WerewolfManager getWerewolfManager() {
        return werewolfManager;
    }

    public SkinManager getSkinManager() {
        return skinManager;
    }

    public VampireIntegration getVampireIntegration() {
        return vampireIntegration;
    }

    public ResourcePackServer getResourcePackServer() {
        return resourcePackServer;
    }

    /**
     * Static getter for the plugin instance.
     * Useful for accessing the plugin from static contexts, but use with caution.
     * Consider dependency injection where possible.
     * @return The singleton instance of WerewolfPlugin.
     */
    public static WerewolfPlugin getInstance() {
        return plugin;
    }
}

