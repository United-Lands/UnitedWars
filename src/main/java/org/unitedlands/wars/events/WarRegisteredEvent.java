package org.unitedlands.wars.events;

import org.unitedlands.wars.classes.war.War;

public class WarRegisteredEvent extends WarEvent {

    public WarRegisteredEvent(War war) {
        super(war);
    }

}
