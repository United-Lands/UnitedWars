package org.unitedlands.wars.classes.wargoal;

import java.util.HashSet;
import java.util.Set;

import org.unitedlands.unitedlands.classes.Country;
import org.unitedlands.unitedlands.classes.GeopolObject;
import org.unitedlands.unitedlands.classes.Region;
import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;
import org.unitedlands.wars.classes.config.UnitedWarsConfig;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.classes.war.WarFaction;
import org.unitedlands.wars.classes.war.WarFactionRole;
import org.unitedlands.wars.classes.warzone.WarZone;
import org.unitedlands.wars.events.WarGoalValidationEvent;
import org.unitedlands.wars.events.WarInviteEvent;

public class WarGoalRevolt extends WarGoal {

    public WarGoalRevolt() {
        super("revolt");
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
            if (!region.getCountry().equals(settlement.getCountry())) {
                return new ValidationResult(false, "The target region doesn't belong to the settlement's country.");
            }
            if (settlement.equals(settlement.getCountry().getCapital())) {
                return new ValidationResult(false, "A country's capital can't revolt.");
            }

             // Trigger external validation (e.g. in UnitedPolitics)
            var externalValidationEvent = new WarGoalValidationEvent(this, settlement, region.getCountry());
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

        var declaringSettlement = (Settlement) declarer;

        var declarerScoreCap = UnitedWarsConfig.get().warGoalSettings().get("revolt").scoreCaps().get("attacker").val();

        WarFaction declarerFaction = new WarFaction(war, WarFactionRole.ATTACKER, "Rebels", -65536);
        declarerFaction.setFactionLeaderId(declaringSettlement.getUuid());
        declarerFaction.addSettlement(declaringSettlement);
        declarerFaction.addWinCondition("reach_score", declarerScoreCap);
        declarerFaction.addWinCondition("own_all_war_zones");
        declarerFaction.addLoseCondition("own_no_war_zones");

        factions.add(declarerFaction);

        var targetRegion = (Region) target;

        var targetCountry = targetRegion.getCountry();
        if (targetCountry == null) {
            United.logger().error("Unable to create revolt factions, missing target country.");
            return null;
        }

        var targetScoreCap = UnitedWarsConfig.get().warGoalSettings().get("revolt").scoreCaps().get("defender").val();

        WarFaction targetFaction = new WarFaction(war, WarFactionRole.DEFENDER, "Loyalist", -16776961); 
        targetFaction.setFactionLeaderId(targetCountry.getUuid());
        targetFaction.addCountry(targetCountry);
        targetFaction.addWinCondition("reach_score", targetScoreCap);
        targetFaction.addWinCondition("last_faction_alive");
        targetFaction.addWinCondition("own_all_war_zones");

        factions.add(targetFaction);

        return factions;
    }

    @Override
    public void onWarRegistered(War war) {
        var attackerFaction = war.getWarFaction(WarFactionRole.ATTACKER);
        if (attackerFaction == null) {
            United.logger().error("No attacker faction found in revolt war goal registration.");
            return;
        }

        var revoltingSettlement = UnitedLandsDataManager.instance().getSettlement(attackerFaction.getFactionLeaderId());
        if (revoltingSettlement == null) {
            United.logger().error("No declaring settlement found in revolt war goal registration.");
            return;
        }

        for (var citizen : revoltingSettlement.getCitizens()) {
            citizen.removeCountryRanks();
            citizen.save();
        }
    }

    @Override
    public Set<WarZone> createWarZones(GeopolObject declarer, GeopolObject target, War war) {

        var attackerFactions = war.getWarFactionMap().get(WarFactionRole.ATTACKER);
        var defenderFactions = war.getWarFactionMap().get(WarFactionRole.DEFENDER);
        if (attackerFactions.size() == 0 || defenderFactions.size() == 0) {
            United.logger().error("Unexpected number of factions in revolt war goal.", "UnitedWars");
            return new HashSet<>();
        }

        Set<WarZone> zones = new HashSet<>();
        zones.add(createSettlementZone(war, attackerFactions.getFirst(), (Settlement) declarer));
        zones.add(createRegionZone(war, defenderFactions.getFirst(), (Region) target));

        return zones;
    }

