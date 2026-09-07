package net.chamosmp.chamoitemskins.util;

import net.chamosmp.sqdlib.paper.util.ColorUtil;
import net.chamosmp.sqdlib.paper.util.LanguageUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public final class MessageUtil {
    private final LanguageUtil langManager;

    public MessageUtil(LanguageUtil langUtil) {
        this.langManager = langUtil;
    }

    public void sendLangMessage(Audience player, String key, Map<?, ?> placeholders) {
        sendMessage(player, langManager.getMessage(key, placeholders));
    }

    public void sendLangMessage(Audience player, String key) {
        sendMessage(player, langManager.getMessage(key));
    }

    /**
     * Send a message to an audience, with no placeholders
     *
     * @param audience The audience to send this message to
     * @param message  The message to send
     */
    public static void sendMessage(@NotNull Audience audience, @NotNull String message) {
        Player player = null;
        if (audience instanceof Player p) {
            player = p;
        }
        audience.sendMessage(parse(player, message, Map.of()));
    }

    @ApiStatus.Obsolete
    public static @NotNull Component parse(@NotNull String message) {
        return ColorUtil.parse(message);
    }

    @ApiStatus.Obsolete
    public static @NotNull Component parse(Player player, @NotNull String message, @NotNull Map<?, ?> placeholders) {
        return ColorUtil.parse(player, message, placeholders);
    }

    @ApiStatus.Obsolete
    public static @NotNull List<String> placeholder(@NotNull List<String> message, @NotNull Map<?, ?> placeholders) {
        return ColorUtil.placeholder(message, placeholders);
    }

    @ApiStatus.Obsolete
    public static @NotNull String placeholder(@NotNull String message, @NotNull Map<?, ?> placeholders) {
        return ColorUtil.placeholder(message, placeholders);
    }
}
