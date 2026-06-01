package at.minich.opserver.farmworld;

import org.bukkit.*;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Manages the farm world: creation, access, and reset.
 */
public class FarmWorldManager {

    private final String worldName;
    private World farmWorld;

    public FarmWorldManager(String worldName) {
        this.worldName = worldName;
    }

    /**
     * Loads or creates the farm world. Call on plugin enable.
     */
    public void initialize() {
        farmWorld = Bukkit.getWorld(worldName);
        if (farmWorld == null) {
            farmWorld = createWorld();
        }
    }

    private World createWorld() {
        WorldCreator creator = new WorldCreator(worldName);
        creator.type(WorldType.NORMAL);
        creator.generateStructures(true);
        return Bukkit.createWorld(creator);
    }

    public World getFarmWorld() {
        return farmWorld;
    }

    /**
     * Teleports a player to the farm world spawn.
     */
    public void teleportToFarmWorld(Player player) {
        if (farmWorld == null) {
            initialize();
        }
        Location spawnLoc = farmWorld.getSpawnLocation();
        // Ensure a safe Y level
        spawnLoc.setY(spawnLoc.getWorld().getHighestBlockYAt(spawnLoc) + 1);
        player.teleport(spawnLoc);
        player.sendMessage("§a§lFarm World §r§7— teleported successfully!");
    }

    /**
     * Resets the farm world: unloads, deletes folder, recreates.
     */
    public boolean reset() {
        // Kick players back to main world first
        if (farmWorld != null) {
            World mainWorld = Bukkit.getWorlds().get(0);
            for (Player p : farmWorld.getPlayers()) {
                p.teleport(mainWorld.getSpawnLocation());
                p.sendMessage("§c§lFarm World §r§7is being reset. You were teleported back.");
            }
            Bukkit.unloadWorld(farmWorld, false);
            farmWorld = null;
        }

        // Delete world folder
        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (worldFolder.exists()) {
            deleteDirectory(worldFolder);
        }

        // Recreate
        farmWorld = createWorld();
        return farmWorld != null;
    }

    private void deleteDirectory(File dir) {
        try {
            Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path directory, IOException exc) throws IOException {
                    Files.delete(directory);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            Bukkit.getLogger().severe("[OpServer] Failed to delete farm world directory: " + e.getMessage());
        }
    }
}
