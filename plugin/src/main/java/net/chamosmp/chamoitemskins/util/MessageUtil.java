// --- plugin/src/main/java/net/chamosmp/chamoitemskins/util/MessageUtil.java ---
package net.chamosmp.chamoitemskins.util;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility for sending Adventure/MiniMessage formatted messages.
 */
public final class MessageUtil {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final boolean PAPI_PRESENT = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    private static final Map<String, YamlConfiguration> LANGUAGE_MAP = new HashMap<>();
    private static String defaultLanguage = "en";

    private MessageUtil() {}

    public static void loadLanguages(@NotNull Plugin plugin) {
        LANGUAGE_MAP.clear();
        File folder = new File(plugin.getDataFolder(), "languages");
        if (!folder.exists()) folder.mkdirs();

        defaultLanguage = plugin.getConfig().getString("default-language", "en").toLowerCase();

        File[] files = folder.listFiles((dir, name) -> name.startsWith("messages_") && name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String lang = file.getName().substring(9, file.getName().length() - 4);
                LANGUAGE_MAP.put(lang.toLowerCase(), YamlConfiguration.loadConfiguration(file));
            }
        }
    }

    /**
     * Converts a legacy-formatted message string into a MiniMessage-compatible string.
     *
     * @param message the legacy-formatted message
     * @return the converted MiniMessage string
     */
    public static String legacyToMiniMessage(String message) {
        String oneChar = message.replace("§", "&");
        String twoChar = oneChar.replace("<&>", "&");
        String black = twoChar.replace("&0", "<black>");
        String dark_blue = black.replace("&1", "<dark_blue>");
        String dark_green = dark_blue.replace("&2", "<dark_green>");
        String dark_aqua = dark_green.replace("&3", "<dark_aqua>");
        String dark_red = dark_aqua.replace("&4", "<dark_red>");
        String dark_purple = dark_red.replace("&5", "<dark_purple>");
        String gold = dark_purple.replace("&6", "<gold>");
        String gray = gold.replace("&7", "<gray>");
        String dark_gray = gray.replace("&8", "<dark_gray>");
        String blue = dark_gray.replace("&9", "<blue>");
        String green = blue.replace("&a", "<green>");
        String aqua = green.replace("&b", "<aqua>");
        String red = aqua.replace("&c", "<red>");
        String light_purple = red.replace("&d", "<light_purple>");
        String yellow = light_purple.replace("&e", "<yellow>");
        String white = yellow.replace("&f", "<white>");
        String minecoin_gold = white.replace("&g", "<#DDD605>");
        String material_quartz = minecoin_gold.replace("&h", "<#E3D4D1>");
        String material_iron = material_quartz.replace("&i", "<#CECACA>");
        String material_netherite = material_iron.replace("&j", "<#443A3B>");
        String material_gold = material_netherite.replace("&p", "<#DEB12D>");
        String material_emerald = material_gold.replace("&q", "<#119F36>");
        String material_diamond = material_emerald.replace("&s", "<#2CBAA8>");
        String material_lapis = material_diamond.replace("&t", "<#21497B>");
        String material_amethyst = material_lapis.replace("&u", "<#9A5CC6>");
        String material_resin = material_amethyst.replace("&v", "<#EB7114>");
        String party_blue_color = material_resin.replace("&w", "<#8CB3FF>");
        String bold = party_blue_color.replace("&l", "<b>");
        String italic = bold.replace("&o", "<i>");
        String underline = italic.replace("&n", "<u>");
        String strikethrough = underline.replace("&m", "<st>");

        return strikethrough.replace("&k", "<obf>");
    }

    public static @NotNull String getMessage(@NotNull String key, @NotNull String lang) {
        YamlConfiguration config = LANGUAGE_MAP.get(lang.toLowerCase());
        if (config == null) config = LANGUAGE_MAP.get(defaultLanguage);
        if (config == null) return key;
        return config.getString(key, key);
    }

    public static void sendLocalizedMessage(@NotNull Player player, @NotNull String key) {
        sendLocalizedMessage(player, key, Map.of());
    }

    public static void sendLocalizedMessage(@NotNull Player player, @NotNull String key, @NotNull Map<String, String> placeholders) {
        String lang = player.getLocale().split("_")[0].toLowerCase();
        String message = getMessage(key, lang);
        player.sendMessage(parse(player, message, placeholders));
    }

    public static void sendMessage(@NotNull Audience audience, @NotNull String message) {
        Player player = null;
        if (audience instanceof Player p) {
            player = p;
        }
        audience.sendMessage(parse(player, message, Map.of()));
    }

    public static void sendMessage(@NotNull Audience audience, @NotNull String message, @NotNull Map<String, String> placeholders) {
        Player player = null;
        if (audience instanceof Player p) {
            player = p;
        }
        audience.sendMessage(parse(player, message, placeholders));
    }

    public static @NotNull Component parse(@NotNull String message) {
        return MINI_MESSAGE.deserialize(message);
    }

    public static @NotNull Component parse(Player player, @NotNull String message, @NotNull Map<String, String> placeholders) {
        String resolved = message;
        for (var entry : placeholders.entrySet()) {
            resolved = resolved.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        
        if (PAPI_PRESENT && player != null) {
            resolved = PlaceholderAPI.setPlaceholders(player, resolved);
        }
        
        return MINI_MESSAGE.deserialize(resolved);
    }
}
