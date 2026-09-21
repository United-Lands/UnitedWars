package org.unitedlands.wars.classes.wargoal;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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

public class RevoltWarGoal extends WarGoal {

    public RevoltWarGoal() {
        super("revolt");
        this.description = UnitedWars.instance().getConfig().getString("war-goal-settings.revolt.description");
    }

    @Override
    public ValidationResult validate(GeopolObject declarer, GeopolObject target) {

        if ((declarer instanceof Settlement settlement) && (target instanceof Region region)) {

            if (!settlement.hasCountry()) {
                return new ValidationResult(false, "The settlement doesn't belong to a country.");
            }
            if (!settlement.hasRegion()) {
                return new ValidationResult(false, "The settlement doesn't belong to a region.");
            }
            if (!settlement.getRegion().equals(region)) {
                return new ValidationResult(false, "The target region is not the settlement's region.");
            }
            if (!region.hasCountry()) {
                return new ValidationResult(false, "The target region doesn't belong to a country.");
            }

        } else {
            return new ValidationResult(false, "The provided GeopolObject types are not suitable for this war goal.");
        }
        return new ValidationResult(true, null);
    }

    @Override
    public Set<WarFaction> createFactions(UUID declarer, UUID target, War war) {
        Set<WarFaction> factions = new HashSet<>();

        var declaringSettlement = UnitedLandsDataManager.instance().getSettlement(declarer);
        if (declaringSettlement == null) {
            United.logger().error("Unable to create revolt factions, missing declarer object.", "UnitedLands");
            return null;
        }

        // Remove the revolting town from the nation. If the revolt fails, they will be
        // put back.
        for (var citizen : declaringSettlement.getCitizens()) {
            citizen.removeCountryRanks();
            citizen.save();
        }
        declaringSettlement.removeCountry();
        declaringSettlement.saveAndRender();

        var declarerScoreCap = UnitedWars.instance().getConfig().getInt("war-goal-settings.revolt.scorecaps.attacker", 30000);

        WarFaction declarerFaction = new WarFaction();
        declarerFaction.setUuid(UUID.randomUUID());
        declarerFaction.setName("Rebels");
        declarerFaction.setColor(-65536);
        declarerFaction.setRole(WarFactionRole.ATTACKER);
        declarerFaction.setFactionLeaderId(declarer);
        declarerFaction.addSettlement(declaringSettlement);
        declarerFaction.setWar(war);
        declarerFaction.addWinCondition("reach_score", declarerScoreCap);
        declarerFaction.addWinCondition("own_all_war_zones");
        declarerFaction.addLoseCondition("own_no_war_zones");

        factions.add(declarerFaction);

        var targetRegion = UnitedLandsDataManager.instance().getRegion(target);
        if (targetRegion == null) {
            United.logger().error("Unable to create revolt factions, missing region object.", "UnitedLands");
            return null;
        }

        var targetCountry = targetRegion.getCountry();
        if (targetCountry == null) {
            United.logger().error("Unable to create revolt factions, missing target country.", "UnitedLands");
            return null;
        }

        var targetScoreCap = UnitedWars.instance().getConfig().getInt("war-goal-settings.revolt.scorecaps.defender", 30000);

        WarFaction targetFaction = new WarFaction();
        targetFaction.setUuid(UUID.randomUUID());
        targetFaction.setName("Loyalist");
        targetFaction.setColor(-16776961);
        targetFaction.setRole(WarFactionRole.DEFENDER);
        targetFaction.setFactionLeaderId(targetCountry.getUuid());
        targetFaction.addCountry(targetCountry);
        targetFaction.setWar(war);
        targetFaction.addWinCondition("reach_score", targetScoreCap);
        targetFaction.addWinCondition("last_faction_alive");
        targetFaction.addWinCondition("own_all_war_zones");

        factions.add(targetFaction);

        return factions;
    }

    @Override
    public Set<WarZone> createWarZones(UUID declaringSettement, UUID targetRegion, War war) {

        var attackerFactions = war.getWarFactionMap().get(WarFactionRole.ATTACKER);
        var defenderFactions = war.getWarFactionMap().get(WarFactionRole.DEFENDER);
        if (attackerFactions.size() == 0 || defenderFactions.size() == 0) {
            United.logger().error("Unexpected number of factions in revolt war goal.", "UnitedWars");
            return new HashSet<>();
        }

        Set<WarZone> zones = new HashSet<>();
        zones.add(createSettlementZone(war, attackerFactions.getFirst(), declaringSettement));
        zones.add(createRegionZone(war, defenderFactions.getFirst(), targetRegion));

        return zones;
    }

