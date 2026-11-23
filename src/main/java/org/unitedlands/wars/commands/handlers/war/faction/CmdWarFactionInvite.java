package org.unitedlands.wars.commands.handlers.war.faction;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;
import org.unitedlands.wars.classes.war.WarFaction;
import org.unitedlands.wars.managers.WarManager;

@UnitedSubCommand(
    parent = CmdWarFaction.class, 
    name = "invite", 
    description = "Invites a geopolitical entty to the war faction", 
    usage = "/war faction invite <facion_name> <target>", 
    playerOnly = true, 
    catchAll = true
)
public class CmdWarFactionInvite implements UnitedCommandExecutor {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        switch (args.length) {
            case 1:
                var player = (Player) sender;
                var playerWars = WarManager.instance().getPlayerWars(player);
                if (playerWars.size() > 0) {
                    List<WarFaction> playerFactions = new ArrayList<>();
                    for (var war : playerWars) {
                        playerFactions.add(war.getPlayerFaction(player));
                    }
                    return playerFactions.stream().map(WarFaction::getName).toList();
                }
                break;
            case 2:
                var options = UnitedLandsDataManager.instance().getCountryNames();
                options.addAll(UnitedLandsDataManager.instance().getSettlementNames());
                return options;
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

        var receiver = UnitedLandsDataManager.instance().getGeopolObject(args[1]);
        if (receiver == null) {
            United.messenger().send(sender, "errors.object-not-found", args[1]);
            return;
        }

        var warGoal = faction.getWar().getWarGoal();
        var result = warGoal.validateInvite(faction, receiver);

        if (!result.valid()) {
            United.messenger().sendRaw(sender, "<error>" + result.message() + "</error>");
            return;
        }

        warGoal.joinWar(receiver, faction);

        United.messenger().broadcast("player.invite.joined", receiver.getDisplayName(), faction.getColoredName(), faction.getWar().getCleanTitle());
    }

}
