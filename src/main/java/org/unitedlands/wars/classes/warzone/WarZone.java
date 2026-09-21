package org.unitedlands.wars.classes.warzone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.unitedlands.restoration.UnitedRestoration;
import org.unitedlands.unitedlands.classes.Coordinates;
import org.unitedlands.unitedlands.classes.db.Identifiable;
import org.unitedlands.unitedlands.integrations.Pl3xMap.Pl3xMapRenderer;
import org.unitedlands.unitedlands.libs.ormlite.field.DatabaseField;
import org.unitedlands.unitedlands.libs.ormlite.table.DatabaseTable;
import org.unitedlands.utils.United;
import org.unitedlands.wars.UnitedWars;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.classes.war.WarFaction;
import org.unitedlands.wars.managers.SiegeManager;
import org.unitedlands.wars.managers.WarManager;

@DatabaseTable
public class WarZone implements Identifiable {

    public static record CaptureZoneChunk(Coordinates coords, String type, boolean occupiable) {
    };

    @DatabaseField(id = true, width = 36, canBeNull = false)
    private UUID uuid;

    @DatabaseField(canBeNull = false)
    protected String type;
    @DatabaseField(width = 36, canBeNull = false)
    protected UUID geopolObjectId;

    @DatabaseField(width = 36, canBeNull = false, columnName = "war_id")
    private UUID warId;

    private transient War war;

    @DatabaseField(columnName = "is_occupied")
    private boolean isOccupied;
    @DatabaseField(width = 36, columnName = "faction_id")
    private UUID factionId;
    @DatabaseField(width = 36, columnName = "occupier_id")
    private UUID occupierId;

    private transient boolean siegeEnabled;

    private transient WarFaction faction;
    private transient WarFaction occupier;

    private boolean stateChanged;

    protected transient Set<Coordinates> area = new HashSet<>();
    protected transient Set<Coordinates> griefArea = new HashSet<>();
    protected transient Set<CaptureZoneChunk> captureArea = new HashSet<>();

