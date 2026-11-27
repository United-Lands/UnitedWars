package org.unitedlands.wars.managers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.Coordinates;
import org.unitedlands.utils.United;
import org.unitedlands.wars.classes.config.UnitedWarsConfig;
import org.unitedlands.wars.classes.db.WarsDbService;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.classes.war.WarFaction;
import org.unitedlands.wars.classes.warcondition.WarConditionLastFactionAlive;
import org.unitedlands.wars.classes.warcondition.WarConditionOwnAllWarZones;
import org.unitedlands.wars.classes.warcondition.WarConditionOwnMinimumWarZones;
import org.unitedlands.wars.classes.warcondition.WarConditionOwnNoWarZones;
import org.unitedlands.wars.classes.warcondition.WarConditionReachScore;
import org.unitedlands.wars.classes.warcondition.WarCondition;
import org.unitedlands.wars.classes.wargoal.WarGoalClaimDispute;
import org.unitedlands.wars.classes.wargoal.WarGoalConquest;
import org.unitedlands.wars.classes.wargoal.WarGoalRevolt;
import org.unitedlands.wars.classes.wargoal.WarGoalSkirmish;
import org.unitedlands.wars.classes.wargoal.WarGoalSubjugation;
import org.unitedlands.wars.classes.wargoal.WarGoal;
import org.unitedlands.wars.classes.warzone.WarZone;
import org.unitedlands.wars.events.WarEndEvent;
import org.unitedlands.wars.events.WarPreRegisterEvent;
import org.unitedlands.wars.events.WarRegisteredEvent;
import org.unitedlands.wars.events.WarStartEvent;
import org.unitedlands.wars.utils.WarcampUtils;

public class WarManager {

    private static WarManager instance;

    public static final String DEFAULT_MILITARY_RANK = "default";

    public static WarManager instance() {
        return instance;
    }

    private final DatabaseManager databaseManager;

    private Map<String, WarGoal> warGoals = new HashMap<>();
    private Map<String, WarCondition> warConditions = new HashMap<>();

    private Map<UUID, War> pendingWars = new HashMap<>();
    private Map<UUID, War> activeWars = new HashMap<>();
    private Map<UUID, WarFaction> warFactions = new HashMap<>();
    private Map<UUID, WarZone> warZones = new HashMap<>();

    public WarManager(DatabaseManager databaseManager) {
        instance = this;
        this.databaseManager = databaseManager;

        initializeWarGoals();
        initializeWarConditions();
    }

    private void initializeWarGoals() {
        for (var warGoal : List.of(new WarGoalSkirmish(), new WarGoalRevolt(), new WarGoalClaimDispute(), new WarGoalConquest(),
                new WarGoalSubjugation())) {
            warGoals.put(warGoal.getId(), warGoal);
        }
    }

    private void initializeWarConditions() {
        for (var warCondition : List.of(new WarConditionReachScore(), new WarConditionOwnNoWarZones(), new WarConditionOwnMinimumWarZones(),
                new WarConditionOwnAllWarZones(), new WarConditionLastFactionAlive())) {
            warConditions.put(warCondition.getId(), warCondition);
        }
    }

    public void loadWars() {

        CompletableFuture<List<War>> warFuture = databaseManager.getWarService().getAllAsync();
        CompletableFuture<List<WarFaction>> warFactionsFuture = databaseManager.getWarFactionService().getAllAsync();
        CompletableFuture<List<WarZone>> warZonesFuture = databaseManager.getWarZoneService().getAllAsync();

        try {
            // Wait for the queries, then build the cache on the calling (main) thread
            CompletableFuture.allOf(warFuture, warFactionsFuture, warZonesFuture).get();
            buildWars(warFuture.join());
            buildWarfactions(warFactionsFuture.join());
            buildWarZones(warZonesFuture.join());

            // Markers of active wars are rendered in buildWarZones
            SiegeManager.instance().loadSiegeChunks();

        } catch (Exception ex) {
            United.logger().error("Loading failed: " + ex.getMessage(), "UnitedWars");
            throw new RuntimeException("App init failed", ex);
        }

    }

