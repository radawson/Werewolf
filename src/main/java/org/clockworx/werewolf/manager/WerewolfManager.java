package org.clockworx.werewolf.manager;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.clockworx.werewolf.WerewolfPlugin;
import org.clockworx.werewolf.config.WerewolfConfig;
import org.clockworx.werewolf.database.DatabaseManager;
import org.clockworx.werewolf.entity.WerewolfPlayer;
import org.clockworx.werewolf.event.WerewolfCureEvent;
import org.clockworx.werewolf.event.WerewolfTransformationEvent;
import org.clockworx.werewolf.integration.VampireIntegration;

/**
 * Core manager for werewolf functionality.
 * Handles player transformations, state management, and database operations.
 */
public class WerewolfManager {
    
    private final WerewolfPlugin plugin;
    private final WerewolfConfig config;
    private final DatabaseManager databaseManager;
    private final SkinManager skinManager;
    private final VampireIntegration vampireIntegration;
    
    // Use ConcurrentHashMap as loading/saving might happen async
    private final Map<UUID, WerewolfPlayer> werewolfCache = new ConcurrentHashMap<>();
    
    /**
     * Creates a new WerewolfManager.
     *
     * @param plugin The WerewolfPlugin instance.
     * @param skinManager The SkinManager instance.
     * @param vampireIntegration The VampireIntegration instance (can be null).
     */
    public WerewolfManager(WerewolfPlugin plugin, SkinManager skinManager, VampireIntegration vampireIntegration) {
        this.plugin = plugin;
        this.config = plugin.getWerewolfConfig();
        this.databaseManager = plugin.getDatabaseManager();
        this.skinManager = skinManager;
        this.vampireIntegration = vampireIntegration;
    }
    