    @Override
    public void resolve(War war) {

        var attackerFactions = war.getWarFactionMap().get(WarFactionRole.ATTACKER);
        if (attackerFactions.size() != 1) {
            United.logger().error("Unexpected number of factions in revolt war goal resolution.");
            return;
        } else {

            var attackingSettlement = UnitedLandsDataManager.instance().getSettlement(attackerFactions.getFirst().getFactionLeaderId());
            if (attackingSettlement == null) {
                United.logger().error("No declaring settlement found in revolt war goal resolution.");
                return;
            }

            var region = attackingSettlement.getRegion();
            if (region == null || !region.hasCountry()) {
                United.logger().error("No region or region country found in revolt war goal resolution.");
                return;
            }

            var country = region.getCountry();

            if (war.getWinningFaction() == null || !war.getWinningFaction().equals(attackerFactions.getFirst())) {
                // TODO Cooldowns
            } else if (country.getCapital() != null && region.equals(country.getCapital().getRegion())) {
                takeOverCountry(country, attackingSettlement);
            } else {
                for (var settlement : region.getSettlements(country)) {
                    transferSettlement(settlement, null);
                }
                transferRegion(region, null);
                country.saveAndRender();
            }

        }
    }

    private void takeOverCountry(Country country, Settlement revoltingSettlement) {
        country.setCapital(revoltingSettlement);

        for (var citizen : country.getCitizens()) {
            citizen.removeCountryRanks();
            citizen.save();
        }

        var newLeader = revoltingSettlement.getMayor();
        if (newLeader != null) {
            newLeader.addCountryRank("leader");
            newLeader.save();
        } else {
            United.logger().warning("Revolting settlement " + revoltingSettlement.getName() + " has no mayor, country " + country.getName() + " is left without a leader.");
        }

        country.saveAndRender();
    }

    @Override
    public ValidationResult validateInvite(WarFaction inviter, GeopolObject receiver) {
        // Conquests only allow inviting other countries
        if (!(receiver instanceof Country) && !(receiver instanceof Settlement)) {
            return new ValidationResult(false, United.messenger().get("errors.invite.invalid-receiver"));
        }


        // Check special requirements for settlements (they must be in the same region as the revolting settlement)

        if (inviter.getRole() == WarFactionRole.ATTACKER) {
            // Check conditions for the revolting settlement
            var revoltingSettlement = UnitedLandsDataManager.instance().getSettlement(inviter.getFactionLeaderId());
            if (receiver instanceof Settlement targetSettlement) {
                if (targetSettlement.equals(revoltingSettlement)) {
                    return new ValidationResult(false, United.messenger().get("errors.invite.invalid-receiver"));
                }
                if (!revoltingSettlement.getRegion().equals(targetSettlement.getRegion())) {
                    return new ValidationResult(false, United.messenger().get("errors.invite.receiver-must-be-in-same-region"));
                }
            } else if (receiver instanceof Country country) {
                // Catch stupid cases where a revolting settlements invites the country they are revolting against
                if (revoltingSettlement.getRegion().getCountry().equals(country)) {
                    return new ValidationResult(false, United.messenger().get("errors.invite.invalid-receiver"));
                }
            }
        } else {
            // Countries can't get help for revolts.
            return new ValidationResult(false, United.messenger().get("errors.invite.invite-not-supported"));
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

        // Both countries and settlements can join in a revolt war.

        // var country = UnitedLandsDataManager.instance().getCountry(joiner);
        // if (country != null) {
        //     handleCountryJoin(country, war);
        // } else {
        //     var settlement = UnitedLandsDataManager.instance().getSettlement(joiner);
        //     if (settlement != null) {
        //         handleSettlementJoin(settlement, war);
        //     }
        // }

    }




}
