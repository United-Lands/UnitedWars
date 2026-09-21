package org.unitedlands.wars.classes.db;

import java.util.UUID;

import org.unitedlands.unitedlands.libs.ormlite.dao.Dao;
import org.unitedlands.wars.classes.war.WarFaction;

public class WarFactionService extends BaseDbService<WarFaction> {

    public WarFactionService(Dao<WarFaction, UUID> dao) {
        super(dao);
    }

}
