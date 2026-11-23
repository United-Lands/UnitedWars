package org.unitedlands.wars.classes.wargoal;

import java.util.HashSet;
import java.util.Set;
import org.unitedlands.unitedlands.classes.Country;
import org.unitedlands.unitedlands.classes.GeopolObject;
import org.unitedlands.unitedlands.classes.Region;
import org.unitedlands.unitedlands.classes.events.region.RegionClaimedEvent;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.classes.war.WarFaction;
import org.unitedlands.wars.classes.war.WarFactionRole;
import org.unitedlands.wars.classes.warcondition.WarConditionOwnAllWarZones;
import org.unitedlands.wars.classes.warcondition.WarConditionReachScore;
import org.unitedlands.wars.classes.warzone.WarZone;
import org.unitedlands.wars.events.WarGoalValidationEvent;

public class WarGoalClaimDispute extends WarGoal {

    public static final String ID = "claim-dispute";

    public WarGoalClaimDispute() {
        super(ID);
    }

    @Override
    public ValidationResult validate(GeopolObject declarer, GeopolObject target) {

        if ((declarer instanceof Country country) && (target instanceof Region region)) {

            if (region.getClaimantCountry() == null) {
                return new ValidationResult(false, "The region is not being claimed.");
            }

            if (region.getClaimantCountry().equals(country)) {
                return new ValidationResult(false, "The region is already being claimed by the country.");
            }

            // Trigger external validation (e.g. in UnitedPolitics)
            var externalValidationEvent = new WarGoalValidationEvent(this, country, region.getClaimantCountry());
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
            United.logger().error("Unable to create claim dispute factions, missing declarer or target object.");
            return null;
        }

        var claimantScoreCap = getScoreCap("claimant");

        WarFaction faction1 = new WarFaction(war, WarFactionRole.CLAIMANT, country.getName(), -65536);
        faction1.setFactionLeaderId(country.getUuid());
        faction1.addCountry(country);
        faction1.addWinCondition(WarConditionReachScore.ID, claimantScoreCap);
        faction1.addWinCondition(WarConditionOwnAllWarZones.ID);

        factions.add(faction1);

        var targetCountry = region.getClaimantCountry();

        WarFaction faction2 = new WarFaction(war, WarFactionRole.CLAIMANT, targetCountry.getName(), -16776961);
        faction2.setFactionLeaderId(targetCountry.getUuid());
        faction2.addCountry(targetCountry);
        faction2.addWinCondition(WarConditionReachScore.ID, claimantScoreCap);
        faction2.addWinCondition(WarConditionOwnAllWarZones.ID);

        factions.add(faction2);

        return factions;
    }

    @Override
    public Set<WarZone> createWarZones(GeopolObject declarerCountry, GeopolObject targetRegion, War war) {
        Set<WarZone> zones = new HashSet<>();
        zones.add(createRegionZone(war, null, (Region) targetRegion));
        return zones;
    }

    @Override
    public void resolve(War war) {

        // Don't do anything in case of a draw
        if (war.getWinningFaction() == null)
            return;

        var region = UnitedLandsDataManager.instance().getRegion(war.getWarTargetId());
        if (region == null) {
            United.logger().error("Could not get region for war goal claim-dispute", "UnitedWars");
            return;
        }

        // Faction leaders in claim disputes are always countries.
        var country = UnitedLandsDataManager.instance().getCountry(war.getWinningFaction().getFactionLeaderId());
        if (country == null) {
            United.logger().error("Could not get winning country for war goal claim-dispute", "UnitedWars");
            return;
        }

        var previousCountry = region.getCountry();

        region.setAdministrator(country.getLeader());
        transferRegion(region, country);
        country.saveAndRender();
        if (previousCountry != null && !previousCountry.equals(country))
            previousCountry.saveAndRender();

        new RegionClaimedEvent(region, country).callEvent();
    }


    
    @Override
    public ValidationResult validateInvite(WarFaction inviter, GeopolObject receiver) {
        // Claims disputes have to be fought alone
        return new ValidationResult(false, United.messenger().get("errors.invite.invite-not-supported"));
    }

    @Override
    public void joinWar(GeopolObject joiner, WarFaction faction) {
        // TODO Auto-generated method stub

    }

}