    private void buildWars(List<War> loadedWars) {
        for (var war : loadedWars) {
            if (war.hasEnded()) {
                // No need to load ended wars into memory
                continue;
            }
            if (war.isActive()) {
                activeWars.put(war.getUuid(), war);
            } else {
                pendingWars.put(war.getUuid(), war);
            }
        }
        United.logger().info("Loaded " + getWars().size() + " war(s) from the database.", "UnitedWars");

    }

    private void buildWarfactions(List<WarFaction> loadedWarFactions) {
        for (var faction : loadedWarFactions) {
            var war = faction.getWar();
            if (war == null) {
                // Faction belongs to an ended war
                continue;
            }
            addWarFaction(faction);
        }
        United.logger().info("Loaded " + loadedWarFactions.size() + " war faction(s) from the database.", "UnitedWars");
    }

    private void buildWarZones(List<WarZone> loadedWarZones) {
        for (var zone : loadedWarZones) {
            var war = zone.getWar();
            if (war == null) {
                // Zone belongs to an ended war
                continue;
            }

            switch (zone.getType()) {
            case "settlement":
                var settlementZone = zone.asSettlementWarZone();
                settlementZone.generateAreas();
                if (war.isActive())
                    settlementZone.renderMarkers();
                addWarZone(settlementZone);
                break;
            case "region":
                var regionZone = zone.asRegiontWarZone();
                regionZone.generateAreas();
                if (war.isActive())
                    regionZone.renderMarkers();
                addWarZone(regionZone);
                break;
            case "warcamp":
                var warcampZone = zone.asWarcampWarZone();
                warcampZone.generateAreas();
                if (war.isActive())
                    warcampZone.renderMarkers();
                addWarZone(warcampZone);
                break;
            }

            var cachedZone = getWarZone(zone.getUuid());
            if (cachedZone != null)
                cachedZone.setStateChanged(false);
        }
        United.logger().info("Loaded " + loadedWarZones.size() + " war zone(s) from the database.", "UnitedWars");

    }

    public void handleWars() {

        Set<War> startingWars = new HashSet<>();
        for (War war : pendingWars.values()) {
            if (war.canBeStarted()) {
                startingWars.add(war);
            }
        }

        for (var startingWar : startingWars) {
            pendingWars.remove(startingWar.getUuid());
            activeWars.put(startingWar.getUuid(), startingWar);
        }

        for (War war : startingWars) {
            startWar(war);
        }

        Set<War> endingWars = new HashSet<>();
        for (War war : activeWars.values()) {
            war.awardActivityScores();
            if (war.canBeEnded()) {
                endingWars.add(war);
            }
        }

        for (var endingWar : endingWars) {
            activeWars.remove(endingWar.getUuid());
        }

        for (War war : endingWars) {
            endWar(war);
        }

        saveChangedData();
    }

    public void saveChangedData() {
        for (var war : getWars()) {
            if (war.stateChanged()) {
                war.setStateChanged(false);
                WarsDbService.onWriteFailed(databaseManager.getWarService().updateAsync(war),
                        "war " + war.getUuid(), () -> war.setStateChanged(true));
            }
        }
        for (var warZone : warZones.values()) {
            if (warZone.stateChanged()) {
                warZone.setStateChanged(false);
                WarsDbService.onWriteFailed(databaseManager.getWarZoneService().updateAsync(warZone),
                        "war zone " + warZone.getUuid(), () -> warZone.setStateChanged(true));
            }
        }
        for (var warFaction : warFactions.values()) {
            if (warFaction.stateChanged()) {
                warFaction.setStateChanged(false);
                WarsDbService.onWriteFailed(databaseManager.getWarFactionService().updateAsync(warFaction),
                        "war faction " + warFaction.getUuid(), () -> warFaction.setStateChanged(true));
            }
        }
    }

    public void updateTownImmunities() {

    }

