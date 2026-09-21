package org.unitedlands.wars.classes.war;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.MissingFormatArgumentException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.Coordinates;
import org.unitedlands.unitedlands.classes.GeopolObject;
import org.unitedlands.unitedlands.classes.db.Identifiable;
import org.unitedlands.unitedlands.libs.ormlite.field.DataType;
import org.unitedlands.unitedlands.libs.ormlite.field.DatabaseField;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.utils.CoordinateUtils;
import org.unitedlands.unitedlands.utils.SerializationUtils;
import org.unitedlands.utils.United;
import org.unitedlands.wars.UnitedWars;
import org.unitedlands.wars.classes.wargoal.WarGoal;
import org.unitedlands.wars.classes.warzone.WarZone;
import org.unitedlands.wars.events.WarScoreEvent;
import org.unitedlands.wars.managers.WarManager;
import org.unitedlands.wars.managers.WarMetaDataManager;

public class War implements Identifiable {

    @DatabaseField(id = true, width = 36, canBeNull = false)
    private UUID uuid;
    @DatabaseField(canBeNull = false)
    private Long timestamp;

    @DatabaseField(canBeNull = false)
    private String title;
    @DatabaseField(width = 512, canBeNull = true)
    private String description;

    @DatabaseField(canBeNull = false, columnName = "war_goal_id")
    private String warGoalId;
    private transient WarGoal warGoal;

    @DatabaseField(width = 36, canBeNull = true, columnName = "winning_faction_id")
    private UUID winningFactionId;
    private transient WarFaction winningFaction;
    @DatabaseField(canBeNull = true, columnName = "winning_condition")
    private String winningCondition;

    @DatabaseField(canBeNull = false, columnName = "scheduled_begin_time")
    private Long scheduledBeginTime;
    @DatabaseField(canBeNull = false, columnName = "scheduled_end_time")
    private Long scheduledEndTime;
    @DatabaseField(canBeNull = true, columnName = "effective_end_time")
    private Long effectiveEndTime;

    @DatabaseField(columnName = "is_active")
    private Boolean active = false;
    @DatabaseField(columnName = "is_ended")
    private Boolean ended = false;

    @DatabaseField(width = 36, canBeNull = true, columnName = "war_target_id")
    private UUID warTargetId;

    @DatabaseField(dataType = DataType.LONG_STRING, columnName = "war_factions_serialized")
    private String warFactionsSerialized;
    private transient Set<WarFaction> warFactions;

    @DatabaseField(dataType = DataType.LONG_STRING, columnName = "war_zones_serialized")
    private String warZonesSerialized;
    private transient Set<WarZone> warZones;

    private transient Map<Player, WarFaction> playerFactions = new HashMap<>();
    private transient Map<WarFaction, Set<Player>> factionPlayers = new HashMap<>();

    private transient boolean stateChanged = false;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCleanTitle() {
        if (title == null)
            return null;
        return title.replace("_", " ");
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public WarGoal getWarGoal() {
        if (warGoal == null && warGoalId != null)
            warGoal = WarManager.instance().getWarGoal(warGoalId);
        return warGoal;
    }

    public void setWarGoal(WarGoal warGoal) {
        this.warGoalId = warGoal.getId();
        this.warGoal = warGoal;
    }

    public WarFaction getWinningFaction() {
        if (winningFaction == null && winningFactionId != null)
            winningFaction = WarManager.instance().getWarFaction(winningFactionId);
        return winningFaction;
    }

    public void setWinningFaction(WarFaction faction) {
        if (faction == null) {
            this.winningFaction = null;
            this.winningFactionId = null;
            return;
        }
        this.winningFactionId = faction.getUuid();
        this.winningFaction = faction;
    }

    public String getWinningCondition() {
        return winningCondition;
    }

    public void setWinningCondition(String winningCondition) {
        this.winningCondition = winningCondition;
    }

    public Long getScheduledBeginTime() {
        return scheduledBeginTime;
    }

    public void setScheduledBeginTime(Long scheduledBeginTime) {
        this.scheduledBeginTime = scheduledBeginTime;
    }

    public Long getScheduledEndTime() {
        return scheduledEndTime;
    }

    public void setScheduledEndTime(Long scheduledEndTime) {
        this.scheduledEndTime = scheduledEndTime;
    }

    public Long getEffectiveEndTime() {
        return effectiveEndTime;
    }

    public void setEffectiveEndTime(Long effectiveEndTime) {
        this.effectiveEndTime = effectiveEndTime;
        this.stateChanged = true;
    }

    public boolean stateChanged() {
        return stateChanged;
    }

    public void setStateChanged(boolean stateChanged) {
        this.stateChanged = stateChanged;
    }

    public Boolean isActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
        this.stateChanged = true;
    }

