package org.clockworx.werewolf.entity;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Represents a player's werewolf data (POJO - Plain Old Java Object).
 * This class primarily holds state. Logic involving external services
 * (config, db, events, effects, permissions) is handled by WerewolfManager.
 */
public class WerewolfPlayer {

    // Core identifiers
    private final UUID uuid;
    private String name;

    // Core Werewolf State
    private boolean isWerewolf;
    private TransformationState transformationState;
    private WerewolfType werewolfType;

    // Skin data for restoration
    private String originalSkinValue;
    private String originalSkinSignature;

    // Timestamps
    private long lastTransformationTime;
    private long lastCureTime;

    /**
     * Enumeration of transformation states.
     */
    public enum TransformationState {
        HUMAN,
        TRANSFORMING,
        WEREWOLF
    }

    /**
     * Enumeration of werewolf types.
     */
    public enum WerewolfType {
        ALPHA,
        WITHERFANG,
        SILVERMANE,
        BLOODMOON
    }

    /**
     * Constructor for creating a new WerewolfPlayer.
     *
     * @param uuid The player's UUID.
     * @param name The player's name.
     */
    public WerewolfPlayer(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.isWerewolf = false;
        this.transformationState = TransformationState.HUMAN;
        this.werewolfType = WerewolfType.ALPHA; // Default type
        this.lastTransformationTime = 0;
        this.lastCureTime = 0;
    }

    /**
     * Gets the player's UUID.
     *
     * @return The UUID.
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Gets the player's name.
     *
     * @return The name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the player's name.
     *
     * @param name The new name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Checks if the player is a werewolf.
     *
     * @return True if the player is a werewolf, false otherwise.
     */
    public boolean isWerewolf() {
        return isWerewolf;
    }

    /**
     * Sets the werewolf status. This is an internal method.
     * Use WerewolfManager.setWerewolfStatus() for proper state management.
     *
     * @param isWerewolf True to make werewolf, false to cure.
     */
    public void setWerewolfInternal(boolean isWerewolf) {
        this.isWerewolf = isWerewolf;
        if (isWerewolf) {
            this.transformationState = TransformationState.WEREWOLF;
        } else {
            this.transformationState = TransformationState.HUMAN;
        }
    }

    /**
     * Gets the current transformation state.
     *
     * @return The transformation state.
     */
    public TransformationState getTransformationState() {
        return transformationState;
    }

    /**
     * Sets the transformation state.
     *
     * @param state The new transformation state.
     */
    public void setTransformationState(TransformationState state) {
        this.transformationState = state;
    }

    /**
     * Gets the werewolf type.
     *
     * @return The werewolf type.
     */
    public WerewolfType getWerewolfType() {
        return werewolfType;
    }

    /**
     * Sets the werewolf type.
     *
     * @param type The new werewolf type.
     */
    public void setWerewolfType(WerewolfType type) {
        this.werewolfType = type;
    }

    /**
     * Gets the original skin value for restoration.
     *
     * @return The original skin value.
     */
    public String getOriginalSkinValue() {
        return originalSkinValue;
    }

    /**
     * Sets the original skin value.
     *
     * @param value The original skin value.
     */
    public void setOriginalSkinValue(String value) {
        this.originalSkinValue = value;
    }

    /**
     * Gets the original skin signature for restoration.
     *
     * @return The original skin signature.
     */
    public String getOriginalSkinSignature() {
        return originalSkinSignature;
    }

    /**
     * Sets the original skin signature.
     *
     * @param signature The original skin signature.
     */
    public void setOriginalSkinSignature(String signature) {
        this.originalSkinSignature = signature;
    }

    /**
     * Gets the timestamp of the last transformation.
     *
     * @return The timestamp in milliseconds.
     */
    public long getLastTransformationTime() {
        return lastTransformationTime;
    }

    /**
     * Sets the timestamp of the last transformation.
     *
     * @param time The timestamp in milliseconds.
     */
    public void setLastTransformationTime(long time) {
        this.lastTransformationTime = time;
    }

    /**
     * Gets the timestamp of the last cure.
     *
     * @return The timestamp in milliseconds.
     */
    public long getLastCureTime() {
        return lastCureTime;
    }

    /**
     * Sets the timestamp of the last cure.
     *
     * @param time The timestamp in milliseconds.
     */
    public void setLastCureTime(long time) {
        this.lastCureTime = time;
    }

    /**
     * Gets the Bukkit Player object if online.
     *
     * @return The Player object, or null if offline.
     */
    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    /**
     * Checks if the player is currently online.
     *
     * @return True if online, false otherwise.
     */
    public boolean isOnline() {
        return getPlayer() != null;
    }
}

