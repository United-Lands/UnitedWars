package org.unitedlands.wars.classes.warcondition;

import org.unitedlands.utils.United;
import org.unitedlands.wars.UnitedWars;
import org.unitedlands.wars.classes.war.WarFaction;

public abstract class WarCondition {

    protected String id;

    public abstract boolean evaluate(WarFaction faction, Integer param);

    public String getId() {
        return id;
    }

    public String getDescription() {
        return United.messenger().get("war-conditions." + id, UnitedWars.instance());
    }

}
