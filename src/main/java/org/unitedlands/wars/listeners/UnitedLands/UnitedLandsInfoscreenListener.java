package org.unitedlands.wars.listeners.UnitedLands;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.unitedlands.unitedlands.classes.events.infoscreen.CountryInfoScreenEvent;
import org.unitedlands.wars.classes.config.UnitedWarsConfig;

import net.kyori.adventure.text.minimessage.MiniMessage;

public class UnitedLandsInfoscreenListener implements Listener {

    @EventHandler
    public void onCountryInfoScreen(CountryInfoScreenEvent event) {

       var mobilizationAttr = event.getCountry().getModifiedAttribute("MOBILIZATION", UnitedWarsConfig.get().geopolAttributeDefaults().get("MOBILIZATION"));
       if (mobilizationAttr != null)
       {
            var mobilizationComponent = MiniMessage.miniMessage().deserialize(
                "<gray>Mobilization: " + mobilizationAttr.getCurrentValue() + "</gray>");
            event.getInfoScreen().addComponent("mobilization", mobilizationComponent);
       }

       var mercAttr = event.getCountry().getModifiedAttribute("MAX_MERCENARIES", UnitedWarsConfig.get().geopolAttributeDefaults().get("MAX_MERCENARIES"));
       if (mercAttr != null)
       {
            var mobilizationComponent = MiniMessage.miniMessage().deserialize(
                "<gray>Maximim mercenaries: " + mercAttr.getCurrentValue() + "</gray>");
            event.getInfoScreen().addComponent("mercenaries", mobilizationComponent);
       }


    }
}
