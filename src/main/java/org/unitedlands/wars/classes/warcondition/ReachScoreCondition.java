package org.unitedlands.wars.classes.warcondition;

import org.unitedlands.wars.classes.war.WarFaction;

public class ReachScoreCondition extends WarCondition {

    public ReachScoreCondition() {
        this.id = "reach_score";
        this.description = "Reach score";
    }

    @Override
    public boolean evaluate(WarFaction faction, Integer param) {
        return faction.getScore() >= param;
    }

}
