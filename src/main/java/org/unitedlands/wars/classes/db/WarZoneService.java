package org.unitedlands.wars.classes.db;

import java.util.UUID;

import org.unitedlands.unitedlands.libs.ormlite.dao.Dao;
import org.unitedlands.wars.classes.warzone.WarZone;

public class WarZoneService extends BaseDbService<WarZone> {

    public WarZoneService(Dao<WarZone, UUID> dao) {
        super(dao);
    }

}
