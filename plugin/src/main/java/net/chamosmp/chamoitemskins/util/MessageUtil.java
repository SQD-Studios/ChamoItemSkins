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
        audience.sendMessage(parse(audience instanceof Player p ? p : null, message, Map.of()));
    }

    public static void sendMessage(@NotNull Audience audience, @NotNull String message, @NotNull Map<String, String> placeholders) {
        audience.sendMessage(parse(audience instanceof Player p ? p : null, message, placeholders));
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
