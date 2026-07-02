// --- plugin/src/main/java/net/chamosmp/chamoitemskins/util/NoteUtil.java ---
package net.chamosmp.chamoitemskins.util;

import net.chamosmp.chamoitemskins.api.model.Skin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Timer;

/**
 * Utility for creating and identifying physical skin notes.
 */
public final class NoteUtil {
    public static final NamespacedKey SKIN_ID_KEY = new NamespacedKey("chamoitemskins", "skin_id");

    private NoteUtil() {}

    public static @NotNull ItemStack createNote(
            @NotNull Plugin plugin,
            @NotNull Skin skin,
            @NotNull Material defaultMaterial,
            @NotNull String displayNameTemplate,
            @NotNull List<String> loreTemplate
    ) {
        Material material = skin.noteMaterial() != null ? skin.noteMaterial() : defaultMaterial;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        String time = "Permanent";
        if (meta != null) {
            Map<String, String> placeholders = Map.of("skin_name", skin.name(), "time", time);
            
            meta.displayName(MessageUtil.parse(null, displayNameTemplate, placeholders));
            meta.lore(loreTemplate.stream()
                    .map(line -> MessageUtil.parse(null, line, placeholders))
                    .toList());
            
            meta.getPersistentDataContainer().set(SKIN_ID_KEY, PersistentDataType.STRING, skin.id());
            item.setItemMeta(meta);
        }
        
        return item;
    }

    public static boolean isNote(@NotNull ItemStack item) {
        if (item.getType().isAir() || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(SKIN_ID_KEY, PersistentDataType.STRING);
    }

    public static String getSkinId(@NotNull ItemStack item) {
        if (!isNote(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(SKIN_ID_KEY, PersistentDataType.STRING);
    }

    /**
     *
     * @param plugin The plugin instance
     * @param skin The skin to create the the note
     * @param defaultMaterial The default material
     * @param displayNameTemplate The display name template
     * @param loreTemplate The lore template
     * @param time In days
     * @return The ItemStack note
     */
    public static @NotNull ItemStack createNote(
            @NotNull Plugin plugin,
            @NotNull Skin skin,
            @NotNull Material defaultMaterial,
            @NotNull String displayNameTemplate,
            @NotNull List<String> loreTemplate,
            int time
    ) {
        Material material = skin.noteMaterial() != null ? skin.noteMaterial() : defaultMaterial;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            Map<String, String> placeholders = Map.of("skin_name", skin.name(), "time", time + "Days");

            meta.displayName(MessageUtil.parse(null, displayNameTemplate, placeholders));
            meta.lore(loreTemplate.stream()
                    .map(line -> MessageUtil.parse(null, line, placeholders))
                    .toList());

            meta.getPersistentDataContainer().set(SKIN_ID_KEY, PersistentDataType.STRING, skin.id());
            item.setItemMeta(meta);
        }

        return item;
    }

}
