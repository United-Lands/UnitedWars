package org.unitedlands.wars.commands;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;

@UnitedCommand(
        name = "warcamp",
        aliases = { "wc" },
        description = "Warcamp commands",
        usage = "/warcamp <command>",
        playerOnly = true
)
public class CmdWarcamp implements UnitedCommandExecutor {

    @Override
    public void handleCommand(CommandSender sender, String[] args) { }
    
}