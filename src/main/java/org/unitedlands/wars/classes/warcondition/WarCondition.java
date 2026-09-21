package org.unitedlands.wars.classes.warcondition;

import java.util.Map;

import org.unitedlands.wars.classes.war.WarFaction;

public abstract class WarCondition {

    protected String id;
    protected String description;

    public abstract boolean evaluate(WarFaction faction, Integer param);

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, String> getMessageReplacements() {
        return Map.of(
                "condition-id", getId(),
                "condition-description", getDescription());
    }

}
