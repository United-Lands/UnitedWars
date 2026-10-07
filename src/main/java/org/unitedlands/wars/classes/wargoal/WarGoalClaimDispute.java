package org.unitedlands.wars.classes.wargoal;

import java.util.HashSet;
import java.util.Set;
import org.unitedlands.unitedlands.classes.Country;
import org.unitedlands.unitedlands.classes.GeopolObject;
import org.unitedlands.unitedlands.classes.Region;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;
import org.unitedlands.wars.classes.config.UnitedWarsConfig;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.classes.war.WarFaction;
import org.unitedlands.wars.classes.war.WarFactionRole;
import org.unitedlands.wars.classes.warzone.WarZone;
import org.unitedlands.wars.events.WarGoalValidationEvent;

public class WarGoalClaimDispute extends WarGoal {

    public WarGoalClaimDispute() {
        super("claim-dispute");
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
            United.logger().error("Unable to create claim dispute factions, missing declarer or target object.", "UnitedLands");
            return null;
        }

        var claimantScoreCap = UnitedWarsConfig.get().warGoalSettings().get("claim-dispute").scoreCaps().get("claimant").val();

        WarFaction faction1 = new WarFaction(war, WarFactionRole.CLAIMANT, country.getName(), -65536);
        faction1.setFactionLeaderId(country.getUuid());
        faction1.addCountry(country);
        faction1.addWinCondition("reach_score", claimantScoreCap);
        faction1.addWinCondition("own_all_war_zones");

        factions.add(faction1);

        var targetCountry = region.getClaimantCountry();

        WarFaction faction2 = new WarFaction(war, WarFactionRole.CLAIMANT, targetCountry.getName(), -16776961);
        faction2.setFactionLeaderId(targetCountry.getUuid());
        faction2.addCountry(targetCountry);
        faction2.addWinCondition("reach_score", claimantScoreCap);
        faction2.addWinCondition("own_all_war_zones");

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

        // Claim disputed only have one war zone (the disputed region)
        if (war.getWarZones().size() != 1) {
            United.logger().error("Too many war zones for war goal claim-dispute", "UnitedWars");
            return;
        }

        var zone = (war.getWarZones().stream().findFirst()).get();
        var region = UnitedLandsDataManager.instance().getRegion(zone.getGeopolObjectId());
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

        region.setCountry(country);
        region.saveAndRender();
        country.addRegion(region);
        country.saveAndRender();
    }

    @Override
    public void joinWar(GeopolObject joiner, WarFaction faction) {
        // TODO Auto-generated method stub

    }

}
