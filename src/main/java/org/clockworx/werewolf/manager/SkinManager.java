package org.clockworx.werewolf.manager;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.clockworx.werewolf.WerewolfPlugin;
import org.clockworx.werewolf.entity.WerewolfPlayer;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;

/**
 * Manages werewolf skin changes using Paper API and texture values.
 * Loads skin textures from skins.yml (texture values from MineSkin.org).
 * Handles applying and removing werewolf skins from players.
 */
public class SkinManager {
    
    private final WerewolfPlugin plugin;
    private final Map<String, ProfileProperty> werewolfSkins;
    private FileConfiguration skinsConfig;
    
    /**
     * Creates a new SkinManager.
     *
     * @param plugin The WerewolfPlugin instance.
     */
    public SkinManager(WerewolfPlugin plugin) {
        this.plugin = plugin;
        this.werewolfSkins = new HashMap<>();
        this.skinsConfig = null;
        loadSkins();
    }
    
    /**
     * Loads werewolf skin textures from skins.yml.
     * Texture values should be obtained from MineSkin.org.
     */
    private void loadSkins() {
        File skinsFile = new File(plugin.getDataFolder(), "skins.yml");
        
        // Create default skins.yml if it doesn't exist
        if (!skinsFile.exists()) {
            plugin.saveResource("skins.yml", false);
            plugin.getLogger().info("Created default skins.yml. Please add texture values from MineSkin.org");
        }
        
        // Load skins.yml
        skinsConfig = YamlConfiguration.loadConfiguration(skinsFile);
        ConfigurationSection skinsSection = skinsConfig.getConfigurationSection("skins");
        
        if (skinsSection == null) {
            plugin.getLogger().warning("No 'skins' section found in skins.yml. No skins will be loaded.");
            return;
        }
        
        int loadedCount = 0;
        for (String skinName : skinsSection.getKeys(false)) {
            ConfigurationSection skinSection = skinsSection.getConfigurationSection(skinName);
            if (skinSection != null) {
                String value = skinSection.getString("value", "");
                String signature = skinSection.getString("signature", "");
                
                if (value != null && !value.isEmpty()) {
                    // Create ProfileProperty with texture value
                    ProfileProperty skinProperty = new ProfileProperty("textures", value, signature != null ? signature : "");
                    werewolfSkins.put(skinName.toLowerCase(), skinProperty);
                    loadedCount++;
                    plugin.getLogger().info("Loaded skin texture: " + skinName);
                } else {
                    plugin.getLogger().warning("Skin '" + skinName + "' has no texture value. Skipping.");
                }
            }
        }
        
        if (loadedCount == 0) {
            plugin.getLogger().warning("No valid skin textures loaded from skins.yml. " +
                "Please add texture values from MineSkin.org to skins.yml");
        } else {
            plugin.getLogger().info("Loaded " + loadedCount + " werewolf skin texture(s).");
        }
    }
    
