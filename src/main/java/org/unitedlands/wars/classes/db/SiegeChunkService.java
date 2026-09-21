package org.unitedlands.wars.classes.db;

import java.util.UUID;

import org.unitedlands.unitedlands.libs.ormlite.dao.Dao;
import org.unitedlands.wars.classes.war.SiegeChunk;

public class SiegeChunkService extends BaseDbService<SiegeChunk> {

    public SiegeChunkService(Dao<SiegeChunk, UUID> dao) {
        super(dao);
    }

}
