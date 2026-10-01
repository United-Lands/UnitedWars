package org.unitedlands.wars.commands.handlers.war.faction.set;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.wars.commands.handlers.war.faction.CmdWarFaction;

@UnitedSubCommand(
    parent          = CmdWarFaction.class, 
    name            = "set", 
    playerOnly      = true
)
public class CmdWarFactionSet implements UnitedCommandExecutor {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {
    }

}
