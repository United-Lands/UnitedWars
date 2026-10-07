package org.unitedlands.wars.classes.wargoal;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.unitedlands.unitedlands.classes.GeopolObject;
import org.unitedlands.unitedlands.classes.Region;
import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.classes.war.WarFaction;
import org.unitedlands.wars.classes.warzone.WarZoneRegion;
import org.unitedlands.wars.classes.warzone.WarZoneSettlement;
import org.unitedlands.wars.classes.warzone.WarZone;

public abstract class WarGoal {

    private final String id;
    protected String description;

    public static record ValidationResult(boolean valid, String message) {}

    public WarGoal(String id) {
        this.id = id;
    }

    public abstract ValidationResult validate(GeopolObject declarer, GeopolObject target);

    public abstract Set<WarFaction> createFactions(GeopolObject declarer, GeopolObject target, War war);

    public abstract Set<WarZone> createWarZones(GeopolObject declarer, GeopolObject target, War war);

    public abstract void joinWar(GeopolObject joiner, WarFaction faction);

    public abstract void resolve(War war);

    protected WarZone createSettlementZone(War war, WarFaction owningFaction, Settlement settlement) {
        var settlementZone = new WarZoneSettlement();
        settlementZone.setUuid(UUID.randomUUID());
        settlementZone.setWar(war);
        if (owningFaction != null) {
            settlementZone.setFaction(owningFaction);
            settlementZone.setOccupier(owningFaction);
        }
        settlementZone.setGeopolObjectId(settlement.getUuid());
        settlementZone.generateAreas();
        return settlementZone;
    }

    protected WarZone createRegionZone(War war, WarFaction owningFaction, Region region) {
        var regionZone = new WarZoneRegion();
        regionZone.setUuid(UUID.randomUUID());
        regionZone.setWar(war);
        regionZone.setGeopolObjectId(region.getUuid());
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
