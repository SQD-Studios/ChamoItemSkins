// --- api/src/main/java/net/chamosmp/chamoitemskins/api/model/Skin.java ---
package net.chamosmp.chamoitemskins.api.model;

import org.bukkit.Material;
import java.util.List;

/**
 * Represents a cosmetic item skin.
 *
 * @param id           Unique skin identifier.
 * @param name         MiniMessage display name.
 * @param modelId      BetterModel model ID.
 * @param itemType     The material this skin can be applied to.
 * @param enabled      Whether the skin is currently active in the plugin.
 * @param noteMaterial Optional override for the physical note item material.
 * @param displayItem  Configuration for the item shown in GUIs.
 */
public record Skin(
        String id,
        String name,
        String modelId,
        Material itemType,
        boolean enabled,
        Material noteMaterial,
        DisplayItem displayItem
) {
    public record DisplayItem(
            Material material,
            String name,
            List<String> lore,
            boolean glow
    ) {}
}
