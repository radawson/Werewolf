package org.clockworx.werewolf.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.entity.Player;
import org.clockworx.werewolf.WerewolfPlugin;
import org.clockworx.werewolf.entity.WerewolfPlayer;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;

/**
 * Manages werewolf skin changes using Paper API and resource packs.
 * Handles applying and removing werewolf skins from players.
 */
public class SkinManager {
    
    private final WerewolfPlugin plugin;
    private final Map<String, ProfileProperty> werewolfSkins;
    
    /**
     * Creates a new SkinManager.
     *
     * @param plugin The WerewolfPlugin instance.
     */
    public SkinManager(WerewolfPlugin plugin) {
        this.plugin = plugin;
        this.werewolfSkins = new HashMap<>();
        loadSkins();
    }
    
    /**
     * Loads werewolf skin configurations from the config.
     * In a full implementation, this would load actual skin textures.
     */
    private void loadSkins() {
        // For now, we'll create placeholder skin properties
        // In a full implementation, these would be loaded from the resource pack
        // or from skin URLs/names in the config
        
        Map<String, String> skinTypes = plugin.getWerewolfConfig().getSkinTypes();
        for (Map.Entry<String, String> entry : skinTypes.entrySet()) {
            // Create a placeholder ProfileProperty
            // In reality, you would need actual skin texture data
            // This is a simplified version
            plugin.getLogger().info("Loaded skin type: " + entry.getKey() + " -> " + entry.getValue());
        }
        
        plugin.getLogger().info("Skin configurations loaded.");
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
     *
     * @param skinType The werewolf type (alpha, witherfang, etc.).
     * @return The ProfileProperty for the skin, or null if not found.
     */
    private ProfileProperty getWerewolfSkin(String skinType) {
        // This is a placeholder implementation
        // In a full implementation, you would:
        // 1. Load skin textures from the resource pack
        // 2. Convert them to ProfileProperty format
        // 3. Cache them for reuse
        
        // For now, return null to indicate skin loading needs to be implemented
        // The actual skin data would come from the resource pack or skin service
        return werewolfSkins.get(skinType.toLowerCase());
    }
    
    /**
     * Applies the resource pack to a player if configured.
     *
     * @param player The player to apply the resource pack to.
     */
    private void applyResourcePack(Player player) {
        String resourcePackUrl = plugin.getWerewolfConfig().getResourcePackUrl();
        if (resourcePackUrl != null && !resourcePackUrl.isEmpty()) {
            String hash = plugin.getWerewolfConfig().getResourcePackHash();
            boolean required = plugin.getWerewolfConfig().isResourcePackRequired();
            
            try {
                // Convert hash string to byte array if provided
                if (hash != null && !hash.isEmpty()) {
                    // Hash should be provided as hex string, convert to byte array
                    byte[] hashBytes = hexStringToByteArray(hash);
                    player.setResourcePack(resourcePackUrl, hashBytes, required);
                } else {
                    // Use the method that doesn't require hash
                    player.setResourcePack(resourcePackUrl, required);
                }
                plugin.getLogger().info("Applied resource pack to player: " + player.getName());
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

