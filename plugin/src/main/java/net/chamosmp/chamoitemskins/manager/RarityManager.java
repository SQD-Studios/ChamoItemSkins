// --- plugin/src/main/java/net/chamosmp/chamoitemskins/manager/RarityManager.java ---
package net.chamosmp.chamoitemskins.manager;

import net.chamosmp.chamoitemskins.api.model.Rarity;
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
public final class RarityManager {
    private final Plugin plugin;
    private final Map<String, Rarity> rarities = new LinkedHashMap<>();
    private boolean enabled;

    /**
     * Constructs a new RarityManager.
     * @param plugin The plugin instance.
     */
    public RarityManager(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads rarities from the given configuration.
     * @param config The configuration to load from.
     */
    public void load(@NotNull FileConfiguration config) {
        rarities.clear();
        ConfigurationSection section = config.getConfigurationSection("rarities");
        if (section == null) {
            enabled = false;
            plugin.getLogger().warning("No rarities block in config.yml — rarity features disabled.");
            return;
        }

        enabled = section.getBoolean("enabled", true);
        for (String key : section.getKeys(false)) {
            if (key.equalsIgnoreCase("enabled")) {
                continue;
            }
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) {
                continue;
            }
            String name = entry.getString("name", key);
            String color = entry.getString("color", "");
            int priority = entry.getInt("priority", 0);
            rarities.put(key.toLowerCase(), new Rarity(key.toLowerCase(), name, color, priority));
        }

        if (rarities.isEmpty()) {
            enabled = false;
            plugin.getLogger().warning("No rarity entries defined in config.yml — rarity features disabled.");
        }
    }

    /**
     * Checks if rarity features are enabled.
     * @return True if enabled.
     */
    public boolean isEnabled() {
        return enabled && !rarities.isEmpty();
    }

    /**
     * Gets all loaded rarities, sorted by priority.
     * @return A list of rarities.
     */
    public @NotNull List<Rarity> getRarities() {
        return rarities.values().stream()
                .sorted((a, b) -> Integer.compare(a.priority(), b.priority()))
                .toList();
    }

    /**
     * Gets a rarity by its ID.
     * @param id The rarity ID.
     * @return An optional containing the rarity if found.
     */
    public @NotNull Optional<Rarity> getRarity(@NotNull String id) {
        return Optional.ofNullable(rarities.get(id.toLowerCase()));
    }

    /**
     * Gets the default rarity (the one with the lowest priority).
     * @return The default rarity.
     */
    public @NotNull Rarity getDefaultRarity() {
        return getRarities().stream().findFirst()
                .orElse(new Rarity("common", "Common", "<gray>", 0));
    }

    /**
     * Resolves a rarity ID to a Rarity object, falling back to default if not found.
     * @param id The rarity ID to resolve.
     * @return The resolved rarity.
     */
    public @NotNull Rarity resolve(@Nullable String id) {
        if (id == null || id.isBlank()) {
            return getDefaultRarity();
        }
        return getRarity(id).orElseGet(() -> {
            plugin.getLogger().warning("Unknown rarity '" + id + "' — using default.");
            return getDefaultRarity();
        });
    }

    /**
     * Gets an unmodifiable map of all rarities.
     * @return The rarity map.
     */
    public @NotNull Map<String, Rarity> getRarityMap() {
        return Collections.unmodifiableMap(rarities);
    }
}
