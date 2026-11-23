package org.unitedlands.wars.classes.infoscreens;

import java.util.ArrayList;
import java.util.List;

import org.unitedlands.unitedlands.classes.Country;
import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.wars.UnitedWars;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.managers.WarMetaDataManager;

public class WarParticipantsScreen extends UnitedWarInfoScreen {

    public WarParticipantsScreen(War war) {
        super();

        // Reuse the Unitedlands header style for consistency
        var header = buildHeader(war.getCleanTitle());
        addComponent("header", header);

        var warGoal = war.getWarGoal();
        var warGoalId = warGoal.getId();

        addComponent("wargoal", "war-goals." + warGoalId + ".display-name", UnitedWars.instance());

        //addComponent("factions-header", "info-screens.war-info.faction-header", UnitedWars.instance());

        var factionCounter = 1;
        for (var faction : war.getWarFactions()) {

            addComponent("faction" + factionCounter, "info-screens.war-participants.faction-header", UnitedWars.instance(), faction.getColoredCleanName(),
                        faction.getRole().toString());

            if (faction.getCountries() != null && faction.getCountries().size() > 0) {
                addComponent(
                    "factioncountries" + factionCounter, 
                    "info-screens.war-participants.faction-countries", 
                    UnitedWars.instance(),
                    String.join(", ", faction.getCountries().stream().map(Country::getDisplayName).toList()));
            }

            if (faction.getSettlements() != null && faction.getCountries().size() > 0) {
                addComponent(
                    "factionsettlements" + factionCounter, 
                    "info-screens.war-participants.faction-settlements", 
                    UnitedWars.instance(),
                    String.join(", ", faction.getSettlements().stream().map(Settlement::getCleanName).toList()));
            }

            var players = war.getFactionPlayers(faction);
            if (players != null && players.size() > 0) {

                List<String> playerInfos = new ArrayList<>();
                for (var player : players) {
                    var lives = WarMetaDataManager.instance().getWarLives(player, war);
                    if (lives <= 0) {
                        playerInfos.add("<dark_gray><st>" + player.getName() + "</st></dark_gray>");
                    } else {
                        playerInfos.add(player.getName() + "(" + lives + ")");
                    }
                }

                addComponent(
                    "factionplayers" + factionCounter, 
                    "info-screens.war-participants.faction-players", 
                    UnitedWars.instance(),
                    String.join(", ", playerInfos));
            }


            addComponent("divider" + factionCounter, "info-screens.war-info.faction-divider", UnitedWars.instance());
            factionCounter++;
        }

    }

}
