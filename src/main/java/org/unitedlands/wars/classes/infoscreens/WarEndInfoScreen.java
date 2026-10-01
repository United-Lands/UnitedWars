package org.unitedlands.wars.classes.infoscreens;

import org.unitedlands.utils.United;
import org.unitedlands.wars.UnitedWars;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.managers.WarManager;

public class WarEndInfoScreen extends UnitedWarInfoScreen {

    public WarEndInfoScreen(War war) {
        super();

        var header = buildHeader(war.getCleanTitle());
        addComponent("header", header);

        var warGoal = war.getWarGoal();
        var warGoalId = warGoal.getId();

        addComponent("wargoal", "war-goals." + warGoalId + ".display-name", UnitedWars.instance());

        addComponent("info", "info-screens.war-end.info", UnitedWars.instance());

        if (war.getWinningFaction() != null) {

            var faction = war.getWinningFaction();

            addComponent("winner", "info-screens.war-end.winner", UnitedWars.instance(), faction.getColoredCleanName(), faction.getRole().toString());

            if (war.getWinningCondition() != null) {
                var condition = WarManager.instance().getWarCondition(war.getWinningCondition());
                addComponent("win-condition", "info-screens.war-end.win-condition", UnitedWars.instance(), condition.getDescription());
            } else {
                addComponent("win-condition", "info-screens.war-end.win-by-highest-score", UnitedWars.instance());
            }

            var reward = United.messenger().get( "war-goals." + warGoalId + ".end-messages." + faction.getRole().toString(), UnitedWars.instance());
            addComponent("reward", "info-screens.war-end.reward", UnitedWars.instance(), reward);

        } else {
            addComponent("draw", "info-screens.war-end.draw", UnitedWars.instance());

            var reward = United.messenger().get( "war-goals." + warGoalId + ".end-messages.DRAW", UnitedWars.instance());
            addComponent("reward", "info-screens.war-end.reward", UnitedWars.instance(), reward);
        }

    }

}
