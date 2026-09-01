package net.chamosmp.chamoitemskins.manager;

import net.chamosmp.chamoitemskins.api.objects.Rarity;
import net.chamosmp.chamoitemskins.api.service.RarityService;
import net.chamosmp.chamoitemskins.util.LoggerUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
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
public final class RarityManager implements RarityService {
    private final Map<String, Rarity> rarities = new LinkedHashMap<>();
    private boolean enabled;

    /**
     * Constructs a new RarityManager.
     *
     */
    public RarityManager(@NotNull FileConfiguration config) {
        load(config);
    }

    /**
     * Loads rarities from the given configuration.
     *
     * @param config The configuration to load from.
     */
    private void load(@NotNull FileConfiguration config) {
        rarities.clear();
        ConfigurationSection section = config.getConfigurationSection("rarities");
        if (section == null) {
            enabled = false;
            LoggerUtil.log(LoggerUtil.LogType.WARNING, "No rarities block in config.yml - rarity features disabled.");
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
            LoggerUtil.log(LoggerUtil.LogType.WARNING, "No rarities defined in config.yml - rarity features disabled.");
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled && !rarities.isEmpty();
    }

    @Override
    public @NotNull List<Rarity> getRarities() {
        return rarities.values().stream()
                .sorted((a, b) -> Integer.compare(a.priority(), b.priority()))
                .toList();
    }

    @Override
    public @NotNull Optional<Rarity> getRarity(@NotNull String id) {
        return Optional.ofNullable(rarities.get(id.toLowerCase()));
    }

    @Override
    public @NotNull Rarity getDefaultRarity() {
        return getRarities().stream().findFirst()
                .orElse(new Rarity("common", "Common", "<gray>", 0));
    }

    @Override
    public @NotNull Rarity resolve(@Nullable String id) {
        if (id == null || id.isBlank()) {
            return getDefaultRarity();
        }
        return getRarity(id).orElseGet(() -> {
            LoggerUtil.log(LoggerUtil.LogType.WARNING, "Unknown rarity '" + id + "' - using default.");
            return getDefaultRarity();
        });
    }

    @Override
    public @NotNull Map<String, Rarity> getRarityMap() {
        return Collections.unmodifiableMap(rarities);
    }
}
