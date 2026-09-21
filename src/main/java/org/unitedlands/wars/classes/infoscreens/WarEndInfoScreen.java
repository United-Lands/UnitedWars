package org.unitedlands.wars.classes.infoscreens;

import java.util.Map;

import org.bukkit.plugin.Plugin;
import org.unitedlands.interfaces.IMessageProvider;
import org.unitedlands.unitedlands.classes.infoscreen.InfoScreen;
import org.unitedlands.wars.classes.war.War;
import org.unitedlands.wars.managers.WarManager;

public class WarEndInfoScreen extends InfoScreen {

    public WarEndInfoScreen(Plugin plugin, IMessageProvider messageProvider, War war) {
        super();

        var msgs = messageProvider.getSection("info-screens.war-end");

        var header = buildHeader(war.getCleanTitle());
        addComponent("header", header);
        addComponent("info", msgs.get("info"), Map.of());

        if (war.getWinningFaction() != null) {

            var faction = war.getWinningFaction();
            var replacements = faction.getMessageReplacements();
            addComponent("winner", msgs.get("winner"), replacements);

            var condition = WarManager.instance().getWarCondition(war.getWinningCondition());
            var conditionReplaments = condition.getMessageReplacements();
            addComponent("win-condition", msgs.get("win-condition"), conditionReplaments);

        } else {
            addComponent("draw", msgs.get("draw"), Map.of());
        }

    }

}
