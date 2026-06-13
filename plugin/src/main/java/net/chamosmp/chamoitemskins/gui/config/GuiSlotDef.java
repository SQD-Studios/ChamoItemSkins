// --- plugin/src/main/java/net/chamosmp/chamoitemskins/gui/config/GuiSlotDef.java ---
package net.chamosmp.chamoitemskins.gui.config;

import org.bukkit.Material;
import java.util.List;

/**
 * Definition of a GUI slot from configuration.
 *
 * @param type     The type of the slot.
 * @param slot     The slot index (0-53).
 * @param material The material of the item in this slot.
 * @param name     The display name (MiniMessage).
 * @param lore     The lore (MiniMessage list).
 * @param glow     Whether the item should glow.
 */
public record GuiSlotDef(
        SlotType type,
        int slot,
        Material material,
        String name,
        List<String> lore,
        boolean glow
) {}
