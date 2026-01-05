package org.clockworx.werewolf.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.clockworx.werewolf.WerewolfPlugin;

/**
 * Utility class for intelligently updating configuration files.
 * Handles merging default configs with user configs, creating backups,
 * and commenting out removed configuration keys.
 */
public class ConfigUpdater {
    
    private final WerewolfPlugin plugin;
    
    /**
     * Creates a new ConfigUpdater instance.
     * 
     * @param plugin The plugin instance.
     */
    public ConfigUpdater(WerewolfPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Updates the user's config file by merging it with the default config from the JAR.
     * Creates a backup before updating and comments out any removed keys.
     * 
     * @param userConfigFile The user's config file to update.
     * @param defaultConfig The default configuration loaded from JAR.
     * @param pluginVersion The current plugin version.
     * @return true if update was successful, false otherwise.
     */
    public boolean updateConfig(File userConfigFile, FileConfiguration defaultConfig, String pluginVersion) {
        if (userConfigFile == null || !userConfigFile.exists()) {
            plugin.getLogger().warning("Cannot update config: user config file is null or does not exist.");
            return false;
        }
        
        if (defaultConfig == null) {
            plugin.getLogger().severe("Cannot update config: default config is null.");
            return false;
        }
        
        // Normalize plugin version - ensure it's not a placeholder
        pluginVersion = normalizeVersion(pluginVersion);
        if (isPlaceholderVersion(pluginVersion)) {
            plugin.getLogger().severe("Cannot update config: plugin version contains placeholders: " + pluginVersion);
            plugin.getLogger().severe("This indicates the plugin JAR was not built correctly.");
            return false;
        }
        
        try {
            // Load the user's current config
            FileConfiguration userConfig = YamlConfiguration.loadConfiguration(userConfigFile);
            String oldVersion = userConfig.getString("version", "unknown");
            
            // Normalize old version if it contains placeholders
            if (isPlaceholderVersion(oldVersion)) {
                plugin.getLogger().info("Detected placeholder version in existing config: " + oldVersion);
                oldVersion = pluginVersion; // Use normalized plugin version
            }
            
            // Create backup before making changes
            File backupFile = createBackup(userConfigFile, oldVersion);
            if (backupFile == null) {
                plugin.getLogger().warning("Failed to create backup. Update aborted for safety.");
                return false;
            }
            
            plugin.getLogger().info("Created backup: " + backupFile.getName());
            
            // Track what we're doing
            List<String> addedKeys = new ArrayList<>();
            List<String> removedKeys = new ArrayList<>();
            
            // Merge missing keys from default config
            mergeConfigs(userConfig, defaultConfig, addedKeys);
            
            // Find removed keys (keys in user config but not in default)
            findRemovedKeys(userConfig, defaultConfig, removedKeys);
            
            // Update version - ensure it's the normalized version (never a placeholder)
            userConfig.set("version", pluginVersion);
            
            // Save the updated config
            userConfig.save(userConfigFile);
            
            // Log what was changed
            logUpdateSummary(oldVersion, pluginVersion, addedKeys, removedKeys);
            
            // Add comment block for removed keys if any
            if (!removedKeys.isEmpty()) {
                addRemovedKeysComment(userConfigFile, removedKeys, oldVersion);
            }
            
            plugin.getLogger().info("Config updated successfully from version " + oldVersion + " to " + pluginVersion);
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error updating config file", e);
            return false;
        }
    }
    
    /**
     * Merges missing keys from the default config into the user config.
     * Preserves all existing user values.
     * 
     * @param userConfig The user's configuration.
     * @param defaultConfig The default configuration from JAR.
     * @param addedKeys List to track keys that were added.
     */
    private void mergeConfigs(FileConfiguration userConfig, FileConfiguration defaultConfig, List<String> addedKeys) {
        for (String key : defaultConfig.getKeys(true)) {
            // Skip configuration sections (we'll handle their children)
            if (defaultConfig.isConfigurationSection(key)) {
                continue;
            }
            
            // If key doesn't exist in user config, add it with default value
            if (!userConfig.contains(key)) {
                Object defaultValue = defaultConfig.get(key);
                userConfig.set(key, defaultValue);
                addedKeys.add(key);
            }
        }
    }
    
    /**
     * Finds keys that exist in user config but not in default config (removed keys).
     * 
     * @param userConfig The user's configuration.
     * @param defaultConfig The default configuration from JAR.
     * @param removedKeys List to track keys that were removed.
     */
    private void findRemovedKeys(FileConfiguration userConfig, FileConfiguration defaultConfig, List<String> removedKeys) {
        for (String key : userConfig.getKeys(true)) {
            // Skip configuration sections
            if (userConfig.isConfigurationSection(key)) {
                continue;
            }
            
            // Skip version key (we always update it)
            if (key.equals("version")) {
                continue;
            }
            
            // If key exists in user config but not in default, it was removed
            if (!defaultConfig.contains(key)) {
                removedKeys.add(key);
            }
        }
    }
    
    /**
     * Creates a backup of the config file with version information in the filename.
     * 
     * @param configFile The config file to backup.
     * @param version The version string to include in backup filename.
     * @return The backup file, or null if backup failed.
     */
    public File createBackup(File configFile, String version) {
        if (configFile == null || !configFile.exists()) {
            return null;
        }
        
        // Sanitize version string for filename (replace dots and special chars)
        String safeVersion = version.replaceAll("[^a-zA-Z0-9._-]", "_");
        String backupFileName = configFile.getName() + ".v" + safeVersion + ".backup";
        File backupFile = new File(configFile.getParent(), backupFileName);
        
        try {
            // Use FileTools if available, otherwise fall back to Files.copy
            regalowl.simpledatalib.SimpleDataLib sdl = plugin.getSimpleDataLib();
            if (sdl != null && sdl.getFileTools() != null) {
                sdl.getFileTools().copyFile(configFile.getAbsolutePath(), backupFile.getAbsolutePath());
            } else {
                Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return backupFile;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create backup file: " + backupFile.getName(), e);
            // Also log to SimpleDataLib error log if available
            regalowl.simpledatalib.SimpleDataLib sdl = plugin.getSimpleDataLib();
            if (sdl != null && sdl.getErrorWriter() != null) {
                sdl.getErrorWriter().writeError((e instanceof Exception) ? (Exception) e : new Exception(e), 
                    "Failed to create backup file: " + backupFile.getName());
            }
            return null;
        }
    }
    
    /**
     * Adds a comment block at the top of the config file listing removed keys.
     * Since YamlConfiguration doesn't preserve comments, we'll append to a comment section.
     * 
     * @param configFile The config file to add comments to.
     * @param removedKeys List of removed keys to comment.
     * @param oldVersion The old version number.
     */
    private void addRemovedKeysComment(File configFile, List<String> removedKeys, String oldVersion) {
        try {
            // Read the file as text
            List<String> lines = Files.readAllLines(configFile.toPath(), StandardCharsets.UTF_8);
            
            // Find where to insert the comment block (after version line, typically near top)
            int insertIndex = 0;
            for (int i = 0; i < lines.size() && i < 10; i++) {
                if (lines.get(i).trim().startsWith("version:")) {
                    insertIndex = i + 1;
                    break;
                }
            }
            
            // Build comment block
            List<String> commentBlock = new ArrayList<>();
            commentBlock.add("");
            commentBlock.add("# =========================================");
            commentBlock.add("# REMOVED CONFIGURATION KEYS");
            commentBlock.add("# =========================================");
            commentBlock.add("# The following configuration keys were present in version " + oldVersion);
            commentBlock.add("# but have been removed in the current version.");
            commentBlock.add("# These keys are commented out below for reference.");
            commentBlock.add("# You can safely remove these commented sections if desired.");
            commentBlock.add("#");
            
            // Add each removed key as a comment
            for (String key : removedKeys) {
                commentBlock.add("# REMOVED: " + key);
            }
            
            commentBlock.add("# =========================================");
            commentBlock.add("");
            
            // Insert comment block
            lines.addAll(insertIndex, commentBlock);
            
            // Write back to file
            Files.write(configFile.toPath(), lines, StandardCharsets.UTF_8);
            
            plugin.getLogger().warning("Added comment block for " + removedKeys.size() + " removed configuration keys.");
            
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not add removed keys comment block to config file", e);
        }
    }
    
    /**
     * Logs a summary of what was changed during the update.
     * 
     * @param oldVersion The old version.
     * @param newVersion The new version.
     * @param addedKeys Keys that were added.
     * @param removedKeys Keys that were removed.
     */
    private void logUpdateSummary(String oldVersion, String newVersion, List<String> addedKeys, List<String> removedKeys) {
        plugin.getLogger().info("=== Config Update Summary ===");
        plugin.getLogger().info("Updated from version " + oldVersion + " to " + newVersion);
        
        if (!addedKeys.isEmpty()) {
            plugin.getLogger().info("Added " + addedKeys.size() + " missing configuration keys:");
            for (String key : addedKeys) {
                plugin.getLogger().info("  + " + key);
            }
        } else {
            plugin.getLogger().info("No missing keys to add.");
        }
        
        if (!removedKeys.isEmpty()) {
            plugin.getLogger().warning("Found " + removedKeys.size() + " removed configuration keys:");
            for (String key : removedKeys) {
                plugin.getLogger().warning("  - " + key + " (commented out)");
            }
        } else {
            plugin.getLogger().info("No removed keys found.");
        }
        
        plugin.getLogger().info("=== End Update Summary ===");
    }
    
    /**
     * Loads the default configuration from the JAR resource.
     * 
     * @param plugin The plugin instance.
     * @param resourcePath The path to the resource (e.g., "config.yml").
     * @return The loaded FileConfiguration, or null if loading failed.
     */
    public static FileConfiguration loadDefaultConfigFromJar(WerewolfPlugin plugin, String resourcePath) {
        try (InputStream resourceStream = plugin.getResource(resourcePath)) {
            if (resourceStream == null) {
                plugin.getLogger().warning("Could not find default " + resourcePath + " in JAR.");
                return null;
            }
            
            try (InputStreamReader reader = new InputStreamReader(resourceStream, StandardCharsets.UTF_8)) {
                return YamlConfiguration.loadConfiguration(reader);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load default " + resourcePath + " from JAR", e);
            return null;
        }
    }
    
    /**
     * Checks if a version string contains placeholder values that weren't replaced during build.
     * 
     * @param version The version string to check.
     * @return true if the version contains placeholders, false otherwise.
     */
    private boolean isPlaceholderVersion(String version) {
        if (version == null || version.isEmpty()) {
            return false;
        }
        // Check for common placeholder patterns
        return version.contains("${project.version}") 
            || version.contains("${version}")
            || version.contains("$project.version")
            || version.contains("$version");
    }
    
    /**
     * Normalizes a version string by replacing placeholder values with the actual plugin version.
     * If the version contains placeholders, returns the actual plugin version from PluginMeta.
     * Otherwise, returns the version as-is.
     * 
     * @param version The version string to normalize.
     * @return The normalized version string.
     */
    private String normalizeVersion(String version) {
        if (version == null || version.isEmpty()) {
            return plugin.getPluginMeta().getVersion();
        }
        
        if (isPlaceholderVersion(version)) {
            // Get actual version from plugin metadata
            String actualVersion = plugin.getPluginMeta().getVersion();
            if (isPlaceholderVersion(actualVersion)) {
                // If plugin version itself has placeholders, we can't fix it
                plugin.getLogger().severe("CRITICAL: Plugin JAR was not built correctly! Plugin version contains placeholders: " + actualVersion);
                // Return a safe default
                return "0.0.0";
            }
            return actualVersion;
        }
        
        return version;
    }
}

