// --- api/src/main/java/net/chamosmp/chamoitemskins/api/model/Rarity.java ---
package net.chamosmp.chamoitemskins.api.model;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a user-defined rarity tier for skins.
 *
 * @param id       Config key identifier.
 * @param name     MiniMessage display name.
 * @param color    MiniMessage color prefix applied before the name.
 * @param priority Sort order; lower values appear first.
 */
public record Rarity(
        @NotNull String id,
        @NotNull String name,
        @NotNull String color,
        int priority
) {
    /**
     * Returns the combined MiniMessage display string for this rarity.
     * @return Returns the colorized name of the rarity, configured from the config
     */
    public @NotNull String getDisplayName() {
        if (color.isBlank()) {
            return name;
        }
        return color + name;
    }
}
