package org.unitedlands.wars.classes.war;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.unitedlands.unitedlands.classes.Coordinates;
import org.unitedlands.unitedlands.classes.db.Identifiable;
import org.unitedlands.unitedlands.libs.ormlite.field.DatabaseField;
import org.unitedlands.utils.United;
import org.unitedlands.wars.UnitedWars;
import org.unitedlands.wars.classes.warzone.WarZone;
import org.unitedlands.wars.events.SiegeChunkHealthChangeEvent;
import org.unitedlands.wars.events.WarScoreEvent;
import org.unitedlands.wars.managers.WarManager;

public class SiegeChunk implements Identifiable {

    @DatabaseField(id = true, width = 36, canBeNull = false)
    private UUID uuid;

    @DatabaseField(width = 36, canBeNull = false)
    private UUID warId;
    public transient War war;

    @DatabaseField(width = 36, canBeNull = false)
    private UUID warZoneId;
    public transient WarZone warZone;

    @DatabaseField(columnName = "world_name")
    private String worldName;
    @DatabaseField
    private int x;
    @DatabaseField
    private int z;
    private transient Coordinates coordinates;

    @DatabaseField(columnName = "type")
    private String type = "default";

    @DatabaseField(columnName = "score_value")
    private int scoreValue = 0;

    @DatabaseField(columnName = "occupiable")
    private boolean occupiable = false;

    @DatabaseField(columnName = "is_occupied")
    private boolean occupied = false;

    private transient boolean stateChanged = false;

    public Coordinates getCoordinates() {
        if (coordinates == null)
            coordinates = new Coordinates(this.x, this.z, this.worldName);
        return coordinates;
    }

    public void setCoordinates(Coordinates coordinates) {
        this.x = coordinates.getX();
        this.z = coordinates.getZ();
        this.worldName = coordinates.getWorldName();
        this.coordinates = coordinates;
    }

    @DatabaseField(width = 36, columnName = "faction_owner_id")
    private UUID ownerId;
    public transient WarFaction owner;

    @DatabaseField(columnName = "max_health")
    private int maxHealth = 0;

    @DatabaseField(columnName = "current_health")
    private int currentHealth = 0;

    private transient Map<WarFaction, LinkedHashSet<Player>> playersInChunk = new HashMap<>();

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public War getWar() {
        if (war == null && warId != null)
            war = WarManager.instance().getWar(warId);
        return war;
    }

    public void setWar(War war) {
        this.warId = war.getUuid();
        this.war = war;
    }

    public WarZone getWarZone() {
        if (warZone == null && warZoneId != null)
            warZone = WarManager.instance().getWarZone(warZoneId);
        return warZone;
    }

    public void setWarZone(WarZone warZone) {
        this.warZoneId = warZone.getUuid();
        this.warZone = warZone;
    }

    public WarFaction getOwner() {
        if (owner == null && ownerId != null)
            owner = WarManager.instance().getWarFaction(ownerId);
        return owner;
    }

    public void setOwner(WarFaction warFaction) {
        if (warFaction == null) {
            this.owner = null;
            this.ownerId = null;
        }
        this.ownerId = warFaction.getUuid();
        this.owner = warFaction;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
    }

    public int getCurrentHealth() {
        return currentHealth;
    }

    public void setCurrentHealth(int currentHealth) {
        this.currentHealth = currentHealth;
    }

    public int getScoreValue() {
        return scoreValue;
    }

    public void setScoreValue(int scoreValue) {
        this.scoreValue = scoreValue;
    }

    public boolean isOccupiable() {
        return occupiable;
    }

    public void setOccupiable(boolean occupiable) {
        this.occupiable = occupiable;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }

    public boolean stateChanged() {
        return stateChanged;
    }

    public void setStateChanged(boolean stateChanged) {
        this.stateChanged = stateChanged;
    }

    public boolean hasPlayersInChunk() {
        return playersInChunk.values().stream().collect(Collectors.summingInt(LinkedHashSet::size)) > 0;
    }

    public Map<WarFaction, LinkedHashSet<Player>> getPlayersInChunk() {
        return playersInChunk;
    }

    public void addPlayer(Player player) {
        var playerFaction = getWar().getPlayerFaction(player);
        if (playerFaction == null)
            return;

        if (!getWar().getWarFactions().contains(playerFaction))
            return;

        var factionList = playersInChunk.computeIfAbsent(playerFaction, v -> new LinkedHashSet<Player>());
        factionList.addLast(player);
    }

    public void removePlayer(Player player) {
        var playerFaction = getWar().getPlayerFaction(player);
        if (playerFaction == null)
            return;

        var factionList = playersInChunk.computeIfAbsent(playerFaction, v -> new LinkedHashSet<Player>());
        factionList.remove(player);
    }

