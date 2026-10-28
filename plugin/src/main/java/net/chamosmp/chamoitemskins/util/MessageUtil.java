package net.chamosmp.chamoitemskins.util;

import com.sqdstudios.sqdlib.paper.util.ColorUtil;
import com.sqdstudios.sqdlib.paper.util.LanguageUtil;
import net.kyori.adventure.audience.Audience;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class MessageUtil {
    private final LanguageUtil langManager;

    public MessageUtil(LanguageUtil langUtil) {
        this.langManager = langUtil;
    }

    public static void sendMessage(@NotNull Audience audience, @NotNull String message) {
        Player player = null;
        if (audience instanceof Player p) {
            player = p;
        }
        audience.sendMessage(ColorUtil.parse(player, message, Map.of()));
    }

    public void sendLangMessage(Audience player, String key, Map<?, ?> placeholders) {
        sendMessage(player, langManager.getMessage(key, placeholders));
    }

    public void sendLangMessage(Audience player, String key) {
        sendMessage(player, langManager.getMessage(key));
    }
}