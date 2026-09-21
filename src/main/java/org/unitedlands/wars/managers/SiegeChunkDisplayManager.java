package org.unitedlands.wars.managers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.entity.Player;
import org.unitedlands.unitedlands.classes.Coordinates;
import org.unitedlands.wars.classes.war.SiegeChunk;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class SiegeChunkDisplayManager {

    private static SiegeChunkDisplayManager instance;

    private Map<Coordinates, BossBar> healthBars = new HashMap<>();
    private Map<Coordinates, Set<Player>> healthBarViewers = new HashMap<>();

    public static SiegeChunkDisplayManager instance() {
        return instance;
    }

    public SiegeChunkDisplayManager() {
        instance = this;
    }

    public void updateHealthBar(SiegeChunk siegeChunk) {
        var healthBar = healthBars.get(siegeChunk.getCoordinates());
        if (healthBar == null)
            return;

        updateHealthBar(healthBar, siegeChunk);
    }

    public void updateHealthBar(BossBar healthBar, SiegeChunk siegeChunk) {

        var progress = Math.max(0f, (float) siegeChunk.getCurrentHealth() / (float) siegeChunk.getMaxHealth());

        String label = "";
        Color color = Color.GREEN;

        if (siegeChunk.isOccupied() || siegeChunk.getWarZone().isOccupied()) {
            label += "<red>(Occupied)</red>";
            color = Color.RED;
        } else if (!siegeChunk.getWarZone().isSiegeEnabled()) {
            label += "<gray>(Siege disabled)</gray>";
            color = Color.WHITE;
        } else {
            var owner = siegeChunk.getOwner();
            if (owner != null) {
                label += "Owner: " + owner.getColoredName();
            } else {
                label += "<gray>Unowned</gray>";
            }

            label += " (" + siegeChunk.getCurrentHealth() + "/" + siegeChunk.getMaxHealth() + ")";

            if (progress <= 0.66f)
                color = Color.YELLOW;
            else if (progress <= 0.33f)
                color = Color.RED;
        }

        if (progress <= 0.66f)
            color = Color.YELLOW;
        else if (progress <= 0.33f)
            color = Color.RED;

        healthBar.name(MiniMessage.miniMessage().deserialize(label));
        healthBar.progress(progress);
        healthBar.color(color);

        healthBars.put(siegeChunk.getCoordinates(), healthBar);
    }

    public void addPlayerToHealthBar(SiegeChunk siegeChunk, Player player) {

        var healthBar = healthBars.get(siegeChunk.getCoordinates());
        if (healthBar == null) {
            healthBar = createHealthBar(siegeChunk);
        }
        healthBar.addViewer(player);

        var viewerList = healthBarViewers.computeIfAbsent(siegeChunk.getCoordinates(), v -> new HashSet<>());
        viewerList.add(player);
    }

    public void removePlayerFromHealthBar(SiegeChunk siegeChunk, Player player) {

        var healthBar = createHealthBar(siegeChunk);
        healthBar.removeViewer(player);

        var viewerList = healthBarViewers.computeIfAbsent(siegeChunk.getCoordinates(), v -> new HashSet<>());
        viewerList.remove(player);

        if (viewerList.size() == 0) {
            healthBars.remove(siegeChunk.getCoordinates());
            healthBarViewers.remove(siegeChunk.getCoordinates());
        }
    }

    private BossBar createHealthBar(SiegeChunk siegeChunk) {
        var healthBar = healthBars.get(siegeChunk.getCoordinates());
        if (healthBar == null) {
            healthBar = BossBar.bossBar(
                    Component.text(""),
                    1f,
                    BossBar.Color.GREEN,
                    BossBar.Overlay.NOTCHED_10);
            healthBars.put(siegeChunk.getCoordinates(), healthBar);
        }
        updateHealthBar(healthBar, siegeChunk);
        return healthBar;
    }

    public void removeHealthBar(SiegeChunk siegeChunk) {
        var viewers = healthBarViewers.get(siegeChunk.getCoordinates());
        var healthBar = healthBars.get(siegeChunk.getCoordinates());
        if (healthBar != null && healthBarViewers != null) {
            for (var viewer : viewers) {
                healthBar.removeViewer(viewer);
            }
        }
    }

}