    public Boolean hasEnded() {
        return ended;
    }

    public void setEnded(Boolean ended) {
        this.ended = ended;
        this.stateChanged = true;
    }

    public UUID getWarTargetId() {
        return warTargetId;
    }

    public void setWarTargetId(UUID warTargetId) {
        this.warTargetId = warTargetId;
    }

    public boolean hasWarFaction(WarFaction warFaction) {
        return getWarFactions().contains(warFaction);
    }

    public void addWarFaction(WarFaction warFaction) {
        var c = new HashSet<>(getWarFactions());
        c.add(warFaction);
        setWarFactions(c);
    }

    public void removeWarFaction(WarFaction warFaction) {
        var c = new HashSet<>(getWarFactions());
        c.remove(warFaction);
        setWarFactions(c);
    }

    public Set<WarFaction> getWarFactions() {
        if (this.warFactions == null) {
            this.warFactions = SerializationUtils.deserializeUuidListToSet(warFactionsSerialized, WarManager.instance()::getWarFaction);
        }
        return this.warFactions;
    }

    public Map<WarFactionRole, List<WarFaction>> getWarFactionMap() {
        return getWarFactions().stream().collect(Collectors.groupingBy(WarFaction::getRole));
    }

    public WarFaction getWarFaction(WarFactionRole role) {
        return getWarFactions().stream().filter(f -> f.getRole() == role).findFirst().orElse(null);
    }

    public void setWarFactions(Set<WarFaction> warFactions) {
        this.warFactions = warFactions;
        this.warFactionsSerialized = SerializationUtils.serializeIdentifiableList(warFactions);
    }

    public boolean hasWarZone(WarZone warZone) {
        return getWarZones().contains(warZone);
    }

    public void addWarZone(WarZone warZone) {
        var c = new HashSet<>(getWarZones());
        c.add(warZone);
        setWarZones(c);
    }

    public void removeWarZone(WarZone warZone) {
        var c = new HashSet<>(getWarZones());
        c.remove(warZone);
        setWarZones(c);
    }

    public Set<WarZone> getWarZones() {
        if (this.warZones == null) {
            this.warZones = SerializationUtils.deserializeUuidListToSet(warZonesSerialized, WarManager.instance()::getWarZone);
        }
        return this.warZones;
    }

    public void setWarZones(Set<WarZone> warZones) {
        this.warZones = warZones;
        this.warZonesSerialized = SerializationUtils.serializeIdentifiableList(warZones);
    }

    public static War create(WarGoal warGoal, GeopolObject declarer, GeopolObject target, String title, String description) {

        War war = new War();
        war.setUuid(UUID.randomUUID());
        war.setTitle(title);
        war.setDescription(description);
        war.setWarGoal(warGoal);
        war.setWarTargetId(target.getUuid());

        var config = UnitedWars.instance().getConfig().getConfigurationSection("war-goal-settings." + warGoal.getId());
        if (config == null)
            throw new MissingFormatArgumentException("Could not find config section for war goal " + warGoal.getId());

        long warmup = config.getLong("warmup-time");
        long duration = config.getLong("duration");

        war.setTimestamp(System.currentTimeMillis());
        war.setScheduledBeginTime(System.currentTimeMillis() + (warmup * 1000));
        war.setScheduledEndTime(System.currentTimeMillis() + (warmup * 1000) + (duration * 1000));

        var factions = warGoal.createFactions(declarer.getUuid(), target.getUuid(), war);
        if (factions == null) {
            United.logger().error("Could not create war factions, cancelling war creation.");
            return null;
        }
        war.setWarFactions(factions);

        var warZones = warGoal.createWarZones(declarer.getUuid(), target.getUuid(), war);
        if (warZones == null) {
            United.logger().error("Could not create war zones, cancelling war creation.");
            return null;
        }
        war.setWarZones(warZones);

        return war;
    }

