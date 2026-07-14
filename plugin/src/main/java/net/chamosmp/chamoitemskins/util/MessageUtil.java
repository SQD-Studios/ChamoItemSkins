// --- plugin/src/main/java/net/chamosmp/chamoitemskins/util/MessageUtil.java ---
package net.chamosmp.chamoitemskins.util;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import net.chamosmp.chamoitemskins.manager.LanguageManager;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility for sending Adventure/MiniMessage formatted messages.
 */
public final class MessageUtil {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final boolean PAPI_PRESENT = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    private final LanguageManager langManager;

    public MessageUtil(LanguageManager langManager) {
        this.langManager = langManager;
    }

    public void sendLangMessage(Audience player, String key, Map<String, String> placeholders) {
        sendMessage(player, legacyToMiniMessage(langManager.getMessage(key, placeholders)));
    }

    public void sendLangMessage(Audience player, String key) {
        sendMessage(player, legacyToMiniMessage(langManager.getMessage(key)));
    }

    /**
     * Converts a legacy-formatted message string into a MiniMessage-compatible string.
     *
     * @param message The legacy-formatted message
     * @return The message with MiniMessage tags instead of legacy
     */
    public static String legacyToMiniMessage(String message) {
        String oneChar = message.replace("§", "&");
        String black = oneChar.replace("&0", "<black>");
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
        String bold = white.replace("&l", "<b>");
        String italic = bold.replace("&o", "<i>");
        String underline = italic.replace("&n", "<u>");
        String strikethrough = underline.replace("&m", "<st>");

        return strikethrough.replace("&k", "<obf>");
    }

    /**
     * Send a message to an audience, with no placeholders
     * @param audience The audience to send this message to
     * @param message The message to send
     */
    public static void sendMessage(@NotNull Audience audience, @NotNull String message) {
        Player player = null;
        if (audience instanceof Player p) {
            player = p;
        }
        audience.sendMessage(parse(player, message, Map.of()));
    }

    /**
     * Send a message to a player
     * @param audience The instance to send a message to (A player or console)
     * @param message The message to send, with mini message formatting
     * @param placeholders The placeholders, forward parameter for parse
     */
    public static void sendMessage(@NotNull Audience audience, @NotNull String message, @NotNull Map<String, String> placeholders) {
        Player player = null;
        if (audience instanceof Player p) {
            player = p;
        }
        audience.sendMessage(parse(player, message, placeholders));
    }

    public static @NotNull Component parse(@NotNull String message) {
        return MINI_MESSAGE.deserialize(legacyToMiniMessage(message));
    }

    public static @NotNull Component parse(Player player, @NotNull String message, @NotNull Map<String, String> placeholders) {
        String resolved = message;
        for (var entry : placeholders.entrySet()) {
            resolved = resolved.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        
        if (PAPI_PRESENT && player != null) {
            resolved = PlaceholderAPI.setPlaceholders(player, resolved);
        }
        
        return MINI_MESSAGE.deserialize(legacyToMiniMessage(resolved));
    }

    public static @NotNull List<String> placeholder(@NotNull List<String> message, @NotNull Map<String, String> placeholders) {
        List<String> resolved = new ArrayList<>(message); // copy to avoid altering input
        for (var entry : placeholders.entrySet()) {
            String key = "{" + entry.getKey() + "}";
            String value = entry.getValue();
            resolved.replaceAll(s -> s.replace(key, value));
        }
        return resolved;
    }

    public static @NotNull String placeholder(@NotNull String message, @NotNull Map<String, String> placeholders) {
        String resolved = message;
        for (var entry : placeholders.entrySet()) {
            resolved = resolved.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        return resolved;
    }
}
