package org.unitedlands.wars.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.unitedlands.unitedlands.classes.Coordinates;
import org.unitedlands.utils.United;
import org.unitedlands.wars.classes.db.WarsDbService;
import org.unitedlands.wars.classes.war.SiegeChunk;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.classes.war.WarFaction;

public class SiegeManager {

    private static SiegeManager instance;

    public static SiegeManager instance() {
        return instance;
    }

    private final DatabaseManager databaseManager;
    private Map<Coordinates, SiegeChunk> siegeChunks = new HashMap<>();
    private Set<SiegeChunk> activeSiegeChunks = new HashSet<>();

    public SiegeManager(DatabaseManager databaseManager) {
        instance = this;
        this.databaseManager = databaseManager;
    }

    public void loadSiegeChunks() {
        CompletableFuture<List<SiegeChunk>> siegeChunksFuture = databaseManager.getSiegeChunkService().getAllAsync();

        try {
            buildSiegeChunks(siegeChunksFuture.get());
        } catch (Exception ex) {
            United.logger().error("Loading failed: " + ex.getMessage(), "UnitedWars");
            throw new RuntimeException("App init failed", ex);
        }
    }

    private void buildSiegeChunks(List<SiegeChunk> loadedSiegeChunks) {
        Set<SiegeChunk> orphanedChunks = new HashSet<>();
        for (var siegeChunk : loadedSiegeChunks) {
            // Chunks of wars that are no longer loaded would block new sieges at the same coordinates.
            if (WarManager.instance().getWar(siegeChunk.getWarId()) == null) {
                orphanedChunks.add(siegeChunk);
                continue;
            }
            siegeChunks.put(siegeChunk.getCoordinates(), siegeChunk);
        }
        United.logger().info("Loaded " + siegeChunks.size() + " siege chunk(s) from the database.", "UnitedWars");

        if (!orphanedChunks.isEmpty()) {
            United.logger().info("Removing " + orphanedChunks.size() + " siege chunk(s) of ended wars.", "UnitedWars");
            databaseManager.getSiegeChunkService().deleteAllAsync(orphanedChunks);
        }
    }

    public void handleSiegeChunks() {
        for (var siegeChunk : new ArrayList<>(activeSiegeChunks)) {
            siegeChunk.updateHealth();
            SiegeChunkDisplayManager.instance().updateHealthBar(siegeChunk);
        }
        saveChangedSiegeChunks();
    }

    public void saveChangedSiegeChunks() {
        for (var siegeChunk : siegeChunks.values()) {
            if (!siegeChunk.stateChanged())
                continue;
            siegeChunk.setStateChanged(false);
            WarsDbService.onWriteFailed(databaseManager.getSiegeChunkService().updateAsync(siegeChunk),
                    "siege chunk " + siegeChunk.getUuid(), () -> siegeChunk.setStateChanged(true));
        }
    }

    public void removePlayerFromAllSiegeChunks(Player player) {
        for (var siegeChunk : new ArrayList<>(activeSiegeChunks)) {
            siegeChunk.removePlayer(player);
            if (!siegeChunk.hasPlayersInChunk())
                activeSiegeChunks.remove(siegeChunk);
            SiegeChunkDisplayManager.instance().removePlayerFromHealthBar(siegeChunk, player);
        }
    }

    public void updateSieges() {
        for (var war : WarManager.instance().getActiveWars()) {
            war.updateSiegesInWarZones();
        }
    }

    public SiegeChunk getSiegeChunk(Coordinates coords) {
        return siegeChunks.get(coords);
    }

    public void updatePlayersInChunk(Player player, Coordinates fromCoordinates, Coordinates toCoordinates) {

        if (fromCoordinates != null && siegeChunks.containsKey(fromCoordinates)) {
            removePlayerFromSiegeChunk(player, fromCoordinates);
        }

        // TODO: Reactivate!!

        // if (player.getGameMode() != GameMode.SURVIVAL)
        // return;
        if (player.hasPotionEffect(PotionEffectType.INVISIBILITY) || player.isInvisible())
            return;

        if (toCoordinates != null) {
            if (!siegeChunks.containsKey(toCoordinates)) {
                if (createSiegeChunk(player, toCoordinates))
                    addPlayerToSiegeChunk(player, toCoordinates);
            } else {
                addPlayerToSiegeChunk(player, toCoordinates);
            }
        }

    }

    private boolean createSiegeChunk(Player player, Coordinates toCoordinates) {

        Set<War> playerWars = WarManager.instance().getActivePlayerWars(player);
        for (var war : playerWars) {

            if (war.hasChunkInWarZone(toCoordinates)) {

                var zone = war.getWarZoneForChunk(toCoordinates);

                var siegeChunk = SiegeChunk.create(zone, toCoordinates);
                if (siegeChunk == null) {
                    United.logger().error("Could not create siege chunk at " + toCoordinates, "UnitedWars");
                    return false;
                }

                siegeChunks.put(toCoordinates, siegeChunk);
                siegeChunk.setStateChanged(false);
                databaseManager.getSiegeChunkService().createAsync(siegeChunk);

                return true;
            }
        }

        return false;
    }

    private void addPlayerToSiegeChunk(Player player, Coordinates toCoordinates) {

        var siegeChunk = siegeChunks.get(toCoordinates);
        if (siegeChunk == null)
            return;

        if (WarMetaDataManager.instance().getWarLives(player, siegeChunk.getWar()) == 0)
            return;

        siegeChunk.addPlayer(player);
        activeSiegeChunks.add(siegeChunk);
        SiegeChunkDisplayManager.instance().addPlayerToHealthBar(siegeChunk, player);
    }

    private void removePlayerFromSiegeChunk(Player player, Coordinates fromCoordinates) {

        var siegeChunk = siegeChunks.get(fromCoordinates);
        if (siegeChunk == null)
            return;
        siegeChunk.removePlayer(player);
        if (!siegeChunk.hasPlayersInChunk())
            activeSiegeChunks.remove(siegeChunk);
        SiegeChunkDisplayManager.instance().removePlayerFromHealthBar(siegeChunk, player);
    }

    public boolean isSiegeChunkOccupied(Coordinates coords) {
        if (!siegeChunks.containsKey(coords))
            return false;
        return siegeChunks.get(coords).isOccupied();
    }

    public WarFaction getSiegeChunkOwner(Coordinates coords) {
        if (!siegeChunks.containsKey(coords))
            return null;
        return siegeChunks.get(coords).getOwner();
    }

    public void cleanupSiegeChunks(War war) {

        Set<SiegeChunk> warSiegeChunks = siegeChunks.values().stream()
                .filter(c -> war.getUuid().equals(c.getWarId()))
                .collect(Collectors.toSet());

        if (warSiegeChunks.isEmpty())
            return;

        for (var siegeChunk : warSiegeChunks) {
            activeSiegeChunks.remove(siegeChunk);
            siegeChunks.remove(siegeChunk.getCoordinates());
            SiegeChunkDisplayManager.instance().removeHealthBar(siegeChunk);
        }

        databaseManager.getSiegeChunkService().deleteAllAsync(warSiegeChunks);

    }

}
