package org.unitedlands.wars.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.unitedlands.wars.UnitedWars;
import org.unitedlands.wars.managers.WarManager;
import org.unitedlands.wars.managers.WarMetaDataManager;
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
        WarMetaDataManager.instance().validateWarLivesMetaData(event.getPlayer());
        WarMetaDataManager.instance().validateFactionPermissions(event.getPlayer());
        if (!WarManager.instance().anyWarsPending() && !WarManager.instance().anyWarsActive())
            return;
        WarManager.instance().updatePlayerLists();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!WarManager.instance().anyWarsPending() && !WarManager.instance().anyWarsActive())
            return;

        // Delay by one tick so the quitting player is actually considered offline
        Bukkit.getScheduler().runTaskLater(UnitedWars.instance(), () -> {
            WarManager.instance().updatePlayerLists();
        }, 1);
    }

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
