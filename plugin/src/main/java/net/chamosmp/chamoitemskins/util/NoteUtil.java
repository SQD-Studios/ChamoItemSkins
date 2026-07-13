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
    public static NamespacedKey EXPIRATION_KEY;

    public static void init(@NotNull Plugin plugin) {
        SKIN_ID_KEY = new NamespacedKey(plugin, "skin_id");
        EXPIRATION_KEY = new NamespacedKey(plugin, "expiration");
    }


    private NoteUtil() {}

    public static @NotNull ItemStack createNote(
            @NotNull Plugin plugin,
            @NotNull Skin skin,
            @NotNull Material defaultMaterial,
            @NotNull List<String> loreTemplate
    ) {
        return createNote(plugin, skin, defaultMaterial, loreTemplate, -1);
    }




    public static boolean isNote(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        if (SKIN_ID_KEY == null) return false;
        return item.getItemMeta().getPersistentDataContainer().has(SKIN_ID_KEY);
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
