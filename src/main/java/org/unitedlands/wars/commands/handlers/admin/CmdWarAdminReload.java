package org.unitedlands.wars.commands.handlers.admin;

import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.registrars.messages.UnitedMessagesRegistrar;
import org.unitedlands.utils.United;
import org.unitedlands.wars.UnitedWars;
import org.unitedlands.wars.commands.CmdWarAdmin;
import org.unitedlands.wars.schedulers.WarScheduler;

@UnitedSubCommand(
    parent = CmdWarAdmin.class, 
    name = "reload", 
    description = "Reloads configd", 
    usage = "/uwa reload"
)

public class CmdWarAdminReload implements UnitedCommandExecutor {

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        WarScheduler.instance().shutdown();

        UnitedWars.instance().reloadConfig();
        UnitedMessagesRegistrar.reload(UnitedWars.instance());

        WarScheduler.instance().initialize();

        United.messenger().send(sender, "reload");
    }

    

}
