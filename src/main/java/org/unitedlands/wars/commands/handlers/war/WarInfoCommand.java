package org.unitedlands.wars.commands.handlers.war;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.wars.classes.infoscreens.WarInfoScreen;
import org.unitedlands.wars.commands.CmdWar;
import org.unitedlands.wars.managers.WarManager;

@UnitedSubCommand(
    parent = CmdWar.class, 
    name = "info", 
    description = "Shows information on a war", 
    usage = "/war info <war>", 
    playerOnly = true
)
public class WarInfoCommand implements UnitedCommandExecutor {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1)
            return WarManager.instance().getWarTitles();
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 1) {
            sendUsage(sender);
            return;
        }

        var war = WarManager.instance().getWar(args[0]);
        if (war == null) {
            // TODO: Error
            return;
        }

        var warInfoScreen = new WarInfoScreen(war);
        warInfoScreen.send(sender);

    }

}
