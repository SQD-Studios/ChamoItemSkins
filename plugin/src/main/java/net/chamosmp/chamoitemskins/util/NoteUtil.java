package net.chamosmp.chamoitemskins.util;

import net.chamosmp.chamoitemskins.api.objects.Skin;
import net.chamosmp.chamoitemskins.listener.NoteListener;
import net.chamosmp.sqdlib.paper.note.NoteMaker;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;

/**
 * Utility for creating and identifying physical skin notes.
 */
public final class NoteUtil {
    private final NoteMaker noteMaker;
    private final Plugin plugin;

    public static NamespacedKey SKIN_ID_KEY;
    public static NamespacedKey EXPIRATION_KEY;

    public NoteUtil(Plugin plugin, NoteListener noteListener) {
        this.noteMaker = new NoteMaker(plugin);
        this.plugin = plugin;

        SKIN_ID_KEY = new NamespacedKey(plugin, "skin_id");
        EXPIRATION_KEY = new NamespacedKey(plugin, "expiration");

        this.noteMaker.addListener(noteListener);
    }

    public static @Nullable String getSkinId(@NotNull ItemStack item) {
        return item.getItemMeta().getPersistentDataContainer().get(SKIN_ID_KEY, PersistentDataType.STRING);
    }

    public @NotNull ItemStack createNote(
            @NotNull Skin skin,
            @NotNull Material defaultMaterial,
            @NotNull List<String> loreTemplate,
            int timeInDays
    ) {
        FileConfiguration config = plugin.getConfig();

        Material material = skin.noteMaterial() != null ? skin.noteMaterial() : defaultMaterial;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        Map<String, String> placeholders;
        String displayNameTemplate;

        if (timeInDays > 0) {
            displayNameTemplate = config.getString("note.temporary-name", "<gold><bold>Skin Note");
            placeholders = Map.of("skin_name", skin.name(), "time_left", String.valueOf(timeInDays));
        } else {
            displayNameTemplate = config.getString("note.display-name", "<gold><bold>Skin Note");
            placeholders = Map.of("skin_name", skin.name(), "time_left", "Permanent");
        }

        meta.customName(MessageUtil.parse(null, displayNameTemplate, placeholders));
        meta.lore(loreTemplate.stream()
                .map(line -> MessageUtil.parse(null, line, placeholders))
                .toList());

        item.setItemMeta(meta);

        return noteMaker.createNote(item, pdc -> {
            pdc.set(SKIN_ID_KEY, PersistentDataType.STRING, skin.id());
            pdc.set(EXPIRATION_KEY, PersistentDataType.INTEGER, timeInDays);
        });
    }
}