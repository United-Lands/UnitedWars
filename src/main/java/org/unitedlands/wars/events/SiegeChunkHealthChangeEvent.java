package org.unitedlands.wars.events;

import org.unitedlands.wars.classes.war.SiegeChunk;
import org.unitedlands.wars.classes.war.War;

public class SiegeChunkHealthChangeEvent extends WarEvent {

    private final SiegeChunk siegeChunk;
    private int healthChange;

    public SiegeChunkHealthChangeEvent(War war, SiegeChunk siegeChunk, int healthChange) {
        super(war);
        this.siegeChunk = siegeChunk;
        this.healthChange = healthChange;
    }

    public SiegeChunk getSiegeChunk() {
        return siegeChunk;
    }

    public int getHealthChange() {
        return healthChange;
    }

    public void setHealthChange(int healthChange) {
        this.healthChange = healthChange;
    }

}