    @Override
    public void resolve(War war) {

        var attackerFactions = war.getWarFactionMap().get(WarFactionRole.ATTACKER);
        if (attackerFactions.size() != 1) {
            United.logger().error("Unexpected number of factions in revolt war goal resolution.", "UnitedLands");
            return;
        } else {

            var attackingSettlement = UnitedLandsDataManager.instance().getSettlement(attackerFactions.getFirst().getFactionLeaderId());
            if (attackingSettlement == null) {
                United.logger().error("No declaring settlement found in revolt war goal resolution.", "UnitedLands");
                return;
            }

            var region = attackingSettlement.getRegion();
            if (region == null || !region.hasCountry()) {
                United.logger().error("No region or region country found in revolt war goal resolution.", "UnitedLands");
                return;
            }

            var country = region.getCountry();

            if (!war.getWinningFaction().equals(attackerFactions.getFirst())) {
                // The revolt has failed. Force the settlement back into the nation and apply
                // cooldowns.
                // TODO Cooldowns
                attackingSettlement.setCountry(country);
                attackingSettlement.saveAndRender();
            } else {
                // The revolt was a success. Liberate all nation towns in the region and clear
                // the region owner.
                for (var settlement : region.getSettlements()) {
                    if (!settlement.hasCountry())
                        continue;
                    for (var citizen : settlement.getCitizens()) {
                        citizen.removeCountryRanks();
                        citizen.save();
                    }
                    settlement.removeCountry();
                    settlement.saveAndRender();
                }
                region.removeCountry();
                region.saveAndRender();
                country.removeRegion(region);
                country.saveAndRender();
            }

        }
    }

    @Override
    public void joinWar(UUID joiner, War war) {

        // Both countries and settlements can join in a revolt war.

        var country = UnitedLandsDataManager.instance().getCountry(joiner);
        if (country != null) {
            handleCountryJoin(country, war);
        } else {
            var settlement = UnitedLandsDataManager.instance().getSettlement(joiner);
            if (settlement != null) {
                handleSettlementJoin(settlement, war);
            }
        }

    }

    private void handleCountryJoin(Country joiningCountry, War war) {

        // Countries can be either allies of the defender (joining the defending side)
        // or unassociated countries (joining the attacking rebel side)

        // Checking for defending join first

        var defenderFaction = war.getWarFaction(WarFactionRole.DEFENDER);
        if (defenderFaction == null) {
            United.logger().error("Critical error: No DEFENDER faction found in revolt war goal join.", "UnitedLands");
            return;
        }

        var defendingCountry = UnitedLandsDataManager.instance().getCountry(defenderFaction.getFactionLeaderId());
        if (defendingCountry == null) {
            United.logger().error("Critical error: No defender country found in revolt war goal join.", "UnitedLands");
            return;
        }

        if (defendingCountry.getAllies().contains(joiningCountry)) {
            handleAllyJoin(joiningCountry, defenderFaction, war);
        } else {

            // Not a case of an ally joining the defender, so it must be an unassociated
            // country supporting the attacker.

            var attackerFaction = war.getWarFaction(WarFactionRole.ATTACKER);
            if (attackerFaction == null) {
                United.logger().error("Critical error: No ATTACKER faction found in revolt war goal join.", "UnitedLands");
                return;
            }

            handleSupporterJoin(joiningCountry, attackerFaction, war);
        }
    }

    private void handleAllyJoin(Country joiningCountry, WarFaction faction, War war) {
        faction.addCountry(joiningCountry);
        war.refreshOnlinePlayerFactions();
    }

    private void handleSupporterJoin(Country joiningCountry, WarFaction faction, War war) {
        faction.addCountry(joiningCountry);
        war.refreshOnlinePlayerFactions();
    }

    private void handleSettlementJoin(Settlement settlement, War war) {
        // TODO: Check for region, unset settlement country, let them join, add war zone
    }

}
