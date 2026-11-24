package org.unitedlands.wars.classes.warcondition;

import java.util.stream.Collectors;

import org.unitedlands.wars.classes.war.WarFaction;

public class WarConditionOwnMinimumWarZones extends WarCondition {

    public static final String ID = "own_minimum_war_zones";

    public WarConditionOwnMinimumWarZones() {
        this.id = ID;
    }

    @Override
    public boolean evaluate(WarFaction faction, Integer param) {
        if (param == null)
            return false;
        var warZones = faction.getWar().getObjectiveWarZones();
        var controlledZones = warZones.stream().filter(z -> faction.equals(z.getOccupier()))
                .collect(Collectors.counting());
        return controlledZones >= param;
    }

}
