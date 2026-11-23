package org.unitedlands.wars.commands.handlers.admin.war;

import java.util.List;
import java.util.Random;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.classes.wargoal.WarGoalConquest;
import org.unitedlands.wars.classes.wargoal.WarGoalRevolt;
import org.unitedlands.wars.classes.wargoal.WarGoalSkirmish;
import org.unitedlands.wars.classes.wargoal.WarGoalSubjugation;
import org.unitedlands.wars.managers.WarManager;
import org.unitedlands.wars.utils.GeopolUtils;

@UnitedSubCommand(
    parent = CmdWarAdminWar.class, 
    name = "create", 
    description = "Creates a new war", 
    usage = "/uwa war create <war_goal> <attacker> <target>", 
    catchAll = true
)
public class CmdWarAdminWarCreate implements UnitedCommandExecutor {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {

        switch (args.length) {
        case 1:
            return WarManager.instance().getWarGoalIds();
        case 2:
            switch (args[0]) {
            case WarGoalConquest.ID, WarGoalSubjugation.ID:
                return UnitedLandsDataManager.instance().getCountryNames();
            case "plunder", WarGoalSkirmish.ID, WarGoalRevolt.ID:
                return UnitedLandsDataManager.instance().getSettlementNames();
            }
        case 3:
            switch (args[0]) {
            case WarGoalConquest.ID, WarGoalRevolt.ID:
                return UnitedLandsDataManager.instance().getRegionNames();
            case "plunder", WarGoalSkirmish.ID, WarGoalSubjugation.ID:
                return UnitedLandsDataManager.instance().getSettlementNames();
            case "INDEPENDENCE":
                return UnitedLandsDataManager.instance().getCountryNames();
            }
        }

        return null;

    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length < 3) {
            sendUsage(sender);
            return;
        }

        var warGoal = WarManager.instance().getWarGoal(WarGoalSkirmish.ID);
        try {
            warGoal = WarManager.instance().getWarGoal(args[0]);
        } catch (Exception ex) {
            United.messenger().send(sender, "errors.invalid-war-goal", args[0]);
            return;
        }

        var obj1 = GeopolUtils.getGeopolObject(args[1]);
        if (obj1 == null) {
            United.messenger().send(sender, "errors.object-not-found", args[1]);
            return;
        }

        var obj2 = GeopolUtils.getGeopolObject(args[2]);
        if (obj2 == null) {
            United.messenger().send(sender, "errors.object-not-found", args[2]);
            return;
        }

        String title = "Unnamed_War_" + (new Random()).nextInt(10000, 99999);
        String description = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua.";

        if (args.length >= 4)
            title = args[3];

        War war = War.create(warGoal, obj1, obj2, title, description);
        if (war == null) {
            United.messenger().send(sender, "errors.generic");
            return;
        }

        WarManager.instance().registerWar(war);
    }

}
