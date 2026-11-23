package org.unitedlands.wars.events;

import org.unitedlands.unitedlands.classes.GeopolObject;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.classes.war.WarFaction;

public class WarInviteEvent extends WarEvent {

    private final GeopolObject receiver;
    private final WarFaction inviter;

    private String cancelMessage;

    public WarInviteEvent(War war, WarFaction inviter, GeopolObject receiver) {
        super(war);
        this.receiver = receiver;
        this.inviter = inviter;
    }

    public String getCancelMessage() {
        return cancelMessage;
    }

    public void setCancelMessage(String cancelMessage) {
        this.cancelMessage = cancelMessage;
    }
    
    public GeopolObject getReceiver() {
        return receiver;
    }

    public WarFaction getInviter() {
        return inviter;
    }


}
