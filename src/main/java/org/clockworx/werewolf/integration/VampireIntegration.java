package org.clockworx.werewolf.integration;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.clockworx.werewolf.WerewolfPlugin;

/**
 * Handles integration with the Vampire plugin.
 * Provides methods to check if a player is a vampire and prevent conflicts.
 */
public class VampireIntegration {
    
    private final WerewolfPlugin plugin;
    private Plugin vampirePlugin;
    private boolean available;
    
    /**
     * Creates a new VampireIntegration instance.
     *
     * @param plugin The WerewolfPlugin instance.
     */
    public VampireIntegration(WerewolfPlugin plugin) {
        this.plugin = plugin;
        this.vampirePlugin = plugin.getServer().getPluginManager().getPlugin("Vampire");
        this.available = vampirePlugin != null && vampirePlugin.isEnabled();
        
        if (available) {
            plugin.getLogger().info("Vampire plugin integration enabled.");
        } else {
            plugin.getLogger().info("Vampire plugin not found or disabled. Integration unavailable.");
        }
    }
    
    /**
     * Checks if the Vampire plugin is available and enabled.
     *
     * @return True if Vampire plugin is available, false otherwise.
     */
    public boolean isAvailable() {
        return available;
    }
    
    /**
     * Checks if a player is a vampire.
     * This method attempts to use the Vampire plugin's API if available.
     *
     * @param player The player to check.
     * @return True if the player is a vampire, false otherwise.
     */
    public boolean isVampire(Player player) {
        if (!available || player == null) {
            return false;
        }
        
        try {
            // Try to access Vampire plugin's API
            // This assumes Vampire exposes a static method or manager
            // We'll use reflection to safely access it
            Class<?> vampirePluginClass = vampirePlugin.getClass();
            
            // Try to get VampirePlugin.getInstance()
            java.lang.reflect.Method getInstanceMethod = vampirePluginClass.getMethod("getInstance");
            Object vampireInstance = getInstanceMethod.invoke(null);
            
            // Try to get VampireManager
            java.lang.reflect.Method getManagerMethod = vampirePluginClass.getMethod("getVampireManager");
            Object vampireManager = getManagerMethod.invoke(vampireInstance);
            
            // Try to call isVampire method
            java.lang.reflect.Method isVampireMethod = vampireManager.getClass()
                .getMethod("isVampire", UUID.class);
            Boolean result = (Boolean) isVampireMethod.invoke(vampireManager, player.getUniqueId());
            
            return result != null && result;
        } catch (Exception e) {
            // If reflection fails, assume player is not a vampire
            plugin.debug("Failed to check vampire status via API: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Checks if a player is a vampire by UUID.
     *
     * @param uuid The player's UUID.
     * @return True if the player is a vampire, false otherwise.
     */
    public boolean isVampire(UUID uuid) {
        if (!available || uuid == null) {
            return false;
        }
        
        Player player = plugin.getServer().getPlayer(uuid);
        if (player != null) {
            return isVampire(player);
        }
        
        // If player is offline, we can't check via API
        // Return false as we can't determine status
        return false;
    }
    
    /**
     * Checks if transformation should be prevented due to vampire conflict.
     *
     * @param player The player attempting to transform.
     * @return True if transformation should be prevented, false otherwise.
     */
    public boolean shouldPreventTransformation(Player player) {
        if (!available || !plugin.getWerewolfConfig().isPreventVampireConflict()) {
            return false;
        }
        
        return isVampire(player);
    }
}

