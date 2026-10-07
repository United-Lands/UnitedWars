package org.unitedlands.wars.classes.infoscreens;

import org.unitedlands.unitedlands.classes.infoscreen.InfoScreen;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.classes.wargoal.WarGoalClaimDispute;
import org.unitedlands.wars.classes.wargoal.WarGoalConquest;
import org.unitedlands.wars.classes.wargoal.WarGoalRevolt;
import org.unitedlands.wars.classes.wargoal.WarGoalSkirmish;
import org.unitedlands.wars.classes.wargoal.WarGoalSubjugation;

public class UnitedWarInfoScreen extends InfoScreen {

    protected String getTargetName(War war)
    {
        var warGoal = war.getWarGoal();
        if (
            warGoal instanceof WarGoalClaimDispute || 
            warGoal instanceof WarGoalConquest ||
            warGoal instanceof WarGoalRevolt
        ) {
            return UnitedLandsDataManager.instance().getRegion(war.getWarTargetId()).getCleanName();
        } else if (
            warGoal instanceof WarGoalSkirmish ||
            warGoal instanceof WarGoalSubjugation
        ) {
            return UnitedLandsDataManager.instance().getSettlement(war.getWarTargetId()).getCleanName();
        }

        return "(Unknown Target)";
    }

}
