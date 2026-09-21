package org.unitedlands.wars.classes.infoscreens;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.plugin.Plugin;
import org.unitedlands.interfaces.IMessageProvider;
import org.unitedlands.unitedlands.classes.infoscreen.InfoScreen;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.managers.WarManager;

public class WarDeclaredInfoScreen extends InfoScreen {

    public WarDeclaredInfoScreen(Plugin plugin, IMessageProvider messageProvider, War war) {
        super();

        var msgs = messageProvider.getSection("info-screens.war-declared");

        // Reuse the Unitedlands header style for consistency
        var header = buildHeader(war.getCleanTitle());
        addComponent("header", header);

        var warReplacements = war.getMessageReplacements();

        addComponent("description", msgs.get("decription"), warReplacements);
        addComponent("wargoal", msgs.get("war-goal"), warReplacements);

        addComponent("factionHeader", msgs.get("faction-header"), Map.of());

        var factionCounter = 1;
        for (var faction : war.getWarFactions()) {

            var factionReplacements = faction.getMessageReplacements();
            addComponent("faction" + factionCounter, msgs.get("faction-entry"), factionReplacements);

            addComponent("win-conditions", msgs.get("faction-win-conditions-header"), Map.of());
            
            var winConditionCounter = 1;
            for (var entry : faction.getWinConditions().entrySet()) {

                var condition = WarManager.instance().getWarCondition(entry.getKey());
                var conditionReplacements = new HashMap<>(condition.getMessageReplacements());
                conditionReplacements.put(
                        "condition-value",
                        entry.getValue() != null ? "(" + String.valueOf(entry.getValue()) + ")" : "");

                addComponent("faction" + factionCounter + "-win-condition-entry" + winConditionCounter,
                        msgs.get("faction-win-conditions-entry"),
                        conditionReplacements);

                winConditionCounter++;
            }

            addComponent("lose-conditions", msgs.get("faction-lose-conditions-header"), Map.of());

            var loseConditionCounter = 1;
            for (var entry : faction.getLoseConditions().entrySet()) {

                var condition = WarManager.instance().getWarCondition(entry.getKey());
                var conditionReplacements = new HashMap<>(condition.getMessageReplacements());
                conditionReplacements.put(
                        "condition-value",
                        entry.getValue() != null ? "(" + String.valueOf(entry.getValue()) + ")" : "");

                addComponent("faction" + factionCounter + "-lose-condition-entry" + loseConditionCounter,
                        msgs.get("faction-lose-conditions-entry"),
                        conditionReplacements);

                loseConditionCounter++;
            }

            factionCounter++;
        }

        addComponent("timer-info", msgs.get("timer-info"), warReplacements);

    }

}