    /**
     * Applies a werewolf skin to a player.
     * Stores the original skin for later restoration.
     *
     * @param player The player to apply the skin to.
     * @param werewolfPlayer The WerewolfPlayer data.
     * @return True if the skin was applied successfully, false otherwise.
     */
    public boolean applySkin(Player player, WerewolfPlayer werewolfPlayer) {
        if (player == null || werewolfPlayer == null) {
            return false;
        }
        
        if (!plugin.getWerewolfConfig().isSkinChangeEnabled()) {
            return false;
        }
        
        try {
            // Get the player's current profile
            PlayerProfile profile = player.getPlayerProfile();
            
            // Store original skin if not already stored
            if (werewolfPlayer.getOriginalSkinValue() == null) {
                ProfileProperty textures = profile.getProperties().stream()
                    .filter(prop -> prop.getName().equals("textures"))
                    .findFirst()
                    .orElse(null);
                
                if (textures != null) {
                    werewolfPlayer.setOriginalSkinValue(textures.getValue());
                    werewolfPlayer.setOriginalSkinSignature(textures.getSignature());
                }
            }
            
            // Get the werewolf type and corresponding skin
            String skinType = werewolfPlayer.getWerewolfType().name().toLowerCase();
            ProfileProperty werewolfSkin = getWerewolfSkin(skinType);
            
            if (werewolfSkin != null) {
                // Remove existing textures property
                profile.getProperties().removeIf(prop -> prop.getName().equals("textures"));
                
                // Add the werewolf skin
                profile.getProperties().add(werewolfSkin);
                
                // Apply the updated profile
                player.setPlayerProfile(profile);
                
                // Apply resource pack if configured
                applyResourcePack(player);
                
                plugin.getLogger().info("Applied werewolf skin to player: " + player.getName());
                return true;
            } else {
                plugin.getLogger().warning("No werewolf skin found for type: " + skinType);
                return false;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to apply werewolf skin to player: " + player.getName(), e);
            return false;
        }
    }
    
    /**
     * Removes the werewolf skin and restores the player's original skin.
     *
     * @param player The player to restore the skin for.
     * @param werewolfPlayer The WerewolfPlayer data.
     * @return True if the skin was restored successfully, false otherwise.
     */
    public boolean removeSkin(Player player, WerewolfPlayer werewolfPlayer) {
        if (player == null || werewolfPlayer == null) {
            return false;
        }
        
        try {
            // Get the player's current profile
            PlayerProfile profile = player.getPlayerProfile();
            
            // Remove werewolf skin textures
            profile.getProperties().removeIf(prop -> prop.getName().equals("textures"));
            
            // Restore original skin if available
            if (werewolfPlayer.getOriginalSkinValue() != null) {
                String signature = werewolfPlayer.getOriginalSkinSignature();
                ProfileProperty originalSkin = new ProfileProperty("textures", 
                    werewolfPlayer.getOriginalSkinValue(), 
                    signature != null ? signature : "");
                profile.getProperties().add(originalSkin);
            }
            
            // Apply the updated profile
            player.setPlayerProfile(profile);
            
            plugin.getLogger().info("Restored original skin for player: " + player.getName());
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to restore original skin for player: " + player.getName(), e);
            return false;
        }
    }
    
    /**
     * Gets the werewolf skin property for a given type.
     * Loads from skins.yml (texture values from MineSkin.org).
     *
     * @param skinType The werewolf type (alpha, witherfang, etc.).
     * @return The ProfileProperty for the skin, or null if not found.
     */
    private ProfileProperty getWerewolfSkin(String skinType) {
        return werewolfSkins.get(skinType.toLowerCase());
    }
    
    /**
     * Reloads skin textures from skins.yml.
     * Useful after updating skins.yml without restarting the server.
     */
    public void reloadSkins() {
        werewolfSkins.clear();
        loadSkins();
    }
    
    /**
     * Gets the number of loaded werewolf skins.
     *
     * @return The number of loaded skins.
     */
    public int getLoadedSkinCount() {
        return werewolfSkins.size();
    }
    
    /**
     * Applies the resource pack to a player if configured.
     *
     * @param player The player to apply the resource pack to.
     */
    private void applyResourcePack(Player player) {
        String resourcePackUrl = null;
        String hash = null;
        byte[] hashBytes = null;
        
        // Try to use ResourcePackServer if available
        org.clockworx.werewolf.server.ResourcePackServer server = plugin.getResourcePackServer();
        if (server != null && server.isStarted()) {
            String host = plugin.getWerewolfConfig().getResourcePackServerHost();
            resourcePackUrl = server.getResourcePackUrl(host);
            hash = server.getResourcePackHashHex();
            hashBytes = server.getResourcePackHash();
        }
        
        // Fall back to config values if server not available
        if (resourcePackUrl == null || resourcePackUrl.isEmpty()) {
            resourcePackUrl = plugin.getWerewolfConfig().getResourcePackUrl();
        }
        
        if (hash == null || hash.isEmpty()) {
            hash = plugin.getWerewolfConfig().getResourcePackHash();
        }
        
        if (resourcePackUrl != null && !resourcePackUrl.isEmpty()) {
            boolean required = plugin.getWerewolfConfig().isResourcePackRequired();
            
            try {
                // Paper API method signatures:
                // - setResourcePack(String url, byte[] hash)
                // - setResourcePack(String url)
                // The 'required' parameter is handled via server.properties or a different mechanism
                if (hashBytes != null) {
                    // Use byte array hash from server if available
                    player.setResourcePack(resourcePackUrl, hashBytes);
                } else if (hash != null && !hash.isEmpty()) {
                    // Convert hex string hash to byte array
                    hashBytes = hexStringToByteArray(hash);
                    player.setResourcePack(resourcePackUrl, hashBytes);
                } else {
                    // No hash provided, use simple method
                    player.setResourcePack(resourcePackUrl);
                }
                
                // Note: The 'required' parameter is typically set in server.properties
                // or handled by the server configuration, not via the API method
                if (required) {
                    plugin.getLogger().info("Resource pack applied to " + player.getName() + " (required: set in server.properties)");
                } else {
                    plugin.getLogger().info("Resource pack applied to " + player.getName());
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to apply resource pack to player: " + player.getName(), e);
            }
        }
    }
    
    /**
     * Converts a hex string to a byte array.
     *
     * @param hex The hex string.
     * @return The byte array.
     */
    private byte[] hexStringToByteArray(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }
}

