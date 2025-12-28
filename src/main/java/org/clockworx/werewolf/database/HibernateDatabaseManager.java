package org.clockworx.werewolf.database;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.clockworx.werewolf.WerewolfPlugin;
import org.clockworx.werewolf.entity.WerewolfPlayer;
import org.clockworx.werewolf.entity.WerewolfPlayerEntity;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

/**
 * Hibernate-based implementation of database operations for werewolf players.
 * Handles all database operations using Hibernate ORM.
 */
public class HibernateDatabaseManager {
    
    private final WerewolfPlugin plugin;
    private final Executor asyncExecutor;

    /**
     * Creates a new HibernateDatabaseManager.
     *
     * @param plugin The main WerewolfPlugin instance
     */
    public HibernateDatabaseManager(WerewolfPlugin plugin) {
        this.plugin = plugin;
        // Use Paper's async scheduler for better integration with server task tracking
        this.asyncExecutor = task -> 
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
    }

    /**
     * Initializes the database manager.
     * The SessionFactory is initialized lazily on first use.
     */
    public void initialize() {
        plugin.getLogger().log(Level.INFO, "HibernateDatabaseManager instance created. Schema managed by Flyway.");
    }

    /**
     * Shuts down the database manager.
     * The actual SessionFactory shutdown is handled centrally in WerewolfPlugin.onDisable.
     */
    public void shutdown() {
        plugin.getLogger().log(Level.INFO, "HibernateDatabaseManager shutdown called (actual SessionFactory shutdown managed elsewhere).");
    }

    /**
     * Helper method to execute transactional code safely using Paper's async scheduler.
     *
     * @param <T> The return type of the transaction
     * @param function The transaction function to execute
     * @return A CompletableFuture that completes with the transaction result
     */
    private <T> CompletableFuture<T> executeTransaction(TransactionFunction<T> function) {
        return CompletableFuture.supplyAsync(() -> {
            Transaction tx = null;
            try (Session session = HibernateConfig.getSessionFactory(plugin).openSession()) {
                tx = session.beginTransaction();
                T result = function.apply(session);
                tx.commit();
                return result;
            } catch (Exception e) {
                if (tx != null && tx.isActive()) {
                    try {
                        tx.rollback();
                    } catch (Exception rbEx) {
                        plugin.getLogger().log(Level.SEVERE, "Transaction rollback failed", rbEx);
                    }
                }
                plugin.getLogger().log(Level.SEVERE, "Database transaction failed", e);
                throw new RuntimeException("Database transaction failed", e);
            }
        }, asyncExecutor);
    }

    // Helper functional interface for transactions
    @FunctionalInterface
    private interface TransactionFunction<T> {
        T apply(Session session) throws Exception;
    }

    /**
     * Simplified execute function for operations returning Void.
     *
     * @param function The void transaction function to execute
     * @return A CompletableFuture that completes when the transaction is done
     */
    private CompletableFuture<Void> executeTransactionVoid(VoidTransactionFunction function) {
        return CompletableFuture.runAsync(() -> {
            Transaction tx = null;
            try (Session session = HibernateConfig.getSessionFactory(plugin).openSession()) {
                tx = session.beginTransaction();
                function.apply(session);
                tx.commit();
            } catch (Exception e) {
                if (tx != null && tx.isActive()) {
                    try {
                        tx.rollback();
                    } catch (Exception rbEx) {
                        plugin.getLogger().log(Level.SEVERE, "Transaction rollback failed", rbEx);
                    }
                }
                plugin.getLogger().log(Level.SEVERE, "Database transaction failed", e);
                throw new RuntimeException("Database transaction failed", e);
            }
        }, asyncExecutor);
    }

    // Helper functional interface for void transactions
    @FunctionalInterface
    private interface VoidTransactionFunction {
        void apply(Session session) throws Exception;
    }

