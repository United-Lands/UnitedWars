package org.unitedlands.wars.classes.infoscreens;

import org.unitedlands.unitedlands.classes.infoscreen.InfoScreen;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.managers.WarManager;

public class WarInfoScreen extends InfoScreen {

    public WarInfoScreen(War war) {
        super();

        // Reuse the Unitedlands header style for consistency
        var header = buildHeader(war.getCleanTitle());
        addComponent("header", header);

        addComponent("description", "info-screens.war-info.decription", war.getDescription());
        addComponent("wargoal", "info-screens.war-info.war-goal", war.getWarGoal().getId(), war.getWarGoal().getDescription());

        addComponent("factionHeader", "info-screens.war-info.faction-header");

        var factionCounter = 1;
        for (var faction : war.getWarFactions()) {

            addComponent("faction" + factionCounter, "info-screens.war-info.faction-entry", 
                faction.getColoredName(),
                faction.getRole().toString(),
                String.valueOf(faction.getScore())
            );

            addComponent("win-conditions", "info-screens.war-info.faction-win-conditions-header");
            
            var winConditionCounter = 1;
            for (var entry : faction.getWinConditions().entrySet()) {

                var condition = WarManager.instance().getWarCondition(entry.getKey());
                var conditionValueStr = entry.getValue() != null ? "(" + String.valueOf(entry.getValue()) + ")" : "";

                addComponent("faction" + factionCounter + "-win-condition-entry" + winConditionCounter,
                        "info-screens.war-info.faction-win-conditions-entry",
                        condition.getDescription(),
                        conditionValueStr
                );
                winConditionCounter++;
            }

            addComponent("lose-conditions", "info-screens.war-info.faction-lose-conditions-header");

            var loseConditionCounter = 1;
            for (var entry : faction.getLoseConditions().entrySet()) {

                var condition = WarManager.instance().getWarCondition(entry.getKey());
                var conditionValueStr = entry.getValue() != null ? "(" + String.valueOf(entry.getValue()) + ")" : "";

                addComponent("faction" + factionCounter + "-lose-condition-entry" + loseConditionCounter,
                        "info-screens.war-info.faction-lose-conditions-entry",
                        condition.getDescription(),
                        conditionValueStr
                );
                loseConditionCounter++;
            }

            factionCounter++;
        }

        addComponent("timer-info", "info-screens.war-info.timer-info");

    }

}
