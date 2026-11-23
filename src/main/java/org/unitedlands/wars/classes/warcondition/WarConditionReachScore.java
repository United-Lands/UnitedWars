package org.unitedlands.wars.classes.warcondition;

import org.unitedlands.wars.classes.war.WarFaction;

public class WarConditionReachScore extends WarCondition {

    public static final String ID = "reach_score";

    public WarConditionReachScore() {
        this.id = ID;
        this.description = "Reach score";
    }

    @Override
    public boolean evaluate(WarFaction faction, Integer param) {
        if (param == null)
            return false;
        return faction.getScore() >= param;
    }

}
