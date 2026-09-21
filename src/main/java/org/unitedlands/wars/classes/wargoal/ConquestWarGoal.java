package org.unitedlands.wars.classes.wargoal;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.util.Vector;
import org.unitedlands.unitedlands.classes.Country;
import org.unitedlands.unitedlands.classes.GeopolObject;
import org.unitedlands.unitedlands.classes.Region;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;
import org.unitedlands.wars.UnitedWars;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.classes.war.WarFaction;
import org.unitedlands.wars.classes.war.WarFactionRole;
import org.unitedlands.wars.classes.warzone.WarZone;

public class ConquestWarGoal extends WarGoal {

    public ConquestWarGoal() {
        super("conquest");
        this.description = UnitedWars.instance().getConfig().getString("war-goal-settings.conquest.description");
    }

    @Override
    public ValidationResult validate(GeopolObject declarer, GeopolObject target) {

        if ((declarer instanceof Country country) && (target instanceof Region region)) {

            if (!region.hasCountry()) {
                return new ValidationResult(false, "The target region is unowned.");
            }
            if (region.getCountry().equals(country)) {
                return new ValidationResult(false, "The target region already belongs to the country.");
            }

        } else {
            return new ValidationResult(false, "The provided GeopolObject types are not suitable for this war goal.");
        }
        return new ValidationResult(true, null);
    }

    @Override
    public Set<WarFaction> createFactions(UUID declarerCountry, UUID targetRegion, War war) {

        Set<WarFaction> factions = new HashSet<>();

        var country = UnitedLandsDataManager.instance().getCountry(declarerCountry);
        var region = UnitedLandsDataManager.instance().getRegion(targetRegion);
        if (country == null || region == null) {
            United.logger().error("Unable to create conquest factions, missing declarer or target object.", "UnitedLands");
            return null;
        }

        WarFaction attackerFaction = new WarFaction(war, WarFactionRole.ATTACKER, country.getName(), -65536);
        attackerFaction.setRole(WarFactionRole.ATTACKER);
        attackerFaction.setFactionLeaderId(declarerCountry);
        attackerFaction.addCountry(country);

        var attackerScoreCap = UnitedWars.instance().getConfig().getInt("war-goal-settings.conquest.scorecaps.attacker", 30000);
        attackerFaction.addWinCondition("reach_score", attackerScoreCap);
        attackerFaction.addWinCondition("own_all_war_zones");
        attackerFaction.addLoseCondition("own_no_war_zones");

        factions.add(attackerFaction);

        var targetCountry = region.getCountry();

        WarFaction defenderFaction = new WarFaction(war, WarFactionRole.DEFENDER, targetCountry.getName(), -16776961);
        defenderFaction.setFactionLeaderId(targetCountry.getUuid());
        defenderFaction.addCountry(targetCountry);

        var defenderScoreCap = UnitedWars.instance().getConfig().getInt("war-goal-settings.conquest.scorecaps.defender", 30000);
        defenderFaction.addWinCondition("reach_score", defenderScoreCap);
        defenderFaction.addWinCondition("own_all_war_zones");
        defenderFaction.addLoseCondition("own_no_war_zones");
        defenderFaction.setWar(war);

        factions.add(defenderFaction);

        return factions;
    }

