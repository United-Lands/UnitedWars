package org.unitedlands.wars.listeners;

import java.util.List;
import java.util.UUID;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Trident;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;
import org.unitedlands.wars.UnitedWars;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.classes.war.WarFaction;
import org.unitedlands.wars.classes.war.WarScoreType;
import org.unitedlands.wars.events.WarLivesUpdateEvent;
import org.unitedlands.wars.events.WarScoreEvent;
import org.unitedlands.wars.managers.WarManager;
import org.unitedlands.wars.managers.WarMetaDataManager;

public class PlayerDeathListener implements Listener {

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

        if (!WarManager.instance().anyWarsActive())
            return;

        var victim = event.getEntity();
        List<String> worldBlacklist = UnitedWars.instance().getConfig().getStringList("world-blacklist");
        String world = victim.getLocation().getWorld().getName();
        if (worldBlacklist.contains(world)) {
            return;
        }

        // Check if the victim is in a war
        var victimWars = WarManager.instance().getActivePlayerWars(victim);
        if (victimWars == null || victimWars.isEmpty()) {
            return;
        }

        // Check if this is a death caused by an entity
        EntityDamageEvent damageEvent = victim.getLastDamageCause();
        if (!(damageEvent instanceof EntityDamageByEntityEvent))
            return;

        // TODO: See if we can account for multiple shared wars
        War sharedWar = null;

        String killerRank = "default";
        WarFaction killerFaction = null;
        Integer killerWarLives = 0;

        LivingEntity killer = findKillingEntity(victim, (EntityDamageByEntityEvent) damageEvent);

        if (killer == null) {
            // There's no killer. Cancel.
            return;
        } else {
            if (killer instanceof Player playerKiller) {

                // If killer is a player, get the first war they share with the victim.
                // If they don't share any, cancel.
                for (var victimWar : victimWars) {
                    if (victimWar.isPlayerInWar(playerKiller)) {
                        sharedWar = victimWar;
                        break;
                    }
                }
                if (sharedWar == null)
                    return;

                // Populate the killer data based on the shared war
                var killerCitizen = UnitedLandsDataManager.instance().getCitizen(playerKiller);
                if (killerCitizen == null)
                    return;
                killerRank = WarManager.instance().getCitizenMilitaryRank(killerCitizen);
                killerFaction = sharedWar.getPlayerFaction(playerKiller);
                killerWarLives = WarMetaDataManager.instance().getWarLives(playerKiller, sharedWar);

            } else {

                // If the killer is an NPC, see if it shares its war with the victim. If so,
                // populate the killer data, otherwise cancel.
                var pdc = killer.getPersistentDataContainer();
                if (!pdc.get(new NamespacedKey(UnitedWars.instance(), "npc"), PersistentDataType.BOOLEAN)) {
                    return;
                }

                var warId = pdc.get(new NamespacedKey(UnitedWars.instance(), "war"), PersistentDataType.STRING);
                var factionId = pdc.get(new NamespacedKey(UnitedWars.instance(), "faction"), PersistentDataType.STRING);
                var rank = pdc.get(new NamespacedKey(UnitedWars.instance(), "rank"), PersistentDataType.STRING);
                var npcWar = WarManager.instance().getWar(UUID.fromString(warId));
                if (npcWar == null)
                    return;

                for (var victimWar : victimWars) {
                    if (victimWar.equals(npcWar)) {
                        sharedWar = victimWar;
                        break;
                    }
                }
                if (sharedWar == null)
                    return;

                killerFaction = WarManager.instance().getWarFaction(UUID.fromString(factionId));
                if (killerFaction == null)
                    return;

                killerRank = rank;
                killerWarLives = 1;
            }
        }

        // If the players are not tracked by UnitedLands, don't continue
        var victimCitizen = UnitedLandsDataManager.instance().getCitizen(victim);
        if (victimCitizen == null)
            return;

        // Get the victim's and killer's military rank
        String victimMilitaryRank = WarManager.instance().getCitizenMilitaryRank(victimCitizen);

        // Skip if the killer is a civilian
        if (victimMilitaryRank.equals("default") || killerRank.equals("default")) {
            return;
        }

        boolean isVictimLeader = false;
        // if (victimRes.isMayor() || victimRes.isKing()) {
        if (victimCitizen.isMayor() || victimCitizen.isLeader()) {
            isVictimLeader = true;
        }

        // Get the victim's military rank

        Double killMultiplier = UnitedWars.instance().getConfig().getDouble("military-ranks." + killerRank + ".score-multiplier");
        Double leaderBonusMultiplier = UnitedWars.instance().getConfig().getDouble("score-settings.pvp-kill.leader-kill-bonus-multiplier");
        Integer reward = UnitedWars.instance().getConfig().getInt("score-settings.pvp-kill.rank-scores." + victimMilitaryRank);

