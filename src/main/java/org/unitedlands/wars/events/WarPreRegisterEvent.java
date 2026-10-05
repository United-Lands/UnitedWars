package org.unitedlands.wars.events;

import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.events.interfaces.Validatable;

public class WarPreRegisterEvent extends WarEvent implements Validatable {

    private boolean isValid = true;
    private String validationMessage;

    public WarPreRegisterEvent(War war) {
        super(war);
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean isValid) {
        this.isValid = isValid;
    }

    public String getValidationMessage() {
        return validationMessage;
    }

    public void setValidationMessage(String cancellationMessage) {
        this.validationMessage = cancellationMessage;
    }

}
