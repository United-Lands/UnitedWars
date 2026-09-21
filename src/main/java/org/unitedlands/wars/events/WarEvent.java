package org.unitedlands.wars.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.unitedlands.wars.classes.war.War;

public class WarEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled;

    private final War war;

    public WarEvent(War war) {
        this.war = war;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public War getWar() {
        return war;
    }

}
