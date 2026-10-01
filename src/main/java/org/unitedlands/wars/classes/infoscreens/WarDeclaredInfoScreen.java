package org.unitedlands.wars.classes.infoscreens;

import org.unitedlands.utils.United;
import org.unitedlands.wars.UnitedWars;
import org.unitedlands.wars.classes.war.War;

public class WarDeclaredInfoScreen extends UnitedWarInfoScreen {

    public WarDeclaredInfoScreen(War war) {
        super();
        // Reuse the Unitedlands header style for consistency
        var header = buildHeader(war.getCleanTitle());
        addComponent("header", header);

        var warGoal= war.getWarGoal();
        var warGoalId = warGoal.getId();

        addComponent("wargoal", "war-goals." + warGoalId + ".display-name", UnitedWars.instance());
        addComponent("wargoal-description", "war-goals." + warGoalId + ".description", UnitedWars.instance(), getTargetName(war));

        addComponent("custom-decription", "info-screens.war-declared.custom-decription", UnitedWars.instance(), war.getDescription());

        // addComponent("description", "war-goals." + warGoalId + ".description", UnitedWars.instance());
        // addComponent("win", "war-goals." + warGoalId + ".win", UnitedWars.instance());
        
        // addComponent("factionHeader", "info-screens.war-declared.faction-header", UnitedWars.instance());


        var factionCounter = 1;
        for (var faction : war.getWarFactions()) {

            var scoreCap = faction.getWinConditions().get("reach_score");
            if (scoreCap != null)
            {
                addComponent("faction" + factionCounter, 
                    "info-screens.war-declared.faction-entry-maxscore", 
                    UnitedWars.instance(), 
                    faction.getColoredCleanName(),
                    faction.getRole().toString(),
                    String.valueOf(faction.getScore()),
                    String.valueOf(scoreCap)
                );
            } else {
                addComponent("faction" + factionCounter, 
                    "info-screens.war-declared.faction-entry", 
                    UnitedWars.instance(), 
                    faction.getColoredCleanName(),
                    faction.getRole().toString(),
                    String.valueOf(faction.getScore())
                );
            }

            factionCounter++;
        }

        addComponent("timer", "info-screens.war-declared.timer-info", UnitedWars.instance(), United.formatter().formatDuration(war.getScheduledBeginTime() - System.currentTimeMillis()));

    }

}
