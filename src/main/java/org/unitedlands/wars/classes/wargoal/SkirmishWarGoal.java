package org.unitedlands.wars.classes.wargoal;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.unitedlands.unitedlands.classes.GeopolObject;
import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.utils.United;
import org.unitedlands.wars.UnitedWars;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.classes.war.WarFaction;
import org.unitedlands.wars.classes.war.WarFactionRole;
import org.unitedlands.wars.classes.warzone.WarZone;

public class SkirmishWarGoal extends WarGoal {

    public SkirmishWarGoal() {
        super("skirmish");
        this.description = UnitedWars.instance().getConfig().getString("war-goal-settings.skirmish.description");
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

        Set<WarFaction> factions = new HashSet<>();

        var declaringSettlement = UnitedLandsDataManager.instance().getSettlement(declarer);
        var targeSettlement = UnitedLandsDataManager.instance().getSettlement(target);

        if (declaringSettlement == null || targeSettlement == null) {
            United.logger().error("Unable to create skirmish factions, missing declarer or target object.", "UnitedLands");
            return null;
        }

        var declarerScoreCap = UnitedWars.instance().getConfig().getInt("war-goal-settings.skirmish.scorecaps.attacker", 10000);

        WarFaction declarerFaction = new WarFaction(war, WarFactionRole.ATTACKER, declaringSettlement.getName(), -65536);
        declarerFaction.setFactionLeaderId(declarer);
        declarerFaction.addSettlement(declaringSettlement);

        declarerFaction.addWinCondition("reach_score", declarerScoreCap);
        declarerFaction.addWinCondition("last_faction_alive");
        declarerFaction.addWinCondition("own_all_war_zones");
        declarerFaction.addLoseCondition("own_no_war_zones");

        factions.add(declarerFaction);

        var targetScoreCap = UnitedWars.instance().getConfig().getInt("war-goal-settings.skirmish.scorecaps.defender", 10000);

        WarFaction targetFaction = new WarFaction(war, WarFactionRole.DEFENDER, targeSettlement.getName(), -16776961);
        targetFaction.setFactionLeaderId(target);
        targetFaction.addSettlement(targeSettlement);

        targetFaction.addWinCondition("reach_score", targetScoreCap);
        targetFaction.addWinCondition("last_faction_alive");
        targetFaction.addWinCondition("own_all_war_zones");
        targetFaction.addLoseCondition("own_no_war_zones");

        factions.add(targetFaction);

        return factions;
    }

    @Override
    public Set<WarZone> createWarZones(UUID declarer, UUID target, War war) {

        var attackerFactions = war.getWarFactionMap().get(WarFactionRole.ATTACKER);
        var defenderFactions = war.getWarFactionMap().get(WarFactionRole.DEFENDER);
        if (attackerFactions.size() != 1 || defenderFactions.size() != 1) {
            United.logger().error("Unexpected number of factions in skirmish war goal.", "UnitedWars");
            return new HashSet<>();
        }

        Set<WarZone> zones = new HashSet<>();
        zones.add(createSettlementZone(war, attackerFactions.getFirst(), declarer));
        zones.add(createSettlementZone(war, defenderFactions.getFirst(), target));

        return zones;
    }

    @Override
    public void joinWar(UUID joiner, War war) {
        // Skirmish wars don't allow anyone to join
    }

    @Override
    public void resolve(War war) {
        United.logger().debug("Resolve war goal here...");
    }

}
