package org.unitedlands.wars.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.unitedlands.restoration.UnitedRestoration;
import org.unitedlands.utils.United;
import org.unitedlands.wars.UnitedWars;
import org.unitedlands.wars.classes.config.UnitedWarsConfig;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;

public class WarcampUtils {

    public static void place(Chunk chunk) {

        var world = chunk.getWorld();
        var chunkX = chunk.getX();
        var chunkZ = chunk.getZ();

        var future = UnitedRestoration.instance().getChunkSnapshotManager().saveSnapshot(chunk);

        future.thenAccept(saved -> {

            if (!Boolean.TRUE.equals(saved)) {
                United.logger().error("Could not back up chunk " + chunkX + "," + chunkZ + ", warcamp was not placed.");
                return;
            }

            var clipboard = loadRandomTemplate();
            if (clipboard == null)
                return;

            var plugin = UnitedWars.instance();
            if (!plugin.isEnabled())
                return;
            Bukkit.getScheduler().runTask(plugin, () -> paste(world, chunkX, chunkZ, clipboard));

        }).exceptionally(ex -> {
            United.logger().error("Error while creating warcamp chunk: " + ex.getMessage());
            return null;
        });

    }

    private static Clipboard loadRandomTemplate() {

        int numTemplates = UnitedWarsConfig.get().siegeSettings().warcampTemplates();
        if (numTemplates <= 0)
            return null;

        var randomTemplate = ThreadLocalRandom.current().nextInt(1, numTemplates + 1);

        String path = UnitedWars.instance().getDataFolder().getPath() + File.separator + "schematics" + File.separator + "Camp" + randomTemplate + ".schem";
        File file = new File(path);
        if (!file.exists()) {
            United.logger().error("Schematic " + path + " could not be loaded.");
            return null;
        }

        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) {
            United.logger().error("Unknown schematic format: " + path);
            return null;
        }

        try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            return reader.read();
        } catch (IOException ioException) {
            United.logger().error("Could not read schematic file.");
            return null;
        }
    }

    private static void paste(org.bukkit.World world, int chunkX, int chunkZ, Clipboard clipboard) {

        var pasteLocX = (chunkX * 16) + 8;
        var pasteLocZ = (chunkZ * 16) + 8;
        var pasteLocY = world.getHighestBlockYAt(pasteLocX, pasteLocZ) + 1;

        World worldEditWorld = BukkitAdapter.adapt(world);

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(worldEditWorld)) {
            ClipboardHolder holder = new ClipboardHolder(clipboard);
            AffineTransform transform = new AffineTransform().rotateY(ThreadLocalRandom.current().nextInt(4) * 90d);
            holder.setTransform(transform);
            Operation operation = holder.createPaste(editSession).to(BlockVector3.at(pasteLocX, pasteLocY, pasteLocZ)).ignoreAirBlocks(false).build();
            Operations.complete(operation);
        } catch (WorldEditException exception) {
            United.logger().error(exception.getMessage());
        }
    }

    public static void removeWarcamp(Chunk chunk) {

        var future = UnitedRestoration.instance().getChunkSnapshotManager().restoreSnapshot(chunk.getWorld(), chunk.getX(), chunk.getZ());

        future.thenAccept(restored -> {
            if (Boolean.TRUE.equals(restored))
                United.logger().info("Warcamp successfully removed.");
            else
                United.logger().error("Could not restore warcamp chunk " + chunk.getX() + "," + chunk.getZ() + ".");
        }).exceptionally(ex -> {
            United.logger().error("Error while restoring warcamp chunk: " + ex.getMessage());
            return null;
        });

    }

}