    @Override
    public Set<WarZone> createWarZones(UUID declarerCountry, UUID targetRegionId, War war) {
        Set<WarZone> zones = new HashSet<>();

        var attackerFactions = war.getWarFactionMap().get(WarFactionRole.ATTACKER);
        var defenderFactions = war.getWarFactionMap().get(WarFactionRole.DEFENDER);
        if (attackerFactions.size() != 1 || defenderFactions.size() != 1) {
            United.logger().error("Unexpected number of factions in conquest war goal.", "UnitedWars");
            return new HashSet<>();
        }

        var region = UnitedLandsDataManager.instance().getRegion(targetRegionId);

        // If the target region has one or more nation towns, those will be the target
        // of the conquest. Otherwise the region center will be the war zone.
        if (region.getSettlements().size() > 0) {
            var countrySettlements = region.getSettlements().stream().filter(s -> s.hasCountry()).toList();
            if (countrySettlements.size() > 0) {
                for (var countrySettlement : countrySettlements) {
                    zones.add(createSettlementZone(war, defenderFactions.getFirst(), countrySettlement.getUuid()));
                }
            } else {
                zones.add(createRegionZone(war, defenderFactions.getFirst(), targetRegionId));
            }
        } else {
            zones.add(createRegionZone(war, defenderFactions.getFirst(), targetRegionId));
        }

        // To allow counter attacks for the defenders, find the attacker region that is
        // closest to the target region and also make it a war zone according to the
        // same rules as above.
        var attackerCountry = UnitedLandsDataManager.instance().getCountry(attackerFactions.getFirst().getFactionLeaderId());
        if (attackerCountry == null) {
            United.logger().error("Mising attacking country in war goal conquest.", "UnitedWars");
            return new HashSet<>();
        }

        Vector regionHome = new Vector(region.getHomeChunkCoordinatesX(), 0, region.getHomeChunkCoordinatesZ());

        Region closestAttackerRegion = null;
        Double closestDistance = Double.POSITIVE_INFINITY;
        for (var attackerRegion : attackerCountry.getRegions()) {
            Vector attackerRegionHome = new Vector(attackerRegion.getHomeChunkCoordinatesX(), 0, attackerRegion.getHomeChunkCoordinatesZ());
            var dist = regionHome.distanceSquared(attackerRegionHome);
            if (dist < closestDistance) {
                closestDistance = dist;
                closestAttackerRegion = attackerRegion;
            }
        }

        if (closestAttackerRegion.getSettlements().size() > 0) {
            var attackerCountrySettlements = closestAttackerRegion.getSettlements().stream().filter(s -> s.hasCountry()).toList();
            if (attackerCountrySettlements.size() > 0) {
                for (var attackerCountrySettlement : attackerCountrySettlements) {
                    zones.add(createSettlementZone(war, attackerFactions.getFirst(), attackerCountrySettlement.getUuid()));
                }
            } else {
                zones.add(createRegionZone(war, attackerFactions.getFirst(), closestAttackerRegion.getUuid()));
            }
        } else {
            zones.add(createRegionZone(war, attackerFactions.getFirst(), closestAttackerRegion.getUuid()));
        }

        return zones;
    }

    @Override
    public void resolve(War war) {

        // Don't do anything in case of a draw
        if (war.getWinningFaction() == null)
            return;

        // Don't do anything if the attackers didn't win
        if (war.getWinningFaction().getRole() != WarFactionRole.ATTACKER)
            return;

        var conqueredRegion = UnitedLandsDataManager.instance().getRegion(war.getWarTargetId());
        if (conqueredRegion == null) {
            United.logger().error("Could not get region for war goal conquest", "UnitedWars");
            return;
        }

        // Faction leaders in conquest wars are always countries.
        var winningCountry = UnitedLandsDataManager.instance().getCountry(war.getWinningFaction().getFactionLeaderId());
        if (winningCountry == null) {
            United.logger().error("Could not get winning country for war goal conquest", "UnitedWars");
            return;
        }

        var losingCountry = conqueredRegion.getCountry();
        losingCountry.removeRegion(conqueredRegion);
        losingCountry.saveAndRender();

        winningCountry.addRegion(conqueredRegion);
        conqueredRegion.setCountry(winningCountry);
        for (var settlement : conqueredRegion.getSettlements()) {
            if (settlement.hasCountry()) {
                for (var citizen : settlement.getCitizens())
                {
                    citizen.removeCountryRanks();
                    citizen.save();
                }
                settlement.setCountry(winningCountry);
                settlement.saveAndRender();
            }
        }
        conqueredRegion.saveAndRender();
        winningCountry.saveAndRender();

    }

    @Override
    public void joinWar(UUID joiner, War war) {

    }

}
