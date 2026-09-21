package org.unitedlands.wars.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.unitedlands.utils.United;
import org.unitedlands.wars.UnitedWars;
import org.unitedlands.wars.classes.infoscreens.WarDeclaredInfoScreen;
import org.unitedlands.wars.classes.infoscreens.WarEndInfoScreen;
import org.unitedlands.wars.events.WarEndEvent;
import org.unitedlands.wars.events.WarPreRegisterEvent;
import org.unitedlands.wars.events.WarRegisteredEvent;
import org.unitedlands.wars.events.WarScoreEvent;
import org.unitedlands.wars.events.WarStartEvent;
import org.unitedlands.wars.utils.MessageProvider;

public class WarEventsListener implements Listener {

    private final MessageProvider messageProvider;

    public WarEventsListener(MessageProvider messageProvider) {
        this.messageProvider = messageProvider;
    }

    @EventHandler
    public void OnWarPreRegister(WarPreRegisterEvent event) {

    }

    @EventHandler
    public void OnWarRegistered(WarRegisteredEvent event) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_6, 1.0f, 1.0f);
        }

        var warDeclaredScreen = new WarDeclaredInfoScreen(UnitedWars.instance(), messageProvider, event.getWar());
        warDeclaredScreen.send(Bukkit.getServer());
    }

    @EventHandler
    public void OnWarStart(WarStartEvent event) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_7, 1.0f, 1.0f);
        }
        United.messenger().broadcast("war-started", event.getWar().getTitle());
    }

    @EventHandler
    public void OnWarEnd(WarEndEvent event) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_2, 1.0f, 1.0f);
        }

        var warEndScreen = new WarEndInfoScreen(UnitedWars.instance(), messageProvider, event.getWar());
        warEndScreen.send(Bukkit.getServer());
    }

    @EventHandler
    public void OnWarScore(WarScoreEvent event) {

        // Logger.log("WarScoreEvent: " + event.getFinalScore() + " points scored by
        // faction " + event.getFaction().getName() + " for " + event.getType());

        var silent = false;
        var message = "score-default";

        var notificationSettings = UnitedWars.instance().getConfig().getConfigurationSection("notification-settings." + event.getType());
        if (notificationSettings != null) {
            silent = notificationSettings.getBoolean("silent", false);
            message = notificationSettings.getString("message", "score-default");
        }

        // TODO: genetare record

        event.getFaction().addScore(event.getFinalScore());

        if (!silent && event.getPlayer() != null) {
            United.messenger().send(event.getPlayer(), message, String.valueOf(event.getFinalScore()));
        }

    }
}