    /**
     * Handles player joining the server.
     * Loads their data from the database and caches it.
     *
     * @param player The player who joined.
     */
    public void handlePlayerJoin(Player player) {
        UUID uuid = player.getUniqueId();
        String playerName = player.getName();
        plugin.debug("Handling join for player: " + playerName + " (" + uuid + ")");
        
        // Create and cache a default/placeholder object immediately
        WerewolfPlayer cachedWP = werewolfCache.computeIfAbsent(uuid, key -> {
            plugin.debug("Creating initial cache entry for " + playerName);
            return new WerewolfPlayer(uuid, playerName);
        });
        
        // Load actual player data from the database asynchronously
        databaseManager.getPlayer(uuid).thenAcceptAsync(loadedWerewolfPlayer -> {
            if (loadedWerewolfPlayer != null) {
                plugin.debug("Loaded data for player: " + playerName + ". Updating cached object.");
                loadedWerewolfPlayer.setName(playerName); // Ensure name is current
                
                // Update the cached object with loaded data
                updateCachedPlayer(cachedWP, loadedWerewolfPlayer);
                
                // Schedule skin/effects update on main thread if player is werewolf
                if (cachedWP.isWerewolf()) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Player onlinePlayer = Bukkit.getPlayer(uuid);
                        if (onlinePlayer != null && onlinePlayer.isOnline()) {
                            applyTransformation(onlinePlayer, cachedWP);
                        }
                    });
                }
            } else {
                plugin.debug("No existing data found for " + playerName + ". Initial cache entry is sufficient.");
            }
        }).exceptionally(ex -> {
            plugin.error("Failed to load player data for " + playerName + ". Using default cache entry.", ex);
            return null;
        });
    }
    
    /**
     * Handles player quitting the server.
     * Saves their data to the database and removes them from the cache.
     *
     * @param player The player who quit.
     */
    public void handlePlayerQuit(Player player) {
        UUID uuid = player.getUniqueId();
        String playerName = player.getName();
        plugin.debug("Handling quit for player: " + playerName + " (" + uuid + ")");
        
        WerewolfPlayer werewolfPlayer = werewolfCache.remove(uuid);
        if (werewolfPlayer != null) {
            plugin.debug("Saving data for quitting player: " + werewolfPlayer.getName());
            saveOrUpdateWerewolfPlayer(werewolfPlayer);
        } else {
            plugin.debug("Player " + playerName + " not found in cache during quit handling.");
        }
    }
    
    /**
     * Sets a player's werewolf status.
     *
     * @param uuid The player's UUID.
     * @param isWerewolf True to make werewolf, false to cure.
     * @param reason Optional reason for the change (for logging/messaging).
     * @return True if the status was changed, false otherwise.
     */
    public boolean setWerewolfStatus(UUID uuid, boolean isWerewolf, String reason) {
        WerewolfPlayer wp = getOrCreateWerewolfPlayer(uuid);
        if (wp == null) {
            plugin.getLogger().warning("Cannot set werewolf status for invalid UUID: " + uuid);
            return false;
        }
        
        // Check for vampire conflict if transforming to werewolf
        if (isWerewolf && vampireIntegration != null && 
            vampireIntegration.shouldPreventTransformation(Bukkit.getPlayer(uuid))) {
            plugin.getLogger().warning("Cannot transform player " + wp.getName() + " to werewolf: player is a vampire.");
            return false;
        }
        
        boolean changed = wp.isWerewolf() != isWerewolf;
        if (changed) {
            // Call the internal setter on the POJO
            wp.setWerewolfInternal(isWerewolf);
            
            if (isWerewolf) {
                wp.setLastTransformationTime(System.currentTimeMillis());
            } else {
                wp.setLastCureTime(System.currentTimeMillis());
            }
            
            Player player = wp.getPlayer();
            if (player != null && player.isOnline()) {
                if (isWerewolf) {
                    applyTransformation(player, wp);
                    // Fire transformation event
                    WerewolfTransformationEvent event = new WerewolfTransformationEvent(player, wp, reason);
                    Bukkit.getPluginManager().callEvent(event);
                } else {
                    removeTransformation(player, wp);
                    // Fire cure event
                    WerewolfCureEvent event = new WerewolfCureEvent(player, wp, reason);
                    Bukkit.getPluginManager().callEvent(event);
                }
            }
            
            plugin.debug("Set player " + wp.getName() + " werewolf status to " + isWerewolf);
            
            // Save change to DB
            saveOrUpdateWerewolfPlayer(wp);
            return true;
        } else {
            plugin.debug("Player " + wp.getName() + " already has werewolf status " + isWerewolf);
            return false;
        }
    }
    
    /**
     * Applies werewolf transformation to a player.
     * This includes skin changes and any effects.
     *
     * @param player The player to transform.
     * @param werewolfPlayer The WerewolfPlayer data.
     */
    public void applyTransformation(Player player, WerewolfPlayer werewolfPlayer) {
        if (player == null || werewolfPlayer == null || !player.isOnline()) {
            return;
        }
        
        if (!config.isTransformationEnabled()) {
            return;
        }
        
        // Apply skin change
        if (config.isSkinChangeEnabled()) {
            skinManager.applySkin(player, werewolfPlayer);
        }
        
        // Set transformation state
        werewolfPlayer.setTransformationState(WerewolfPlayer.TransformationState.WEREWOLF);
        
        plugin.debug("Applied transformation to player: " + player.getName());
    }
    
    /**
     * Removes werewolf transformation from a player.
     * This includes restoring the original skin.
     *
     * @param player The player to cure.
     * @param werewolfPlayer The WerewolfPlayer data.
     */
    public void removeTransformation(Player player, WerewolfPlayer werewolfPlayer) {
        if (player == null || werewolfPlayer == null || !player.isOnline()) {
            return;
        }
        
        // Remove skin change
        if (config.isSkinChangeEnabled()) {
            skinManager.removeSkin(player, werewolfPlayer);
        }
        
        // Set transformation state
        werewolfPlayer.setTransformationState(WerewolfPlayer.TransformationState.HUMAN);
        
        plugin.debug("Removed transformation from player: " + player.getName());
    }
    
    /**
     * Gets or creates the WerewolfPlayer object for a given UUID.
     * Tries the cache first, then the database, then creates a new object if not found.
     *
     * @param uuid The player's UUID.
     * @return The existing or newly created WerewolfPlayer object.
     */
    public WerewolfPlayer getOrCreateWerewolfPlayer(UUID uuid) {
        // Try cache first
        WerewolfPlayer wp = werewolfCache.get(uuid);
        if (wp != null) {
            return wp;
        }
        
        // Try to load from database (synchronously for immediate return)
        try {
            CompletableFuture<WerewolfPlayer> future = databaseManager.getPlayer(uuid);
            wp = future.get(); // Block until loaded
            if (wp != null) {
                werewolfCache.put(uuid, wp);
                return wp;
            }
        } catch (Exception e) {
            plugin.error("Error loading werewolf player from database: " + uuid, e);
        }
        
        // Create new player
        String name = "Unknown";
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            name = player.getName();
        } else {
            // Try offline player
            org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            if (offlinePlayer.hasPlayedBefore()) {
                name = offlinePlayer.getName();
            }
        }
        
        wp = new WerewolfPlayer(uuid, name);
        werewolfCache.put(uuid, wp);
        return wp;
    }
    
    /**
     * Gets a cached WerewolfPlayer by UUID.
     *
     * @param uuid The player's UUID.
     * @return The WerewolfPlayer if cached, null otherwise.
     */
    public WerewolfPlayer getCachedWerewolfPlayer(UUID uuid) {
        return werewolfCache.get(uuid);
    }
    
    /**
     * Checks if a player is a werewolf.
     *
     * @param uuid The player's UUID.
     * @return True if the player is a werewolf, false otherwise.
     */
    public boolean isWerewolf(UUID uuid) {
        WerewolfPlayer wp = getCachedWerewolfPlayer(uuid);
        return wp != null && wp.isWerewolf();
    }
    
    /**
     * Checks if a player is a vampire (via integration).
     *
     * @param player The player to check.
     * @return True if the player is a vampire, false otherwise.
     */
    public boolean isVampire(Player player) {
        if (vampireIntegration == null || !vampireIntegration.isAvailable()) {
            return false;
        }
        return vampireIntegration.isVampire(player);
    }
    
    /**
     * Loads all werewolf data from the database on startup.
     * Populates the initial cache.
     */
    public void loadWerewolves() {
        werewolfCache.clear();
        databaseManager.getAllWerewolves().thenAcceptAsync(players -> {
            if (players != null) {
                players.forEach(wp -> werewolfCache.put(wp.getUuid(), wp));
                plugin.getLogger().info("Loaded " + werewolfCache.size() + " werewolf player records from database.");
            } else {
                plugin.getLogger().warning("Failed to load player data from database (getAllWerewolves returned null).");
            }
        }).exceptionally(ex -> {
            plugin.error("Error loading player data from database.", ex);
            return null;
        });
    }
    
    /**
     * Gets all cached werewolf players.
     *
     * @return A list of all cached WerewolfPlayer objects.
     */
    public List<WerewolfPlayer> getAllCachedWerewolves() {
        return new java.util.ArrayList<>(werewolfCache.values());
    }
    
    /**
     * Saves a single WerewolfPlayer to the database.
     *
     * @param wp The WerewolfPlayer to save.
     */
    public void saveOrUpdateWerewolfPlayer(WerewolfPlayer wp) {
        if (wp == null) return;
        databaseManager.savePlayer(wp);
        // Update cache as well
        werewolfCache.put(wp.getUuid(), wp);
        plugin.debug("[WerewolfManager] Saved/Updated player data for UUID: " + wp.getUuid());
    }
    
    /**
     * Saves all currently cached werewolf player data to the database.
     * Usually called periodically or on shutdown.
     */
    public void saveAllWerewolves() {
        int count = 0;
        for (WerewolfPlayer wp : werewolfCache.values()) {
            databaseManager.savePlayer(wp).join();
            count++;
        }
        if (count > 0) {
            plugin.getLogger().info("Saved data for " + count + " werewolf players.");
        }
    }
    
    /**
     * Shuts down the manager and saves all pending data.
     */
    public void shutdown() {
        saveAllWerewolves();
        werewolfCache.clear();
    }
    
    /**
     * Updates a cached player object with data from a loaded player.
     *
     * @param cached The cached player object.
     * @param loaded The loaded player object with new data.
     */
    private void updateCachedPlayer(WerewolfPlayer cached, WerewolfPlayer loaded) {
        cached.setName(loaded.getName());
        cached.setWerewolfInternal(loaded.isWerewolf());
        cached.setTransformationState(loaded.getTransformationState());
        cached.setWerewolfType(loaded.getWerewolfType());
        cached.setOriginalSkinValue(loaded.getOriginalSkinValue());
        cached.setOriginalSkinSignature(loaded.getOriginalSkinSignature());
        cached.setLastTransformationTime(loaded.getLastTransformationTime());
        cached.setLastCureTime(loaded.getLastCureTime());
    }
}

