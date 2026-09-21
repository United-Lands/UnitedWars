package org.unitedlands.wars.events;

import org.bukkit.entity.Player;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.classes.war.WarFaction;
import org.unitedlands.wars.classes.war.WarScoreType;

public class WarScoreEvent extends WarEvent {

    private final Player player;
    private final WarFaction faction;
    private final WarScoreType type;
    private final Integer rawScore;
    private Integer finalScore;

    public WarScoreEvent(War war, Player player, WarFaction faction, WarScoreType type, Integer rawScore) {
        super(war);
        this.player = player;
        this.faction = faction;
        this.type = type;
        this.rawScore = rawScore;
        this.finalScore = rawScore;
    }

    public Player getPlayer() {
        return player;
    }

    public WarFaction getFaction() {
        return faction;
    }

    public WarScoreType getType() {
        return type;
    }

    public Integer getRawScore() {
        return rawScore;
    }

    public Integer getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(Integer finalScore) {
        this.finalScore = finalScore;
    }

}
