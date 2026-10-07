package org.unitedlands.wars.classes.warcondition;

import org.unitedlands.wars.classes.war.WarFaction;

public class WarConditionOwnNoWarZones extends WarCondition {    

    public WarConditionOwnNoWarZones() {
        this.id = "own_no_war_zones";
        this.description = "Own no war zones";
    }

    @Override
    public boolean evaluate(WarFaction faction, Integer param) {
        var warZones = faction.getWar().getWarZones();
        return warZones.stream().noneMatch(z -> faction.equals(z.getOccupier()));
    }

}
