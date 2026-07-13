// --- plugin/src/main/java/net/chamosmp/chamoitemskins/util/NoteUtil.java ---
package net.chamosmp.chamoitemskins.util;

import net.chamosmp.chamoitemskins.api.model.Skin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Utility for creating and identifying physical skin notes.
 */
public final class NoteUtil {
    public static NamespacedKey SKIN_ID_KEY;

    private NoteUtil() {}

    public static @NotNull ItemStack createNote(
            @NotNull Plugin plugin,
            @NotNull Skin skin,
            @NotNull Material defaultMaterial,
            @NotNull List<String> loreTemplate
    ) {
        return createNote(plugin, skin, defaultMaterial, loreTemplate, -1);
    }


    public static NamespacedKey EXPIRATION_KEY;

    public static boolean isNote(@NotNull ItemStack item) {
        if (item.getType().isAir() || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(SKIN_ID_KEY, PersistentDataType.STRING);
    }

    public static String getSkinId(@NotNull ItemStack item) {
        if (!isNote(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(SKIN_ID_KEY, PersistentDataType.STRING);
    }

    public static @NotNull ItemStack createNote(
            @NotNull Plugin plugin,
            @NotNull Skin skin,
            @NotNull Material defaultMaterial,
            @NotNull List<String> loreTemplate,
            int timeInDays
    ) {
        SKIN_ID_KEY = new NamespacedKey("chamoitemskins", "skin_id");
        EXPIRATION_KEY = new NamespacedKey(plugin, "note_expiration");
        FileConfiguration config = plugin.getConfig();

        Material material = skin.noteMaterial() != null ? skin.noteMaterial() : defaultMaterial;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.getPersistentDataContainer().set(EXPIRATION_KEY, PersistentDataType.INTEGER, timeInDays);

            Map<String, String> placeholders;
            String displayNameTemplate;
            if (timeInDays > 0) {
                displayNameTemplate = config.getString("note.temporary-name", "<gold><bold>Skin Note");
                placeholders = Map.of("skin_name", skin.name(), "time_left", String.valueOf(timeInDays));
            } else {
                displayNameTemplate = config.getString("note.display-name", "<gold><bold>Skin Note");
                placeholders = Map.of("skin_name", skin.name(), "time_left", "Permanent");
            }
            meta.displayName(MessageUtil.parse(null, displayNameTemplate, placeholders));
            meta.lore(loreTemplate.stream()
                    .map(line -> MessageUtil.parse(null, line, placeholders)) // {time_left} is still raw
                    .toList());

            meta.getPersistentDataContainer().set(SKIN_ID_KEY, PersistentDataType.STRING, skin.id());
            item.setItemMeta(meta);
        }

        return item;
    }

}
