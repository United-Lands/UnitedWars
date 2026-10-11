package org.unitedlands.wars.classes.warzone;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.unitedlands.unitedlands.classes.Coordinates;
import org.unitedlands.unitedlands.integrations.Pl3xMap.Pl3xMapRenderer;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;
import org.unitedlands.wars.UnitedWars;
import org.unitedlands.wars.classes.config.UnitedWarsConfig;

public class WarZoneRegion extends WarZone {

    public WarZoneRegion() {
        this.type = "region";
    }

    @Override 
    public String getName() {
        return UnitedLandsDataManager.instance().getRegion(this.geopolObjectId).getCleanName();
    }

    @Override
    public void generateAreas() {

        if (getGeopolObjectId() == null) {
            United.logger().error("No GeopolObject id found, cannot generate war zone area for " + getUuid());
            return;
        }

        var region = UnitedLandsDataManager.instance().getRegion(getGeopolObjectId());
        if (region == null) {
            United.logger().error("Could not find region with id " + getGeopolObjectId() + " for war zone " + getUuid(),
                    "UnitedLands");
            return;
        }

        var homeChunk = region.getHomeChunkCoordinates();
        this.captureArea.add(new CaptureZoneChunk(homeChunk, "region-home", true));

        var startX = homeChunk.getX() - 2;
        var startZ = homeChunk.getZ() - 2;
        var endX = homeChunk.getX() + 2;
        var endZ = homeChunk.getZ() + 2;

        for (var x = startX; x <= endX; x++) {
            for (var z = startZ; z <= endZ; z++) {
                var coords = new Coordinates(x, z, homeChunk.getWorldName());
                this.area.add(coords);
            }
        }

    }

    @Override 
    public void renderMarkers() {

        if (!UnitedWarsConfig.get().siegeSettings().captureMarkers().use())
            return;

        var layerKey = UnitedWarsConfig.get().siegeSettings().captureMarkers().layerKey();
        var layerName = UnitedWarsConfig.get().siegeSettings().captureMarkers().layerName();

        Bukkit.getScheduler().runTaskAsynchronously(UnitedWars.instance(), () -> {
            var marker = "siege";
            for (var chunk : captureArea) {
                if ("region-home".equals(chunk.type()))
                    marker = "home";
                var location = new Location(chunk.coords().getWorld(), chunk.coords().getX() * 16 + 8, 0, chunk.coords().getZ() * 16 + 8);
                Pl3xMapRenderer.instance().renderMarker(location.getWorld().getName(), location, marker, getMarkerKey(chunk.coords()), layerKey, layerName);
            }
        });
    }

    @Override 
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
