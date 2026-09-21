package org.unitedlands.wars.classes.warzone;

import org.unitedlands.unitedlands.classes.Coordinates;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;

public class RegionWarZone extends WarZone {

    public RegionWarZone() {
        this.type = "region";
    }

    @Override
    public void generateAreas() {

        if (getGeopolObjectId() == null) {
            United.logger().error("No GeopolObject id found, cannot generate war zone area for " + getUuid(), "UnitedLands");
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

    // public void createMarker(Location beaconLoc, Material mineral) {
    //     World world = beaconLoc.getWorld();
    //     int cx = beaconLoc.getBlockX();
    //     int cy = beaconLoc.getBlockY();
    //     int cz = beaconLoc.getBlockZ();

    //     int minX = cx - 2, maxX = cx + 2;
    //     int minY = cy - 1, maxY = cy + 4;
    //     int minZ = cz - 2, maxZ = cz + 2;

    //     for (int x = cx - 1; x <= cx + 1; x++) {
    //         for (int z = cz - 1; z <= cz + 1; z++) {
    //             setBlock(world, x, cy - 1, z, mineral);
    //         }
    //     }

    //     setBlock(world, cx, cy, cz, Material.BEACON);

    //     for (int y = cy + 1; y < maxY; y++) {
    //         setBlock(world, cx, y, cz, Material.AIR);
    //     }

    //     for (int x = minX; x <= maxX; x++) {
    //         for (int y = minY; y <= maxY; y++) {
    //             for (int z = minZ; z <= maxZ; z++) {
    //                 boolean isShell = (x == minX || x == maxX
    //                         || y == minY || y == maxY
    //                         || z == minZ || z == maxZ);
    //                 if (!isShell)
    //                     continue;

    //                 if (x == cx && y == cy && z == cz)
    //                     continue;
    //                 if (y == cy - 1 && Math.abs(x - cx) <= 1 && Math.abs(z - cz) <= 1)
    //                     continue;

    //                 setBlock(world, x, y, z, Material.BARRIER);
    //             }
    //         }
    //     }
    // }

    // private static void setBlock(World world, int x, int y, int z, Material type) {
    //     Block block = world.getBlockAt(x, y, z);
    //     block.setType(type, false);
    // }

    // public static void removeMarker(Location beaconLoc) {

    //     World world = beaconLoc.getWorld();
    //     int cx = beaconLoc.getBlockX();
    //     int cy = beaconLoc.getBlockY();
    //     int cz = beaconLoc.getBlockZ();
    //     int radius = 2;
    //     int cageHeight = 4;

    //     for (int x = cx - radius; x <= cx + radius; x++) {
    //         for (int y = cy - 1; y <= cy + cageHeight; y++) {
    //             for (int z = cz - radius; z <= cz + radius; z++) {
    //                 world.getBlockAt(x, y, z).setType(Material.AIR, false);
    //             }
    //         }
    //     }
    // }
}
