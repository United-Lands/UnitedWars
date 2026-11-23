package org.unitedlands.wars.classes.db;

import java.util.UUID;

import org.unitedlands.libs.ormlite.dao.Dao;
import org.unitedlands.wars.classes.war.War;

public class WarService extends WarsDbService<War> {

    public WarService(Dao<War, UUID> dao) {
        super(dao);
    }

}
