package net.chamosmp.chamoitemskins.util;

import net.chamosmp.chamoitemskins.scheduler.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Utility for getting chat input from players.
 */
public final class ChatInputUtil implements Listener {
    private final Plugin plugin;
    private final Map<UUID, Consumer<String>> pendingInputs = new ConcurrentHashMap<>();
    private final Map<UUID, Supplier<Collection<String>>> pendingSuggestions = new ConcurrentHashMap<>();

    public ChatInputUtil(Plugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void getInput(Player player, String prompt, Consumer<String> callback) {
        getInput(player, prompt, null, callback);
    }

    public void getInput(Player player, String prompt, Supplier<Collection<String>> suggestions, Consumer<String> callback) {
        player.closeInventory();
        MessageUtil.sendMessage(player, prompt + " Type <red>cancel<reset> to cancel.");
        pendingInputs.put(player.getUniqueId(), callback);
        if (suggestions != null) {
            pendingSuggestions.put(player.getUniqueId(), suggestions);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!pendingInputs.containsKey(uuid)) return;

        event.setCancelled(true);
        pendingSuggestions.remove(uuid);
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

    @EventHandler
    public void onTabComplete(org.bukkit.event.server.TabCompleteEvent event) {
        if (!(event.getSender() instanceof Player player)) return;
        var suggestionsSupplier = pendingSuggestions.get(player.getUniqueId());
        if (suggestionsSupplier == null) return;

        String buffer = event.getBuffer();
        // Chat tab complete usually doesn't have a '/' prefix if it's just chat
        if (buffer.startsWith("/")) return;

        Collection<String> all = suggestionsSupplier.get();
        if (all == null || all.isEmpty()) return;

        String lastWord = buffer.substring(buffer.lastIndexOf(' ') + 1).toLowerCase();
        List<String> matched = all.stream()
                .filter(s -> s.toLowerCase().startsWith(lastWord))
                .toList();

        if (!matched.isEmpty()) {
            event.setCompletions(new ArrayList<>(matched));
        }
    }
}
