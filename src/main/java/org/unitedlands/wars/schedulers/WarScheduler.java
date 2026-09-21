package org.unitedlands.wars.schedulers;

import org.bukkit.scheduler.BukkitTask;
import org.unitedlands.utils.United;
import org.unitedlands.wars.UnitedWars;
import org.unitedlands.wars.managers.SiegeManager;
import org.unitedlands.wars.managers.WarEventManager;
import org.unitedlands.wars.managers.WarManager;

public class WarScheduler {

    private static WarScheduler instance;

    public static WarScheduler instance() {
        return instance;
    }

    private BukkitTask warSchedulerTask;

    public WarScheduler() {
        instance = this;
    }

    public void initialize() {

        Long checkInterval = UnitedWars.instance().getConfig().getInt("warscheduler.check-interval", 15) * 20L;
        warSchedulerTask = UnitedWars.instance().getServer().getScheduler().runTaskTimer(UnitedWars.instance(),
                this::run, checkInterval,
                checkInterval);

        United.logger().info("War scheduler set to running with interval: " + checkInterval + " ticks.");
        United.logger().info("War scheduler initialized");
    }

    public void run() {
        WarManager.instance().handleWars();
        WarManager.instance().updateTownImmunities();
        WarEventManager.instance().handleEvents();
        SiegeManager.instance().updateSieges();
        SiegeManager.instance().handleSiegeChunks();
    }

    public void shutdown() {
        if (warSchedulerTask != null) {
            warSchedulerTask.cancel();
            United.logger().info("War scheduler stopped.");
        } else {
            United.logger().info("War scheduler was not running.");
        }
    }
}
