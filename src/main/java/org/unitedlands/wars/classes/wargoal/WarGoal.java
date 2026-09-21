package org.unitedlands.wars.classes.wargoal;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.unitedlands.unitedlands.classes.GeopolObject;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.classes.war.WarFaction;
import org.unitedlands.wars.classes.warzone.RegionWarZone;
import org.unitedlands.wars.classes.warzone.SettlementWarZone;
import org.unitedlands.wars.classes.warzone.WarZone;

public abstract class WarGoal {

    private final String id;
    protected String description;

    public static record ValidationResult(boolean valid, String message) {}

    public WarGoal(String id) {
        this.id = id;
    }

    public abstract ValidationResult validate(GeopolObject declarer, GeopolObject target);

    public abstract Set<WarFaction> createFactions(UUID declarer, UUID target, War war);

    public abstract Set<WarZone> createWarZones(UUID declarer, UUID target, War war);

    public abstract void joinWar(UUID joiner, War war);

    public abstract void resolve(War war);

    protected WarZone createSettlementZone(War war, WarFaction owningFaction, UUID settlementId) {
        var settlementZone = new SettlementWarZone();
        settlementZone.setUuid(UUID.randomUUID());
        settlementZone.setWar(war);
        if (owningFaction != null) {
            settlementZone.setFaction(owningFaction);
            settlementZone.setOccupier(owningFaction);
        }
        settlementZone.setGeopolObjectId(settlementId);
        settlementZone.generateAreas();
        return settlementZone;
    }

    protected WarZone createRegionZone(War war, WarFaction owningFaction, UUID regionId) {
        var regionZone = new RegionWarZone();
        regionZone.setUuid(UUID.randomUUID());
        regionZone.setWar(war);
        regionZone.setGeopolObjectId(regionId);
        if (owningFaction != null) {
            regionZone.setFaction(owningFaction);
            regionZone.setOccupier(owningFaction);
        }
        regionZone.generateAreas();
        return regionZone;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, String> getMessageReplacements() {
        return Map.of("war-goal-id", getId().toUpperCase(), "war-goal-description", getDescription());
    }
}