    /**
     * Gets a player's werewolf data from the database.
     *
     * @param uuid The player's UUID
     * @return A CompletableFuture that completes with the player's werewolf data, or null if not found
     */
    public CompletableFuture<WerewolfPlayer> getPlayer(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = HibernateConfig.getSessionFactory(plugin).openSession()) {
                WerewolfPlayerEntity entity = session.get(WerewolfPlayerEntity.class, uuid);
                return entity != null ? convertToWerewolfPlayer(entity) : null;
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error getting player from database: " + uuid, e);
                return null;
            }
        }, asyncExecutor);
    }

    /**
     * Saves a player's werewolf data to the database.
     *
     * @param player The player's werewolf data
     * @return A CompletableFuture that completes when the save is done
     */
    public CompletableFuture<Void> savePlayer(WerewolfPlayer player) {
        return executeTransactionVoid(session -> {
            WerewolfPlayerEntity entity = session.get(WerewolfPlayerEntity.class, player.getUuid());
            if (entity == null) {
                entity = convertToEntity(player);
                session.persist(entity);
            } else {
                updateEntityFromPlayer(entity, player);
                session.merge(entity);
            }
        });
    }

    /**
     * Deletes a player's werewolf data from the database.
     *
     * @param uuid The player's UUID
     * @return A CompletableFuture that completes when the deletion is done
     */
    public CompletableFuture<Void> deletePlayer(UUID uuid) {
        return executeTransactionVoid(session -> {
            WerewolfPlayerEntity entity = session.get(WerewolfPlayerEntity.class, uuid);
            if (entity != null) {
                session.remove(entity);
            }
        });
    }

    /**
     * Gets all werewolf players from the database.
     *
     * @return A CompletableFuture that completes with a list of all werewolf players
     */
    public CompletableFuture<List<WerewolfPlayer>> getAllWerewolves() {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = HibernateConfig.getSessionFactory(plugin).openSession()) {
                Query<WerewolfPlayerEntity> query = session.createQuery(
                    "FROM WerewolfPlayerEntity WHERE isWerewolf = true", 
                    WerewolfPlayerEntity.class
                );
                List<WerewolfPlayerEntity> entities = query.getResultList();
                return entities.stream()
                    .map(this::convertToWerewolfPlayer)
                    .collect(Collectors.toList());
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error getting all werewolves from database", e);
                return List.of();
            }
        }, asyncExecutor);
    }
    
    /**
     * Gets all players from the database (werewolf or not).
     *
     * @return A CompletableFuture that completes with a list of all players
     */
    public CompletableFuture<List<WerewolfPlayer>> getAllPlayers() {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = HibernateConfig.getSessionFactory(plugin).openSession()) {
                Query<WerewolfPlayerEntity> query = session.createQuery(
                    "FROM WerewolfPlayerEntity", 
                    WerewolfPlayerEntity.class
                );
                List<WerewolfPlayerEntity> entities = query.getResultList();
                return entities.stream()
                    .map(this::convertToWerewolfPlayer)
                    .collect(Collectors.toList());
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error getting all players from database", e);
                return List.of();
            }
        }, asyncExecutor);
    }

    /**
     * Checks if a player is a werewolf.
     *
     * @param uuid The player's UUID
     * @return A CompletableFuture that completes with true if the player is a werewolf, false otherwise
     */
    public CompletableFuture<Boolean> isWerewolf(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = HibernateConfig.getSessionFactory(plugin).openSession()) {
                WerewolfPlayerEntity entity = session.get(WerewolfPlayerEntity.class, uuid);
                return entity != null && entity.isWerewolf();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error checking if player is werewolf: " + uuid, e);
                return false;
            }
        }, asyncExecutor);
    }

    /**
     * Converts a WerewolfPlayerEntity to a WerewolfPlayer POJO.
     *
     * @param entity The entity to convert
     * @return The WerewolfPlayer POJO
     */
    private WerewolfPlayer convertToWerewolfPlayer(WerewolfPlayerEntity entity) {
        WerewolfPlayer player = new WerewolfPlayer(entity.getUuid(), entity.getName());
        player.setWerewolfInternal(entity.isWerewolf());
        
        try {
            player.setTransformationState(WerewolfPlayer.TransformationState.valueOf(
                entity.getTransformationState()));
        } catch (IllegalArgumentException e) {
            player.setTransformationState(WerewolfPlayer.TransformationState.HUMAN);
        }
        
        try {
            player.setWerewolfType(WerewolfPlayer.WerewolfType.valueOf(
                entity.getWerewolfType()));
        } catch (IllegalArgumentException e) {
            player.setWerewolfType(WerewolfPlayer.WerewolfType.ALPHA);
        }
        
        player.setOriginalSkinValue(entity.getOriginalSkinValue());
        player.setOriginalSkinSignature(entity.getOriginalSkinSignature());
        player.setLastTransformationTime(entity.getLastTransformationTime());
        player.setLastCureTime(entity.getLastCureTime());
        
        return player;
    }

    /**
     * Converts a WerewolfPlayer POJO to a WerewolfPlayerEntity.
     *
     * @param player The player POJO to convert
     * @return The entity
     */
    private WerewolfPlayerEntity convertToEntity(WerewolfPlayer player) {
        WerewolfPlayerEntity entity = new WerewolfPlayerEntity(player.getUuid(), player.getName());
        updateEntityFromPlayer(entity, player);
        return entity;
    }

    /**
     * Updates an entity with data from a WerewolfPlayer POJO.
     *
     * @param entity The entity to update
     * @param player The player POJO with the new data
     */
    private void updateEntityFromPlayer(WerewolfPlayerEntity entity, WerewolfPlayer player) {
        entity.setName(player.getName());
        entity.setWerewolf(player.isWerewolf());
        entity.setTransformationState(player.getTransformationState().name());
        entity.setWerewolfType(player.getWerewolfType().name());
        entity.setOriginalSkinValue(player.getOriginalSkinValue());
        entity.setOriginalSkinSignature(player.getOriginalSkinSignature());
        entity.setLastTransformationTime(player.getLastTransformationTime());
        entity.setLastCureTime(player.getLastCureTime());
    }
}

