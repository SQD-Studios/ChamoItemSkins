// --- plugin/src/main/java/net/chamosmp/chamoitemskins/manager/RarityManager.java ---
package net.chamosmp.chamoitemskins.manager;

import net.chamosmp.chamoitemskins.api.model.Rarity;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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

    public RarityManager(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

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

    public boolean isEnabled() {
        return enabled && !rarities.isEmpty();
    }

    public @NotNull List<Rarity> getRarities() {
        return rarities.values().stream()
                .sorted((a, b) -> Integer.compare(a.priority(), b.priority()))
                .toList();
    }

    public @NotNull Optional<Rarity> getRarity(@NotNull String id) {
        return Optional.ofNullable(rarities.get(id.toLowerCase()));
    }

    public @NotNull Rarity getDefaultRarity() {
        return getRarities().stream().findFirst()
                .orElse(new Rarity("common", "Common", "<gray>", 0));
    }

    public @NotNull Rarity resolve(@Nullable String id) {
        if (id == null || id.isBlank()) {
            return getDefaultRarity();
        }
        return getRarity(id).orElseGet(() -> {
            plugin.getLogger().warning("Unknown rarity '" + id + "' — using default.");
            return getDefaultRarity();
        });
    }

    public @NotNull Map<String, Rarity> getRarityMap() {
        return Collections.unmodifiableMap(rarities);
    }
}
