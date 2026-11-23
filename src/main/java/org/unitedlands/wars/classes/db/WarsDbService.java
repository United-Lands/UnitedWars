package org.unitedlands.wars.classes.db;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.unitedlands.classes.database.BaseDbService;
import org.unitedlands.classes.database.Identifiable;
import org.unitedlands.libs.ormlite.dao.Dao;
import org.unitedlands.utils.United;
import org.unitedlands.wars.UnitedWars;

/**
 * UnitedWars layer over the shared UnitedLib service.
 */
public abstract class WarsDbService<T extends Identifiable> extends BaseDbService<T> {

    protected WarsDbService(Dao<T, UUID> dao) {
        super(dao);
    }

    /**
     * Runs {@code onFailure} on the main thread if the write failed or affected no
     * rows. Used to re-flag entities as dirty so the next save cycle retries them.
     */
    public static void onWriteFailed(CompletableFuture<Boolean> future, String description, Runnable onFailure) {
        future.whenComplete((success, ex) -> {
            if (ex == null && Boolean.TRUE.equals(success))
                return;
            United.logger().warning("[DB] Write of " + description + " did not succeed, will retry.", "UnitedWars");
            var plugin = UnitedWars.instance();
            if (plugin != null && plugin.isEnabled())
                Bukkit.getScheduler().runTask(plugin, onFailure);
        });
    }
}
