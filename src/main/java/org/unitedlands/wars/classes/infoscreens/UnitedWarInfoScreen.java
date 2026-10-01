package org.unitedlands.wars.classes.infoscreens;

import org.unitedlands.unitedlands.classes.infoscreen.InfoScreen;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.classes.wargoal.ClaimDisputeWarGoal;
import org.unitedlands.wars.classes.wargoal.ConquestWarGoal;
import org.unitedlands.wars.classes.wargoal.RevoltWarGoal;
import org.unitedlands.wars.classes.wargoal.SkirmishWarGoal;
import org.unitedlands.wars.classes.wargoal.SubjugationWarGoal;

public class UnitedWarInfoScreen extends InfoScreen {

    protected String getTargetName(War war)
    {
        var warGoal = war.getWarGoal();
        if (
            warGoal instanceof ClaimDisputeWarGoal || 
            warGoal instanceof ConquestWarGoal ||
            warGoal instanceof RevoltWarGoal
        ) {
            return UnitedLandsDataManager.instance().getRegion(war.getWarTargetId()).getCleanName();
        } else if (
            warGoal instanceof SkirmishWarGoal ||
            warGoal instanceof SubjugationWarGoal
        ) {
            return UnitedLandsDataManager.instance().getSettlement(war.getWarTargetId()).getCleanName();
        }

        return "(Unknown Target)";
    }

}
