package net.chamosmp.chamoitemskins.util;

import net.chamosmp.chamoitemskins.scheduler.SchedulerUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.server.TabCompleteEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Utility for getting chat input from players.
 */
public final class ChatInputUtil implements Listener {
    private final Plugin plugin;
    private final Map<UUID, Consumer<String>> pendingInputs = new ConcurrentHashMap<>();
    private final Map<UUID, Supplier<Collection<String>>> pendingSuggestions = new ConcurrentHashMap<>();
    private final DialogUtil dialogUtil;

    public ChatInputUtil(Plugin plugin, DialogUtil dialogUtil) {
        this.dialogUtil = dialogUtil;
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * "The util means, the dialog/chat thingy, not to be confused"
     * @param player The player to open the util to
     * @param prompt The prompt to give the player
     * @param callback Callback is the piece of code to be executed
     * @param key (Dialog Only) The key, the buttons/dialog should have
     * @param title (Dialog Only) The title in the dialog
     * @param defaultValue (Dialog Only) The value the text prompt should have, useful for editing already defined configs.
     */
    public void getInput(Player player, Component prompt, Consumer<String> callback, String key, Component title, String defaultValue) {
        if (!dialogUtil.canUseDialogs()) {
            player.closeInventory();
            MessageUtil.sendMessage(player, prompt + " <red>Type cancel to cancel.");
            pendingInputs.put(player.getUniqueId(), callback);
        } else {
            dialogUtil.getInput(title, player, key, prompt, defaultValue, callback);
        }
    }

    /**
     * "The util means, the dialog/chat thingy, not to be confused"
     * @param player The player to open the util to
     * @param prompt The prompt to give the player
     * @param callback Callback is the piece of code to be executed
     * @param key (Dialog Only) The key, the buttons/dialog should have
     * @param title (Dialog Only) The title in the dialog
     */
    public void getInput(Player player, Component prompt, Consumer<String> callback, String key, Component title) {
        if (!dialogUtil.canUseDialogs()) {
            player.closeInventory();
            MessageUtil.sendMessage(player, prompt + " <red>Type cancel to cancel.");
            pendingInputs.put(player.getUniqueId(), callback);
        } else {
            dialogUtil.getInput(title, player, key, prompt, callback);
        }
    }

    public void getYesNo(Player player, Consumer<String> callback, String key, Component title) {
        if (!dialogUtil.canUseDialogs()) {
            player.closeInventory();
            MessageUtil.sendMessage(player, title + " <yellow>(yes or no) <red>Type cancel to cancel.");
            pendingInputs.put(player.getUniqueId(), callback);
        } else {
            dialogUtil.getYesNo(title, player, key, callback);
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
    public void onTabComplete(TabCompleteEvent event) {
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
