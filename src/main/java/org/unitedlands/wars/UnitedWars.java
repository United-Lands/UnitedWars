package org.unitedlands.wars;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.unitedlands.classes.ConfigFile;
import org.unitedlands.unitedlands.libs.ormlite.logger.LoggerFactory;
import org.unitedlands.unitedlands.libs.ormlite.logger.NullLogBackend;
import org.unitedlands.utils.United;
import org.unitedlands.wars.integrations.LuckPermsIntegration;
import org.unitedlands.wars.listeners.PlayerDeathListener;
import org.unitedlands.wars.listeners.ServerEventListener;
import org.unitedlands.wars.listeners.UnitedLandsListener;
import org.unitedlands.wars.listeners.WarEventsListener;
import org.unitedlands.wars.managers.DatabaseManager;
import org.unitedlands.wars.managers.SiegeChunkDisplayManager;
import org.unitedlands.wars.managers.SiegeManager;
import org.unitedlands.wars.managers.WarEventManager;
import org.unitedlands.wars.managers.WarManager;
import org.unitedlands.wars.managers.WarMetaDataManager;
import org.unitedlands.wars.schedulers.WarScheduler;
import org.unitedlands.wars.utils.MessageProvider;

public class UnitedWars extends JavaPlugin {

    private static UnitedWars instance;

    private ConfigFile messageConfig;
    private MessageProvider messageProvider;

    private DatabaseManager databaseManager;
    private LuckPermsIntegration luckPermsIntegration;

    @Override
    public void onEnable() {

        LoggerFactory.setLogBackendFactory(new NullLogBackend.NullLogBackendFactory());

        instance = this;

        saveDefaultConfig();
        messageConfig = new ConfigFile(this, "messages/en_GB.yml");
        messageProvider = new MessageProvider(messageConfig.get());

        createManagers();

        registerListeners();

        loadIntegrations();

        getLogger().info("UnitedWars initialized.");
    }

    @Override
    public void onDisable() {
        databaseManager.close();
        WarScheduler.instance().shutdown();
    }

    private void createManagers() {
        databaseManager = new DatabaseManager();
        databaseManager.initialize();

        new WarManager(databaseManager);
        new SiegeManager(databaseManager);
        new SiegeChunkDisplayManager();
        new WarEventManager();
        new WarMetaDataManager();
        new WarScheduler();
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ServerEventListener(), this);
        getServer().getPluginManager().registerEvents(new WarEventsListener(messageProvider), this);
        getServer().getPluginManager().registerEvents(new UnitedLandsListener(), this);
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

    public MessageProvider getMessageProvider() {
        return messageProvider;
    }

    public LuckPermsIntegration getLuckPermsIntegration() {
        return luckPermsIntegration;
    }

}
