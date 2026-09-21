package org.unitedlands.wars.commands;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;

@UnitedCommand(
        name = "unitedwarsadmin",
        aliases = { "uwa" },
        description = "UnitedWars Admin commands",
        usage = "/uwa <command>",
        permission = "united.wars.admin"
)
public class CmdWarAdmin implements UnitedCommandExecutor {

    @Override
    public void handleCommand(CommandSender sender, String[] args) { }
    
}
