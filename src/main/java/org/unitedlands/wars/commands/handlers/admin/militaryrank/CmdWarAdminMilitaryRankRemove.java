package org.unitedlands.wars.commands.handlers.admin.militaryrank;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;
import org.unitedlands.wars.managers.WarManager;
import org.unitedlands.wars.managers.WarMetaDataManager;

@UnitedSubCommand(
    parent = CmdWarAdminMilitaryRank.class,
    name = "remove",
    description = "Removes the military rank of a citizen",
    usage = "/uwa militaryrank remove <player>",
    catchAll = true
)
public class CmdWarAdminMilitaryRankRemove implements UnitedCommandExecutor {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1)
            return UnitedLandsDataManager.instance().getCitizenNames();
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 1) {
            sendUsage(sender);
            return;
        }

        var targetCitizen = UnitedLandsDataManager.instance().getCitizen(args[0]);
        if (targetCitizen == null) {
            United.messenger().send(sender, "errors.player-not-found", args[0]);
            return;
        }

        var rank = WarManager.instance().getCitizenMilitaryRank(targetCitizen);
        if (rank.equals(WarManager.DEFAULT_MILITARY_RANK)) {
            United.messenger().send(sender, "admin.military-rank.no-rank", targetCitizen.getName());
            return;
        }

        WarMetaDataManager.instance().removeMilitaryRank(targetCitizen);

        United.messenger().send(sender, "admin.military-rank.remove-success", rank, targetCitizen.getName());

        var targetPlayer = Bukkit.getPlayer(targetCitizen.getUuid());
        if (targetPlayer != null && !targetPlayer.equals(sender))
            United.messenger().send(targetPlayer, "player.military-rank.lost", rank);
    }

}
