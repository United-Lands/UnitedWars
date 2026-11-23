package org.unitedlands.wars.classes.wargoal;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.util.Vector;
import org.unitedlands.unitedlands.classes.Country;
import org.unitedlands.unitedlands.classes.GeopolObject;
import org.unitedlands.unitedlands.classes.Region;
import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.utils.United;
import org.unitedlands.wars.classes.config.UnitedWarsConfig;
import org.unitedlands.wars.classes.config.UnitedWarsConfig.WarGoalSetting;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.classes.war.WarFaction;
import org.unitedlands.wars.classes.warzone.WarZoneRegion;
import org.unitedlands.wars.classes.warzone.WarZoneSettlement;
import org.unitedlands.wars.classes.warzone.WarZoneWarcamp;
import org.unitedlands.wars.classes.warzone.WarZone;

public abstract class WarGoal {

    private final String id;
    protected String description;

    public static record ValidationResult(boolean valid, String message) {
    }

    public WarGoal(String id) {
        this.id = id;
    }

    public abstract ValidationResult validate(GeopolObject declarer, GeopolObject target);

    public abstract Set<WarFaction> createFactions(GeopolObject declarer, GeopolObject target, War war);

    public abstract Set<WarZone> createWarZones(GeopolObject declarer, GeopolObject target, War war);

    public abstract ValidationResult validateInvite(WarFaction inviter, GeopolObject receiver);

    public abstract void joinWar(GeopolObject joiner, WarFaction faction);

    public abstract void resolve(War war);

    public void onWarRegistered(War war) {
    }

    public WarGoalSetting getSettings() {
        var settings = UnitedWarsConfig.get().warGoalSettings();
        if (settings == null || !settings.has(id)) {
            United.logger().error("Missing config section for war goal " + id, "UnitedWars");
            return null;
        }
        return settings.get(id);
    }

    protected Integer getScoreCap(String side) {
        var settings = getSettings();
        if (settings == null)
            return null;
        var scoreCaps = settings.scoreCaps();
        if (scoreCaps == null || !scoreCaps.has(side)) {
            United.logger().error("Missing score cap '" + side + "' for war goal " + id + ", score win condition will never trigger.", "UnitedWars");
            return null;
        }
        return scoreCaps.get(side).val();
    }

    public WarZone createSettlementZone(War war, WarFaction owningFaction, Settlement settlement) {
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

    public WarZone createRegionZone(War war, WarFaction owningFaction, Region region) {
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

    protected Set<WarZone> createRegionTargetZones(War war, WarFaction owningFaction, Region region) {
        Set<WarZone> zones = new HashSet<>();
        var countrySettlements = region.getSettlements().stream().filter(s -> s.hasCountry()).toList();
        if (countrySettlements.isEmpty()) {
            zones.add(createRegionZone(war, owningFaction, region));
        } else {
            for (var settlement : countrySettlements) {
                zones.add(createSettlementZone(war, owningFaction, settlement));
            }
        }
        return zones;
    }

    protected Set<WarZone> createCounterAttackZones(War war, WarFaction owningFaction, Country country, Vector origin, Region excludedRegion) {
        Region closestRegion = null;
        double closestDistance = Double.POSITIVE_INFINITY;
        for (var region : country.getRegions()) {
            if (region.equals(excludedRegion))
                continue;
            var regionHome = new Vector(region.getHomeChunkCoordinatesX(), 0, region.getHomeChunkCoordinatesZ());
            var dist = origin.distanceSquared(regionHome);
            if (dist < closestDistance) {
                closestDistance = dist;
                closestRegion = region;
            }
        }

        if (closestRegion != null)
            return createRegionTargetZones(war, owningFaction, closestRegion);

        Set<WarZone> zones = new HashSet<>();
        if (excludedRegion != null) {
            for (var settlement : excludedRegion.getSettlements()) {
                if (settlement.hasCountry() && country.equals(settlement.getCountry()))
                    zones.add(createSettlementZone(war, owningFaction, settlement));
            }
        }
        return zones;
    }

    protected void transferRegion(Region region, Country newCountry) {
        var oldCountry = region.getCountry();
        if (oldCountry != null)
            oldCountry.removeRegion(region);

        if (newCountry != null) {
            region.setCountry(newCountry);
            newCountry.addRegion(region);
        } else {
            region.removeCountry();
        }
        region.saveAndRender();
    }

    protected void transferSettlement(Settlement settlement, Country newCountry) {
        var oldCountry = settlement.getCountry();
        if (oldCountry != null) {
            oldCountry.removeSettlement(settlement);
            for (var citizen : settlement.getCitizens()) {
                citizen.removeCountryRanks();
                citizen.save();
            }
        }

        if (newCountry != null) {
            settlement.setCountry(newCountry);
            newCountry.addSettlement(settlement);
        } else {
            settlement.removeCountry();
        }
        settlement.saveAndRender();
    }

    public WarZone createWarcampZone(War war, WarFaction owningFaction) {
        var warcampZone = new WarZoneWarcamp();
        warcampZone.setUuid(UUID.randomUUID());
        warcampZone.setWar(war);
        if (owningFaction != null) {
            warcampZone.setFaction(owningFaction);
            warcampZone.setOccupier(owningFaction);
        }
        warcampZone.generateAreas();
        return warcampZone;
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
