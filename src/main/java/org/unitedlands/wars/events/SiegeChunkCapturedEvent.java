package org.unitedlands.wars.events;

import org.unitedlands.wars.classes.war.SiegeChunk;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.classes.war.WarFaction;

public class SiegeChunkCapturedEvent extends WarEvent {

    private final SiegeChunk siegeChunk;
    private final WarFaction previousOwner;
    private final WarFaction newOwner;
    
    public SiegeChunkCapturedEvent(War war, SiegeChunk siegeChunk, WarFaction previousOwner, WarFaction newOwner) {
        super(war);
        this.siegeChunk = siegeChunk;
        this.previousOwner = previousOwner;
        this.newOwner = newOwner;
    }

    public SiegeChunk getSiegeChunk() {
        return siegeChunk;
    }

    public WarFaction getPreviousOwner() {
        return previousOwner;
    }

    public WarFaction getNewOwner() {
        return newOwner;
    }


}
