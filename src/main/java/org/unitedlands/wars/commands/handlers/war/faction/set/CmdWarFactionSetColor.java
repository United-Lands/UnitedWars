package org.unitedlands.wars.commands.handlers.war.faction.set;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.utils.ColorUtils;
import org.unitedlands.utils.United;
import org.unitedlands.wars.classes.war.WarFaction;
import org.unitedlands.wars.managers.WarManager;

@UnitedSubCommand(
    parent = CmdWarFactionSet.class, 
    name = "color", 
    description = "Changes a faction's color", 
    usage = "/war faction set color <facion_name> <hex_color>", 
    playerOnly = true, 
    catchAll = true
)
public class CmdWarFactionSetColor implements UnitedCommandExecutor {

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

        if (args.length != 2) {
            sendUsage(sender);
            return;
        }

        var player = (Player) sender;

        var faction = WarManager.instance().getWarFaction(args[0]);
        if (faction == null) {
            United.messenger().send(sender, "errors.object-not-found", args[0]);
            return;
        }

        if (!faction.isFactionLeader(player.getUniqueId())) {
            United.messenger().send(sender, "player.faction.not-faction-leader");
            return;
        }

        if (!ColorUtils.isValidHexColor(args[1])) {
            United.messenger().send(sender, "errors.invalid-color");
            return;
        }

        faction.setColor(ColorUtils.hexToColor(args[1]).getRGB());
        WarManager.instance().getDatabaseManager().getWarFactionService().updateAsync(faction);

        United.messenger().send(player, "player.faction.setcolor-success", args[1]);
    }

}
