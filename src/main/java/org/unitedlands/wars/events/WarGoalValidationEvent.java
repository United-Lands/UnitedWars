package org.unitedlands.wars.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.unitedlands.unitedlands.classes.GeopolObject;
import org.unitedlands.wars.classes.wargoal.WarGoal;
import org.unitedlands.wars.events.interfaces.Validatable;

public class WarGoalValidationEvent extends Event implements Validatable {

    private static final HandlerList handlers = new HandlerList();
    public static HandlerList getHandlerList() {
        return handlers;
    }

    private boolean isValid = true;
    private String validationMessage;

    private final WarGoal warGoal;
    private final GeopolObject attacker;
    private final GeopolObject defender;

    public WarGoalValidationEvent(WarGoal warGoal, GeopolObject attacker, GeopolObject defender) {
        this.warGoal = warGoal;
        this.attacker = attacker;
        this.defender = defender;
    }

    public GeopolObject getAttacker() {
        return attacker;
    }

    public GeopolObject getDefender() {
        return defender;
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

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public WarGoal getWarGoal() {
        return warGoal;
    }

}
