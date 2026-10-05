package org.unitedlands.wars.listeners.UnitedLands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.unitedlands.unitedlands.classes.events.country.CountryPreRemoveEvent;
import org.unitedlands.unitedlands.classes.events.region.RegionClaimStartEvent;
import org.unitedlands.unitedlands.classes.events.region.RegionDoubleClaimEvent;
import org.unitedlands.utils.United;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.managers.WarManager;

public class UnitedLandsCountryListener implements Listener {

    @EventHandler(ignoreCancelled = false)
    public void onDoubleClaim(RegionDoubleClaimEvent event) {
        // TODO: Move string to config
        event.setConfirmationMessage(
                "<red>This region is already being claimed. If you approve this claim, you will trigger a war with the claiming country. Continue?</red>");
        event.setCancelled(false);
    }

    @EventHandler(ignoreCancelled = false)
    public void onDoubleClaimStart(RegionClaimStartEvent event) {

        if (event.isDoubleClaim()) {

            event.setCancelled(true);

            var region = event.getRegion();

            var attackingCountry = event.getCountry();
            var targetCountry = region.getClaimantCountry();

            if (targetCountry == null) {
                United.logger().warning("No claimant country for double claim event.", "UnitedLands");
                return;
            }

            var warGoal = WarManager.instance().getWarGoal("claim-dispute");
            var title = "Claim_Dispute_" + region.getName();
            var description = attackingCountry.getCleanName() + " is fighting " + targetCountry.getCleanName() + " over control of " + region.getCleanName()
                    + ".";

            War war = War.create(warGoal, attackingCountry, region, title, description);
            if (war == null) {
                United.messenger().send(event.getPlayer(), "errors.generic");
                return;
            }

            region.setClaimEndTime(null);
            region.setClaimStartTime(null);
            region.removeClaimantCountry();
            region.cancelClaimTask();
            region.saveAndRender();

            WarManager.instance().registerWar(war);
        }

    }

    public void onCountryPreRemove(CountryPreRemoveEvent event) {

        var country = event.getCountry();

        // If a country gets removed by UnitedLands (e.g. by manual deletion, upkeep,
        // lost wars etc.), iterate all wars to see if the country is part of it, and
        // act accordingly

        List<War> warsToEnd = new ArrayList<>();
        for (var war : WarManager.instance().getWars()) {

            // The event might have been caused by a war that has just ended. Ignore.
            if (war.hasEnded())
                continue;

            for (var faction : war.getWarFactions()) {
                if (faction.getFactionLeaderId().equals(country.getUuid())) {
                    // Loss of the faction leader means immediate end of the war
                    warsToEnd.add(war);
                } else {
                    // Remove country from faction
                    if (faction.hasCountry(country)) {
                        faction.removeCountry(country);
                    }
                    // If no other faction members remain, end the war
                    if (faction.getCountries().size() == 0 && faction.getSettlements().size() == 0) {
                        warsToEnd.add(war);
                    }
                }
            }

            for (var warToEnd : warsToEnd) {
                WarManager.instance().endWar(warToEnd);
            }

        }
    }

}
