package org.unitedlands.wars.commands.handlers.war;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.unitedlands.UnitedLib;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.wars.UnitedWars;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.classes.war.WarFaction;
import org.unitedlands.wars.commands.CmdWar;
import org.unitedlands.wars.managers.WarManager;

@UnitedSubCommand(
    parent = CmdWar.class, 
    name = "createnpc", 
    description = "Creates an npc for a war", 
    usage = "/war createnpc <war> <faction>", 
    catchAll = true, 
    playerOnly = true,
    permission = "united.wars.admin"
)
public class CmdWarCreateNpc implements UnitedCommandExecutor {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1)
            return WarManager.instance().getActivePlayerWars(((Player) sender)).stream().map(War::getTitle).toList();
        if (args.length == 2) {
            var war = WarManager.instance().getWar(args[0]);
            if (war != null)
                return war.getWarFactions().stream().map(WarFaction::getName).toList();
        }
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length != 2)
            return;

        var player = (Player) sender;
        var war = WarManager.instance().getWar(args[0]);
        if (war == null || !war.isActive()) {
            return;
        }

        var faction = WarManager.instance().getWarFaction(args[1]);
        if (faction == null ) {
            return;
        }

        var newMobId = UnitedLib.getInstance().getMobFactory().createMobAtLocation("TownGuard", player.getLocation(), 1, faction.getUuid().toString());
        UnitedLib.getInstance().getMobFactory().setName(newMobId, "Town Guard (" + faction.getColoredName() + ")");

        var entity = Bukkit.getServer().getEntity(newMobId);

        var pbc = entity.getPersistentDataContainer();
        pbc.set(new NamespacedKey(UnitedWars.instance(), "npc"), PersistentDataType.BOOLEAN, true);
        pbc.set(new NamespacedKey(UnitedWars.instance(), "rank"), PersistentDataType.STRING, "soldier");
        pbc.set(new NamespacedKey(UnitedWars.instance(), "faction"), PersistentDataType.STRING, faction.getUuid().toString());
        pbc.set(new NamespacedKey(UnitedWars.instance(), "war"), PersistentDataType.STRING, faction.getWar().getUuid().toString());
    }

}
