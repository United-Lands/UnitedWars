package org.unitedlands.wars;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import org.unitedlands.utils.United;
import org.unitedlands.classes.database.BaseDbService;
import org.unitedlands.wars.integrations.LuckPermsIntegration;
import org.unitedlands.wars.listeners.PlayerDeathListener;
import org.unitedlands.wars.listeners.ServerEventListener;
import org.unitedlands.wars.listeners.WarEventsListener;
import org.unitedlands.wars.listeners.UnitedLands.UnitedLandsCountryListener;
import org.unitedlands.wars.listeners.UnitedLands.UnitedLandsInfoscreenListener;
import org.unitedlands.wars.listeners.UnitedLands.UnitedLandsMovementListener;
import org.unitedlands.wars.managers.DatabaseManager;
import org.unitedlands.wars.managers.SiegeChunkDisplayManager;
import org.unitedlands.wars.managers.SiegeManager;
import org.unitedlands.wars.managers.WarManager;
import org.unitedlands.wars.managers.WarMetaDataManager;
import org.unitedlands.wars.schedulers.WarScheduler;

import org.unitedlands.libs.ormlite.logger.LoggerFactory;
import org.unitedlands.libs.ormlite.logger.NullLogBackend;

public class UnitedWars extends JavaPlugin {

    private static UnitedWars instance;
    private DatabaseManager databaseManager;
    private LuckPermsIntegration luckPermsIntegration;

    @Override
    public void onEnable() {

        LoggerFactory.setLogBackendFactory(new NullLogBackend.NullLogBackendFactory());

        instance = this;

        saveDefaultConfig();

        createManagers();

        registerListeners();

        loadIntegrations();

        getLogger().info("UnitedWars initialized.");
    }

    @Override
    public void onDisable() {

        // Stop the scheduler first so no new changes are produced
        if (WarScheduler.instance() != null)
            WarScheduler.instance().shutdown();

        if (databaseManager == null)
            return;

        // Flush everything that changed since the last scheduler run
        try {
            if (WarManager.instance() != null)
                WarManager.instance().saveChangedData();
            if (SiegeManager.instance() != null)
                SiegeManager.instance().saveChangedSiegeChunks();
        } catch (Exception ex) {
            United.logger().error("Could not flush war data on shutdown: " + ex.getMessage(), "UnitedWars");
        }

        // The write executor is shared with other UnitedLib plugins and shut down by
        // UnitedLib. Only wait for our own writes before closing the pool they use.
        if (databaseManager.getConnectionSource() != null)
            BaseDbService.awaitPendingWrites(databaseManager.getConnectionSource(), 30);

        databaseManager.close();
    }

    private void createManagers() {
        databaseManager = new DatabaseManager();
        databaseManager.initialize();

        new WarManager(databaseManager);
        new SiegeManager(databaseManager);
        new SiegeChunkDisplayManager();
        new WarMetaDataManager();
        new WarScheduler();
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ServerEventListener(), this);
        getServer().getPluginManager().registerEvents(new WarEventsListener(), this);
        getServer().getPluginManager().registerEvents(new UnitedLandsMovementListener(), this);
        getServer().getPluginManager().registerEvents(new UnitedLandsCountryListener(), this);
        getServer().getPluginManager().registerEvents(new UnitedLandsInfoscreenListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(), this);
    }

    private void loadIntegrations() {
        Plugin mythicMobs = Bukkit.getPluginManager().getPlugin("LuckPerms");
        if (mythicMobs != null && mythicMobs.isEnabled()) {
            United.logger().info("LuckPerms found, enabling perm-based features.", "UnitedWars");
            luckPermsIntegration = new LuckPermsIntegration();
        } else {
            United.logger().info("LuckPerms not found, perm-based features will be disabled", "UnitedWars");
        }
    }

    public static UnitedWars instance() {
        return instance;
    }

    public LuckPermsIntegration getLuckPermsIntegration() {
        return luckPermsIntegration;
    }

}
