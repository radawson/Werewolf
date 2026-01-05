package org.clockworx.werewolf.listeners;

import java.lang.reflect.Method;
import java.util.UUID;

import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.clockworx.werewolf.WerewolfPlugin;

/**
 * Listener for Vampire plugin events (if available).
 * Handles cross-plugin integration to prevent conflicts.
 * 
 * Uses reflection to safely access Vampire event classes since they're in a different plugin.
 */
public class VampireListener implements Listener {
    
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
     * Handles vampire transformation events using reflection.
     * Prevents werewolf players from becoming vampires.
     * 
     * This handler listens for EventVampirePlayerVampireChange events from the Vampire plugin.
     *
     * @param event The Bukkit event (will be checked via reflection if it's a Vampire event).
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVampirePlayerVampireChange(Event event) {
        // Use reflection to check if this is a Vampire event
        String eventClassName = event.getClass().getName();
        if (!eventClassName.contains("EventVampirePlayerVampireChange") || 
            !eventClassName.contains("vampire")) {
            return; // Not a Vampire event, ignore
        }
        
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
            plugin.debug("Error handling vampire event: " + e.getMessage());
        }
    }
    
    /**
     * Handles vampire infection change events using reflection.
     * Prevents werewolf players from being infected.
     * 
     * This handler listens for EventVampirePlayerInfectionChange events from the Vampire plugin.
     *
     * @param event The Bukkit event (will be checked via reflection if it's a Vampire event).
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVampirePlayerInfectionChange(Event event) {
        // Use reflection to check if this is a Vampire infection event
        String eventClassName = event.getClass().getName();
        if (!eventClassName.contains("EventVampirePlayerInfectionChange") || 
            !eventClassName.contains("vampire")) {
            return; // Not a Vampire event, ignore
        }
        
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

