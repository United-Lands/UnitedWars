package org.unitedlands.wars.commands.handlers.war.mercenary;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.wars.commands.CmdWar;

@UnitedSubCommand(
    parent          = CmdWar.class, 
    name            = "mercenary", 
    playerOnly      = true
)
public class CmdWarMercenary implements UnitedCommandExecutor {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

    }

}
