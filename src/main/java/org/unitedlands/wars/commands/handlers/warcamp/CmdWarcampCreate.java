package org.unitedlands.wars.commands.handlers.warcamp;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;
import org.unitedlands.wars.classes.war.WarFaction;
import org.unitedlands.wars.commands.CmdWarcamp;
import org.unitedlands.wars.managers.SiegeManager;
import org.unitedlands.wars.managers.WarManager;
import org.unitedlands.wars.utils.WarcampUtils;

@UnitedSubCommand(
    parent = CmdWarcamp.class, 
    name = "create", 
    description = "Creates a war camp", 
    usage = "/warcamp create <faction>", 
    playerOnly = true
)
public class CmdWarcampCreate implements UnitedCommandExecutor {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1) {
            var player = (Player) sender;
            var playerWars = WarManager.instance().getPlayerWars(player);
            if (playerWars.size() > 0) {
                List<WarFaction> playerFactions = new ArrayList<>();
                for (var war : playerWars) {
                    playerFactions.add(war.getPlayerFaction(player));
                }
                return playerFactions.stream().map(WarFaction::getName).toList();
            }
        }
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 1) {
            sendUsage(sender);
            return;
        }

        var player = (Player) sender;

        var faction = WarManager.instance().getWarFaction(args[0]);
        if (faction == null) {
            United.messenger().send(sender, "errors.object-not-found", args[0]);
            return;
        }

        // if (faction.getWar().isActive()) {
        //     United.messenger().send(sender, "player.warcamp.war-active");
        //     return;
        // }

        if (!faction.isFactionLeader(player.getUniqueId())) {
            United.messenger().send(sender, "player.faction.not-faction-leader");
            return;
        }

        if (faction.getWarCampLocation() != null) {
            United.messenger().send(sender, "player.warcamp.already-set");
            return;
        }

        var location = player.getLocation();

        var settlement = UnitedLandsDataManager.instance().getSettlement(location);
        if (settlement != null) {
            United.messenger().send(sender, "player.warcamp.not-wilderness");
            return;
        }

        faction.setWarCampLocation(location);
        WarManager.instance().getDatabaseManager().getWarFactionService().updateAsync(faction);

        var war = faction.getWar();
        var warcamp = war.createWarcamp(faction);

        WarcampUtils.place(location.getChunk());

        WarManager.instance().addWarZone(warcamp);
        SiegeManager.instance().updateSieges();
        WarManager.instance().getDatabaseManager().getWarZoneService().createAsync(warcamp);

        //WarcampManager.instance().registerWarCamp(faction, location);

    }

}
