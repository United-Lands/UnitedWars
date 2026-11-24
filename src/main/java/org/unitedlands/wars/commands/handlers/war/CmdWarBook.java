package org.unitedlands.wars.commands.handlers.war;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.classes.Confirmation;
import org.unitedlands.unitedlands.classes.GeopolObject;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;
import org.unitedlands.wars.classes.config.UnitedWarsConfig;
import org.unitedlands.wars.classes.wargoal.WarGoalConquest;
import org.unitedlands.wars.classes.wargoal.WarGoalRevolt;
import org.unitedlands.wars.classes.wargoal.WarGoalSkirmish;
import org.unitedlands.wars.classes.wargoal.WarGoalSubjugation;
import org.unitedlands.wars.commands.CmdWar;
import org.unitedlands.wars.managers.WarManager;
import org.unitedlands.wars.utils.WarBookUtils;

@UnitedSubCommand(parent = CmdWar.class, name = "book", description = "Creates a new war book", usage = "/war book <war_goal> [target_name]", catchAll = true, playerOnly = true)
public class CmdWarBook implements UnitedCommandExecutor {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        switch (args.length) {
        case 1:
            return WarManager.instance().getWarGoalIds();
        case 2:
            switch (args[0]) {
            case WarGoalRevolt.ID:
                return null;
            case WarGoalSkirmish.ID, WarGoalSubjugation.ID, "plunder":
                return UnitedLandsDataManager.instance().getSettlementNames();
            case WarGoalConquest.ID:
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
        if (args.length == 1 && !WarGoalRevolt.ID.equals(args[0])) {
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

        var goalSettings = warGoal.getSettings();
        if (goalSettings == null) {
            United.messenger().send(sender, "errors.generic");
            return;
        }

        var requiredRank = goalSettings.requiredRank();
        if (requiredRank != null) {
            if (!(citizen.getSettlementRanks().contains(requiredRank) || citizen.getCountryRanks().contains(requiredRank))) {
                United.messenger().send(sender, "player.war.book.no-rank");
                return;
            }
        }

        GeopolObject declarer = null;
        GeopolObject target = null;

        if (WarGoalSkirmish.ID.equals(args[0]) || "plunder".equals(args[0])) {
            declarer = citizen.getSettlement();
            target = UnitedLandsDataManager.instance().getSettlement(args[1]);
            if (target == null) {
                United.messenger().send(sender, "player.war.book.target-invalid", args[0]);
                return;
            }
        } else if (WarGoalRevolt.ID.equals(args[0])) {
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

            if (WarGoalSubjugation.ID.equals(args[0])) {
                declarer = citizen.getSettlement().getCountry();
                target = UnitedLandsDataManager.instance().getSettlement(args[1]);
                if (target == null) {
                    United.messenger().send(sender, "player.war.book.target-invalid", args[0]);
                    return;
                }
            } else if (WarGoalConquest.ID.equals(args[0])) {
                declarer = citizen.getSettlement().getCountry();
                target = UnitedLandsDataManager.instance().getRegion(args[1]);
                if (target == null) {
                    United.messenger().send(sender, "player.war.book.target-invalid", args[0]);
                    return;
                }
            }

        }

        // Do validation (internal & external)
        var validationResult = warGoal.validate(declarer, target);
        if (!validationResult.valid()) {
            United.messenger().send(sender, "player.war.book.validation-error", validationResult.message());
            return;
        }

        var cost = goalSettings.cost();
        var mobilisation = declarer.getAttribute("MOBILIZATION", UnitedWarsConfig.get().geopolAttributeDefaults().get("MOBILIZATION"));
        if (mobilisation.getCurrentValue() < cost) {
            United.messenger().send(sender, "player.war.book.cost-too-high", String.valueOf(cost), String.valueOf(mobilisation.getCurrentValue()));
            return;
        }

        Confirmation confirmation = new Confirmation("warbook");
        GeopolObject finalDeclarer = declarer;
        GeopolObject finalTarget = target;

        confirmation.setRunnable(() -> {

            ItemStack warBook = WarBookUtils.createWarBook(warGoal.getId(), finalDeclarer.getUuid(), finalTarget.getUuid());

            mobilisation.setCurrentValue(mobilisation.getCurrentValue() - cost);
            finalDeclarer.addAttribute("MOBILIZATION", mobilisation);
            finalDeclarer.save();


            var overflow = player.getInventory().addItem(warBook);
            if (overflow != null && !overflow.isEmpty()) {
                for (var set : overflow.entrySet()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), set.getValue());
                }
            }

            United.messenger().send(player, "war-book-created");

        }).setTitle(United.messenger().get("player.war.book.confirm-cost", String.valueOf(cost))).setSender(player).setReceiver(player)
                .send();


    }

}
