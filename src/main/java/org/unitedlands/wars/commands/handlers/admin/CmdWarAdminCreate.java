package org.unitedlands.wars.commands.handlers.admin;

import java.util.List;
import java.util.Random;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.commands.CmdWarAdmin;
import org.unitedlands.wars.managers.WarManager;
import org.unitedlands.wars.utils.GeopolUtils;

@UnitedSubCommand(parent = CmdWarAdmin.class, name = "create", description = "Creates a new war", usage = "/uwa creare <war_goal> <attacker> <target>", catchAll = true)
public class CmdWarAdminCreate implements UnitedCommandExecutor {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {

        switch (args.length) {
        case 1:
            return WarManager.instance().getWarGoalIds();
        case 2:
            switch (args[0]) {
            case "conquest", "subjugation":
                return UnitedLandsDataManager.instance().getCountryNames();
            case "plunder", "skirmish", "revolt":
                return UnitedLandsDataManager.instance().getSettlementNames();
            }
        case 3:
            switch (args[0]) {
            case "conquest", "revolt":
                return UnitedLandsDataManager.instance().getRegionNames();
            case "plunder", "skirmish", "subjugation":
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

        var warGoal = WarManager.instance().getWarGoal("skirmish");
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
