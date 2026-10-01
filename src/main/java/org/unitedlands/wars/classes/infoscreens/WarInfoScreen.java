package org.unitedlands.wars.classes.infoscreens;

import java.util.ArrayList;
import java.util.List;

import org.unitedlands.utils.United;
import org.unitedlands.wars.UnitedWars;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.managers.WarManager;

public class WarInfoScreen extends UnitedWarInfoScreen {

    public WarInfoScreen(War war) {
        super();

        // Reuse the Unitedlands header style for consistency
        var header = buildHeader(war.getCleanTitle());
        addComponent("header", header);

        var warGoal = war.getWarGoal();
        var warGoalId = warGoal.getId();

        addComponent("wargoal", "war-goals." + warGoalId + ".display-name", UnitedWars.instance());

        addComponent("factions-header", "info-screens.war-info.faction-header", UnitedWars.instance());

        var factionCounter = 1;
        for (var faction : war.getWarFactions()) {

            var scoreCap = faction.getWinConditions().get("reach_score");
            if (scoreCap != null) {
                addComponent("faction" + factionCounter, "info-screens.war-info.faction-entry-maxscore", UnitedWars.instance(), faction.getColoredCleanName(),
                        faction.getRole().toString(), String.valueOf(faction.getScore()), String.valueOf(scoreCap));
            } else {
                addComponent("faction" + factionCounter, "info-screens.war-info.faction-entry", UnitedWars.instance(), faction.getColoredCleanName(),
                        faction.getRole().toString(), String.valueOf(faction.getScore()));
            }

            List<String> winConditions = new ArrayList<>();
            for (var entry : faction.getWinConditions().entrySet()) {
                var condition = WarManager.instance().getWarCondition(entry.getKey());
                var conditionValueStr = entry.getValue() != null ? " (" + String.valueOf(entry.getValue()) + ")" : "";
                winConditions.add(condition.getDescription() + conditionValueStr);
            }

            addComponent("win-faction-" + factionCounter, "info-screens.war-info.faction-win", UnitedWars.instance(), String.join(", ", winConditions));

            List<String> loseConditions = new ArrayList<>();
            for (var entry : faction.getLoseConditions().entrySet()) {
                var condition = WarManager.instance().getWarCondition(entry.getKey());
                var conditionValueStr = entry.getValue() != null ? "(" + String.valueOf(entry.getValue()) + ")" : "";
                loseConditions.add(condition.getDescription() + conditionValueStr);
            }

            addComponent("lose-faction-" + factionCounter, "info-screens.war-info.faction-lose", UnitedWars.instance(), String.join(", ", loseConditions));

            addComponent("divider" + factionCounter, "info-screens.war-info.faction-divider", UnitedWars.instance());
            factionCounter++;
        }

        List<String> zoneStrings = new ArrayList<>();
        for (var zone : war.getWarZones()) {
            var zoneString = zone.getName();
            switch (zone.getType()) {
            case "settlement":
                zoneString += " (Settlement)";
                break;
            case "region":
                zoneString += " (Region)";
            default:
                break;
            }
            if (zone.isOccupied()) {
                if (zone.getOccupier() != null) {
                    zoneString = "<u><" + zone.getOccupier().getColorHex() + ">" + zoneString + "</" + zone.getOccupier().getColorHex() + "></u>";
                }
            } else {
                if (zone.getFaction() != null) {
                    zoneString = "<" + zone.getFaction().getColorHex() + ">" + zoneString + "</" + zone.getFaction().getColorHex() + ">";
                }
            }
            zoneStrings.add(zoneString);
        }

        addComponent("zones-header", "info-screens.war-info.zones-header", UnitedWars.instance());
        addComponent("zones", "info-screens.war-info.zones", UnitedWars.instance(), String.join(", ", zoneStrings));

        if (war.isActive()) {
            addComponent("timer", "info-screens.war-info.timer-info", UnitedWars.instance(),
                    United.formatter().formatDuration(war.getScheduledEndTime() - System.currentTimeMillis()));
        }
        else {
            addComponent("timer", "info-screens.war-declared.timer-info", UnitedWars.instance(), 
                United.formatter().formatDuration(war.getScheduledBeginTime() - System.currentTimeMillis()));

        }
    }

}