    public boolean canBeStarted() {
        var currentTime = System.currentTimeMillis();
        if (scheduledBeginTime <= currentTime && scheduledEndTime >= currentTime && !active) {
            return true;
        }
        return false;
    }

    public boolean canBeEnded() {

        if (!active)
            return false;

        var currentTime = System.currentTimeMillis();

        // See if time has run out
        if (scheduledEndTime <= currentTime) {
            WarFaction winner = null;
            // Determine winner by highest score. If there is more than one faction with the
            // highest score, no one wins.
            int highestScore = Integer.MIN_VALUE;
            for (var faction : getWarFactions()) {
                if (faction.getScore() == highestScore) {
                    // We have a faction that has the same score as another faction.
                    // No one wins.
                    winner = null;
                } else if (faction.getScore() > highestScore) {
                    highestScore = faction.getScore();
                    winner = faction;
                }
            }
            setWinningFaction(winner);
            return true;
        }

        // Check all losing conditions

        for (var faction : getWarFactions()) {

            for (var entry : faction.getLoseConditions().entrySet()) {

                var condition = WarManager.instance().getWarCondition(entry.getKey());
                if (condition == null) {
                    United.logger().error("Unknown condition: " + entry.getKey());
                    continue;
                }
                if (condition.evaluate(faction, entry.getValue())) {
                    faction.setLost(true);
                    // TODO: Trigger faction lost event?
                }
            }
        }

        // Check all winning conditions

        List<WarFaction> winners = new ArrayList<>();
        List<String> winningConditions = new ArrayList<>();

        for (var faction : getWarFactions()) {

            for (var entry : faction.getWinConditions().entrySet()) {

                var condition = WarManager.instance().getWarCondition(entry.getKey());
                if (condition == null) {
                    United.logger().error("Unknown condition: " + entry.getKey());
                    continue;
                }

                if (condition.evaluate(faction, entry.getValue())) {
                    // Only add a winning faction once for the first condition encountered
                    if (!winners.contains(faction)) {
                        winners.add(faction);
                        winningConditions.add(entry.getKey());
                    }
                }
            }
        }

        if (winners.size() == 1 && winningConditions.size() == 1) {
            // Exactly one faction won
            setWinningFaction(winners.get(0));
            setWinningCondition(winningConditions.get(0));
            return true;
        } else if (winners.size() > 1) {
            // Multiple simultaneous winners. End the war, but don't declare a winning
            // faction
            return true;
        }

        return false;
    }

    public void resolveWarGoal() {
        getWarGoal().resolve(this);
    }

    public void refreshOnlinePlayerFactions() {

        // Remove all offline players
        if (playerFactions.keySet().removeIf(p -> !p.isOnline())) {
            for (var factionPlayerList : factionPlayers.values())
                factionPlayerList.removeIf(p -> !p.isOnline());
        }

        var onlinePlayers = Bukkit.getServer().getOnlinePlayers();

        for (var player : onlinePlayers) {

            if (playerFactions.containsKey(player))
                continue;

            var citizen = UnitedLandsDataManager.instance().getCitizen(player);
            if (citizen == null) {
                United.logger().warning("Could not get citizen data of player " + player.getUniqueId(), "UnitedLands");
                continue;
            }

            if (!citizen.hasSettlement())
                continue;
            if (!WarManager.instance().citizenHasMilitaryRank(citizen))
                continue;

            var faction = getFaction(citizen);
            if (faction != null) {
                playerFactions.put(player, faction);
                var factionPlayerList = factionPlayers.computeIfAbsent(faction, v -> new HashSet<>());
                factionPlayerList.add(player);

                WarMetaDataManager.instance().addWarLives(player, this);
                WarMetaDataManager.instance().addFactionPermission(player, faction);
            }
        }

    }

