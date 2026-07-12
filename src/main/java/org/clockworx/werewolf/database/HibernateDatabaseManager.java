package org.clockworx.werewolf.database;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.bukkit.plugin.IllegalPluginAccessException;
import org.clockworx.data.hibernate.HibernateSessionManager;
import org.clockworx.werewolf.WerewolfPlugin;
import org.clockworx.werewolf.entity.WerewolfPlayer;
import org.clockworx.werewolf.entity.WerewolfPlayerEntity;
import org.hibernate.query.Query;

/**
 * Hibernate-based implementation of database operations for werewolf players.
 * Handles all database operations using Hibernate ORM.
 *
 * <p>Session/transaction infrastructure is provided by the shared clockworx-data
 * library ({@link HibernateSessionManager}), which owns the lazily initialized
 * SessionFactory and provides async transaction helpers with the shutdown guards
 * originally implemented here (short-circuiting during shutdown and tolerating
 * classloader-teardown errors). This class contributes the Werewolf-specific
 * entity, queries, and entity/domain conversions.</p>
 */
public class HibernateDatabaseManager {
    
    private final WerewolfPlugin plugin;
    
    /** Shared session/transaction manager from the clockworx-data library. */
    private final HibernateSessionManager sessions;

    /**
     * Dedicated daemon pool for all DB work. NOT Bukkit's async scheduler: scheduler async tasks are
     * only dispatched once the server ticks (after onEnable), so a DB future joined during enable would
     * deadlock the main thread. A plain executor runs immediately at both enable- and run-time.
     */
    private final java.util.concurrent.ExecutorService dbPool =
            java.util.concurrent.Executors.newCachedThreadPool(r -> {
                Thread th = new Thread(r, "werewolf-db");
                th.setDaemon(true);
                return th;
            });

    /**
     * Creates a new HibernateDatabaseManager.
     *
     * @param plugin The main WerewolfPlugin instance
     */
    public HibernateDatabaseManager(WerewolfPlugin plugin) {
        this.plugin = plugin;
        Executor asyncExecutor = task -> {
            try {
                dbPool.execute(task);
            } catch (java.util.concurrent.RejectedExecutionException e) {
                task.run(); // pool shut down during disable — run inline
            }
        };
        this.sessions = new HibernateSessionManager(
                plugin.getWerewolfConfig().getDatabaseSettings(),
                List.of(WerewolfPlayerEntity.class),
                asyncExecutor,
                plugin.getLogger());
    }

    /**
     * @return true once shutdown of the shared session manager has begun
     */
    private boolean isShuttingDown() {
        return sessions != null && sessions.isShuttingDown();
    }

    /**
     * Initializes the database manager.
     * The SessionFactory is initialized lazily on first use.
     */
    public void initialize() {
        plugin.getLogger().log(Level.INFO, "HibernateDatabaseManager instance created. Schema managed by Flyway.");
    }

    /**
     * Shuts down the database manager: marks the shared session manager as
     * shutting down (so in-flight operations short-circuit) and closes the
     * SessionFactory and its connection pool.
     */
    public void shutdown() {
        sessions.shutdown();
        dbPool.shutdown();
        plugin.getLogger().log(Level.INFO, "HibernateDatabaseManager shut down.");
    }

    /**
     * Gets a player's werewolf data from the database.
     *
     * @param uuid The player's UUID
     * @return A CompletableFuture that completes with the player's werewolf data, or null if not found
     */
    public CompletableFuture<WerewolfPlayer> getPlayer(UUID uuid) {
        return sessions.executeRead(session -> {
            WerewolfPlayerEntity entity = session.get(WerewolfPlayerEntity.class, uuid);
            return entity != null ? convertToWerewolfPlayer(entity) : null;
        }, null);
    }

    /**
     * Saves a player's werewolf data to the database.
     *
     * @param player The player's werewolf data
     * @return A CompletableFuture that completes when the save is done
     */
    public CompletableFuture<Void> savePlayer(WerewolfPlayer player) {
        return sessions.executeTransactionVoid(session -> {
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
        return sessions.executeTransactionVoid(session -> {
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
        return sessions.executeRead(session -> {
            Query<WerewolfPlayerEntity> query = session.createQuery(
                "FROM WerewolfPlayerEntity WHERE isWerewolf = true", 
                WerewolfPlayerEntity.class
            );
            List<WerewolfPlayerEntity> entities = query.getResultList();
            // Empty list is a valid result (no werewolves in database)
            return entities.stream()
                .map(this::convertToWerewolfPlayer)
                .collect(Collectors.toList());
        }, List.of());
    }
    
    /**
     * Gets all players from the database (werewolf or not).
     *
     * @return A CompletableFuture that completes with a list of all players
     */
    public CompletableFuture<List<WerewolfPlayer>> getAllPlayers() {
        return sessions.executeRead(session -> {
            Query<WerewolfPlayerEntity> query = session.createQuery(
                "FROM WerewolfPlayerEntity", 
                WerewolfPlayerEntity.class
            );
            List<WerewolfPlayerEntity> entities = query.getResultList();
            return entities.stream()
                .map(this::convertToWerewolfPlayer)
                .collect(Collectors.toList());
        }, List.of());
    }

    /**
     * Checks if a player is a werewolf.
     *
     * @param uuid The player's UUID
     * @return A CompletableFuture that completes with true if the player is a werewolf, false otherwise
     */
    public CompletableFuture<Boolean> isWerewolf(UUID uuid) {
        return sessions.executeRead(session -> {
            WerewolfPlayerEntity entity = session.get(WerewolfPlayerEntity.class, uuid);
            return entity != null && entity.isWerewolf();
        }, false);
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
