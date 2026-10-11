package org.unitedlands.wars.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

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

        var future = UnitedRestoration.instance().getChunkSnapshotManager().saveSnapshot(chunk);

        CompletableFuture.allOf(future).thenRun(() -> {

            // Place a random WorldEdit schematic

            int numTemplates = UnitedWarsConfig.get().siegeSettings().warcampTemplates();
            if (numTemplates == 0)
                return;

            var randomTemplate = (int) (Math.round(Math.random() * (numTemplates - 1)) + 1);

            String path = UnitedWars.instance().getDataFolder().getPath() + File.separator + "schematics" + File.separator + "Camp" + randomTemplate + ".schem";
            File file = new File(path);
            if (!file.exists()) {
                United.logger().error("Schematic " + path + " could not be loaded.");
                return;
            }

            Clipboard clipboard = null;

            ClipboardFormat format = ClipboardFormats.findByFile(file);
            try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
                clipboard = reader.read();
            } catch (IOException ioException) {
                United.logger().error("Could not read schematic file.");
                return;
            }

            var world = chunk.getWorld();
            var pasteLocX = (chunk.getX() * 16) + 8;
            var pasteLocZ = (chunk.getZ() * 16) + 8;
            var pasteLocY = world.getHighestBlockYAt(pasteLocX, pasteLocZ) + 1;

            World worldEditWorld = BukkitAdapter.adapt(world);

            try (EditSession editSession = WorldEdit.getInstance().newEditSession(worldEditWorld)) {
                ClipboardHolder holder = new ClipboardHolder(clipboard);
                AffineTransform transform = new AffineTransform().rotateY(Math.round(Math.random() * 3) * 90d);
                holder.setTransform(transform);
                Operation operation = holder.createPaste(editSession).to(BlockVector3.at(pasteLocX, pasteLocY, pasteLocZ)).ignoreAirBlocks(false).build();
                Operations.complete(operation);
            } catch (WorldEditException exception) {
                United.logger().error(exception.getMessage());
                return;
            }

        }).exceptionally(ex -> {
            United.logger().error("Error while creating warcamp chunk: " + ex.getMessage());
            return null;
        });

    }

    public static void removeWarcamp(Chunk chunk) {

        var future = UnitedRestoration.instance().getChunkSnapshotManager().restoreSnapshot(chunk.getWorld(), chunk.getX(), chunk.getZ());
        
        CompletableFuture.allOf(future).thenRun(() -> {
            United.logger().info("Warcamp successfully removed.");
        }).exceptionally(ex -> {
            United.logger().error("Error while restoring warcamp chunk: " + ex.getMessage());
            return null;
        });

    }

}
