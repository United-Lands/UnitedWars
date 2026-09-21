package org.unitedlands.wars.commands.handlers.war;

import java.util.List;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.classes.GeopolObject;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;
import org.unitedlands.wars.UnitedWars;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.commands.CmdWar;
import org.unitedlands.wars.managers.WarManager;
import org.unitedlands.wars.utils.GeopolUtils;
import org.unitedlands.wars.utils.WarBookUtils;

@UnitedSubCommand(
    parent = CmdWar.class, 
    name = "declare", 
    description = "Declares a war", 
    usage = "/war declare", 
    playerOnly = true
)
public class CmdWarDeclare implements UnitedCommandExecutor {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        var player = (Player) sender;

        var heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || !heldItem.getType().equals(Material.WRITTEN_BOOK) || !WarBookUtils.isWarBook(heldItem)) {
            United.messenger().send(sender, "error-signed-war-book-missing");
            return;
        }

        var warBookData = WarBookUtils.getWarBookData(heldItem);
        if (warBookData == null) {
            United.messenger().send(sender, "error-signed-war-book-missing");
            return;
        }

        var citizen = UnitedLandsDataManager.instance().getCitizen(player);
        if (citizen == null) {
            United.messenger().send(sender, "errors.no-citizen-data");
            return;
        }

        var warGoal = WarManager.instance().getWarGoal((String) warBookData.get("wargoal"));

        var requiredRank = UnitedWars.instance().getConfig().getString("war-goal-settings." + warGoal.getId() + ".required-rank");
        if (requiredRank != null) {
            if (!(citizen.getSettlementRanks().contains(requiredRank) || citizen.getCountryRanks().contains(requiredRank))) {
                United.messenger().send(sender, "player.war.book.no-rank");
                return;
            }
        }

        GeopolObject declarer = GeopolUtils.getGeopolObject(UUID.fromString((String) warBookData.get("declarerId")));
        GeopolObject target = GeopolUtils.getGeopolObject(UUID.fromString((String) warBookData.get("targetId")));

        if (declarer == null || target == null) {
            United.messenger().send(sender, "errors.generic");
            return;
        }

        var validationResult = warGoal.validate(declarer, target);
        if (!validationResult.valid()) {
            United.messenger().send(sender, "player.war.book.validation-error", validationResult.message());
            return;
        }

        var warName = WarBookUtils.getWarName(heldItem).replace(" ", "_");
        var warDescription = WarBookUtils.getWarDescription(heldItem);

        War war = War.create(warGoal, declarer, target, warName, warDescription);
        if (war == null) {
            United.messenger().send(sender, "errors.generic");
            return;
        }

        WarManager.instance().registerWar(war);

        player.getInventory().setItem(EquipmentSlot.HAND, new ItemStack(Material.AIR));
    }



}
