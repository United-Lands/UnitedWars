package org.unitedlands.wars.listeners.UnitedLands;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.unitedlands.unitedlands.classes.events.base.PlayerChangeChunkEvent;
import org.unitedlands.unitedlands.classes.events.player.PlayerEnterSettlementEvent;
import org.unitedlands.unitedlands.utils.CoordinateUtils;
import org.unitedlands.utils.United;
import org.unitedlands.wars.managers.SiegeManager;
import org.unitedlands.wars.managers.WarManager;

public class UnitedLandsMovementListener implements Listener {

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

        if (!WarManager.instance().isChunkInWarZone(event.getFromCoordinates()) && !WarManager.instance().isChunkInWarZone(event.getToCoordinates()))
            return;

        SiegeManager.instance().updatePlayersInChunk(event.getPlayer(), event.getFromCoordinates(), event.getToCoordinates());
    }

}
