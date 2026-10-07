package org.unitedlands.wars.classes.warcondition;

import org.unitedlands.wars.classes.war.WarFaction;

public class WarConditionOwnAllWarZones extends WarCondition {

    public WarConditionOwnAllWarZones() {
        this.id = "own_all_war_zones";
        this.description = "Own all war zones";
    }

    @Override
    public boolean evaluate(WarFaction faction, Integer param) {
        var warZones = faction.getWar().getWarZones();
        return warZones.stream().allMatch(z -> faction.equals(z.getOccupier()));
    }

}
