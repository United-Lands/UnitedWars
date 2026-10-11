package org.unitedlands.wars.classes.warzone;

import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.unitedlands.unitedlands.classes.Coordinates;
import org.unitedlands.unitedlands.classes.SettlementChunk;
import org.unitedlands.unitedlands.integrations.Pl3xMap.Pl3xMapRenderer;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;
import org.unitedlands.wars.UnitedWars;
import org.unitedlands.wars.classes.config.UnitedWarsConfig;

public class WarZoneSettlement extends WarZone {

    public WarZoneSettlement() {
        this.type = "settlement";
    }

    @Override 
    public String getName() {
        return UnitedLandsDataManager.instance().getSettlement(this.geopolObjectId).getCleanName();
    }

    @Override
    public void generateAreas() {

        if (getGeopolObjectId() == null) {
            United.logger().error("No GeopolObject id found, cannot generate war zone area for " + getUuid());
            return;
        }

        var settlement = UnitedLandsDataManager.instance().getSettlement(getGeopolObjectId());
        if (settlement == null) {
            United.logger().error("Could not find settlement with id " + getGeopolObjectId() + " for war zone " + getUuid());
            return;
        }

        this.area = settlement.getChunks().stream().map(SettlementChunk::getCoordinates).collect(Collectors.toSet());

        var homeChunk = settlement.getHomeChunkCoordinates();
        this.captureArea.add(new CaptureZoneChunk(homeChunk, "home", true));

        var fortressChunks = settlement.getChunksOfType("fortress");
        if (fortressChunks != null && fortressChunks.size() > 0) {
            for (var fortressChunk : fortressChunks) {

                this.captureArea.add(new CaptureZoneChunk(fortressChunk.getCoordinates(), "fortress", true));

                var startX = fortressChunk.getCoordinates().getX();
                var startZ = fortressChunk.getCoordinates().getZ();
                var endX = startX + 5;
                var endZ = startZ + 5;

                for (var x = startX; x <= endX; x++) {
                    for (var z = startZ; z <= endZ; z++) {
                        var coords = new Coordinates(x, z, fortressChunk.getWorldName());
                        if (settlement.hasChunkAtCoordinates(coords))
                            this.griefArea.add(coords);
                    }
                }
            }
        }

    }

    public void renderMarkers() {

        if (!UnitedWarsConfig.get().siegeSettings().captureMarkers().use())
            return;

        var layerKey = UnitedWarsConfig.get().siegeSettings().captureMarkers().layerKey();
        var layerName = UnitedWarsConfig.get().siegeSettings().captureMarkers().layerName();

        Bukkit.getScheduler().runTaskAsynchronously(UnitedWars.instance(), () -> {
            var marker = "siege";
            for (var chunk : captureArea) {
                if ("home".equals(chunk.type()))
                    marker = "home";
                else if ("fortress".equals(chunk.type()))
                    marker = "fortress";
                var location = new Location(chunk.coords().getWorld(), chunk.coords().getX() * 16 + 8, 0, chunk.coords().getZ() * 16 + 8);
                Pl3xMapRenderer.instance().renderMarker(location.getWorld().getName(), location, marker, getMarkerKey(chunk.coords()), layerKey, layerName);
            }
        });
    }

    public void removeMarkers() {

        var layerKey = UnitedWarsConfig.get().siegeSettings().captureMarkers().layerKey();
        var layerName = UnitedWarsConfig.get().siegeSettings().captureMarkers().layerName();

        Bukkit.getScheduler().runTaskAsynchronously(UnitedWars.instance(), () -> {
            for (var chunk : captureArea) {
                Pl3xMapRenderer.instance().removeMarker(chunk.coords().getWorld().getName(), getMarkerKey(chunk.coords()), layerKey, layerName);
            }
        });
    }


}
