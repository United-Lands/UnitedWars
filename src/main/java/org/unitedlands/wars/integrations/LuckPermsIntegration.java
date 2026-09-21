package org.unitedlands.wars.integrations;

import java.util.List;

import org.bukkit.entity.Player;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.node.Node;

public class LuckPermsIntegration {

    private LuckPerms luckPerms;

    public LuckPermsIntegration() {
        luckPerms = LuckPermsProvider.get();
    }

    public List<String> getPermissions(Player player) {
        var user = luckPerms.getUserManager().getUser(player.getUniqueId());
        return user.getNodes().stream().map(Node::getKey).toList();
    }

    public void addPermission(Player player, String permission) {
        var user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user != null) {
            user.data().add(Node.builder(permission).build());
            luckPerms.getUserManager().saveUser(user);
        }
    }

    
    public void removePermission(Player player, String permission) {
        var user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user != null) {
            user.data().remove(Node.builder(permission).build());
            luckPerms.getUserManager().saveUser(user);
        }
    }

}
