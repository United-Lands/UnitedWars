package org.unitedlands.wars.classes.wargoal;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.util.Vector;
import org.unitedlands.unitedlands.classes.BankRecordTransactionType;
import org.unitedlands.unitedlands.classes.Country;
import org.unitedlands.unitedlands.classes.GeopolObject;
import org.unitedlands.unitedlands.classes.Region;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.utils.United;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.classes.war.WarFaction;
import org.unitedlands.wars.classes.war.WarFactionRole;
import org.unitedlands.wars.classes.warcondition.WarConditionOwnAllWarZones;
import org.unitedlands.wars.classes.warcondition.WarConditionOwnNoWarZones;
import org.unitedlands.wars.classes.warcondition.WarConditionReachScore;
import org.unitedlands.wars.classes.warzone.WarZone;
import org.unitedlands.wars.events.WarGoalValidationEvent;
import org.unitedlands.wars.events.WarInviteEvent;
import org.unitedlands.wars.managers.WarManager;

public class WarGoalConquest extends WarGoal {

    public static final String ID = "conquest";

    public WarGoalConquest() {
        super(ID);
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
            United.logger().error("Unable to create conquest factions, missing declarer or target object.");
            return null;
        }

        WarFaction attackerFaction = new WarFaction(war, WarFactionRole.ATTACKER, country.getName(), -65536);
        attackerFaction.setRole(WarFactionRole.ATTACKER);
        attackerFaction.setFactionLeaderId(country.getUuid());
        attackerFaction.addCountry(country);

        var attackerScoreCap = getScoreCap("attacker");
        attackerFaction.addWinCondition(WarConditionReachScore.ID, attackerScoreCap);
        attackerFaction.addWinCondition(WarConditionOwnAllWarZones.ID);
        attackerFaction.addLoseCondition(WarConditionOwnNoWarZones.ID);

        factions.add(attackerFaction);

        var targetCountry = region.getCountry();

        WarFaction defenderFaction = new WarFaction(war, WarFactionRole.DEFENDER, targetCountry.getName(), -16776961);
        defenderFaction.setFactionLeaderId(targetCountry.getUuid());
        defenderFaction.addCountry(targetCountry);

        var defenderScoreCap = getScoreCap("defender");
        defenderFaction.addWinCondition(WarConditionReachScore.ID, defenderScoreCap);
        defenderFaction.addWinCondition(WarConditionOwnAllWarZones.ID);
        defenderFaction.addLoseCondition(WarConditionOwnNoWarZones.ID);
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
        zones.addAll(createRegionTargetZones(war, defenderFactions.getFirst(), targetRegion));

        // To allow counter attacks for the defenders, find the attacker region that is
        // closest to the target region and also make it a war zone according to the
        // same rules as above.
        var attackerCountry = (UnitedLandsDataManager.instance().getCountry(attackerFactions.getFirst().getFactionLeaderId()));
        if (attackerCountry == null) {
            United.logger().error("Mising attacking country in war goal conquest.", "UnitedWars");
            return new HashSet<>();
        }

        var targetRegionHome = new Vector(targetRegion.getHomeChunkCoordinatesX(), 0, targetRegion.getHomeChunkCoordinatesZ());

        var counterAttackZones = createCounterAttackZones(war, attackerFactions.getFirst(), attackerCountry, targetRegionHome, null);
        if (counterAttackZones.isEmpty()) {
            United.logger().error("No counter attack zones found for attacking country in war goal conquest.", "UnitedWars");
            return new HashSet<>();
        }
        zones.addAll(counterAttackZones);

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
        if (losingCountry == null || losingCountry.equals(winningCountry)) {
            United.logger().error("Conquered region has no losing country in war goal conquest", "UnitedWars");
            return;
        }

        transferRegion(conqueredRegion, winningCountry);
        for (var settlement : conqueredRegion.getSettlements(losingCountry)) {
            transferSettlement(settlement, winningCountry);
        }
        winningCountry.saveAndRender();

        if (losingCountry.getRegionCount() == 0) {
            United.logger().debug("Country " + losingCountry.getName() + " has lost its last region, removing.");
            removeCountry(winningCountry, losingCountry);
            return;
        }

        var capital = losingCountry.getCapital();
        if (capital == null || conqueredRegion.equals(capital.getRegion())) {
            var newCapital = losingCountry.getSettlements().stream().findAny().orElse(null);
            if (newCapital == null) {
                United.logger().debug("Country " + losingCountry.getName() + " has lost its last settlement, removing.");
                removeCountry(winningCountry, losingCountry);
                return;
            }
            losingCountry.setCapital(newCapital);
        }
        losingCountry.saveAndRender();
    }

    private void removeCountry(Country winningCountry, Country losingCountry) {

        // Remaining money will go to the winning country
        UnitedLandsEconomyManager.instance().deposit(winningCountry.getUuid(), UnitedLandsEconomyManager.instance().getBalance(losingCountry.getUuid()),
               BankRecordTransactionType.WAR, "Dismanteled " + losingCountry.getCleanName());

        UnitedLandsDataManager.instance().removeCountry(losingCountry);
    }

    @Override
    public ValidationResult validateInvite(WarFaction inviter, GeopolObject receiver) {

        // Conquests only allow inviting other countries
        if (!(receiver instanceof Country)) {
            return new ValidationResult(false, United.messenger().get("errors.invite.receiver-must-be-country"));
        }

        // Let other plugins check conditions
        WarInviteEvent warInviteEvent = new WarInviteEvent(inviter.getWar(), inviter, receiver);
        warInviteEvent.callEvent();

        if (warInviteEvent.isCancelled()) {
            return new ValidationResult(false, warInviteEvent.getCancelMessage());
        }

        return new ValidationResult(true, null);
    }

    @Override
    public void joinWar(GeopolObject joiner, WarFaction faction) {

        faction.addCountry((Country) joiner);
        WarManager.instance().getDatabaseManager().getWarFactionService().updateAsync(faction);

        WarManager.instance().updatePlayerLists();
    }


}
