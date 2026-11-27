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
    name = "add",
    description = "Assigns a military rank to a citizen",
    usage = "/uwa militaryrank add <player> <rank>",
    catchAll = true
)
public class CmdWarAdminMilitaryRankAdd implements UnitedCommandExecutor {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> UnitedLandsDataManager.instance().getCitizenNames();
            case 2 -> WarManager.instance().getAssignableMilitaryRanks();
            default -> null;
        };
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 2) {
            sendUsage(sender);
            return;
        }

        var targetCitizen = UnitedLandsDataManager.instance().getCitizen(args[0]);
        if (targetCitizen == null) {
            United.messenger().send(sender, "errors.player-not-found", args[0]);
            return;
        }

        var rank = args[1];
        if (!WarManager.instance().isAssignableMilitaryRank(rank)) {
            United.messenger().send(sender, "admin.military-rank.unknown-rank", rank);
            return;
        }

        if (WarManager.instance().getCitizenMilitaryRank(targetCitizen).equals(rank)) {
            United.messenger().send(sender, "admin.military-rank.already-owned", targetCitizen.getName(), rank);
            return;
        }

        var uniqueHolder = WarManager.instance().getUniqueMilitaryRankHolder(targetCitizen, rank);
        if (uniqueHolder != null) {
            United.messenger().send(sender, "admin.military-rank.unique-taken", rank, uniqueHolder.getName());
            return;
        }

        WarMetaDataManager.instance().setMilitaryRank(targetCitizen, rank);

        United.messenger().send(sender, "admin.military-rank.add-success", rank, targetCitizen.getName());

        var targetPlayer = Bukkit.getPlayer(targetCitizen.getUuid());
        if (targetPlayer != null && !targetPlayer.equals(sender))
            United.messenger().send(targetPlayer, "player.military-rank.received", rank);
    }

}
