package org.unitedlands.wars.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.unitedlands.unitedlands.utils.CoordinateUtils;
import org.unitedlands.wars.UnitedWars;
import org.unitedlands.wars.managers.SiegeManager;
import org.unitedlands.wars.managers.WarManager;
import org.unitedlands.wars.schedulers.WarScheduler;
import org.unitedlands.wars.utils.WarBookUtils;

import net.kyori.adventure.text.Component;

public class ServerEventListener implements Listener {

    public ServerEventListener() {
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        WarScheduler.instance().initialize();
        WarManager.instance().loadWars();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        WarManager.instance().validateMetaData(event.getPlayer());
        if (!WarManager.instance().anyWarsPending() && !WarManager.instance().anyWarsActive())
            return;
        WarManager.instance().updatePlayerLists();
        SiegeManager.instance().updateSieges();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!WarManager.instance().anyWarsPending() && !WarManager.instance().anyWarsActive())
            return;

        SiegeManager.instance().removePlayerFromAllSiegeChunks(event.getPlayer());

        // Delay by one tick so the quitting player is actually considered offline
        Bukkit.getScheduler().runTaskLater(UnitedWars.instance(), () -> {
            WarManager.instance().updatePlayerLists();
            SiegeManager.instance().updateSieges();
        }, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!WarManager.instance().anyWarsActive())
            return;
        SiegeManager.instance().removePlayerFromAllSiegeChunks(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!WarManager.instance().anyWarsActive())
            return;

        var player = event.getPlayer();

        // The player is only moved to the respawn location after this event
        Bukkit.getScheduler().runTask(UnitedWars.instance(), () -> {
            if (!player.isOnline())
                return;
            var coords = CoordinateUtils.locationToChunkCoordinates(player.getLocation());
            if (WarManager.instance().isChunkInWarZone(coords))
                SiegeManager.instance().updatePlayersInChunk(player, null, coords);
        });
    }

    // Workaround for book signing
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBookEdit(PlayerEditBookEvent event) {

        var item = event.getPlayer().getInventory().getItemInMainHand();
        if (!item.getType().equals(Material.WRITABLE_BOOK))
            return;

        if (!WarBookUtils.isWarBook(item))
            return;

        event.setCancelled(true);

        if (event.isSigning()) {

            var newMeta = (BookMeta) event.getNewBookMeta();
            newMeta.displayName(Component.text(newMeta.getTitle()));

            var signedBook = ItemStack.of(Material.WRITTEN_BOOK, 1);
            signedBook.setItemMeta(newMeta);

            // Swap the book a tick later, otherwise Minecraft will overwrite
            // the slot with the old book.
            Bukkit.getScheduler().runTaskLater(UnitedWars.instance(), () -> {
                event.getPlayer().getInventory().setItemInMainHand(signedBook);
            }, 1);

        } else {
            item.setItemMeta(event.getNewBookMeta());
        }

    }

}
