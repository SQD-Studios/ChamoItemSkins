package net.chamosmp.chamoitemskins.util;

import net.chamosmp.chamoitemskins.scheduler.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Utility for getting chat input from players.
 */
public final class ChatInputUtil implements Listener {
    private final Plugin plugin;
    private final Map<UUID, Consumer<String>> pendingInputs = new ConcurrentHashMap<>();

    public ChatInputUtil(Plugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void getInput(Player player, String prompt, Consumer<String> callback) {
        player.closeInventory();
        MessageUtil.sendMessage(player, prompt);
        pendingInputs.put(player.getUniqueId(), callback);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!pendingInputs.containsKey(uuid)) return;

        event.setCancelled(true);
        Consumer<String> callback = pendingInputs.remove(uuid);
        
        String message = event.getMessage();
        if (message.equalsIgnoreCase("cancel")) {
            MessageUtil.sendMessage(event.getPlayer(), "<red>Input cancelled.");
            return;
        }

        if (callback != null) {
            SchedulerUtil.runSync(plugin, () -> callback.accept(message));
        }
    }
}