    public void registerWar(War war) {

        WarPreRegisterEvent warPreRegisterEvent = new WarPreRegisterEvent(war);
        warPreRegisterEvent.callEvent();
        if (warPreRegisterEvent.isCancelled() || !warPreRegisterEvent.isValid())
            return;

        pendingWars.put(war.getUuid(), war);

        // Cache war factions and war zones
        for (var faction : war.getWarFactions())
            addWarFaction(faction);
        for (var zone : war.getWarZones())
            addWarZone(zone);

        // Create database records
        databaseManager.getWarFactionService().createAllAsync(war.getWarFactions());
        databaseManager.getWarZoneService().createAllAsync(war.getWarZones());
        databaseManager.getWarService().createAsync(war);

        war.getWarGoal().onWarRegistered(war);

        war.refreshOnlinePlayerFactions();

        WarRegisteredEvent warRegisteredEvent = new WarRegisteredEvent(war);
        warRegisteredEvent.callEvent();
    }

    public void startWar(War war) {

        // Back chunk backups for griefable areas
        United.logger().info("Starting chunk backups...");
        for (var warZone : war.getWarZones()) {
            warZone.backupGriefZone();
            warZone.renderMarkers();
        }

        war.setActive(true);
        war.refreshOnlinePlayerFactions();
        war.updateSiegesInWarZones();

        WarStartEvent warStartEvent = new WarStartEvent(war);
        warStartEvent.callEvent();
    }

    public void endWar(War war) {

        if (Boolean.TRUE.equals(war.hasEnded()))
            return;

        war.setActive(false);
        war.setEnded(true);
        war.setEffectiveEndTime(System.currentTimeMillis());

        war.resolveWarGoal();

        SiegeManager.instance().cleanupSiegeChunks(war);

        // Clear cache and database for war factions and war zones
        for (var faction : war.getWarFactions()) {

            if (faction.getWarCampLocation() != null)
                WarcampUtils.removeWarcamp(faction.getWarCampLocation().getChunk());

            warFactions.remove(faction.getUuid());
            databaseManager.getWarFactionService().deleteAsync(faction);
        }
        for (var zone : war.getWarZones()) {
            warZones.remove(zone.getUuid());
            databaseManager.getWarZoneService().deleteAsync(zone);
        }

        // Restore griefable areas
        for (var warZone : war.getWarZones()) {
            warZone.removeMarkers();
            warZone.restoreGriefZone();
        }

        activeWars.remove(war.getUuid());
        pendingWars.remove(war.getUuid());

        databaseManager.getWarService().updateAsync(war);

        for (var onlinePlayer : Bukkit.getOnlinePlayers()) {
            validateMetaData(onlinePlayer);
        }

        (new WarEndEvent(war)).callEvent();
    }

    public boolean anyWarsPending() {
        return pendingWars.size() > 0;
    }

    public boolean anyWarsActive() {
        return activeWars.size() > 0;
    }

    public void validateMetaData(Player player) {
        WarMetaDataManager.instance().validateWarLivesMetaData(player);
        WarMetaDataManager.instance().validateFactionPermissions(player);
        WarMetaDataManager.instance().validateMilitaryRankMetaData(player);
    }

    public void updatePlayerLists() {
        for (var pendingWar : pendingWars.values())
            pendingWar.refreshOnlinePlayerFactions();
        for (var activeWar : activeWars.values())
            activeWar.refreshOnlinePlayerFactions();
    }

    public void removeQuittingPlayer() {
        for (var pendingWar : pendingWars.values())
            pendingWar.refreshOnlinePlayerFactions();
        for (var activeWar : activeWars.values())
            activeWar.refreshOnlinePlayerFactions();
    }

    public boolean citizenHasMilitaryRank(Citizen citizen) {

        // Mayors and country leader always count as having a military rank
        if (citizen.isMayor() || citizen.isLeader())
            return true;

        return !getCitizenMilitaryRank(citizen).equals(DEFAULT_MILITARY_RANK);
    }

    public String getCitizenMilitaryRank(Citizen citizen) {
        var rank = WarMetaDataManager.instance().getMilitaryRank(citizen);
        if (rank == null || !UnitedWarsConfig.get().militaryRanks().has(rank))
            return DEFAULT_MILITARY_RANK;
        return rank;
    }

    public List<String> getAssignableMilitaryRanks() {
        return UnitedWarsConfig.get().militaryRanks().keys().stream()
                .filter(rank -> !rank.equals(DEFAULT_MILITARY_RANK))
                .collect(Collectors.toList());
    }

    public boolean isAssignableMilitaryRank(String rank) {
        return !rank.equals(DEFAULT_MILITARY_RANK) && UnitedWarsConfig.get().militaryRanks().has(rank);
    }

