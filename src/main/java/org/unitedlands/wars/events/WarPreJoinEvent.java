package org.unitedlands.wars.events;

import org.unitedlands.unitedlands.classes.GeopolObject;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.classes.war.WarFaction;

public class WarPreJoinEvent extends WarEvent {

    private final GeopolObject joiner;
    private final WarFaction faction;

    private String cancelMessage;

    public WarPreJoinEvent(War war, GeopolObject joiner, WarFaction faction) {
        super(war);
        this.joiner = joiner;
        this.faction = faction;
    }

    public String getCancelMessage() {
        return cancelMessage;
    }

    public void setCancelMessage(String cancelMessage) {
        this.cancelMessage = cancelMessage;
    }
    
    public GeopolObject getJoiner() {
        return joiner;
    }

    public WarFaction getFaction() {
        return faction;
    }


}
