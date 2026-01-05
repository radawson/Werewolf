package org.clockworx.werewolf.listeners;

import java.lang.reflect.Method;
import java.util.UUID;

import org.bukkit.event.Event;
import org.clockworx.werewolf.WerewolfPlugin;

/**
 * Listener for Vampire plugin events (if available).
 * Handles cross-plugin integration to prevent conflicts.
 * 
 * Uses dynamic event registration since we cannot directly
 * import Vampire's event classes. This listener is registered dynamically
 * in WerewolfPlugin after detecting the Vampire plugin.
 */
public class VampireListener {
    
    private final WerewolfPlugin plugin;
    
    /**
     * Creates a new VampireListener.
     *
     * @param plugin The WerewolfPlugin instance.
     */
    public VampireListener(WerewolfPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Event executor that handles all events and filters for Vampire events.
     * This is registered dynamically for specific event types.
     */
    public void onEvent(Event event) {
        // Early return if not a Vampire event
        String eventClassName = event.getClass().getName();
        if (!eventClassName.contains("vampire") || !eventClassName.contains("Event")) {
            return;
        }
        
        // Handle EventVampirePlayerVampireChange
        if (eventClassName.contains("EventVampirePlayerVampireChange")) {
            handleVampireChange(event);
            return;
        }
        
        // Handle EventVampirePlayerInfectionChange
        if (eventClassName.contains("EventVampirePlayerInfectionChange")) {
            handleInfectionChange(event);
            return;
        }
    }
    
    /**
     * Handles vampire transformation events using reflection.
     * Prevents werewolf players from becoming vampires.
     */
    private void handleVampireChange(org.bukkit.event.Event event) {
        try {
            // Get the VampirePlayer from the event using reflection
            Method getVampirePlayerMethod = event.getClass().getMethod("getVampirePlayer");
            Object vampirePlayerObj = getVampirePlayerMethod.invoke(event);
            
            if (vampirePlayerObj == null) {
                return;
            }
            
            // Get the player UUID from VampirePlayer
            Method getUuidMethod = vampirePlayerObj.getClass().getMethod("getUuid");
            UUID playerUuid = (UUID) getUuidMethod.invoke(vampirePlayerObj);
            
            // Get the isVampire boolean to check if they're becoming a vampire
            Method isVampireMethod = event.getClass().getMethod("isVampire");
            Boolean isBecomingVampire = (Boolean) isVampireMethod.invoke(event);
            
            // Only prevent if they're becoming a vampire (not if they're being cured)
            if (isBecomingVampire != null && isBecomingVampire && playerUuid != null) {
                // Check if the player is a werewolf
                if (plugin.getWerewolfManager().isWerewolf(playerUuid)) {
                    // Cancel the event to prevent werewolf from becoming vampire
                    if (event instanceof org.bukkit.event.Cancellable) {
                        ((org.bukkit.event.Cancellable) event).setCancelled(true);
                        plugin.getLogger().info("Prevented vampire transformation for werewolf player: " + playerUuid);
                    }
                }
            }
        } catch (Exception e) {
            plugin.debug("Error handling vampire transformation event: " + e.getMessage());
        }
    }
    
    /**
     * Handles vampire infection change events using reflection.
     * Prevents werewolf players from being infected.
     */
    private void handleInfectionChange(org.bukkit.event.Event event) {
        try {
            // Get the VampirePlayer from the event using reflection
            Method getVampirePlayerMethod = event.getClass().getMethod("getVampirePlayer");
            Object vampirePlayerObj = getVampirePlayerMethod.invoke(event);
            
            if (vampirePlayerObj == null) {
                return;
            }
            
            // Get the player UUID from VampirePlayer
            Method getUuidMethod = vampirePlayerObj.getClass().getMethod("getUuid");
            UUID playerUuid = (UUID) getUuidMethod.invoke(vampirePlayerObj);
            
            // Get the infection level
            Method getInfectionMethod = event.getClass().getMethod("getInfection");
            Double infectionLevel = (Double) getInfectionMethod.invoke(event);
            
            // Only prevent if infection is increasing (becoming infected, not being cured)
            if (infectionLevel != null && infectionLevel > 0.0 && playerUuid != null) {
                // Check if the player is a werewolf
                if (plugin.getWerewolfManager().isWerewolf(playerUuid)) {
                    // Cancel the event to prevent werewolf from being infected
                    if (event instanceof org.bukkit.event.Cancellable) {
                        ((org.bukkit.event.Cancellable) event).setCancelled(true);
                        plugin.getLogger().info("Prevented vampire infection for werewolf player: " + playerUuid);
                    }
                }
            }
        } catch (Exception e) {
            plugin.debug("Error handling vampire infection event: " + e.getMessage());
        }
    }
}

