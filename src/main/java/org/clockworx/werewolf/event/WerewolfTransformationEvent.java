package org.clockworx.werewolf.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.clockworx.werewolf.entity.WerewolfPlayer;

/**
 * Event fired when a player transforms into a werewolf.
 * This event can be cancelled to prevent the transformation.
 */
public class WerewolfTransformationEvent extends Event implements Cancellable {
    
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final WerewolfPlayer werewolfPlayer;
    private final String reason;
    private boolean cancelled;
    
    /**
     * Creates a new WerewolfTransformationEvent.
     *
     * @param player The player who is transforming.
     * @param werewolfPlayer The WerewolfPlayer data.
     * @param reason The reason for the transformation.
     */
    public WerewolfTransformationEvent(Player player, WerewolfPlayer werewolfPlayer, String reason) {
        this.player = player;
        this.werewolfPlayer = werewolfPlayer;
        this.reason = reason;
        this.cancelled = false;
    }
    
    /**
     * Gets the player who is transforming.
     *
     * @return The player.
     */
    public Player getPlayer() {
        return player;
    }
    
    /**
     * Gets the WerewolfPlayer data.
     *
     * @return The WerewolfPlayer.
     */
    public WerewolfPlayer getWerewolfPlayer() {
        return werewolfPlayer;
    }
    
    /**
     * Gets the reason for the transformation.
     *
     * @return The reason.
     */
    public String getReason() {
        return reason;
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}

