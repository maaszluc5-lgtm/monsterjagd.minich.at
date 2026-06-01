package at.minich.opserver.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class DataManager {

    private final JavaPlugin plugin;
    private final File dataFolder;

    public DataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    /**
     * Loads a YAML file from the plugin's data folder.
     * Creates an empty file if it does not exist.
     */
    public YamlConfiguration loadYaml(String relativePath) {
        File file = new File(dataFolder, relativePath);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create file: " + relativePath, e);
            }
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Saves a YamlConfiguration to the given relative path inside the plugin's data folder.
     */
    public void saveYaml(YamlConfiguration config, String relativePath) {
        File file = new File(dataFolder, relativePath);
        file.getParentFile().mkdirs();
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save file: " + relativePath, e);
        }
    }

    /**
     * Returns the plugin data folder.
     */
    public File getDataFolder() {
        return dataFolder;
    }

    /**
     * Ensures a subdirectory exists within the plugin data folder.
     */
    public File ensureDir(String name) {
        File dir = new File(dataFolder, name);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }
}
