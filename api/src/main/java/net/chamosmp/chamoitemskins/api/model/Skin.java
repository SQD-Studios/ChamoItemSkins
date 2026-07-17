// --- api/src/main/java/net/chamosmp/chamoitemskins/api/model/Skin.java ---
package net.chamosmp.chamoitemskins.api.model;

import org.bukkit.Material;
import java.util.List;
import java.util.concurrent.CancellationException;

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
 *
 * @param rarity The rarity of the skin
 * @param animations The animations to apply to the skin
 */
public record Skin(
        String id,
        String name,
        String modelId,
        Rarity rarity,
        List<Category> categories,
        boolean enabled,
        Material noteMaterial,
        DisplayItem displayItem,
        List<String> animations
) {
    /**
     * Represents an item to be displayed in the GUI.
     * @param material    The item material to display.
     * @param name        The name of the item to display.
     * @param lore        The lore of the item to display.
     * @param glow        Whether the item should glow.
     */
    public record DisplayItem(
            Material material,
            String name,
            List<String> lore,
            boolean glow
    ) {}
}
