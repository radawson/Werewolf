package org.clockworx.werewolf.database;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.clockworx.werewolf.WerewolfPlugin;
import org.clockworx.werewolf.entity.WerewolfPlayer;

/**
 * Database manager for werewolf player data.
 * Handles all database operations using Hibernate and SimpleDataLib.
 */
public class DatabaseManager {
    
    private HibernateDatabaseManager hibernateManager;
    
    /**
     * Constructor for DatabaseManager.
     *
     * @param plugin The WerewolfPlugin instance.
     */
    public DatabaseManager(WerewolfPlugin plugin) {
        this.hibernateManager = new HibernateDatabaseManager(plugin);
    }
    
    /**
     * Initializes the database connection and creates necessary tables.
     * This is called automatically during plugin initialization.
     */
    public void initialize() {
        hibernateManager.initialize();
    }
    
    /**
     * Shuts down the database connection.
     * This should be called during plugin shutdown.
     */
    public void shutdown() {
        if (hibernateManager != null) {
            hibernateManager.shutdown();
        }
    }
    
    /**
     * Gets a player's werewolf data from the database.
     *
     * @param uuid The player's UUID.
     * @return A CompletableFuture that completes with the player's werewolf data, or null if not found.
     */
    public CompletableFuture<WerewolfPlayer> getPlayer(UUID uuid) {
        return hibernateManager.getPlayer(uuid);
    }
    
    /**
     * Saves a player's werewolf data to the database.
     *
     * @param player The player's werewolf data.
     * @return A CompletableFuture that completes when the save is done.
     */
    public CompletableFuture<Void> savePlayer(WerewolfPlayer player) {
        return hibernateManager.savePlayer(player);
    }
    
    /**
     * Deletes a player's werewolf data from the database.
     *
     * @param uuid The player's UUID.
     * @return A CompletableFuture that completes when the deletion is done.
     */
    public CompletableFuture<Void> deletePlayer(UUID uuid) {
        return hibernateManager.deletePlayer(uuid);
    }
    
    /**
     * Gets all werewolf players from the database.
     *
     * @return A CompletableFuture that completes with a list of all werewolf players.
     */
    public CompletableFuture<List<WerewolfPlayer>> getAllWerewolves() {
        return hibernateManager.getAllWerewolves();
    }
    
    /**
     * Gets all players from the database (werewolf or not).
     *
     * @return A CompletableFuture that completes with a list of all players.
     */
    public CompletableFuture<List<WerewolfPlayer>> getAllPlayers() {
        return hibernateManager.getAllPlayers();
    }
    
    /**
     * Checks if a player is a werewolf.
     *
     * @param uuid The player's UUID.
     * @return A CompletableFuture that completes with true if the player is a werewolf, false otherwise.
     */
    public CompletableFuture<Boolean> isWerewolf(UUID uuid) {
        return hibernateManager.isWerewolf(uuid);
    }
}

