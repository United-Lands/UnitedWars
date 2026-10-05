package org.unitedlands.wars.events.interfaces;

public interface Validatable {
    void setValid(boolean valid);
    boolean isValid();
    void setValidationMessage(String message);
    String getValidationMessage();
}
