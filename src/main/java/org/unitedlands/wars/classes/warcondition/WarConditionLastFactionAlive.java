package org.unitedlands.wars.classes.warcondition;

import java.util.stream.Collectors;

import org.unitedlands.wars.classes.war.WarFaction;

public class WarConditionLastFactionAlive extends WarCondition {

    public WarConditionLastFactionAlive() {
        this.id = "last_faction_alive";
        this.description = "Be the last faction alive";
    }

    @Override
    public boolean evaluate(WarFaction faction, Integer param) {
        var aliveFactions = faction.getWar().getWarFactions().stream().filter(f -> !f.hasLost())
                .collect(Collectors.toList());
        return aliveFactions.size() == 1 && faction.equals(aliveFactions.get(0));
    }

}
