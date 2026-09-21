package org.unitedlands.wars.classes.wargoal;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.unitedlands.unitedlands.classes.GeopolObject;
import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.classes.war.WarFaction;
import org.unitedlands.wars.classes.warzone.WarZone;

public class PlunderWarGoal extends WarGoal {

    public PlunderWarGoal() {
        super("plunder");
    }

    @Override
    public ValidationResult validate(GeopolObject declarer, GeopolObject target) {

        if ((declarer instanceof Settlement settlement) && (target instanceof Settlement targetSettlement)) {
            // TODO: peacefulness
            // TODO: immunity

            if (settlement.equals(targetSettlement)) {
                return new ValidationResult(false, "Attacker and target are the same.");
            }
        } else {
            return new ValidationResult(false, "The provided GeopolObject types are not suitable for this war goal.");
        }
        return new ValidationResult(true, null);
    }

    @Override
    public Set<WarFaction> createFactions(UUID declarer, UUID target, War war) {
        return null;
    }

    @Override
    public Set<WarZone> createWarZones(UUID declarer, UUID target, War war) {
        Set<WarZone> zones = new HashSet<>();
        return zones;
    }

    @Override
    public void joinWar(UUID joiner, War war) {
        // Plunder wars don't allow anyone to join
    }

    @Override
    public void resolve(War war) {

    }

}
