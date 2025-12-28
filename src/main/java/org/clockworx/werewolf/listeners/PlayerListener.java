package org.clockworx.werewolf.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.clockworx.werewolf.WerewolfPlugin;

/**
 * Listener for player-related events.
 * Handles player join/quit and werewolf state management.
 */
public class PlayerListener implements Listener {
    
    private final WerewolfPlugin plugin;
    
    /**
     * Creates a new PlayerListener.
     *
     * @param plugin The WerewolfPlugin instance.
     */
    public PlayerListener(WerewolfPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handles player join events.
     * Loads werewolf data and applies transformation if needed.
     *
     * @param event The PlayerJoinEvent.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getWerewolfManager().handlePlayerJoin(event.getPlayer());
    }
    
    /**
     * Handles player quit events.
     * Saves werewolf data to the database.
     *
     * @param event The PlayerQuitEvent.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getWerewolfManager().handlePlayerQuit(event.getPlayer());
    }
}

