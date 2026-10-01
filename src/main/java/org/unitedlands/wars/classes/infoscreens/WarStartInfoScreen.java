package org.unitedlands.wars.classes.infoscreens;

import org.unitedlands.utils.United;
import org.unitedlands.wars.UnitedWars;
import org.unitedlands.wars.classes.war.War;

public class WarStartInfoScreen extends UnitedWarInfoScreen {

    public WarStartInfoScreen(War war) {
        super();

        var header = buildHeader(war.getCleanTitle());
        addComponent("header", header);

        var warGoal = war.getWarGoal();
        var warGoalId = warGoal.getId();

        addComponent("wargoal", "war-goals." + warGoalId + ".display-name", UnitedWars.instance());

        addComponent("info", "info-screens.war-start.info", UnitedWars.instance(),
            United.formatter().formatDuration(war.getScheduledEndTime() - System.currentTimeMillis()),
            war.getTitle()
        );

    }

}
