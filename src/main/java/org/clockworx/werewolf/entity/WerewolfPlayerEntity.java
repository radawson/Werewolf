package org.clockworx.werewolf.entity;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity for werewolf player data persistence.
 * This entity maps to the database table for storing werewolf player information.
 */
@Entity
@Table(name = "werewolf_players")
public class WerewolfPlayerEntity {
    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR) // Ensure UUID is stored as VARCHAR
    private UUID uuid;
    
    @Column(nullable = false)
    private String name;
    
    @Column(name = "is_werewolf")
    private boolean isWerewolf;
    
    @Column(name = "transformation_state")
    private String transformationState;
    
    @Column(name = "werewolf_type")
    private String werewolfType;
    
    @Column(name = "original_skin_value", length = 1000)
    private String originalSkinValue;
    
    @Column(name = "original_skin_signature", length = 1000)
    private String originalSkinSignature;
    
    @Column(name = "last_transformation_time")
    private long lastTransformationTime;
    
    @Column(name = "last_cure_time")
    private long lastCureTime;

    // Default constructor required by Hibernate
    protected WerewolfPlayerEntity() {}

    /**
     * Constructor for creating a new WerewolfPlayerEntity.
     *
     * @param uuid The player's UUID.
     * @param name The player's name.
     */
    public WerewolfPlayerEntity(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.isWerewolf = false;
        this.transformationState = WerewolfPlayer.TransformationState.HUMAN.name();
        this.werewolfType = WerewolfPlayer.WerewolfType.ALPHA.name();
        this.lastTransformationTime = 0L;
        this.lastCureTime = 0L;
    }

    // Getters and setters
    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public boolean isWerewolf() { return isWerewolf; }
    public void setWerewolf(boolean werewolf) { isWerewolf = werewolf; }
    
    public String getTransformationState() { return transformationState; }
    public void setTransformationState(String state) { this.transformationState = state; }
    
    public String getWerewolfType() { return werewolfType; }
    public void setWerewolfType(String type) { this.werewolfType = type; }
    
    public String getOriginalSkinValue() { return originalSkinValue; }
    public void setOriginalSkinValue(String value) { this.originalSkinValue = value; }
    
    public String getOriginalSkinSignature() { return originalSkinSignature; }
    public void setOriginalSkinSignature(String signature) { this.originalSkinSignature = signature; }
    
    public long getLastTransformationTime() { return lastTransformationTime; }
    public void setLastTransformationTime(long time) { this.lastTransformationTime = time; }
    
    public long getLastCureTime() { return lastCureTime; }
    public void setLastCureTime(long time) { this.lastCureTime = time; }
}

