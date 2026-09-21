package org.unitedlands.wars.commands.handlers.war;

import java.util.List;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.classes.Confirmation;
import org.unitedlands.unitedlands.classes.GeopolObject;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;
import org.unitedlands.wars.UnitedWars;
import org.unitedlands.wars.classes.wargoal.WarGoal;
import org.unitedlands.wars.commands.CmdWar;
import org.unitedlands.wars.managers.WarManager;
import org.unitedlands.wars.utils.WarBookUtils;

@UnitedSubCommand(
    parent = CmdWar.class, 
    name = "book", 
    description = "Creates a new war book", 
    usage = "/war book <war_goal> [target_name]", 
    catchAll = true, 
    playerOnly = true
)
public class CmdWarBook implements UnitedCommandExecutor {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        switch (args.length) {
        case 1:
            return WarManager.instance().getWarGoalIds();
        case 2:
            switch (args[0]) {
            case "revolt":
                return null;
            case "skirmish", "subjugation", "plunder":
                return UnitedLandsDataManager.instance().getSettlementNames();
            case "conquest":
                return UnitedLandsDataManager.instance().getRegionNames();
            }
            break;
        }
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length == 0) {
            sendUsage(sender);
            return;
        }
        if (args.length == 1 && !"revolt".equals(args[0])) {
            sendUsage(sender);
            return;
        }

        var warGoal = WarManager.instance().getWarGoal(args[0]);
        if (warGoal == null) {
            United.messenger().send(sender, "player.war.book.unknown-war-goal", args[0]);
            return;
        }

        var player = (Player) sender;
        var citizen = UnitedLandsDataManager.instance().getCitizen(player);
        if (citizen == null) {
            United.messenger().send(sender, "errors.no-citizen-data");
            return;
        }
        if (citizen.getSettlement() == null) {
            United.messenger().send(sender, "player.war.book.not-in-settlement");
            return;
        }

        var requiredRank = UnitedWars.instance().getConfig().getString("war-goal-settings." + args[0] + ".required-rank");
        if (requiredRank != null) {
            if (!(citizen.getSettlementRanks().contains(requiredRank) || citizen.getCountryRanks().contains(requiredRank))) {
                United.messenger().send(sender, "player.war.book.no-rank");
                return;
            }
        }

        GeopolObject declarer = null;
        GeopolObject target = null;

        if ("skirmish".equals(args[0]) || "plunder".equals(args[0])) {
            declarer = citizen.getSettlement();
            target = UnitedLandsDataManager.instance().getSettlement(args[1]);
            if (target == null) {
                United.messenger().send(sender, "player.war.book.target-invalid", args[0]);
                return;
            }
        } else if ("revolt".equals(args[0])) {
            declarer = citizen.getSettlement();
            target = citizen.getSettlement().getRegion();
            if (target == null) {
                United.messenger().send(sender, "player.war.book.no-region");
                return;
            }
        } else {

            // All remaining goals require a country
            if (!citizen.getSettlement().hasCountry()) {
                United.messenger().send(sender, "player.war.book.not-in-country");
                return;
            }

            if ("subjugation".equals(args[0])) {
                declarer = citizen.getSettlement().getCountry();
                target = UnitedLandsDataManager.instance().getSettlement(args[1]);
                if (target == null) {
                    United.messenger().send(sender, "player.war.book.target-invalid", args[0]);
                    return;
                }
            } else if ("conquest".equals(args[0])) {
                declarer = citizen.getSettlement().getCountry();
                target = UnitedLandsDataManager.instance().getRegion(args[1]);
                if (target == null) {
                    United.messenger().send(sender, "player.war.book.target-invalid", args[0]);
                    return;
                }
            }

        }

        var validationResult = warGoal.validate(declarer, target);
        if (!validationResult.valid()) {
            United.messenger().send(sender, "player.war.book.validation-error", validationResult.message());
            return;
        }

        // TODO: Mobilisation check

        createDeclarationBook(player, declarer.getUuid(), target.getUuid(), warGoal);

    }

    private void createDeclarationBook(Player player, UUID declarer, UUID target, WarGoal warGoal) {

        // Integer mobilisationCost = plugin.getConfig().getInt("war-goal-settings." +
        // warGoal.toString().toLowerCase() + ".cost", 0);

        Integer mobilisationCost = 0;

        Confirmation confirmation = new Confirmation("warbook");
        confirmation.setRunnable(() -> {

            ItemStack warBook = WarBookUtils.createWarBook(warGoal.getId(), declarer, target);

            // deductCosts(playerTown, mobilisationCost);

            var overflow = player.getInventory().addItem(warBook);
            if (overflow != null && !overflow.isEmpty()) {
                for (var set : overflow.entrySet()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), set.getValue());
                }
            }

            United.messenger().send(player, "war-book-created");

        }).setTitle("<gray>Creating this war declaration book will cost " + mobilisationCost + " mobilisation. Continue?").setSender(player).setReceiver(player)
                .send();
    }

}
