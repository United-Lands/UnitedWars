package org.unitedlands.wars.classes.warcondition;

import java.util.stream.Collectors;

import org.unitedlands.wars.classes.war.WarFaction;

public class WarConditionOwnMinimumWarZones extends WarCondition {

    public WarConditionOwnMinimumWarZones() {
        this.id = "own_minimum_war_zones";
        this.description = "Own minimum war zones";
    }

    @Override
    public boolean evaluate(WarFaction faction, Integer param) {
        var warZones = faction.getWar().getWarZones();
        var controlledZones = warZones.stream().filter(z -> faction.equals(z.getOccupier()))
                .collect(Collectors.counting());
        return controlledZones >= param;
    }

}
