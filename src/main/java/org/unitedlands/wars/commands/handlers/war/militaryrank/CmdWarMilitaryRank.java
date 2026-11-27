package org.unitedlands.wars.commands.handlers.war.militaryrank;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.wars.commands.CmdWar;

@UnitedSubCommand(
    parent          = CmdWar.class,
    name            = "militaryrank",
    playerOnly      = true
)
public class CmdWarMilitaryRank implements UnitedCommandExecutor {

    public static final String PERMISSION = "country.manage.military-ranks";

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

    }

}
