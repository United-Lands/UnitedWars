package org.unitedlands.wars.classes.wargoal;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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

public class ClaimDisputeWarGoal extends WarGoal {

    public ClaimDisputeWarGoal() {
        super("claim_dispute");
        this.description = UnitedWars.instance().getConfig().getString("war-goal-settings.claim_dispute.description");
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
            United.logger().error("Unable to create claim dispute factions, missing declarer or target object.", "UnitedLands");
            return null;
        }

        var claimantScoreCap = UnitedWars.instance().getConfig().getInt("war-goal-settings.claim_dispute.scorecaps.claimant", 30000);

        WarFaction faction1 = new WarFaction(war, WarFactionRole.CLAIMANT, country.getName(), -65536);
        faction1.setFactionLeaderId(declarerCountry);
        faction1.addCountry(country);
        faction1.addWinCondition("reach_score", claimantScoreCap);
        faction1.addWinCondition("own_all_war_zones");

        factions.add(faction1);

        var targetCountry = region.getClaimantCountry();

        WarFaction faction2 = new WarFaction(war, WarFactionRole.CLAIMANT, targetCountry.getName(), -16776961);
        faction2.setFactionLeaderId(targetRegion);
        faction2.addCountry(targetCountry);
        faction2.addWinCondition("reach_score", claimantScoreCap);
        faction2.addWinCondition("own_all_war_zones");

        factions.add(faction2);

        return factions;
    }

    @Override
    public Set<WarZone> createWarZones(UUID declarerCountry, UUID targetRegion, War war) {
        Set<WarZone> zones = new HashSet<>();
        zones.add(createRegionZone(war, null, targetRegion));
        return zones;
    }

    @Override
    public void resolve(War war) {

        // Don't do anything in case of a draw
        if (war.getWinningFaction() == null)
            return;

        // Claim disputed only have one war zone (the disputed region)
        if (war.getWarZones().size() != 1) {
            United.logger().error("Too many war zones for war goal claim_dispute", "UnitedWars");
            return;
        }

        var zone = (war.getWarZones().stream().findFirst()).get();
        var region = UnitedLandsDataManager.instance().getRegion(zone.getGeopolObjectId());
        if (region == null) {
            United.logger().error("Could not get region for war goal claim_dispute", "UnitedWars");
            return;
        }

        // Faction leaders in claim disputes are always countries.
        var country = UnitedLandsDataManager.instance().getCountry(war.getWinningFaction().getFactionLeaderId());
        if (country == null) {
            United.logger().error("Could not get winning country for war goal claim_dispute", "UnitedWars");
            return;
        }

        region.setCountry(country);
        region.saveAndRender();
        country.addRegion(region);
        country.saveAndRender();
    }

    @Override
    public void joinWar(UUID joiner, War war) {
        // TODO Auto-generated method stub

    }

}
