package org.unitedlands.wars.classes.warcondition;

import org.unitedlands.wars.classes.war.WarFaction;

public class WarConditionOwnNoWarZones extends WarCondition {    

    public static final String ID = "own_no_war_zones";

    public WarConditionOwnNoWarZones() {
        this.id = ID;
    }

    @Override
    public boolean evaluate(WarFaction faction, Integer param) {
        var warZones = faction.getWar().getObjectiveWarZones();
        return warZones.stream().noneMatch(z -> faction.equals(z.getOccupier()));
    }

}
