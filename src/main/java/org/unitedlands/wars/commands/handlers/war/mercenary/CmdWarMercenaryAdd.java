package org.unitedlands.wars.commands.handlers.war.mercenary;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;
import org.unitedlands.wars.classes.war.WarFaction;
import org.unitedlands.wars.managers.WarManager;

@UnitedSubCommand(
    parent = CmdWarMercenary.class, 
    name = "add", 
    description = "Adds a mercenary to a war faction", 
    usage = "/war mercenary add <faction> <player>", 
    catchAll = true
)
public class CmdWarMercenaryAdd implements UnitedCommandExecutor {

    
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

        var targetPlayer = Bukkit.getPlayer(args[1]);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            United.messenger().send(sender, "errors.player-not-found", args[1]);
            return;
        }

        var maxMercs = faction.getMaxMercenaries();
        if (maxMercs >= faction.getMercenaryCount()) {
            United.messenger().send(sender, "player.mercenary.add.limit-reached");
            return;
        }

        var playerWars = WarManager.instance().getPlayerWars(player);
        if (playerWars.size() > 0) {
            United.messenger().send(sender, "player.mercenary.add.player-in-war");
            return;
        }

        var targetCitizen = UnitedLandsDataManager.instance().getCitizen(player);
        if (targetCitizen == null) 
            return;

        if (!WarManager.instance().citizenHasMilitaryRank(targetCitizen)){
            United.messenger().send(sender, "player.mercenary.add.no-military-rank");
            return;           
        }

        faction.addMercenary(targetCitizen);
        WarManager.instance().getDatabaseManager().getWarFactionService().updateAsync(faction);

        faction.getWar().refreshOnlinePlayerFactions();

        United.messenger().send(sender, "player.mercenary.add.success", player.getName(), faction.getCleanName());
    }

}
