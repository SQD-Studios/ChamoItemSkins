// --- plugin/src/main/java/net/chamosmp/chamoitemskins/util/ConfigUtil.java ---
package net.chamosmp.chamoitemskins.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Utility for loading and adapting configuration files.
 */
public final class ConfigUtil {

    private ConfigUtil() {}

    /**
     * Loads a configuration file from the plugin folder, or creates it from defaults.
     * Merges missing keys from defaults into existing files.
     *
     * @param plugin   The plugin instance.
     * @param fileName The name of the file (e.g., "config.yml").
     * @return The loaded YamlConfiguration.
     */
    public static @NotNull YamlConfiguration loadOrAdapt(@NotNull Plugin plugin, @NotNull String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        var resourceStream = plugin.getResource(fileName);
        if (resourceStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(resourceStream, StandardCharsets.UTF_8)
            );
            
            boolean changed = false;
            for (String key : defaultConfig.getKeys(true)) {
                if (!config.contains(key)) {
                    // Skip any key that belongs to the categories or rarities sections
                    if (!key.startsWith("categories.") && !key.startsWith("rarities.")) {
                        config.set(key, defaultConfig.get(key));
                        changed = true;
                    }
                }
            }

            if (changed) {
                try {
                    config.save(file);
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not save adapted config " + fileName + ": " + e.getMessage());
                }
            }
        }
        
        return config;
    }

    public static @NotNull YamlConfiguration loadDataFile(@NotNull Plugin plugin, @NotNull String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }
}
