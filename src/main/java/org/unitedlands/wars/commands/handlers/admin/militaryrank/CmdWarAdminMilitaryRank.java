package org.unitedlands.wars.commands.handlers.admin.militaryrank;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.wars.commands.CmdWarAdmin;

@UnitedSubCommand(
    parent          = CmdWarAdmin.class,
    name            = "militaryrank"
)
public class CmdWarAdminMilitaryRank implements UnitedCommandExecutor {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

    }

}
