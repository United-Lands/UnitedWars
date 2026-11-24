package org.unitedlands.wars.classes.wargoal;

import java.util.HashSet;
import java.util.Set;
import org.bukkit.util.Vector;
import org.unitedlands.unitedlands.classes.Country;
import org.unitedlands.unitedlands.classes.GeopolObject;
import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.classes.war.WarFaction;
import org.unitedlands.wars.classes.war.WarFactionRole;
import org.unitedlands.wars.classes.warcondition.WarConditionOwnAllWarZones;
import org.unitedlands.wars.classes.warcondition.WarConditionOwnNoWarZones;
import org.unitedlands.wars.classes.warcondition.WarConditionReachScore;
import org.unitedlands.wars.classes.warzone.WarZone;
import org.unitedlands.wars.events.WarGoalValidationEvent;

public class WarGoalSubjugation extends WarGoal {

    public static final String ID = "subjugation";

    public WarGoalSubjugation() {
        super(ID);
    }

    @Override
    public ValidationResult validate(GeopolObject declarer, GeopolObject target) {

        if ((declarer instanceof Country country) && (target instanceof Settlement settlement)) {
            if (settlement.hasCountry()) {
                if (country.equals(settlement.getCountry())) {
                    return new ValidationResult(false, United.messenger().get("war-goals.subjugation.validation.already-in-country"));
                } else {
                    return new ValidationResult(false, United.messenger().get("war-goals.subjugation.validation.in-other-country"));
                }
            }
            if (settlement.getRegion() == null) {
                return new ValidationResult(false, United.messenger().get("war-goals.subjugation.validation.no-region"));
            }
            if (!settlement.getRegion().hasCountry()) {
                return new ValidationResult(false, United.messenger().get("war-goals.subjugation.validation.region-unowned"));
            }
            if (!country.equals(settlement.getRegion().getCountry())) {
                return new ValidationResult(false, United.messenger().get("war-goals.subjugation.validation.region-not-owned"));
            }

            // Trigger external validation (e.g. in UnitedPolitics)
            var externalValidationEvent = new WarGoalValidationEvent(this, country, settlement);
            externalValidationEvent.callEvent();
            if (!externalValidationEvent.isValid()) {
                return new ValidationResult(false, externalValidationEvent.getValidationMessage());
            }

        } else {
            return new ValidationResult(false, United.messenger().get("errors.invalid-war-goal-targets"));
        }
        return new ValidationResult(true, null);
    }

    @Override
    public Set<WarFaction> createFactions(GeopolObject declarer, GeopolObject target, War war) {

        Set<WarFaction> factions = new HashSet<>();

        var country = (Country) declarer;
        var settlement = (Settlement) target;

        WarFaction attackerFaction = createFaction(war, WarFactionRole.ATTACKER, country.getName(), PRIMARY_FACTION_COLOR, country);

        var attackerScoreCap = getScoreCap("attacker");
        attackerFaction.addWinCondition(WarConditionReachScore.ID, attackerScoreCap);
        attackerFaction.addWinCondition(WarConditionOwnAllWarZones.ID);
        attackerFaction.addLoseCondition(WarConditionOwnNoWarZones.ID);

        factions.add(attackerFaction);

        WarFaction defenderFaction = createFaction(war, WarFactionRole.DEFENDER, settlement.getName(), SECONDARY_FACTION_COLOR, settlement);

        var defenderScoreCap = getScoreCap("defender");
        defenderFaction.addWinCondition(WarConditionReachScore.ID, defenderScoreCap);
        defenderFaction.addWinCondition(WarConditionOwnAllWarZones.ID);
        defenderFaction.addLoseCondition(WarConditionOwnNoWarZones.ID);

        factions.add(defenderFaction);

        return factions;
    }

    @Override
    public Set<WarZone> createWarZones(GeopolObject declarer, GeopolObject target, War war) {
        Set<WarZone> zones = new HashSet<>();

        var attackerFactions = war.getWarFactionMap().get(WarFactionRole.ATTACKER);
        var defenderFactions = war.getWarFactionMap().get(WarFactionRole.DEFENDER);
        if (attackerFactions.size() != 1 || defenderFactions.size() != 1) {
            United.logger().error("Unexpected number of factions in subjugation war goal.", "UnitedWars");
            return new HashSet<>();
        }

        zones.add(createSettlementZone(war, defenderFactions.getFirst(), (Settlement) target));

        // To allow counter attacks for the defenders, find the attacker region that is
        // closest to the target settlement and also make it a war zone. Selection rules
        // are the same as in conquest wars.
        var attackerCountry = UnitedLandsDataManager.instance().getCountry(attackerFactions.getFirst().getFactionLeaderId());
        if (attackerCountry == null) {
            United.logger().error("Mising attacking country in war goal conquest.", "UnitedWars");
            return new HashSet<>();
        }

        var settlement = (Settlement) target;
        var settlementHome = new Vector(settlement.getHomeChunkCoordinatesX(), 0, settlement.getHomeChunkCoordinatesZ());

        var counterAttackZones = createCounterAttackZones(war, attackerFactions.getFirst(), attackerCountry, settlementHome, settlement.getRegion());
        if (counterAttackZones.isEmpty()) {
            United.logger().error("No counter attack zones found for attacking country in war goal subjugation.", "UnitedWars");
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

        if (settlement.hasCountry()) {
            United.logger().error("Target settlement already belongs to a country in war goal subjugation", "UnitedWars");
            return;
        }

        transferSettlement(settlement, country);
        country.saveAndRender();
    }

    
    @Override
    public ValidationResult validateInvite(WarFaction inviter, GeopolObject receiver) {
        // Subjugations have to be fought alone
        return new ValidationResult(false, United.messenger().get("errors.invite.invite-not-supported"));
    }
    
    @Override
    public void joinWar(GeopolObject joiner, WarFaction faction) {

    }

}