    public static SiegeChunk create(WarZone warZone, Coordinates coordinates) {

        ConfigurationSection chunkHealthSettings = UnitedWars.instance().getConfig().getConfigurationSection("siege-settings.chunk-max-health");
        if (chunkHealthSettings == null) {
            United.logger().warning("Couldn't find chunk health settings, aborting.", "UnitedWars");
            return null;
        }
        ConfigurationSection chunkSoreSettings = UnitedWars.instance().getConfig().getConfigurationSection("score-settings.chunk-capture");
        if (chunkSoreSettings == null) {
            United.logger().warning("Couldn't find chunk health settings, aborting.", "UnitedWars");
            return null;
        }

        var maxHealth = 0;
        var chunkType = "default";
        var scoreValue = 10;
        var occupiable = false;

        // Capture zone chunks might have special HP settings
        if (warZone.isInCaptureArea(coordinates)) {
            var captureAreaChunk = warZone.getCaptureAreaChunk(coordinates);
            chunkType = captureAreaChunk.type();
            occupiable = captureAreaChunk.occupiable();
        }

        if (chunkHealthSettings.getKeys(false).contains(chunkType)) {
            maxHealth = chunkHealthSettings.getInt(chunkType);
        } else {
            maxHealth = chunkHealthSettings.getInt("default");
        }

        if (chunkSoreSettings.getKeys(false).contains(chunkType)) {
            scoreValue = chunkSoreSettings.getInt(chunkType);
        } else {
            scoreValue = chunkSoreSettings.getInt("default");
        }

        SiegeChunk siegeChunk = new SiegeChunk();
        siegeChunk.setUuid(UUID.randomUUID());
        siegeChunk.setCoordinates(coordinates);
        siegeChunk.setWar(warZone.getWar());
        siegeChunk.setWarZone(warZone);
        if (warZone.getFaction() != null)
            siegeChunk.setOwner(warZone.getFaction());
        siegeChunk.setType(chunkType);
        siegeChunk.setMaxHealth(maxHealth);
        siegeChunk.setCurrentHealth(maxHealth);
        siegeChunk.setScoreValue(scoreValue);
        siegeChunk.setOccupiable(occupiable);

        return siegeChunk;
    }

    public void updateHealth() {

        if (!getWarZone().isSiegeEnabled() || isOccupied())
            return;

        var factionControl = getDominantWarFaction();
        var leadingFaction = factionControl.leadingFaction();
        if (leadingFaction == null)
            return;

        int damageRate = UnitedWars.instance().getConfig().getInt("siege-settings.health-decay-rate", 1);
        int healRate = UnitedWars.instance().getConfig().getInt("siege-settings.health-restore-rate", 1);
        int healthChange = 0;
        int factor = factionControl.margin();
        if (!UnitedWars.instance().getConfig().getBoolean("use-superiority-multiplier", true))
            factor = 1;

        // Damage if strongest faction is not owner, heal otherwise
        if (getOwner() != leadingFaction) {
            healthChange = -1 * factor * damageRate;
        } else {
            healthChange = factor * healRate;
        }

        var healthChangeEvent = new SiegeChunkHealthChangeEvent(getWar(), this, healthChange);
        healthChangeEvent.callEvent();

        // Listeners may hay changed the desired health change
        if (healthChangeEvent.getHealthChange() == 0)
            return;

        currentHealth = Math.max(0, Math.min(maxHealth, currentHealth + healthChangeEvent.getHealthChange()));

        // Capture and occupy the chunk
        if (currentHealth == 0) {
            setOwner(leadingFaction);
            // Chunks that are part of the capture area of a war zone and are occupiable
            // will be set to fully occupied and can't be reconquered.
            if (isOccupiable()) {
                setOccupied(true);
                getWarZone().checkOccupation();
            }

            var capturingPlayer = playersInChunk.get(leadingFaction).getFirst();
            var captureEvent = new WarScoreEvent(getWar(), capturingPlayer, leadingFaction, WarScoreType.SIEGE_CHUNK_CAPTURE, getScoreValue());
            captureEvent.callEvent();
        }

        this.stateChanged = true;
    }

    private WarFactionControlResult getDominantWarFaction() {

        if (playersInChunk == null || playersInChunk.isEmpty()) {
            return new WarFactionControlResult(null, 0);
        }

        WarFaction topFaction = null;
        int topCount = 0;
        int secondCount = 0;

        for (var entry : playersInChunk.entrySet()) {
            int count = entry.getValue() == null ? 0 : entry.getValue().size();

            if (count > topCount) {
                secondCount = topCount;
                topCount = count;
                topFaction = entry.getKey();
            } else if (count > secondCount) {
                secondCount = count;
            }
        }

        if (topFaction == null || topCount == 0) {
            return new WarFactionControlResult(null, 0);
        }

        return new WarFactionControlResult(topFaction, topCount - secondCount);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SiegeChunk other = (SiegeChunk) obj;
        if (uuid == null) {
            if (other.uuid != null)
                return false;
        } else if (!uuid.equals(other.uuid))
            return false;
        return true;
    }

    private record WarFactionControlResult(WarFaction leadingFaction, int margin) {
    }

}
