package org.unitedlands.wars.commands.handlers.admin.war;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.utils.United;
import org.unitedlands.wars.classes.war.WarFaction;
import org.unitedlands.wars.managers.WarManager;

@UnitedSubCommand(
    parent = CmdWarAdminWar.class, 
    name = "end", 
    description = "Force-ends an ongoing war", 
    usage = "/uwa war end <war> <winning_faction|DRAW> [win_condition]", 
    catchAll = true)

public class CmdWarAdminWarEnd implements UnitedCommandExecutor {

    
    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        switch (args.length) {
            case 1:
                return WarManager.instance().getWarTitles();
            case 2:
                var war = WarManager.instance().getWar(args[0]);
                if (war == null)
                    return null;
                List<String> factionNames = new ArrayList<>(war.getWarFactions().stream().map(WarFaction::getName).toList());
                factionNames.add("DRAW");
                return factionNames;
            case 3:
                if (!args[1].equalsIgnoreCase("DRAW")) {
                    var faction = WarManager.instance().getWarFaction(args[1]);
                    if (faction == null)
                        return null;
                    return faction.getWinConditions().keySet().stream().toList();
                }
            default:
                break;
        };

        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length < 2) {
            sendUsage(sender);
            return;
        }

        var war = WarManager.instance().getWar(args[0]);
        if (war == null) {
            United.messenger().send(sender, "errors.object-not-found", args[0]);
            return;
        }

        if (!args[1].equalsIgnoreCase("DRAW")) {
            var winningFaction = WarManager.instance().getWarFaction(args[1]);
            if (winningFaction == null) {
                United.messenger().send(sender, "errors.object-not-found", args[1]);
                return;
            }

            war.setWinningFaction(winningFaction);
            
            if (args.length > 2) {
                if (!winningFaction.getWinConditions().containsKey(args[2])) {
                    United.messenger().send(sender, "errors.object-not-found", args[2]);
                    return;
                }

                war.setWinningCondition(args[2]);
            }
        }

        WarManager.instance().endWar(war);
    }

}
