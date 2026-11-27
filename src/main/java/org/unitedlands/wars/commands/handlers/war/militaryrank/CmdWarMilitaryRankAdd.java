package org.unitedlands.wars.commands.handlers.war.militaryrank;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.managers.PermissionManager;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;
import org.unitedlands.wars.managers.WarManager;
import org.unitedlands.wars.managers.WarMetaDataManager;

@UnitedSubCommand(
    parent = CmdWarMilitaryRank.class,
    name = "add",
    description = "Assigns a military rank to a citizen of your country",
    usage = "/war militaryrank add <player> <rank>",
    playerOnly = true,
    catchAll = true
)
public class CmdWarMilitaryRankAdd implements UnitedCommandExecutor {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        var citizen = UnitedLandsDataManager.instance().getCitizen((Player) sender);
        if (citizen == null || citizen.getCountry() == null)
            return null;
        return switch (args.length) {
            case 1 -> citizen.getCountry().getCitizens().stream().map(Citizen::getName).toList();
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

        var player = (Player) sender;

        var citizen = UnitedLandsDataManager.instance().getCitizen(player);
        if (citizen == null) {
            United.messenger().send(sender, "errors.no-citizen-data");
            return;
        }

        var country = citizen.getCountry();
        if (country == null) {
            United.messenger().send(sender, "player.military-rank.not-in-country");
            return;
        }

        if (!PermissionManager.instance().hasRankPermission(CmdWarMilitaryRank.PERMISSION, citizen)) {
            United.messenger().send(sender, "player.military-rank.no-permission");
            return;
        }

        var targetCitizen = UnitedLandsDataManager.instance().getCitizen(args[0]);
        if (targetCitizen == null) {
            United.messenger().send(sender, "errors.player-not-found", args[0]);
            return;
        }

        if (!country.equals(targetCitizen.getCountry())) {
            United.messenger().send(sender, "player.military-rank.target-not-in-country", targetCitizen.getName());
            return;
        }

        var rank = args[1];
        if (!WarManager.instance().isAssignableMilitaryRank(rank)) {
            United.messenger().send(sender, "player.military-rank.unknown-rank", rank);
            return;
        }

        if (WarManager.instance().getCitizenMilitaryRank(targetCitizen).equals(rank)) {
            United.messenger().send(sender, "player.military-rank.already-owned", targetCitizen.getName(), rank);
            return;
        }

        var uniqueHolder = WarManager.instance().getUniqueMilitaryRankHolder(targetCitizen, rank);
        if (uniqueHolder != null) {
            United.messenger().send(sender, "player.military-rank.unique-taken", rank, uniqueHolder.getName());
            return;
        }

        WarMetaDataManager.instance().setMilitaryRank(targetCitizen, rank);

        United.messenger().send(sender, "player.military-rank.add-success", rank, targetCitizen.getName());

        var targetPlayer = Bukkit.getPlayer(targetCitizen.getUuid());
        if (targetPlayer != null && !targetPlayer.equals(player))
            United.messenger().send(targetPlayer, "player.military-rank.received", rank);
    }

}