    public void awardActivityScores() {

        var activityPoints = UnitedWars.instance().getConfig().getInt("score-settings.activity", 0);
        if (activityPoints == 0)
            return;

        for (var entry : playerFactions.entrySet()) {
            var activityScoreEvent = new WarScoreEvent(this, entry.getKey(), entry.getValue(), WarScoreType.ACTIVITY, activityPoints);
            activityScoreEvent.callEvent();
        }

    }

    public void updateSiegesInWarZones() {

        var overrideActivityRequirement = UnitedWars.instance().getConfig().getBoolean("siege-settings.override-activity-requirement", false);

        for (var warZone : getWarZones()) {
            warZone.checkOccupation();
            if (warZone.isOccupied())
                continue;
            if (overrideActivityRequirement) {
                warZone.setSiegeEnabled(true);
            } else {
                var toggle = false;
                if (warZone.getFaction() != null) {
                    toggle = getFactionPlayers(warZone.getFaction()).size() > 0;
                    United.logger().info("Toggling sieges for faction " + warZone.getFaction().getName() + " in zone " + warZone.getUuid() + " to " + toggle);
                }
                warZone.setSiegeEnabled(toggle);
            }
        }

    }

    private WarFaction getFaction(Citizen citizen) {
        for (var faction : getWarFactions()) {
            for (var settlement : faction.getSettlements()) {
                if (settlement.equals(citizen.getSettlement())) {
                    return faction;
                }
            }
            for (var country : faction.getCountries()) {
                if (!citizen.getSettlement().hasCountry())
                    continue;
                if (country.equals(citizen.getSettlement().getCountry()))
                    return faction;
            }
            for (var mercenary : faction.getMercenaries()) {
                if (citizen.equals(mercenary)) {
                    return faction;
                }
            }
        }
        return null;
    }

    public Set<Player> getFactionPlayers(WarFaction faction) {
        if (faction == null)
            return new HashSet<>();
        return factionPlayers.computeIfAbsent(faction, v -> new HashSet<>());
    }

    public boolean isPlayerInWar(Player player) {
        return playerFactions.containsKey(player);
    }

    public WarZone getPlayerWarZone(Player player) {
        var playerChunk = CoordinateUtils.locationToChunkCoordinates(player.getLocation());
        return getPlayerWarZone(player, playerChunk);

    }

    public WarZone getPlayerWarZone(Player player, Coordinates coords) {
        return getWarZones().stream().filter(z -> z.isInArea(coords)).findFirst().orElse(null);

    }

    public WarFaction getPlayerFaction(Player player) {
        return playerFactions.get(player);
    }

    public boolean hasChunkInWarZone(Coordinates coords) {
        return getWarZones().stream().anyMatch(z -> z.isInArea(coords));
    }

    public WarZone getWarZoneForChunk(Coordinates coords) {
        return getWarZones().stream().filter(z -> z.isInArea(coords)).findFirst().orElse(null);
    }

    public Map<String, String> getMessageReplacements() {

        Map<String, String> replacements = new HashMap<>(Map.of("war-title", getCleanTitle(), "war-description", getDescription(), "timer-length",
                United.formatter().formatDuration(getScheduledEndTime() - getScheduledBeginTime()), "timer-to-start",
                United.formatter().formatDuration(getScheduledBeginTime() - System.currentTimeMillis()), "timer-to-end",
                United.formatter().formatDuration(getScheduledEndTime() - System.currentTimeMillis())));

        if (getWarGoal() != null) {
            replacements.putAll(getWarGoal().getMessageReplacements());
        }
        return replacements;
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
        War other = (War) obj;
        if (uuid == null) {
            if (other.uuid != null)
                return false;
        } else if (!uuid.equals(other.uuid))
            return false;
        return true;
    }

}
