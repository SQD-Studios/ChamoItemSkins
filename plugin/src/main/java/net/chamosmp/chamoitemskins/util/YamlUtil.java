package net.chamosmp.chamoitemskins.util;

import net.chamosmp.chamoitemskins.api.objects.Category;
import net.chamosmp.chamoitemskins.api.objects.Rarity;
import net.chamosmp.chamoitemskins.api.objects.Skin;
import net.chamosmp.chamoitemskins.api.objects.SkinBundle;
import net.chamosmp.chamoitemskins.manager.CategoryManager;
import net.chamosmp.chamoitemskins.manager.RarityManager;
import net.chamosmp.chamoitemskins.scheduler.SchedulerUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility for reading and writing skins.yml.
 */
public final class YamlUtil {


    private YamlUtil() {
    }

    public static @NotNull List<Skin> loadSkins(@NotNull YamlConfiguration config, @NotNull RarityManager rarityManager, @NotNull CategoryManager categoryManager) {
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
            String rarityId = skinSection.getString("rarity", rarityManager.getDefaultRarity().id());
            Rarity rarity = rarityManager.resolve(rarityId);
            List<String> categoryNames = skinSection.getStringList("categories");
            List<Category> categories = categoryNames.stream()
                    .map(categoryManager::getCategoryByName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            boolean enabled = skinSection.getBoolean("enabled", true);
            Material noteMaterial = Optional.ofNullable(skinSection.getString("note-material"))
                    .map(Material::matchMaterial)
                    .orElse(null);
            List<String> animations = skinSection.getStringList("animations");

            ConfigurationSection displaySection = skinSection.getConfigurationSection("display-item");
            Skin.DisplayItem displayItem = null;
            if (displaySection != null) {
                Material displayMat = Material.matchMaterial(displaySection.getString("id", "AIR"));
                String displayName = displaySection.getString("name", name);
                List<String> displayLore = displaySection.getStringList("lore");
                boolean glow = displaySection.getBoolean("glow", false);
                displayItem = new Skin.DisplayItem(displayMat, displayName, displayLore, glow);
            }

            skins.add(new Skin(id, name, modelId, rarity, categories, enabled, noteMaterial, displayItem, animations));
        }
        return skins;
    }

    public static @NotNull List<SkinBundle> loadBundles(@NotNull YamlConfiguration config) {
        List<SkinBundle> bundles = new ArrayList<>();
        ConfigurationSection section = config.getConfigurationSection("bundles");
        if (section == null) return bundles;

        for (String key : section.getKeys(false)) {
            ConfigurationSection bundleSection = section.getConfigurationSection(key);
            if (bundleSection == null) continue;

            String id = bundleSection.getString("id");
            if (id == null) continue;

            String name = bundleSection.getString("name", "Unknown Bundle");
            List<String> skinIds = bundleSection.getStringList("skins");

            bundles.add(new SkinBundle(id, name, skinIds));
        }
        return bundles;
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
                config.set(path + ".rarity", skin.rarity().id());
                config.set(path + ".categories", skin.categories().stream().map(Category::name).collect(Collectors.toList()));
                config.set(path + ".enabled", skin.enabled());
                config.set(path + ".note-material", skin.noteMaterial() != null ? skin.noteMaterial().name() : null);
                config.set(path + ".animations", skin.animations());

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
                    LoggerUtil.log(LoggerUtil.LogType.SEVERE, "Could not save skins to skins.yml: " + e.getMessage());
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
                    LoggerUtil.log(LoggerUtil.LogType.SEVERE, "Could not delete skins from skins.yml: " + e.getMessage());
                }
            }
        });
    }

    public static void saveBundle(@NotNull Plugin plugin, @NotNull SkinBundle bundle) {
        SchedulerUtil.runAsync(plugin, () -> {
            File file = new File(plugin.getDataFolder(), "skins.yml");
            File tempFile = new File(plugin.getDataFolder(), "skins.yml.tmp");

            synchronized (YamlUtil.class) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

                String path = "bundles." + bundle.id();
                config.set(path + ".id", bundle.id());
                config.set(path + ".name", bundle.name());
                config.set(path + ".skins", bundle.skinIds());

                try {
                    config.save(tempFile);
                    if (file.exists() && !file.delete()) throw new IOException("Could not delete existing skins.yml");
                    if (!tempFile.renameTo(file)) throw new IOException("Could not rename skins.yml.tmp to skins.yml");
                } catch (IOException e) {
                    LoggerUtil.log(LoggerUtil.LogType.SEVERE, "Could not save bundle to skins.yml: " + e.getMessage());
                }
            }
        });
    }

    public static void deleteBundle(@NotNull Plugin plugin, @NotNull String id) {
        SchedulerUtil.runAsync(plugin, () -> {
            File file = new File(plugin.getDataFolder(), "skins.yml");
            File tempFile = new File(plugin.getDataFolder(), "skins.yml.tmp");

            synchronized (YamlUtil.class) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                config.set("bundles." + id, null);

                try {
                    config.save(tempFile);
                    if (file.exists() && !file.delete()) throw new IOException("Could not delete existing skins.yml");
                    if (!tempFile.renameTo(file)) throw new IOException("Could not rename skins.yml.tmp to skins.yml");
                } catch (IOException e) {
                    LoggerUtil.log(LoggerUtil.LogType.SEVERE, "Could not delete bundle" + id + "from skins.yml: " + e.getMessage());
                }
            }
        });
    }

    public static void saveCategory(@NotNull Plugin plugin, @NotNull Category category) {
        SchedulerUtil.runAsync(plugin, () -> {
            File file = new File(plugin.getDataFolder(), "config.yml");
            File tempFile = new File(plugin.getDataFolder(), "config.yml.tmp");

            synchronized (YamlUtil.class) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

                String path = "categories." + category.id();
                config.set(path + ".name", category.name());
                config.set(path + ".items", category.allowedItems());

                try {
                    config.save(tempFile);
                    if (file.exists() && !file.delete()) throw new IOException("Could not delete existing config.yml");
                    if (!tempFile.renameTo(file)) throw new IOException("Could not rename config.yml.tmp to config.yml");
                } catch (IOException e) {
                    LoggerUtil.log(LoggerUtil.LogType.SEVERE, "Could not save category " + category.id() + " to config.yml: " + e.getMessage());
                }
            }
        });
    }

    public static void deleteCategory(@NotNull Plugin plugin, @NotNull String id) {
        SchedulerUtil.runAsync(plugin, () -> {
            File file = new File(plugin.getDataFolder(), "config.yml");
            File tempFile = new File(plugin.getDataFolder(), "config.yml.tmp");

            synchronized (YamlUtil.class) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

                config.set("categories." + id, null);

                try {
                    config.save(tempFile);
                    if (file.exists() && !file.delete()) throw new IOException("Could not delete existing config.yml");
                    if (!tempFile.renameTo(file)) throw new IOException("Could not rename config.yml.tmp to config.yml");
                } catch (IOException e) {
                    LoggerUtil.log(LoggerUtil.LogType.SEVERE, "Could not save category " + id + " to config.yml: " + e.getMessage());
                }
            }
        });
    }
}
