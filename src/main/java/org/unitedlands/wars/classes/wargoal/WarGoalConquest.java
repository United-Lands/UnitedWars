package org.unitedlands.wars.classes.wargoal;

import java.util.HashSet;
import java.util.Set;
import org.bukkit.util.Vector;
import org.unitedlands.unitedlands.classes.Country;
import org.unitedlands.unitedlands.classes.GeopolObject;
import org.unitedlands.unitedlands.classes.Region;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.utils.United;
import org.unitedlands.wars.classes.config.UnitedWarsConfig;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.classes.war.WarFaction;
import org.unitedlands.wars.classes.war.WarFactionRole;
import org.unitedlands.wars.classes.warzone.WarZone;
import org.unitedlands.wars.events.WarGoalValidationEvent;
import org.unitedlands.wars.events.WarPreJoinEvent;

public class WarGoalConquest extends WarGoal {

    public WarGoalConquest() {
        super("conquest");
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

            // Trigger external validation (e.g. in UnitedPolitics)
            var externalValidationEvent = new WarGoalValidationEvent(this, country, region.getCountry());
            externalValidationEvent.callEvent();

            if (!externalValidationEvent.isValid()) {
                return new ValidationResult(false, externalValidationEvent.getValidationMessage());
            }

        } else {
            return new ValidationResult(false, "The provided GeopolObject types are not suitable for this war goal.");
        }
        return new ValidationResult(true, null);
    }

    @Override
    public Set<WarFaction> createFactions(GeopolObject declarer, GeopolObject target, War war) {

        Set<WarFaction> factions = new HashSet<>();

        var country = (Country) declarer;
        var region = (Region) target;
        if (country == null || region == null) {
            United.logger().error("Unable to create conquest factions, missing declarer or target object.", "UnitedLands");
            return null;
        }

        WarFaction attackerFaction = new WarFaction(war, WarFactionRole.ATTACKER, country.getName(), -65536);
        attackerFaction.setRole(WarFactionRole.ATTACKER);
        attackerFaction.setFactionLeaderId(country.getUuid());
        attackerFaction.addCountry(country);

        var attackerScoreCap = UnitedWarsConfig.get().warGoalSettings().get("conquest").scoreCaps().get("attacker").val();
        attackerFaction.addWinCondition("reach_score", attackerScoreCap);
        attackerFaction.addWinCondition("own_all_war_zones");
        attackerFaction.addLoseCondition("own_no_war_zones");

        factions.add(attackerFaction);

        var targetCountry = region.getCountry();

        WarFaction defenderFaction = new WarFaction(war, WarFactionRole.DEFENDER, targetCountry.getName(), -16776961);
        defenderFaction.setFactionLeaderId(targetCountry.getUuid());
        defenderFaction.addCountry(targetCountry);

        var defenderScoreCap = UnitedWarsConfig.get().warGoalSettings().get("conquest").scoreCaps().get("defender").val();
        defenderFaction.addWinCondition("reach_score", defenderScoreCap);
        defenderFaction.addWinCondition("own_all_war_zones");
        defenderFaction.addLoseCondition("own_no_war_zones");
        defenderFaction.setWar(war);

        factions.add(defenderFaction);

        return factions;
    }

    @Override
    public Set<WarZone> createWarZones(GeopolObject declarer, GeopolObject target, War war) {
        Set<WarZone> zones = new HashSet<>();

        var attackerFactions = war.getWarFactionMap().get(WarFactionRole.ATTACKER);
        var defenderFactions = war.getWarFactionMap().get(WarFactionRole.DEFENDER);
        if (attackerFactions.size() != 1 || defenderFactions.size() != 1) {
            United.logger().error("Unexpected number of factions in conquest war goal.", "UnitedWars");
            return new HashSet<>();
        }

        var targetRegion = (Region) target;

        // If the target region has one or more nation towns, those will be the target
        // of the conquest. Otherwise the region center will be the war zone.
        if (targetRegion.getSettlements().size() > 0) {
            var countrySettlements = targetRegion.getSettlements().stream().filter(s -> s.hasCountry()).toList();
            if (countrySettlements.size() > 0) {
                for (var countrySettlement : countrySettlements) {
                    zones.add(createSettlementZone(war, defenderFactions.getFirst(), countrySettlement));
                }
            } else {
                zones.add(createRegionZone(war, defenderFactions.getFirst(), targetRegion));
            }
        } else {
            zones.add(createRegionZone(war, defenderFactions.getFirst(), targetRegion));
        }

        // To allow counter attacks for the defenders, find the attacker region that is
        // closest to the target region and also make it a war zone according to the
        // same rules as above.
        var attackerCountry = (UnitedLandsDataManager.instance().getCountry(attackerFactions.getFirst().getFactionLeaderId()));
        if (attackerCountry == null) {
            United.logger().error("Mising attacking country in war goal conquest.", "UnitedWars");
            return new HashSet<>();
        }

        Vector targetRegionHome = new Vector(targetRegion.getHomeChunkCoordinatesX(), 0, targetRegion.getHomeChunkCoordinatesZ());

        Region closestAttackerRegion = null;
        Double closestDistance = Double.POSITIVE_INFINITY;
        for (var attackerRegion : attackerCountry.getRegions()) {
            Vector attackerRegionHome = new Vector(attackerRegion.getHomeChunkCoordinatesX(), 0, attackerRegion.getHomeChunkCoordinatesZ());
            var dist = targetRegionHome.distanceSquared(attackerRegionHome);
            if (dist < closestDistance) {
                closestDistance = dist;
                closestAttackerRegion = attackerRegion;
            }
        }

        if (closestAttackerRegion.getSettlements().size() > 0) {
            var attackerCountrySettlements = closestAttackerRegion.getSettlements().stream().filter(s -> s.hasCountry()).toList();
            if (attackerCountrySettlements.size() > 0) {
                for (var attackerCountrySettlement : attackerCountrySettlements) {
                    zones.add(createSettlementZone(war, attackerFactions.getFirst(), attackerCountrySettlement));
                }
            } else {
                zones.add(createRegionZone(war, attackerFactions.getFirst(), closestAttackerRegion));
            }
        } else {
            zones.add(createRegionZone(war, attackerFactions.getFirst(), closestAttackerRegion));
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
                for (var citizen : settlement.getCitizens()) {
                    citizen.removeCountryRanks();
                    citizen.save();
                }
                settlement.setCountry(winningCountry);
                settlement.saveAndRender();

                losingCountry.removeSettlement(settlement);
            }
        }
        conqueredRegion.saveAndRender();
        winningCountry.saveAndRender();

        // See if the defeated country still has regions left. If not, delete it.
        if (losingCountry.getRegionCount() == 0) {
            United.logger().debug("Country " + losingCountry.getName() + " has lost its last region, removing.");
            // The country has lost its last region and will be removed.
            removeCountry(winningCountry, losingCountry);

        } else {

            // Check if the defeated country's capital was in the conquered region. If so,
            // select a random other capital. If the country doesn't have any more
            // settlements, remove it.
            var capital = losingCountry.getCapital();
            if (capital.getRegion().equals(conqueredRegion)) {
                var newCapital = losingCountry.getSettlements().stream().findAny().orElse(null);
                if (newCapital != null) {
                    losingCountry.setCapital(newCapital);
                    losingCountry.saveAndRender();
                } else {
                    United.logger().debug("Country " + losingCountry.getName() + " has lost its last settlement, removing.");
                    // The country has no more settlements and wil be removed.
                    removeCountry(winningCountry, losingCountry);
                }
            }
        }

    }

    private void removeCountry(Country winningCountry, Country losingCountry) {

        // Remaining money will go to the winning country
        UnitedLandsEconomyManager.instance().deposit(winningCountry.getUuid(), UnitedLandsEconomyManager.instance().getBalance(losingCountry.getUuid()),
                "Dismanteled " + losingCountry.getCleanName());

        UnitedLandsDataManager.instance().removeCountry(losingCountry);
    }

    @Override
    public void joinWar(GeopolObject joiner, WarFaction faction) {

    }

    public ValidationResult validateJoinConditions(GeopolObject joiner, WarFaction faction) {

        if (!(joiner instanceof Country))
            return new ValidationResult(false, "Only countries can join this war");

        // Let other plugins cancel the join based on their logic (e.g. missing
        // alliances in UnitedPolitics)
        var event = new WarPreJoinEvent(faction.getWar(), joiner, faction);
        event.callEvent();
        if (event.isCancelled())
            return new ValidationResult(false, event.getCancelMessage());

        return new ValidationResult(true, null);
    }

}
