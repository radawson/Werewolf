package org.clockworx.werewolf.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.clockworx.werewolf.WerewolfPlugin;

/**
 * Listener for plugin enable events to detect when other plugins (like Vampire) load.
 * This allows bidirectional integration detection regardless of plugin load order.
 */
public class IntegrationListener implements Listener {
    
    private final WerewolfPlugin plugin;
    
    /**
     * Creates a new IntegrationListener.
     *
     * @param plugin The WerewolfPlugin instance.
     */
    public IntegrationListener(WerewolfPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handles plugin enable events to detect when Vampire loads.
     * If Vampire loads after Werewolf, this will initialize the integration.
     *
     * @param event The PluginEnableEvent.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginEnable(PluginEnableEvent event) {
        if (event.getPlugin().getName().equals("Vampire")) {
            // Re-check for Vampire integration if not already initialized
            if (plugin.getVampireIntegration() == null) {
                plugin.checkVampirePlugin();
            }
        }
    }
}

