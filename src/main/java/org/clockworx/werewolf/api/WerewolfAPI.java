package org.clockworx.werewolf.api;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.clockworx.werewolf.WerewolfPlugin;
import org.clockworx.werewolf.entity.WerewolfPlayer;

/**
 * Public API for the Werewolf plugin.
 * Provides methods for other plugins to interact with werewolf functionality.
 */
public class WerewolfAPI {
    
    private static WerewolfPlugin plugin;
    
    /**
     * Initializes the API with the plugin instance.
     * This is called automatically by the plugin.
     *
     * @param pluginInstance The WerewolfPlugin instance.
     */
    public static void initialize(WerewolfPlugin pluginInstance) {
        plugin = pluginInstance;
    }
    
    /**
     * Checks if a player is a werewolf.
     *
     * @param player The player to check.
     * @return True if the player is a werewolf, false otherwise.
     */
    public static boolean isWerewolf(Player player) {
        if (plugin == null || player == null) {
            return false;
        }
        return plugin.getWerewolfManager().isWerewolf(player.getUniqueId());
    }
    
    /**
     * Checks if a player (by UUID) is a werewolf.
     *
     * @param uuid The player's UUID.
     * @return True if the player is a werewolf, false otherwise.
     */
    public static boolean isWerewolf(UUID uuid) {
        if (plugin == null || uuid == null) {
            return false;
        }
        return plugin.getWerewolfManager().isWerewolf(uuid);
    }
    
    /**
     * Gets the WerewolfPlayer object for a player.
     *
     * @param player The player.
     * @return The WerewolfPlayer object, or null if not found.
     */
    public static WerewolfPlayer getWerewolfPlayer(Player player) {
        if (plugin == null || player == null) {
            return null;
        }
        return plugin.getWerewolfManager().getCachedWerewolfPlayer(player.getUniqueId());
    }
    
    /**
     * Gets the WerewolfPlayer object by UUID.
     *
     * @param uuid The player's UUID.
     * @return The WerewolfPlayer object, or null if not found.
     */
    public static WerewolfPlayer getWerewolfPlayer(UUID uuid) {
        if (plugin == null || uuid == null) {
            return null;
        }
        return plugin.getWerewolfManager().getCachedWerewolfPlayer(uuid);
    }
    
    /**
     * Checks if a player is an alpha werewolf.
     *
     * @param player The player to check.
     * @return True if the player is an alpha werewolf, false otherwise.
     */
    public static boolean isAlpha(Player player) {
        WerewolfPlayer wp = getWerewolfPlayer(player);
        if (wp == null) {
            return false;
        }
        return wp.isWerewolf() && 
               wp.getWerewolfType() == WerewolfPlayer.WerewolfType.ALPHA;
    }
    
    /**
     * Checks if a player is an alpha werewolf by UUID.
     *
     * @param uuid The player's UUID.
     * @return True if the player is an alpha werewolf, false otherwise.
     */
    public static boolean isAlpha(UUID uuid) {
        WerewolfPlayer wp = getWerewolfPlayer(uuid);
        if (wp == null) {
            return false;
        }
        return wp.isWerewolf() && 
               wp.getWerewolfType() == WerewolfPlayer.WerewolfType.ALPHA;
    }
}

