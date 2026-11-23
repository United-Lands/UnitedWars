package org.unitedlands.wars.classes.db;

import org.unitedlands.libs.ormlite.field.DatabaseField;
import org.unitedlands.libs.ormlite.table.DatabaseTable;

/**
 * Tracks the UnitedWars schema version. Uses its own table name so it can't
 * collide with UnitedLands' version table if both plugins share a database.
 */
@DatabaseTable(tableName = "unitedwars_schema_version")
public class SchemaVersion {

    @DatabaseField(id = true)
    private int id = 1;

    @DatabaseField
    private int version = 1;

    public SchemaVersion() {
    }

    public SchemaVersion(int version) {
        this.version = version;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
