package org.unitedlands.wars.classes.warzone;

import java.util.stream.Collectors;

import org.unitedlands.unitedlands.classes.Coordinates;
import org.unitedlands.unitedlands.classes.SettlementChunk;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

public class SettlementWarZone extends WarZone {

    public SettlementWarZone() {
        this.type = "settlement";
    }

    @Override
    public void generateAreas() {

        if (getGeopolObjectId() == null) {
            United.logger().error("No GeopolObject id found, cannot generate war zone area for " + getUuid(), "UnitedLands");
            return;
        }

        var settlement = UnitedLandsDataManager.instance().getSettlement(getGeopolObjectId());
        if (settlement == null) {
            United.logger().error("Could not find settlement with id " + getGeopolObjectId() + " for war zone " + getUuid(),
                    "UnitedLands");
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

}
