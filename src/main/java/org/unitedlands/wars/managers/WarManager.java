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

import org.bukkit.entity.Player;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.Coordinates;
import org.unitedlands.utils.United;
import org.unitedlands.wars.UnitedWars;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.classes.war.WarFaction;
import org.unitedlands.wars.classes.warcondition.LastFactionAliveContition;
import org.unitedlands.wars.classes.warcondition.OwnAllWarZonesCondition;
import org.unitedlands.wars.classes.warcondition.OwnMinimumWarZonesCondition;
import org.unitedlands.wars.classes.warcondition.OwnNoWarZonesCondition;
import org.unitedlands.wars.classes.warcondition.ReachScoreCondition;
import org.unitedlands.wars.classes.warcondition.WarCondition;
import org.unitedlands.wars.classes.wargoal.ClaimDisputeWarGoal;
import org.unitedlands.wars.classes.wargoal.ConquestWarGoal;
import org.unitedlands.wars.classes.wargoal.RevoltWarGoal;
import org.unitedlands.wars.classes.wargoal.SkirmishWarGoal;
import org.unitedlands.wars.classes.wargoal.SubjugationWarGoal;
import org.unitedlands.wars.classes.wargoal.WarGoal;
import org.unitedlands.wars.classes.warzone.WarZone;
import org.unitedlands.wars.events.WarEndEvent;
import org.unitedlands.wars.events.WarPreRegisterEvent;
import org.unitedlands.wars.events.WarRegisteredEvent;
import org.unitedlands.wars.events.WarStartEvent;

public class WarManager {

