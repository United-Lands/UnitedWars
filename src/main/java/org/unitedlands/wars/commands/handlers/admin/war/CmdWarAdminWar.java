package org.unitedlands.wars.commands.handlers.admin.war;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.wars.commands.CmdWarAdmin;

@UnitedSubCommand(
    parent          = CmdWarAdmin.class, 
    name            = "war", 
    playerOnly      = true
)
public class CmdWarAdminWar implements UnitedCommandExecutor {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

    }

}
