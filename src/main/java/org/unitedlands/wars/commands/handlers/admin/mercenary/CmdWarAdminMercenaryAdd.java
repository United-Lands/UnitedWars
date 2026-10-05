package org.unitedlands.wars.commands.handlers.admin.mercenary;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;
import org.unitedlands.wars.managers.WarManager;

@UnitedSubCommand(
    parent = CmdWarAdminMercenary.class, 
    name = "add", 
    description = "Adds a mercenary to a war", 
    usage = "/uwa mercenary add <faction> <player>", 
    catchAll = true
)

public class CmdWarAdminMercenaryAdd implements UnitedCommandExecutor {

    

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> WarManager.instance().getWarFactionNames();
            case 2 -> Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            default -> null;
        };
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 2) {
            sendUsage(sender);
            return;
        }

        var faction = WarManager.instance().getWarFaction(args[0]);
        if (faction == null) {
            United.messenger().send(sender, "errors.object-not-found", args[0]);
            return;
        }
        
        var player = Bukkit.getPlayer(args[1]);
        if (player == null || !player.isOnline()) {
            United.messenger().send(sender, "errors.player-not-found", args[1]);
            return;
        }

        var maxMercs = faction.getMaxMercenaries();
        if (maxMercs >= faction.getMercenaryCount()) {
            United.messenger().send(sender, "admin.mercenary.add.limit-reached");
            return;
        }

        var playerWars = WarManager.instance().getPlayerWars(player);
        if (playerWars.size() > 0) {
            United.messenger().send(sender, "admin.mercenary.add.player-in-war");
            return;
        }

        var citizen = UnitedLandsDataManager.instance().getCitizen(player);
        if (citizen == null) 
            return;

        if (!WarManager.instance().citizenHasMilitaryRank(citizen)){
            United.messenger().send(sender, "admin.mercenary.add.no-military-rank");
            return;           
        }

        faction.addMercenary(citizen);
        WarManager.instance().getDatabaseManager().getWarFactionService().updateAsync(faction);

        faction.getWar().refreshOnlinePlayerFactions();

        United.messenger().send(sender, "admin.mercenary.add.success", player.getName(), faction.getCleanName());
    }

}
