package org.clockworx.werewolf.config;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.clockworx.werewolf.WerewolfPlugin;

/**
 * Configuration manager for the Werewolf plugin.
 * Handles loading and accessing configuration values.
 */
public class WerewolfConfig {
    private final WerewolfPlugin plugin;
    private FileConfiguration config;
    private File configFile;
    
    // Database settings
    private String databaseType;
    private String databaseUrl;
    private String databaseUser;
    private String databasePassword;
    private String databaseTablePrefix;
    
    // General settings
    private boolean debug;
    private boolean saveOnQuit;
    private boolean autoSave;
    private int autoSaveInterval;
    
    // Werewolf settings
    private boolean transformationEnabled;
    private boolean skinChangeEnabled;
    
    // Resource pack server settings
    private boolean resourcePackServerEnabled;
    private int resourcePackServerPort;
    private String resourcePackServerHost;
    
    // Resource pack settings
    private String resourcePackUrl;
    private String resourcePackHash;
    private boolean resourcePackRequired;
    
    // Vampire integration settings
    private boolean vampireIntegrationEnabled;
    private boolean preventVampireConflict;
    
    // Skin settings
    private Map<String, String> skinTypes;
    
    public WerewolfConfig(WerewolfPlugin plugin) {
        this.plugin = plugin;
        this.config = null;
        this.databaseType = "sqlite";
        this.skinTypes = new HashMap<>();
        loadConfig();
    }
    
    /**
     * Loads the configuration from file, creating default if it doesn't exist.
     */
    public void loadConfig() {
        // Use SimpleDataLib FileTools if available, otherwise fall back to manual operations
        regalowl.simpledatalib.SimpleDataLib sdl = plugin.getSimpleDataLib();
        
        if (sdl != null && sdl.getFileTools() != null) {
            // Use FileTools for directory creation
            sdl.getFileTools().makeFolder(plugin.getDataFolder().getAbsolutePath());
        } else {
            // Fallback to manual directory creation
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdir();
            }
        }
        
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            try {
                // Use FileTools if available, otherwise use Bukkit's saveResource
                if (sdl != null && sdl.getFileTools() != null) {
                    String resourcePath = "config.yml";
                    String destPath = configFile.getAbsolutePath();
                    sdl.getFileTools().copyFileFromJar(resourcePath, destPath);
                } else {
                    plugin.saveResource("config.yml", false);
                }
                
                if (configFile.exists()) {
                    plugin.getLogger().info("Created default config.yml from JAR resource.");
                } else {
                    plugin.getLogger().log(Level.SEVERE, "Failed to create config.yml: File was not created after resource extraction.");
                    throw new IllegalStateException("config.yml was not created after resource extraction");
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create config.yml: Resource not found in plugin JAR or extraction failed.", e);
                throw new IllegalStateException("Could not create config.yml", e);
            }
        }
        
        // Load the configuration
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // Load all settings
        loadDatabaseSettings();
        loadGeneralSettings();
        loadWerewolfSettings();
        loadVampireIntegrationSettings();
        loadSkinSettings();
        
