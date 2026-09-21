package org.unitedlands.wars.events;

import org.bukkit.entity.Player;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.classes.war.WarFaction;

public class WarLivesUpdateEvent extends WarEvent {

    private final Player player;
    private final WarFaction faction;

    private final int oldLives;
    private int newLives;

    public WarLivesUpdateEvent(War war, Player player, WarFaction faction, int oldLives) {
        super(war);
        this.player = player;
        this.faction = faction;
        this.oldLives = oldLives;
        this.newLives = oldLives;
    }

    public Player getPlayer() {
        return player;
    }

    public WarFaction getFaction() {
        return faction;
    }

    public int getOldLives() {
        return oldLives;
    }

    public int getNewLives() {
        return newLives;
    }

    public void setNewLives(int newLives) {
        this.newLives = newLives;
    }

}
