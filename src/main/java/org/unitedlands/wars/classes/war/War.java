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
import org.unitedlands.classes.database.Identifiable;
import org.unitedlands.libs.ormlite.field.DataType;
import org.unitedlands.libs.ormlite.field.DatabaseField;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.utils.CoordinateUtils;
import org.unitedlands.unitedlands.utils.SerializationUtils;
import org.unitedlands.utils.United;
import org.unitedlands.wars.classes.config.UnitedWarsConfig;
import org.unitedlands.wars.classes.wargoal.WarGoal;
import org.unitedlands.wars.classes.warzone.WarZone;
import org.unitedlands.wars.classes.warzone.WarZoneWarcamp;
import org.unitedlands.wars.events.WarScoreEvent;
import org.unitedlands.wars.managers.WarManager;
import org.unitedlands.wars.managers.WarMetaDataManager;
import org.unitedlands.wars.utils.Serializers;

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
            this.stateChanged = true;
            return;
        }
        this.winningFactionId = faction.getUuid();
        this.winningFaction = faction;
        this.stateChanged = true;
    }

    public String getWinningCondition() {
        return winningCondition;
    }

    public void setWinningCondition(String winningCondition) {
        this.winningCondition = winningCondition;
        this.stateChanged = true;
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
            this.warFactions = Serializers.deserializeUuidSet(warFactionsSerialized, WarManager.instance()::getWarFaction, "war factions of war " + uuid);
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
        this.stateChanged = true;
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
            this.warZones = Serializers.deserializeUuidSet(warZonesSerialized, WarManager.instance()::getWarZone, "war zones of war " + uuid);
        }
        return this.warZones;
    }

    public Set<WarZone> getObjectiveWarZones() {
        return getWarZones().stream().filter(WarZone::isObjective).collect(Collectors.toSet());
    }

    public void setWarZones(Set<WarZone> warZones) {
        this.warZones = warZones;
        this.warZonesSerialized = SerializationUtils.serializeIdentifiableList(warZones);
        this.stateChanged = true;
    }

    public static War create(WarGoal warGoal, GeopolObject declarer, GeopolObject target, String title, String description) {

        War war = new War();
        war.setUuid(UUID.randomUUID());
        war.setTitle(title);
        war.setDescription(description);
        war.setWarGoal(warGoal);
        war.setWarTargetId(target.getUuid());

        var goalSettings = UnitedWarsConfig.get().warGoalSettings().get(warGoal.getId());
        if (goalSettings == null)
            throw new MissingFormatArgumentException("Could not find config section for war goal " + warGoal.getId());

        long warmup = goalSettings.warmupTime();
        long duration = goalSettings.duration();

        war.setTimestamp(System.currentTimeMillis());
        war.setScheduledBeginTime(System.currentTimeMillis() + (warmup * 1000));
        war.setScheduledEndTime(System.currentTimeMillis() + (warmup * 1000) + (duration * 1000));

        var factions = warGoal.createFactions(declarer, target, war);
        if (factions == null) {
            United.logger().error("Could not create war factions, cancelling war creation.");
            return null;
        }
        war.setWarFactions(factions);

        var warZones = warGoal.createWarZones(declarer, target, war);
        if (warZones == null || warZones.isEmpty()) {
            United.logger().error("Could not create war zones, cancelling war creation.");
            return null;
        }
        war.setWarZones(warZones);

        return war;
    }

    public WarZone createWarcamp(WarFaction faction) {
        var warcampZone = getWarGoal().createWarcampZone(this, faction);
        addWarZone(warcampZone);

        return warcampZone;
    }

    public WarZone getWarcamp(WarFaction faction) {
        return getWarZones().stream().filter(z -> z instanceof WarZoneWarcamp && z.getFaction().equals(faction)).findAny().orElse(null);
    }

    public void removeWarcamp(WarZone zone) {
        removeWarZone(zone);
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

        for (var faction : getWarFactions()) {
            if (faction.hasLost())
                continue;

            for (var entry : faction.getLoseConditions().entrySet()) {

                var condition = WarManager.instance().getWarCondition(entry.getKey());
                if (condition == null) {
                    United.logger().error("Unknown condition: " + entry.getKey());
                    continue;
                }
                if (condition.evaluate(faction, entry.getValue())) {
                    faction.setLost(true);
                    // TODO: Trigger faction lost event?
                    break;
                }
            }
        }

        var remainingFactions = getWarFactions().stream().filter(f -> !f.hasLost()).toList();

        if (remainingFactions.isEmpty()) {
            setWinningFaction(null);
            return true;
        }

        if (remainingFactions.size() == 1) {
            setWinningFaction(remainingFactions.getFirst());
            setWinningCondition("last_faction_alive");
            return true;
        }

        if (scheduledEndTime <= System.currentTimeMillis()) {
            WarFaction winner = null;
            int highestScore = Integer.MIN_VALUE;
            for (var faction : remainingFactions) {
                if (faction.getScore() == highestScore) {
                    winner = null;
                } else if (faction.getScore() > highestScore) {
                    highestScore = faction.getScore();
                    winner = faction;
                }
            }
            setWinningFaction(winner);
            return true;
        }

        List<WarFaction> winners = new ArrayList<>();
        List<String> winningConditions = new ArrayList<>();

        for (var faction : remainingFactions) {

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

        if (winners.size() == 1) {
            // Exactly one faction won
            setWinningFaction(winners.get(0));
            // Set winning condition, if one was met. If there is none, they won by highest
            // score.
            if (winningConditions.size() != 0) {
                setWinningCondition(winningConditions.get(0));
            }
            return true;
        } else if (winners.size() > 1) {
            setWinningFaction(null);
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
                United.logger().warning("Could not get citizen data of player " + player.getUniqueId());
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

        var activityPoints = UnitedWarsConfig.get().scoreSettings().activity();
        if (activityPoints == 0)
            return;

        for (var playerFactionEntry : playerFactions.entrySet()) {

            var playerChunkCoords = CoordinateUtils.locationToChunkCoordinates(playerFactionEntry.getKey().getLocation());
            var warZone = WarManager.instance().getWarZone(playerChunkCoords);

            // Only award activity points if a player is in a war zone and that war zone
            // does NOT belong to the player's faction
            if (warZone == null)
                continue;
            if (playerFactionEntry.getValue().equals(warZone.getOccupier()))
                continue;

            var activityScoreEvent = new WarScoreEvent(this, playerFactionEntry.getKey(), playerFactionEntry.getValue(), WarScoreType.ACTIVITY, activityPoints);
            activityScoreEvent.callEvent();
        }

    }

    public void updateSiegesInWarZones() {

        var overrideActivityRequirement = UnitedWarsConfig.get().siegeSettings().overrideActivityRequirement();

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
        var settlement = citizen.getSettlement();
        for (var faction : getWarFactions()) {
            if (faction.getSettlements().stream().anyMatch(s -> s.equals(settlement)))
                return faction;
        }
        if (settlement.hasCountry()) {
            var country = settlement.getCountry();
            for (var faction : getWarFactions()) {
                if (faction.getCountries().stream().anyMatch(c -> c.equals(country)))
                    return faction;
            }
        }
        for (var faction : getWarFactions()) {
            if (faction.getMercenaries().stream().anyMatch(m -> m.equals(citizen)))
                return faction;
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
