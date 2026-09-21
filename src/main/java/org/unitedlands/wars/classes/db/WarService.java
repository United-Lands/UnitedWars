package org.unitedlands.wars.classes.db;

import java.util.UUID;

import org.unitedlands.unitedlands.libs.ormlite.dao.Dao;
import org.unitedlands.wars.classes.war.War;

public class WarService extends BaseDbService<War> {

    public WarService(Dao<War, UUID> dao) {
        super(dao);
    }

}
