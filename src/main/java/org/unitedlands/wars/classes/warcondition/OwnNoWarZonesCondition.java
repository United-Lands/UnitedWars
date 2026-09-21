package org.unitedlands.wars.classes.warcondition;

import org.unitedlands.wars.classes.war.WarFaction;

public class OwnNoWarZonesCondition extends WarCondition {    

    public OwnNoWarZonesCondition() {
        this.id = "own_no_war_zones";
        this.description = "They own no war zones";
    }

    @Override
    public boolean evaluate(WarFaction faction, Integer param) {
        var warZones = faction.getWar().getWarZones();
        return warZones.stream().noneMatch(z -> faction.equals(z.getOccupier()));
    }

}
