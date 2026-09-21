package org.unitedlands.wars.managers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.metadata.IntegerMetaDataField;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;
import org.unitedlands.wars.UnitedWars;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.classes.war.WarFaction;

public class WarMetaDataManager {

    private static WarMetaDataManager instance;

    private final String WAR_LIVES_KEY = "unitedwar_war_lives_";

    public static WarMetaDataManager instance() {
        return instance;
    }

    public WarMetaDataManager() {
        instance = this;
    }

    public void validateWarLivesMetaData(Player player) {

        var citizen = UnitedLandsDataManager.instance().getCitizen(player);
        if (citizen == null) {
            United.logger().error("Couldn't find citizen data for " + player.getName(), "UnitedWars");
            return;
        }

        // Remove wars that have ended while the player was offline
        var citizenMeta = citizen.getMetadata();
        List<String> oldWarLivesMetas = new ArrayList<>();
        for (var entry : citizenMeta.entrySet()) {
            if (entry.getKey().startsWith(WAR_LIVES_KEY)) {
                var warId = UUID.fromString(entry.getKey().replace(WAR_LIVES_KEY, ""));
                if (WarManager.instance().getWar(warId) == null) {
                    oldWarLivesMetas.add(entry.getKey());
                }
            }
        }
        if (oldWarLivesMetas.size() > 0) {
            for (var oldWarLivesMeta : oldWarLivesMetas)
                citizen.removeMetadata(oldWarLivesMeta);
        }

        // Add wars that have been registered while the player was offline
        ArrayList<IntegerMetaDataField> newWarLivesMetas = new ArrayList<>();
        var playerWars = WarManager.instance().getActivePlayerWars(player);
        for (War war : playerWars) {
            var warKey = getWarLivesMetaKey(war.getUuid());
            if (!citizenMeta.containsKey(warKey)) {
                newWarLivesMetas.add(addWarLivesMeta(citizen, warKey, war));
            }
        }

        if (newWarLivesMetas.size() > 0 || oldWarLivesMetas.size() > 0) {
            citizen.save();
        }
    }

    private IntegerMetaDataField addWarLivesMeta(Citizen citizen, String warKey, War war) {
        var goal = war.getWarGoal().getId();
        var warGoalLives = UnitedWars.instance().getConfig().getInt("war-goal-settings." + goal + ".war-lives", 5);
        var warLivedMetaData = new IntegerMetaDataField(warKey, warGoalLives, war.getCleanTitle() + " War Lives", true);
        citizen.addMetadata(warLivedMetaData);
        return warLivedMetaData;
    }

    private IntegerMetaDataField getOrCreateWarLivesMeta(Citizen citizen, War war) {
        var warKey = getWarLivesMetaKey(war.getUuid());
        var warLivesMeta = (IntegerMetaDataField) citizen.getMetadata(warKey);
        if (warLivesMeta == null) {
            warLivesMeta = addWarLivesMeta(citizen, warKey, war);
            citizen.save();
        }
        return warLivesMeta;
    }

    public void addWarLives(Player player, War war) {
        var citizen = UnitedLandsDataManager.instance().getCitizen(player);
        if (citizen == null) {
            United.logger().error("Couldn't find citizen data for " + player.getName(), "UnitedWars");
            return;
        }
        getOrCreateWarLivesMeta(citizen, war);
    }

    public int getWarLives(Player player, War war) {
        var citizen = UnitedLandsDataManager.instance().getCitizen(player);
        if (citizen == null) {
            United.logger().error("Couldn't find citizen data for " + player.getName(), "UnitedWars");
            return 0;
        }
        var warLivesMeta = getOrCreateWarLivesMeta(citizen, war);
        return warLivesMeta.getValue();
    }

    public void setWarLives(Player player, War war, int warLives) {
        var citizen = UnitedLandsDataManager.instance().getCitizen(player);
        if (citizen == null) {
            United.logger().error("Couldn't find citizen data for " + player.getName(), "UnitedWars");
            return;
        }
        var warLivesMeta = getOrCreateWarLivesMeta(citizen, war);
        if (warLivesMeta == null) {
            return;
        }
        warLivesMeta.setValue(warLives);
        citizen.save();
    }

    public void removeWarLives(Player player, War war, int warLives) {
        var citizen = UnitedLandsDataManager.instance().getCitizen(player);
        if (citizen == null) {
            United.logger().error("Couldn't find citizen data for " + player.getName(), "UnitedWars");
            return;
        }
        var warKey = getWarLivesMetaKey(war.getUuid());
        citizen.removeMetadata(warKey);
        citizen.save();
    }

    private String getWarLivesMetaKey(UUID warId) {
        return WAR_LIVES_KEY + warId;
    }

    // Faction permissions

    public void addFactionPermission(Player player, WarFaction warFaction) {
        if (UnitedWars.instance().getLuckPermsIntegration() == null) {
            United.logger().warning("Permission based features are disabled.", "UnitedWars");
            return;
        }
        UnitedWars.instance().getLuckPermsIntegration().addPermission(player, "faction." + warFaction.getUuid());
    }

    public void validateFactionPermissions(Player player) {

        if (UnitedWars.instance().getLuckPermsIntegration() == null) {
            United.logger().warning("Permission based features are disabled.", "UnitedWars");
            return;
        }

        Set<String> permsToRemove = new HashSet<>();
        for (var perm : UnitedWars.instance().getLuckPermsIntegration().getPermissions(player)) {

            if (!perm.startsWith("faction."))
                continue;

            var factionPerm = perm.split("\\.");
            if (factionPerm.length == 2) {

                if (factionPerm[1].equals("*"))
                    continue;

                try {
                    var faction = WarManager.instance().getWarFaction(UUID.fromString(factionPerm[1]));
                    if (faction == null) {
                        permsToRemove.add(perm);
                    }
                } catch (Exception ex) {
                    United.logger().warning("Error while parsing perm" + perm + " UUID, removing.");
                    permsToRemove.add(perm);
                }
            }
        }

        for (var perm : permsToRemove) {
            UnitedWars.instance().getLuckPermsIntegration().removePermission(player, perm);
        }

    }

}
