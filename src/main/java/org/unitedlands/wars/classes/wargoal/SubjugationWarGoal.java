package org.unitedlands.wars.classes.wargoal;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.util.Vector;
import org.unitedlands.unitedlands.classes.Country;
import org.unitedlands.unitedlands.classes.GeopolObject;
import org.unitedlands.unitedlands.classes.Region;
import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;
import org.unitedlands.wars.UnitedWars;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.classes.war.WarFaction;
import org.unitedlands.wars.classes.war.WarFactionRole;
import org.unitedlands.wars.classes.warzone.WarZone;

public class SubjugationWarGoal extends WarGoal {

    public SubjugationWarGoal() {
        super("subjugation");
        this.description = UnitedWars.instance().getConfig().getString("war-goal-settings.subjugation.description");
    }

    @Override
    public ValidationResult validate(GeopolObject declarer, GeopolObject target) {

        if ((declarer instanceof Country country) && (target instanceof Settlement settlement)) {
            if (settlement.hasCountry()) {
                if (country.equals(settlement.getCountry())) {
                    return new ValidationResult(false, "The settlement is already part of the country.");
                } else {
                    return new ValidationResult(false, "The settlement is already part of a different country.");
                }
            }
            if (settlement.getRegion() == null) {
                return new ValidationResult(false, "The settlement is not in a region.");
            }
            if (!settlement.getRegion().hasCountry()) {
                return new ValidationResult(false, "The settlement is in an unowned region.");
            }
            if (!country.equals(settlement.getRegion().getCountry())) {
                return new ValidationResult(false, "The country doesn't own the target settlement's region.");
            }
        } else {
            return new ValidationResult(false, "The provided GeopolObject types are not suitable for this war goal.");
        }
        return new ValidationResult(true, null);
    }

    @Override
    public Set<WarFaction> createFactions(UUID declarerCountry, UUID targetSettlement, War war) {

        Set<WarFaction> factions = new HashSet<>();

        var country = UnitedLandsDataManager.instance().getCountry(declarerCountry);
        var settlement = UnitedLandsDataManager.instance().getSettlement(targetSettlement);
        if (country == null || settlement == null) {
            United.logger().error("Unable to create subjugation factions, missing declarer or target object.", "UnitedLands");
            return null;
        }

        WarFaction attackerFaction = new WarFaction(war, WarFactionRole.ATTACKER, country.getName(), -65536);
        attackerFaction.setRole(WarFactionRole.ATTACKER);
        attackerFaction.setFactionLeaderId(declarerCountry);
        attackerFaction.addCountry(country);

        var attackerScoreCap = UnitedWars.instance().getConfig().getInt("war-goal-settings.subjugation.scorecaps.attacker", 30000);
        attackerFaction.addWinCondition("reach_score", attackerScoreCap);
        attackerFaction.addWinCondition("own_all_war_zones");
        attackerFaction.addLoseCondition("own_no_war_zones");

        factions.add(attackerFaction);

        WarFaction defenderFaction = new WarFaction(war, WarFactionRole.DEFENDER, settlement.getName(), -16776961);
        defenderFaction.setFactionLeaderId(settlement.getUuid());
        defenderFaction.addSettlement(settlement);

        var defenderScoreCap = UnitedWars.instance().getConfig().getInt("war-goal-settings.subjugation.scorecaps.defender", 30000);
        defenderFaction.addWinCondition("reach_score", defenderScoreCap);
        defenderFaction.addWinCondition("own_all_war_zones");
        defenderFaction.addLoseCondition("own_no_war_zones");
        defenderFaction.setWar(war);

        factions.add(defenderFaction);

        return factions;
    }

    @Override
    public Set<WarZone> createWarZones(UUID declarerCountry, UUID targetSettlementId, War war) {
        Set<WarZone> zones = new HashSet<>();

        var attackerFactions = war.getWarFactionMap().get(WarFactionRole.ATTACKER);
        var defenderFactions = war.getWarFactionMap().get(WarFactionRole.DEFENDER);
        if (attackerFactions.size() != 1 || defenderFactions.size() != 1) {
            United.logger().error("Unexpected number of factions in subjugation war goal.", "UnitedWars");
            return new HashSet<>();
        }

        zones.add(createSettlementZone(war, defenderFactions.getFirst(), targetSettlementId));

        // To allow counter attacks for the defenders, find the attacker region that is
        // closest to the target settlement and also make it a war zone. Selection rules
        // are the same as in conquest wars.
        var attackerCountry = UnitedLandsDataManager.instance().getCountry(attackerFactions.getFirst().getFactionLeaderId());
        if (attackerCountry == null) {
            United.logger().error("Mising attacking country in war goal conquest.", "UnitedWars");
            return new HashSet<>();
        }

        var settlement = UnitedLandsDataManager.instance().getSettlement(targetSettlementId);
        Vector settlementHome = new Vector(settlement.getHomeChunkCoordinatesX(), 0, settlement.getHomeChunkCoordinatesZ());

        Region closestAttackerRegion = null;
        Double closestDistance = Double.POSITIVE_INFINITY;
        for (var attackerRegion : attackerCountry.getRegions()) {
            if (attackerRegion.equals(settlement.getRegion()))
                continue;
            Vector attackerRegionHome = new Vector(attackerRegion.getHomeChunkCoordinatesX(), 0, attackerRegion.getHomeChunkCoordinatesZ());
            var dist = settlementHome.distanceSquared(attackerRegionHome);
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

        var settlement = UnitedLandsDataManager.instance().getSettlement(war.getWarTargetId());
        if (settlement == null) {
            United.logger().error("Could not get settlement for war goal conquest", "UnitedWars");
            return;
        }

        // Faction leaders in conquest wars are always countries.
        var country = UnitedLandsDataManager.instance().getCountry(war.getWinningFaction().getFactionLeaderId());
        if (country == null) {
            United.logger().error("Could not get winning country for war goal conquest", "UnitedWars");
            return;
        }

        settlement.setCountry(country);
        settlement.saveAndRender();
    }

    @Override
    public void joinWar(UUID joiner, War war) {

    }

}
