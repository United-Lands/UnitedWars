package org.unitedlands.wars.classes.warzone;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.unitedlands.unitedlands.integrations.Pl3xMap.Pl3xMapRenderer;
import org.unitedlands.unitedlands.utils.CoordinateUtils;
import org.unitedlands.utils.United;
import org.unitedlands.wars.UnitedWars;
import org.unitedlands.wars.classes.war.SiegeChunk;
import org.unitedlands.wars.classes.config.UnitedWarsConfig;

public class WarZoneWarcamp extends WarZone {

    public WarZoneWarcamp() {
        this.type = "warcamp";
    }

    @Override
    public String getName() {
        return "Warcamp (" + getFaction().getCleanName() + ")";
    }

    @Override
    public boolean isObjective() {
        return false;
    }

    @Override
    public void onSiegeChunkCaptured(SiegeChunk siegeChunk) {
        if (siegeChunk.getOwner() != null)
            setOccupier(siegeChunk.getOwner());
    }

    @Override
    public void generateAreas() {

        if (getFaction().getWarCampLocation() == null) {
            United.logger().error("No war zone location in faction found, cannot generate war zone area for " + getUuid());
            return;
        }

        this.area.add(CoordinateUtils.locationToChunkCoordinates(getFaction().getWarCampLocation()));
    }

    @Override
    public void renderMarkers() {

        if (!UnitedWarsConfig.get().siegeSettings().captureMarkers().use())
            return;

        if (this.getFaction().getWarCampLocation() == null)
            return;

        var layerKey = UnitedWarsConfig.get().siegeSettings().captureMarkers().layerKey();
        var layerName = UnitedWarsConfig.get().siegeSettings().captureMarkers().layerName();

        United.logger().debug("Loading war camp chunk...");
        var coords = CoordinateUtils.locationToChunkCoordinates(this.getFaction().getWarCampLocation());

        Bukkit.getScheduler().runTaskAsynchronously(UnitedWars.instance(), () -> {
            var marker = "camp";
            var location = new Location(coords.getWorld(), coords.getX() * 16 + 8, 0, coords.getZ() * 16 + 8);
            Pl3xMapRenderer.instance().renderMarker(location.getWorld().getName(), location, marker, getMarkerKey(coords), layerKey, layerName);

        });
    }

    @Override
    public void removeMarkers() {

        var layerKey = UnitedWarsConfig.get().siegeSettings().captureMarkers().layerKey();
        var layerName = UnitedWarsConfig.get().siegeSettings().captureMarkers().layerName();

        var coords = CoordinateUtils.locationToChunkCoordinates(this.getFaction().getWarCampLocation());

        Bukkit.getScheduler().runTaskAsynchronously(UnitedWars.instance(), () -> {
            Pl3xMapRenderer.instance().removeMarker(coords.getWorld().toString(), getMarkerKey(coords), layerKey, layerName);

        });
    }

}
