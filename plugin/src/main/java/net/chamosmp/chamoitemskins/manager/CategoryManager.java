// --- plugin/src/main/java/net/chamosmp/chamoitemskins/manager/RarityManager.java ---
package net.chamosmp.chamoitemskins.manager;

import net.chamosmp.chamoitemskins.api.model.Category;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Loads and serves user-defined rarity tiers from config.yml.
 */
public final class CategoryManager {
    private final Plugin plugin;
    private final Map<String, Category> categories = new LinkedHashMap<>();

    private final String DISABLE_MESSAGE = "No category entries defined in config.yml - categories disabled! This will break the plugin. Warning";

    /**
     * Constructs a new CategoryManager.
     * @param plugin The plugin instance.
     */
    public CategoryManager(@NotNull Plugin plugin) {
        this.plugin = plugin;

        load();
    }

    /**
     * Loads categories from the given configuration.
     */
    public void load() {
        FileConfiguration config = plugin.getConfig();
        categories.clear();
        ConfigurationSection section = config.getConfigurationSection("categories");
        if (section == null) {
            plugin.getLogger().warning(DISABLE_MESSAGE);
            return;
        }
        for (String key : section.getKeys(false)) {
            if (key.equalsIgnoreCase("enabled")) {
                continue;
            }
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) {
                continue;
            }
            String name = entry.getString("name", key);
            List<String> allowedItems = entry.getStringList("items");
            categories.put(key.toLowerCase(), new Category(name, allowedItems));
        }

        if (categories.isEmpty()) {
            plugin.getLogger().severe(DISABLE_MESSAGE);
        }
    }

    /**
     * Gets all loaded categories.
     * @return A list of categories.
     */
    public @NotNull List<Category> getCategories() {
        return categories.values().stream()
                .toList();
    }

    /**
     * Gets an unmodifiable map of all categories.
     * @return The rarity map.
     */
    public @NotNull Map<String, Category> getCategoryMap() {
        return Collections.unmodifiableMap(categories);
    }

    public Category getCategoryByName(String name) {
        return categories.values().stream()
                .filter(cat -> cat.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
}
