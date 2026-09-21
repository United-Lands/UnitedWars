package org.unitedlands.wars.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.unitedlands.unitedlands.classes.events.base.PlayerChangeChunkEvent;
import org.unitedlands.unitedlands.classes.events.player.PlayerEnterSettlementEvent;
import org.unitedlands.unitedlands.classes.events.region.RegionClaimStartEvent;
import org.unitedlands.unitedlands.classes.events.region.RegionDoubleClaimEvent;
import org.unitedlands.unitedlands.utils.CoordinateUtils;
import org.unitedlands.utils.United;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.managers.SiegeManager;
import org.unitedlands.wars.managers.WarManager;

public class UnitedLandsListener implements Listener {

    @EventHandler
    public void onEnterSettlement(PlayerEnterSettlementEvent event) {
        if (!WarManager.instance().anyWarsActive())
            return;
        var coords = CoordinateUtils.locationToChunkCoordinates(event.getLocation());

        if (WarManager.instance().isChunkInWarZone(coords)) {
            United.messenger().sendRaw(event.getPlayer(), "<red>Now entering a war zone!</red>");
        }
    }

    @EventHandler
    public void onChangeChunk(PlayerChangeChunkEvent event) {

        if (!WarManager.instance().anyWarsActive())
            return;

        if (!WarManager.instance().isChunkInWarZone(event.getFromCoordinates())
                && !WarManager.instance().isChunkInWarZone(event.getToCoordinates()))
            return;

        SiegeManager.instance().updatePlayersInChunk(event.getPlayer(), event.getFromCoordinates(),
                event.getToCoordinates());
    }

    @EventHandler(ignoreCancelled = false)
    public void onDoubleClaim(RegionDoubleClaimEvent event) {
        // event.setConfirmationMessage(
        //         "<red>This region is already being claimed. If you approve this claim, you will trigger a war with the claiming country. Continue?</red>");
        // event.setCancelled(false);
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

            var warGoal = WarManager.instance().getWarGoal("claim_dispute");
            var title = "Claim Dispute: " + region.getCleanName();
            var description = attackingCountry.getCleanName() + " is fighting " + targetCountry.getCleanName()
                    + " over control of " + region.getCleanName() + ".";

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

}