    public WarZone() {

    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public UUID getGeopolObjectId() {
        return geopolObjectId;
    }

    public void setGeopolObjectId(UUID geopolObjectId) {
        this.geopolObjectId = geopolObjectId;
    }

    public UUID getWarId() {
        return warId;
    }

    public void setWarId(UUID warId) {
        this.warId = warId;
    }

    public UUID getFactionId() {
        return factionId;
    }

    public void setFactionId(UUID factionId) {
        this.factionId = factionId;
    }

    public WarFaction getFaction() {
        if (faction == null && factionId != null)
            faction = WarManager.instance().getWarFaction(factionId);
        return faction;
    }

    public void setFaction(WarFaction faction) {
        this.factionId = faction.getUuid();
        this.faction = faction;
    }

    public UUID getOccupierId() {
        return occupierId;
    }

    public void setOccupierId(UUID occupierId) {
        this.occupierId = occupierId;
    }

    public WarFaction getOccupier() {
        if (occupier == null && occupierId != null)
            occupier = WarManager.instance().getWarFaction(occupierId);
        return occupier;
    }

    public void setOccupier(WarFaction occupier) {
        this.occupierId = occupier.getUuid();
        this.occupier = occupier;
    }

    public War getWar() {
        if (war == null && warId != null)
            war = WarManager.instance().getWar(warId);
        return war;
    }

    public void setWar(War war) {
        this.warId = war.getUuid();
        this.war = war;
    }

    public boolean isOccupied() {
        return isOccupied;
    }

    public void setOccupied(boolean isOccupied) {
        this.isOccupied = isOccupied;
        this.stateChanged = true;
    }

    public boolean stateChanged() {
        return stateChanged;
    }

    public void setStateChanged(boolean stateChanged) {
        this.stateChanged = stateChanged;
    }

    public void generateAreas() {
    }

    public boolean isInArea(Coordinates coordinates) {
        return area.contains(coordinates);
    }

    public boolean isInGriefArea(Coordinates coordinates) {
        return griefArea.contains(coordinates);
    }

    public boolean isInCaptureArea(Coordinates coordinates) {
        return captureArea.stream().anyMatch(c -> c.coords().equals(coordinates));
    }

    public CaptureZoneChunk getCaptureAreaChunk(Coordinates coordinates) {
        return captureArea.stream().filter(c -> c.coords().equals(coordinates)).findFirst().orElse(null);
    }

    public Set<CaptureZoneChunk> getCaptureArea() {
        return captureArea;
    }

    public boolean isSiegeEnabled() {
        return siegeEnabled;
    }

    public void setSiegeEnabled(boolean siegeEnabled) {
        this.siegeEnabled = siegeEnabled;
    }

    public void checkOccupation() {

        if (this.isOccupied)
            return;

        boolean allChunksCaptured = true;
        Map<WarFaction, Integer> occupiers = new HashMap<>();
        for (var chunk : captureArea) {
            if (!SiegeManager.instance().isSiegeChunkOccupied(chunk.coords())) {
                allChunksCaptured = false;
                break;
            }
            var owner = SiegeManager.instance().getSiegeChunkOwner(chunk.coords());
            if (owner != null) {
                occupiers.compute(owner, (factionKey, count) -> count == null ? 1 : count + 1);
            }
        }

        if (allChunksCaptured) {
            setOccupied(true);
            setOccupier(getFactionWithHighestOwnership(occupiers));
        }
    }

    private WarFaction getFactionWithHighestOwnership(Map<WarFaction, Integer> occupiers) {
        if (occupiers == null || occupiers.isEmpty()) {
            return null;
        }

        List<WarFaction> leaders = new ArrayList<>();
        int highestOwnership = Integer.MIN_VALUE;

        for (var entry : occupiers.entrySet()) {
            int ownership = entry.getValue();
            if (ownership > highestOwnership) {
                highestOwnership = ownership;
                leaders.clear();
                leaders.add(entry.getKey());
            } else if (ownership == highestOwnership) {
                leaders.add(entry.getKey());
            }
        }

        if (leaders.isEmpty()) {
            return null;
        }

        return leaders.get(ThreadLocalRandom.current().nextInt(leaders.size()));
    }

    public void renderMarkers() {

        if (!UnitedWars.instance().getConfig().getBoolean("siege-settings.capture-markers.use", false))
            return;

        var layerKey = UnitedWars.instance().getConfig().getString("siege-settings.capture-markers.layer-key", "unitedwars");
        var layerName = UnitedWars.instance().getConfig().getString("siege-settings.capture-markers.layer-name", "Wars");
  
        Bukkit.getScheduler().runTaskAsynchronously(UnitedWars.instance(), () -> {
            var marker = "siege";
            for (var chunk : captureArea) {
                if ("home".equals(chunk.type()) || "region-home".equals(chunk.type()))
                    marker = "home";
                else if ("fortress".equals(chunk.type()))
                    marker = "fortress";
                var location = new Location(chunk.coords().getWorld(), chunk.coords().getX() * 16 + 8, 0, chunk.coords().getZ() * 16 + 8);
                Pl3xMapRenderer.instance().renderMarker(location.getWorld().getName(), location, marker, getMarkerKey(chunk.coords()), layerKey, layerName);
            }
        });
    }

    public void removeMarkers() {
        var layerKey = UnitedWars.instance().getConfig().getString("siege-settings.capture-markers.layer-key", "unitedwars");
        var layerName = UnitedWars.instance().getConfig().getString("siege-settings.capture-markers.layer-name", "Wars");

        Bukkit.getScheduler().runTaskAsynchronously(UnitedWars.instance(), () -> {
            for (var chunk : captureArea) {
                Pl3xMapRenderer.instance().removeMarker(chunk.coords.getWorld().getName(), getMarkerKey(chunk.coords()), layerKey, layerName);
            }
        });
    }

    public void backupGriefZone() {
        var snapshotManager = UnitedRestoration.getInstance().getChunkSnapshotManager();
        United.logger().info("Backing up " + griefArea.size() + " chunks for war zone " + uuid);
        for (var coords : griefArea) {
            coords.getWorld().getChunkAtAsync(coords.getX(), coords.getZ()).thenCompose(chunk -> {
                CompletableFuture<Boolean> saveFuture = snapshotManager.saveSnapshot(chunk);
                if (!chunk.isLoaded())
                    chunk.unload(false);
                return saveFuture;
            }).whenComplete((success, ex) -> {
                if (ex != null) {
                    United.logger().error("Error processing chunk at " + coords + ": " + ex.getMessage());
                } else if (Boolean.TRUE.equals(success)) {
                    United.logger().info("Saved chunk at " + coords);
                }
            });

        }
    }

    public void restoreGriefZone() {
        var snapshotManager = UnitedRestoration.getInstance().getChunkSnapshotManager();
        for (var coords : griefArea) {
            snapshotManager.restoreSnapshot(coords.getWorld(), coords.getX(), coords.getZ()).whenComplete((success, ex) -> {
                if (ex != null) {
                    United.logger().error("Error restoring chunk at " + coords + ": " + ex.getMessage());
                } else if (Boolean.TRUE.equals(success)) {
                    United.logger().info("Sucessfully restored chunk at " + coords + ", removing snapshop.", "UnitedWars");
                }
            });
        }
    }

    private String getMarkerKey(Coordinates coords) {
        return "marker-" + this.uuid + "-" + coords.getX() + "-" + coords.getZ();
    }

    public SettlementWarZone asSettlementWarZone() {

        var settlementWarZone = new SettlementWarZone();
        settlementWarZone.setUuid(this.uuid);
        settlementWarZone.setGeopolObjectId(this.geopolObjectId);
        settlementWarZone.setWarId(this.warId);
        settlementWarZone.setFactionId(this.factionId);
        settlementWarZone.setOccupierId(this.occupierId);
        settlementWarZone.setOccupied(this.isOccupied);

        return settlementWarZone;
    }

    public RegionWarZone asRegiontWarZone() {

        var regionWarZone = new RegionWarZone();
        regionWarZone.setUuid(this.uuid);
        regionWarZone.setGeopolObjectId(this.geopolObjectId);
        regionWarZone.setWarId(this.warId);
        regionWarZone.setFactionId(this.factionId);
        regionWarZone.setOccupierId(this.occupierId);
        regionWarZone.setOccupied(this.isOccupied);

        return regionWarZone;
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
        WarZone other = (WarZone) obj;
        if (uuid == null) {
            if (other.uuid != null)
                return false;
        } else if (!uuid.equals(other.uuid))
            return false;
        return true;
    }

}
