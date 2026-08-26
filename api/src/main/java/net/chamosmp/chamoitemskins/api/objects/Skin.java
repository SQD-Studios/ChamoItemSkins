package net.chamosmp.chamoitemskins.api.objects;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents a cosmetic item skin.
 *
 * @param id           Unique skin identifier.
 * @param name         MiniMessage display name.
 * @param modelId      BetterModel model id - bare renderer name (e.g. {@code demon_knight})
 *                     or explicit item model key ({@code namespace:path}).
 * @param categories   The categories this skin belongs to.
 * @param enabled      Whether the skin is currently active in the plugin.
 * @param noteMaterial Optional override for the physical note item material.
 * @param displayItem  Configuration for the item shown in GUIs.
 * @param rarity       The rarity of the skin
 */
public record Skin(
        @NotNull String id,
        @NotNull String name,
        @NotNull String modelId,
        @Nullable Rarity rarity,
        @Nullable List<Category> categories,
        boolean enabled,
        Material noteMaterial,
        DisplayItem displayItem
) {
    /**
     * Represents an item to be displayed in the GUI.
     *
     * @param material The item material to display.
     * @param name     The name of the item to display.
     * @param lore     The lore of the item to display.
     * @param glow     Whether the item should glow.
     */
    public record DisplayItem(
            Material material,
            String name,
            List<String> lore,
            boolean glow
    ) {
    }
}
