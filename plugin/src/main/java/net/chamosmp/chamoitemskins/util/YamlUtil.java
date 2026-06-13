// --- plugin/src/main/java/net/chamosmp/chamoitemskins/util/YamlUtil.java ---
package net.chamosmp.chamoitemskins.util;

import net.chamosmp.chamoitemskins.api.model.Skin;
import net.chamosmp.chamoitemskins.scheduler.SchedulerUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Utility for reading and writing skins.yml.
 */
public final class YamlUtil {

    private YamlUtil() {}

    public static @NotNull List<Skin> loadSkins(@NotNull YamlConfiguration config) {
        List<Skin> skins = new ArrayList<>();
        ConfigurationSection section = config.getConfigurationSection("skins");
        if (section == null) return skins;

        for (String key : section.getKeys(false)) {
            ConfigurationSection skinSection = section.getConfigurationSection(key);
            if (skinSection == null) continue;

            String id = skinSection.getString("id");
            if (id == null) continue;

            String name = skinSection.getString("name", "Unknown");
            String modelId = skinSection.getString("model-id", "");
            Material itemType = Material.matchMaterial(skinSection.getString("item-type", "AIR"));
            boolean enabled = skinSection.getBoolean("enabled", true);
            Material noteMaterial = Optional.ofNullable(skinSection.getString("note-material"))
                    .map(Material::matchMaterial)
                    .orElse(null);

            ConfigurationSection displaySection = skinSection.getConfigurationSection("display-item");
            Skin.DisplayItem displayItem = null;
            if (displaySection != null) {
                Material displayMat = Material.matchMaterial(displaySection.getString("id", "AIR"));
                String displayName = displaySection.getString("name", name);
                List<String> displayLore = displaySection.getStringList("lore");
                boolean glow = displaySection.getBoolean("glow", false);
                displayItem = new Skin.DisplayItem(displayMat, displayName, displayLore, glow);
            }

            skins.add(new Skin(id, name, modelId, itemType, enabled, noteMaterial, displayItem));
        }
        return skins;
    }

    public static void saveSkin(@NotNull Plugin plugin, @NotNull Skin skin) {
        SchedulerUtil.runAsync(plugin, () -> {
            File file = new File(plugin.getDataFolder(), "skins.yml");
            File tempFile = new File(plugin.getDataFolder(), "skins.yml.tmp");
            
            synchronized (YamlUtil.class) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                
                String path = "skins." + skin.id();
                config.set(path + ".id", skin.id());
                config.set(path + ".name", skin.name());
                config.set(path + ".model-id", skin.modelId());
                config.set(path + ".item-type", skin.itemType().name());
                config.set(path + ".enabled", skin.enabled());
                config.set(path + ".note-material", skin.noteMaterial() != null ? skin.noteMaterial().name() : null);

                if (skin.displayItem() != null) {
                    config.set(path + ".display-item.id", skin.displayItem().material().name());
                    config.set(path + ".display-item.name", skin.displayItem().name());
                    config.set(path + ".display-item.lore", skin.displayItem().lore());
                    config.set(path + ".display-item.glow", skin.displayItem().glow());
                }

                try {
                    config.save(tempFile);
                    if (file.exists() && !file.delete()) {
                        throw new IOException("Could not delete existing skins.yml");
                    }
                    if (!tempFile.renameTo(file)) {
                        throw new IOException("Could not rename skins.yml.tmp to skins.yml");
                    }
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not save skin " + skin.id() + " to skins.yml: " + e.getMessage());
                }
            }
        });
    }

    public static void deleteSkin(@NotNull Plugin plugin, @NotNull String id) {
        SchedulerUtil.runAsync(plugin, () -> {
            File file = new File(plugin.getDataFolder(), "skins.yml");
            File tempFile = new File(plugin.getDataFolder(), "skins.yml.tmp");
            
            synchronized (YamlUtil.class) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                config.set("skins." + id, null);

                try {
                    config.save(tempFile);
                    if (file.exists() && !file.delete()) {
                        throw new IOException("Could not delete existing skins.yml");
                    }
                    if (!tempFile.renameTo(file)) {
                        throw new IOException("Could not rename skins.yml.tmp to skins.yml");
                    }
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not delete skin " + id + " from skins.yml: " + e.getMessage());
                }
            }
        });
    }
}
