package org.unitedlands.wars.commands;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;

@UnitedCommand(
        name = "ulwar",
        aliases = { "war" },
        description = "War commands",
        usage = "/war <command>",
        playerOnly = true
)
public class CmdWar implements UnitedCommandExecutor {

    @Override
    public void handleCommand(CommandSender sender, String[] args) { }
    
}