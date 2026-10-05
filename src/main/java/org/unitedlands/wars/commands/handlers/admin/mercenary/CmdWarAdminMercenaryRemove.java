package org.unitedlands.wars.commands.handlers.admin.mercenary;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.classes.Citizen;
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

public class CmdWarAdminMercenaryRemove implements UnitedCommandExecutor {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> WarManager.instance().getWarFactionNames();
            case 2 -> WarManager.instance().getWarFaction(args[0]).getMercenaries().stream().map(Citizen::getName).toList();
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
        
        var citizen = UnitedLandsDataManager.instance().getCitizen(args[1]);
        if (citizen == null) 
            return;
        
        if (!faction.getMercenaries().contains(citizen)) {
            United.messenger().send(sender, "admin.mercenary.remove.not-mercenary");
            return;
        }

        faction.removeMercenary(citizen);
        WarManager.instance().getDatabaseManager().getWarFactionService().updateAsync(faction);

        faction.getWar().refreshOnlinePlayerFactions();

        United.messenger().send(sender, "admin.mercenary.remove.success", citizen.getName(), faction.getCleanName());
    }

}
