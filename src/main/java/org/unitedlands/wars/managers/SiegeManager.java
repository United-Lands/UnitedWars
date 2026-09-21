package org.unitedlands.wars.managers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.unitedlands.unitedlands.classes.Coordinates;
import org.unitedlands.utils.United;
import org.unitedlands.wars.UnitedWars;
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
            CompletableFuture.allOf(siegeChunksFuture).thenRun(() -> {
                buildSiegeChunks(siegeChunksFuture.join());
            }).get();
        } catch (Exception ex) {
            United.logger().error("Loading failed: " + ex.getMessage(), "UnitedWars");
            throw new RuntimeException("App init failed", ex);
        }
    }

    private void buildSiegeChunks(List<SiegeChunk> loadedSiegeChunks) {
        for (var siegeChunk : loadedSiegeChunks) {
            siegeChunks.put(siegeChunk.getCoordinates(), siegeChunk);
        }
        United.logger().info("Loaded " + loadedSiegeChunks.size() + " siege chunk(s) from the database.", "UnitedWars");
    }

    public void handleSiegeChunks() {
        for (var siegeChunk : activeSiegeChunks) {
            siegeChunk.updateHealth();
            SiegeChunkDisplayManager.instance().updateHealthBar(siegeChunk);

            if (siegeChunk.stateChanged())
                databaseManager.getSiegeChunkService().updateAsync(siegeChunk);
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

        // if (player.getGameMode() != GameMode.SURVIVAL)
        // return;
        if (player.hasPotionEffect(PotionEffectType.INVISIBILITY) || player.isInvisible())
            return;

        if (fromCoordinates != null && siegeChunks.containsKey(fromCoordinates)) {
            removePlayerFromSiegeChunk(player, fromCoordinates);
        }

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

        ConfigurationSection chunkHealthSettings = UnitedWars.instance().getConfig().getConfigurationSection("siege-settings.chunk-max-health");
        if (chunkHealthSettings == null) {
            United.logger().error("Couldn't find chunk health settings, aborting.", "UnitedWars");
            return false;
        }

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
            .filter(c -> c.getWar().equals(war))
            .collect(Collectors.toSet());

        for (var siegeChunk : warSiegeChunks) {

            activeSiegeChunks.remove(siegeChunk);
            siegeChunks.remove(siegeChunk.getCoordinates());
            SiegeChunkDisplayManager.instance().removeHealthBar(siegeChunk);

            databaseManager.getSiegeChunkService().deleteAllAsync(warSiegeChunks);
        }

    }

}
