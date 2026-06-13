// --- plugin/src/main/java/net/chamosmp/chamoitemskins/util/MessageUtil.java ---
package net.chamosmp.chamoitemskins.util;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Utility for sending Adventure/MiniMessage formatted messages.
 */
public final class MessageUtil {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final boolean PAPI_PRESENT = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");

    private MessageUtil() {}

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
