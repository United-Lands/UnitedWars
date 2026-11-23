package org.unitedlands.wars.classes.warcondition;

import org.unitedlands.wars.classes.war.WarFaction;

public class WarConditionOwnAllWarZones extends WarCondition {

    public static final String ID = "own_all_war_zones";

    public WarConditionOwnAllWarZones() {
        this.id = ID;
        this.description = "Own all war zones";
    }

    @Override
    public boolean evaluate(WarFaction faction, Integer param) {
        var warZones = faction.getWar().getObjectiveWarZones();
        return warZones.stream().allMatch(z -> faction.equals(z.getOccupier()));
    }

}
