package org.unitedlands.wars.classes.warcondition;

import java.util.stream.Collectors;

import org.unitedlands.wars.classes.war.WarFaction;

public class OwnMinimumWarZonesCondition extends WarCondition {

    public OwnMinimumWarZonesCondition() {
        this.id = "own_minimum_war_zones";
        this.description = "They own at least the required number of war zones";
    }

    @Override
    public boolean evaluate(WarFaction faction, Integer param) {
        var warZones = faction.getWar().getWarZones();
        var controlledZones = warZones.stream().filter(z -> faction.equals(z.getOccupier()))
                .collect(Collectors.counting());
        return controlledZones >= param;
    }

}