        plugin.getLogger().info("Configuration loaded successfully.");
    }
    
    /**
     * Loads database-related settings from the configuration.
     */
    private void loadDatabaseSettings() {
        ConfigurationSection dbSection = config.getConfigurationSection("database");
        if (dbSection != null) {
            databaseType = dbSection.getString("type", "sqlite");
            databaseUrl = dbSection.getString("url", "jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/werewolf.db");
            databaseUser = dbSection.getString("user", "");
            databasePassword = dbSection.getString("password", "");
            databaseTablePrefix = dbSection.getString("table-prefix", "");
        } else {
            // Set defaults
            databaseType = "sqlite";
            databaseUrl = "jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/werewolf.db";
            databaseUser = "";
            databasePassword = "";
            databaseTablePrefix = "";
        }
    }
    
    /**
     * Loads general settings from the configuration.
     */
    private void loadGeneralSettings() {
        debug = config.getBoolean("debug", false);
        saveOnQuit = config.getBoolean("save-on-quit", true);
        autoSave = config.getBoolean("auto-save.enabled", true);
        autoSaveInterval = config.getInt("auto-save.interval", 300); // 5 minutes default
    }
    
    /**
     * Loads werewolf-specific settings from the configuration.
     */
    private void loadWerewolfSettings() {
        transformationEnabled = config.getBoolean("werewolf.transformation-enabled", true);
        skinChangeEnabled = config.getBoolean("werewolf.skin-change-enabled", true);
        
        ConfigurationSection resourcePackSection = config.getConfigurationSection("resource-pack");
        if (resourcePackSection != null) {
            // Load server settings
            ConfigurationSection serverSection = resourcePackSection.getConfigurationSection("server");
            if (serverSection != null) {
                resourcePackServerEnabled = serverSection.getBoolean("enabled", true);
                resourcePackServerPort = serverSection.getInt("port", 8080);
                resourcePackServerHost = serverSection.getString("host", "localhost");
            } else {
                resourcePackServerEnabled = true;
                resourcePackServerPort = 8080;
                resourcePackServerHost = "localhost";
            }
            
            // Load resource pack settings
            resourcePackUrl = resourcePackSection.getString("url", "");
            resourcePackHash = resourcePackSection.getString("hash", "");
            resourcePackRequired = resourcePackSection.getBoolean("required", false);
        } else {
            resourcePackServerEnabled = true;
            resourcePackServerPort = 8080;
            resourcePackServerHost = "localhost";
            resourcePackUrl = "";
            resourcePackHash = "";
            resourcePackRequired = false;
        }
    }
    
    /**
     * Loads Vampire integration settings from the configuration.
     */
    private void loadVampireIntegrationSettings() {
        ConfigurationSection vampireSection = config.getConfigurationSection("vampire-integration");
        if (vampireSection != null) {
            vampireIntegrationEnabled = vampireSection.getBoolean("enabled", true);
            preventVampireConflict = vampireSection.getBoolean("prevent-conflict", true);
        } else {
            vampireIntegrationEnabled = true;
            preventVampireConflict = true;
        }
    }
    
    /**
     * Loads skin-related settings from the configuration.
     */
    private void loadSkinSettings() {
        skinTypes.clear();
        ConfigurationSection skinSection = config.getConfigurationSection("skins");
        if (skinSection != null) {
            for (String key : skinSection.getKeys(false)) {
                String value = skinSection.getString(key);
                if (value != null) {
                    skinTypes.put(key.toLowerCase(), value);
                }
            }
        }
    }
    
    // Getters
    public FileConfiguration getConfig() {
        return config;
    }
    
    public String getDatabaseType() {
        return databaseType;
    }
    
    public String getDatabaseUrl() {
        return databaseUrl;
    }
    
    public String getDatabaseUser() {
        return databaseUser;
    }
    
    public String getDatabasePassword() {
        return databasePassword;
    }
    
    public String getDatabaseTablePrefix() {
        return databaseTablePrefix;
    }
    
    public boolean isDebug() {
        return debug;
    }
    
    public boolean isSaveOnQuit() {
        return saveOnQuit;
    }
    
    public boolean isAutoSave() {
        return autoSave;
    }
    
    public int getAutoSaveInterval() {
        return autoSaveInterval;
    }
    
    public boolean isTransformationEnabled() {
        return transformationEnabled;
    }
    
    public boolean isSkinChangeEnabled() {
        return skinChangeEnabled;
    }
    
    public boolean isResourcePackServerEnabled() {
        return resourcePackServerEnabled;
    }
    
    public int getResourcePackServerPort() {
        return resourcePackServerPort;
    }
    
    public String getResourcePackServerHost() {
        return resourcePackServerHost;
    }
    
    public String getResourcePackUrl() {
        return resourcePackUrl;
    }
    
    public void setResourcePackUrl(String url) {
        this.resourcePackUrl = url;
        if (config != null) {
            config.set("resource-pack.url", url);
            try {
                config.save(configFile);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to save resource pack URL to config", e);
            }
        }
    }
    
    public String getResourcePackHash() {
        return resourcePackHash;
    }
    
    public void setResourcePackHash(String hash) {
        this.resourcePackHash = hash;
        if (config != null) {
            config.set("resource-pack.hash", hash);
            try {
                config.save(configFile);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to save resource pack hash to config", e);
            }
        }
    }
    
    public boolean isResourcePackRequired() {
        return resourcePackRequired;
    }
    
    public boolean isVampireIntegrationEnabled() {
        return vampireIntegrationEnabled;
    }
    
    public boolean isPreventVampireConflict() {
        return preventVampireConflict;
    }
    
    public Map<String, String> getSkinTypes() {
        return new HashMap<>(skinTypes);
    }
    
    public String getSkinType(String type) {
        return skinTypes.get(type.toLowerCase());
    }
}