    public Citizen getUniqueMilitaryRankHolder(Citizen target, String rank) {
        var rankSettings = UnitedWarsConfig.get().militaryRanks().get(rank);
        if (rankSettings == null || !rankSettings.unique())
            return null;
        var country = target.getCountry();
        if (country == null)
            return null;
        for (var citizen : country.getCitizens()) {
            if (!citizen.equals(target) && getCitizenMilitaryRank(citizen).equals(rank))
                return citizen;
        }
        return null;
    }

    public List<String> getWarGoalIds() {
        return warGoals.keySet().stream().collect(Collectors.toList());
    }

    public WarGoal getWarGoal(String id) {
        return warGoals.get(id);
    }

    public WarCondition getWarCondition(String id) {
        return warConditions.get(id);
    }

    public Collection<War> getWars() {
        var allWars = new ArrayList<>(activeWars.values());
        allWars.addAll(pendingWars.values());
        return allWars;
    }

    public Collection<War> getActiveWars() {
        return activeWars.values();
    }

    public Collection<War> getPendingWars() {
        return pendingWars.values();
    }

    public War getWar(UUID id) {
        if (pendingWars.containsKey(id))
            return pendingWars.get(id);
        if (activeWars.containsKey(id))
            return activeWars.get(id);
        return null;
    }

    public War getWar(String title) {
        var pending = pendingWars.values().stream().filter(w -> title.equals(w.getTitle())).findFirst().orElse(null);
        if (pending != null)
            return pending;
        var active = activeWars.values().stream().filter(w -> title.equals(w.getTitle())).findFirst().orElse(null);
        if (active != null)
            return active;
        return null;
    }

    public List<String> getPendingWarTitles() {
        return pendingWars.values().stream().map(War::getTitle).collect(Collectors.toList());
    }

    public List<String> getActiveWarTitles() {
        return activeWars.values().stream().map(War::getTitle).collect(Collectors.toList());
    }

    public List<String> getWarTitles() {
        var titles = getPendingWarTitles();
        titles.addAll(getActiveWarTitles());
        return titles;
    }

    public void addWarFaction(WarFaction faction) {
        warFactions.put(faction.getUuid(), faction);
    }

    public void removeWarFaction(WarFaction faction) {
        warFactions.remove(faction.getUuid());
    }

    public List<String> getWarFactionNames() {
        return warFactions.values().stream().map(WarFaction::getName).toList();
    }

    public WarFaction getWarFaction(UUID id) {
        return warFactions.get(id);
    }

    public WarFaction getWarFaction(String name) {
        return warFactions.values().stream().filter(f -> name.equals(f.getName())).findFirst().orElse(null);
    }

    public void addWarZone(WarZone warZone) {
        warZones.put(warZone.getUuid(), warZone);
    }

    public void removeWarZone(WarZone warZone) {
        warZones.remove(warZone.getUuid());
    }

    public WarZone getWarZone(UUID id) {
        return warZones.get(id);
    }

    public WarZone getWarZone(Coordinates coords) {
        return warZones.values().stream().filter(z -> z.isInArea(coords)).findAny().orElse(null);
    }

    public boolean isChunkInWarZone(Coordinates coords) {
        return warZones.values().stream().anyMatch(z -> z.isInArea(coords));
    }

    public Set<War> getPendingPlayerWars(Player player) {
        return pendingWars.values().stream().filter(w -> w.isPlayerInWar(player)).collect(Collectors.toSet());
    }

    public Set<War> getActivePlayerWars(Player player) {
        return activeWars.values().stream().filter(w -> w.isPlayerInWar(player)).collect(Collectors.toSet());
    }

    public Set<War> getPlayerWars(Player player) {
        var wars = getPendingPlayerWars(player);
        wars.addAll(getActivePlayerWars(player));
        return wars;
    }

    public WarZone getActivePlayerWarZone(Player player, Coordinates coords) {
        var warZone = getWarZone(coords);
        if (warZone != null) {
            if (getActivePlayerWars(player).stream().anyMatch(w -> w.hasChunkInWarZone(coords)))
                return warZone;
        }
        return null;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

}