        // If either player is out of lives, continue
        var victimWarLives = WarMetaDataManager.instance().getWarLives(victim, sharedWar);

        if (victimWarLives <= 0 || killerWarLives <= 0)
            return;

        // Adjust score based on ranks and bonuses
        var scoreType = WarScoreType.PVP_KILL;
        var adjustedReward = (int) Math.round((double) reward * killMultiplier);
        if (isVictimLeader) {
            scoreType = WarScoreType.PVP_KILL_LEADER;
            adjustedReward = (int) Math.round((double) adjustedReward * leaderBonusMultiplier);
        }

        // Call the score event for the killer
        WarScoreEvent pvpScoreEvent = new WarScoreEvent(sharedWar, null, killerFaction, scoreType, adjustedReward);
        pvpScoreEvent.callEvent();

        // Deduct war lives for the victim
        var newVictimLives = Math.max(0, victimWarLives - 1);

        WarLivesUpdateEvent warLivesUpdateEvent = new WarLivesUpdateEvent(sharedWar, victim, sharedWar.getPlayerFaction(victim), newVictimLives);
        warLivesUpdateEvent.callEvent();

        if (!warLivesUpdateEvent.isCancelled()) {

            WarMetaDataManager.instance().setWarLives(victim, sharedWar, warLivesUpdateEvent.getNewLives());

            if (warLivesUpdateEvent.getNewLives() == 0) {
                // This death eliminated them.
                United.messenger().send(victim, "messages.warlives-final", sharedWar.getCleanTitle());
            } else {
                // Lives still remaining.
                United.messenger().send(victim, "messages.warlives-lost", String.valueOf(warLivesUpdateEvent.getNewLives()), sharedWar.getCleanTitle());
            }
        }

    }

    public static LivingEntity findKillingEntity(Player deceased, EntityDamageByEntityEvent damageEvent) {

        Entity damager = damageEvent.getDamager();
        LivingEntity killer = null;

        // Direct kill
        if (damager instanceof LivingEntity) {
            killer = (LivingEntity) damager;
        }

        // Arrow shot by player
        else if (damager instanceof Arrow arrow) {
            if (arrow.getShooter() instanceof LivingEntity) {
                killer = (LivingEntity) arrow.getShooter();
            }
        }

        // Trident
        else if (damager instanceof Trident trident) {
            if (trident.getShooter() instanceof LivingEntity) {
                killer = (LivingEntity) trident.getShooter();
            }
        }

        // TNT
        else if (damager instanceof TNTPrimed tnt) {
            if (tnt.getSource() instanceof LivingEntity) {
                killer = (LivingEntity) tnt.getSource();
            }
        }

        // Thrown potion
        else if (damager instanceof ThrownPotion potion) {
            if (potion.getShooter() instanceof LivingEntity) {
                killer = (LivingEntity) potion.getShooter();
            }
        }

        // Wolf tamed by player
        else if (damager instanceof Wolf wolf) {
            if (wolf.isTamed() && wolf.getOwner() instanceof LivingEntity) {
                killer = (LivingEntity) wolf.getOwner();
            }
        }

        return killer;
    }

    // public static Entity findKillingPlayer(Player deceased,
    // EntityDamageByEntityEvent damageEvent) {
    // Entity damager = damageEvent.getDamager();
    // Player killer = null;

    // // Direct player kill
    // if (damager instanceof Entity) {
    // killer = (Player) damager;
    // }

    // // Arrow shot by player
    // else if (damager instanceof Arrow arrow) {
    // if (arrow.getShooter() instanceof Player) {
    // killer = (Player) arrow.getShooter();
    // }
    // }

    // // Trident thrown by player
    // else if (damager instanceof Trident trident) {
    // if (trident.getShooter() instanceof Player) {
    // killer = (Player) trident.getShooter();
    // }
    // }

    // // TNT placed by player
    // else if (damager instanceof TNTPrimed tnt) {
    // if (tnt.getSource() instanceof Player) {
    // killer = (Player) tnt.getSource();
    // }
    // }

    // // Thrown potion by player
    // else if (damager instanceof ThrownPotion potion) {
    // if (potion.getShooter() instanceof Player) {
    // killer = (Player) potion.getShooter();
    // }
    // }

    // // Wolf tamed by player
    // else if (damager instanceof Wolf wolf) {
    // if (wolf.isTamed() && wolf.getOwner() instanceof Player) {
    // killer = (Player) wolf.getOwner();
    // }
    // }

    // return killer;
    // }

}
