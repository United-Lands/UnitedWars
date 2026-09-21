package org.unitedlands.wars.classes.db;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import org.unitedlands.unitedlands.classes.db.Identifiable;
import org.unitedlands.unitedlands.libs.ormlite.dao.Dao;

public abstract class BaseDbService<T extends Identifiable> {

    protected final Dao<T, UUID> dao;

    public BaseDbService(Dao<T, UUID> dao) {
        this.dao = dao;
    }

    public CompletableFuture<Optional<T>> getAsync(UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return Optional.ofNullable(dao.queryForId(id));
            } catch (SQLException e) {
                e.printStackTrace();
                return Optional.empty();
            }
        });
    }

    public CompletableFuture<List<T>> getAllAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return dao.queryForAll();
            } catch (SQLException e) {
                e.printStackTrace();
                return Collections.emptyList();
            }
        });
    }

    public CompletableFuture<Boolean> createAsync(T entity) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return dao.create(entity) > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> createAllAsync(Set<T> entities) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return dao.create(entities) > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> updateAsync(T entity) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return dao.update(entity) > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> deleteAsync(T entity) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return dao.delete(entity) > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> deleteAllAsync(Set<T> entities) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return dao.delete(entities) > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }
}
