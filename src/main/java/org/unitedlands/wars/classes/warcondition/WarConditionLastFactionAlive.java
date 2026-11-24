package org.unitedlands.wars.classes.warcondition;

import java.util.stream.Collectors;

import org.unitedlands.wars.classes.war.WarFaction;

public class WarConditionLastFactionAlive extends WarCondition {

    public static final String ID = "last_faction_alive";

    public WarConditionLastFactionAlive() {
        this.id = ID;
    }

    @Override
    public boolean evaluate(WarFaction faction, Integer param) {
        var aliveFactions = faction.getWar().getWarFactions().stream().filter(f -> !f.hasLost())
                .collect(Collectors.toList());
        return aliveFactions.size() == 1 && faction.equals(aliveFactions.get(0));
    }

}
