package org.unitedlands.wars.commands.handlers.war.faction;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.wars.commands.CmdWar;

@UnitedSubCommand(
    parent          = CmdWar.class, 
    name            = "faction", 
    playerOnly      = true
)
public class CmdWarFaction implements UnitedCommandExecutor {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

    }

}