    private static WarManager instance;

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
        warGoals.put("skirmish", new SkirmishWarGoal());
        warGoals.put("revolt", new RevoltWarGoal());
        warGoals.put("claim_dispute", new ClaimDisputeWarGoal());
        warGoals.put("conquest", new ConquestWarGoal());
        warGoals.put("subjugation", new SubjugationWarGoal());
    }

    private void initializeWarConditions() {
        warConditions.put("reach_score", new ReachScoreCondition());
        warConditions.put("own_no_war_zones", new OwnNoWarZonesCondition());
        warConditions.put("own_minimum_war_zones", new OwnMinimumWarZonesCondition());
        warConditions.put("own_all_war_zones", new OwnAllWarZonesCondition());
        warConditions.put("last_faction_alive", new LastFactionAliveContition());
    }

    public void loadWars() {

        CompletableFuture<List<War>> warFuture = databaseManager.getWarService().getAllAsync();
        CompletableFuture<List<WarFaction>> warFactionsFuture = databaseManager.getWarFactionService().getAllAsync();
        CompletableFuture<List<WarZone>> warZonesFuture = databaseManager.getWarZoneService().getAllAsync();

        try {
            CompletableFuture.allOf(warFuture, warFactionsFuture, warZonesFuture).thenRun(() -> {
                buildWars(warFuture.join());
                buildWarfactions(warFactionsFuture.join());
                buildWarZones(warZonesFuture.join());
            }).get();

            SiegeManager.instance().loadSiegeChunks();

            for (var zone : warZones.values()) {
                if (zone.getWar().isActive()) {
                    zone.renderMarkers();
                }
            }

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
            United.logger().info("Loaded " + loadedWars.size() + " war(s) from the database.", "UnitedWars");
        }

    }

    private void buildWarfactions(List<WarFaction> loadedWarFactions) {
        for (var faction : loadedWarFactions) {
            var war = faction.getWar();
            if (war == null) {
                // Faction belongs to an ended war
                continue;
            }
            warFactions.put(faction.getUuid(), faction);
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
                    zone.renderMarkers();
                warZones.put(zone.getUuid(), settlementZone);
                break;
            case "region":
                var regionZone = zone.asRegiontWarZone();
                regionZone.generateAreas();
                if (war.isActive())
                    regionZone.renderMarkers();
                warZones.put(zone.getUuid(), regionZone);
                break;
            }

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

    private void saveChangedData() {
        for (var war : pendingWars.values()) {
            if (war.stateChanged()) {
                databaseManager.getWarService().updateAsync(war);
                war.setStateChanged(false);
            }
        }
        for (var war : activeWars.values()) {
            if (war.stateChanged()) {
                databaseManager.getWarService().updateAsync(war);
                war.setStateChanged(false);
            }
        }
        for (var warZone : warZones.values()) {
            if (warZone.stateChanged()) {
                databaseManager.getWarZoneService().updateAsync(warZone);
                warZone.setStateChanged(false);
            }
        }
        for (var warFaction : warFactions.values()) {
            if (warFaction.stateChanged()) {
                databaseManager.getWarFactionService().updateAsync(warFaction);
                warFaction.setStateChanged(false);
            }
        }
    }

    public void updateTownImmunities() {

    }

    public void registerWar(War war) {

        WarPreRegisterEvent warPreRegisterEvent = new WarPreRegisterEvent(war);
        warPreRegisterEvent.callEvent();
        if (warPreRegisterEvent.isCancelled())
            return;

        pendingWars.put(war.getUuid(), war);

        // Cache war factions and war zones
        for (var faction : war.getWarFactions())
            warFactions.put(faction.getUuid(), faction);
        for (var zone : war.getWarZones())
            warZones.put(zone.getUuid(), zone);

        // Create database records
        databaseManager.getWarFactionService().createAllAsync(war.getWarFactions());
        databaseManager.getWarZoneService().createAllAsync(war.getWarZones());
        databaseManager.getWarService().createAsync(war);

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

        war.resolveWarGoal();

        SiegeManager.instance().cleanupSiegeChunks(war);

        // Clear cache for war factions and war zones
        for (var faction : war.getWarFactions())
            warFactions.remove(faction.getUuid());
        for (var zone : war.getWarZones())
            warZones.remove(zone.getUuid());

        // Restore griefable areas
        for (var warZone : war.getWarZones()) {
            warZone.removeMarkers();
            warZone.restoreGriefZone();
        }

        war.setActive(false);
        war.setEnded(true);
        war.setEffectiveEndTime(System.currentTimeMillis());

        activeWars.remove(war.getUuid());

        databaseManager.getWarService().updateAsync(war);

        (new WarEndEvent(war)).callEvent();
    }

    public boolean anyWarsPending() {
        return pendingWars.size() > 0;
    }

    public boolean anyWarsActive() {
        return activeWars.size() > 0;
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
        var ranks = UnitedWars.instance().getConfig().getConfigurationSection("military-ranks").getKeys(false);
        var intersection = new ArrayList<String>(citizen.getSettlementRanks());
        intersection.retainAll(ranks);
        return intersection.size() > 0;
    }

    public String getCitizenMilitaryRank(Citizen resident) {
        var ranks = getMilitaryRanks();
        var settlementRanks = resident.getSettlementRanks();
        var countryRanks = resident.getCountryRanks();
        for (var level : ranks.keySet()) {
            for (var rank : ranks.get(level)) {
                if (settlementRanks.contains(rank) || countryRanks.contains(rank))
                    return rank;
            }
        }
        return "default";
    }

    public Map<String, List<String>> getMilitaryRanks() {
        Map<String, List<String>> result = new HashMap<>();
        var militaryRanks = UnitedWars.instance().getConfig().getConfigurationSection("military-ranks").getKeys(false);
        for (var configRank : militaryRanks) {
            var level = UnitedWars.instance().getConfig().getString("military-ranks." + configRank + ".level");
            result.computeIfAbsent(level, v -> new ArrayList<String>()).add(configRank);
        }
        return result;
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

    public Collection<War> getActiveWars() {
        return activeWars.values();
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

    public WarFaction getWarFaction(UUID id) {
        return warFactions.get(id);
    }

    public WarFaction getWarFaction(String name) {
        return warFactions.values().stream().filter(f -> name.equals(f.getName())).findFirst().orElse(null);
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

}
