package net.chamosmp.chamoitemskins.util;

import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Utility for opening Paper Dialog API inputs and returning results via callbacks.
 */
public class DialogUtil implements Listener {

    // Maps player UUID -> (input key -> callback)
    private final Map<UUID, Map<String, Consumer<String>>> pending = new ConcurrentHashMap<>();

    public DialogUtil(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Opens a text input dialog for a player and calls {@code callback} with the
     * result when the player confirms, or with {@code null} if they discard.
     *
     * @param title    The title of the dialog
     * @param player   The player to open the dialog to
     * @param key      The key of the text input, used to retrieve the value
     * @param content  The label shown next to the text field
     * @param callback Called with the player's input, or null on discard
     */
    public void getInput(Component title, Player player, String key, Component content, String defaultValue, Consumer<String> callback) {
        if (key == null || key.isBlank()) return;

        // Sanitize: lowercase, replace invalid chars with underscores
        String safeKey = key.toLowerCase().replaceAll("[^a-z0-9_\\-.]", "_");
        if (safeKey.isEmpty() || !Character.isLetter(safeKey.charAt(0))) {
            safeKey = "k_" + safeKey;
        }

        pending.computeIfAbsent(player.getUniqueId(), id -> new ConcurrentHashMap<>())
                .put(safeKey, callback);

        final String fKey = safeKey;
        Dialog dialog = Dialog.create(builder ->
                builder.empty()
                        .base(DialogBase.builder(title)
                                .inputs(List.of(
                                        DialogInput.text(fKey, content)
                                                //.width(300)
                                                .initial(defaultValue != null ? defaultValue : "")
                                                .build()
                                ))
                                .build()
                        )
                        .type(DialogType.confirmation(
                                ActionButton.create(
                                        Component.text("Confirm", TextColor.color(0xAEFFC1)),
                                        Component.text("Click to confirm your input."),
                                        100,
                                        DialogAction.customClick(Key.key("chamoitemskins:" + fKey + "/confirm"), null)
                                ),
                                ActionButton.create(
                                        Component.text("Discard", TextColor.color(0xFFA0B1)),
                                        Component.text("Click to discard your input."),
                                        100,
                                        DialogAction.customClick(Key.key("chamoitemskins:" + fKey + "/discard"), null)
                                )
                        ))
        );

        player.showDialog(dialog);
    }

    public void getInput(Component title, Player player, String key, Component content, Consumer<String> callback) {
        getInput(title, player, key, content, null, callback);
    }

    /**
     * Opens a text input dialog for a player and calls {@code callback} with the
     * result when the player confirms, or with {@code null} if they discard.
     *
     * @param title    The title of the dialog
     * @param player   The player to open the dialog to
     * @param key      The key of the text input, used to retrieve the value
     * @param callback Called with the player's input, or null on discard
     */
    public void getYesNo(Component title, Player player, String key, Consumer<String> callback) {
        if (key == null || key.isBlank()) return;

        // Sanitize: lowercase, replace invalid chars with underscores
        String safeKey = key.toLowerCase().replaceAll("[^a-z0-9_\\-.]", "_");
        if (safeKey.isEmpty() || !Character.isLetter(safeKey.charAt(0))) {
            safeKey = "k_" + safeKey;
        }

        pending.computeIfAbsent(player.getUniqueId(), id -> new ConcurrentHashMap<>())
                .put(safeKey, callback);

        final String fKey = safeKey;
        Dialog dialog = Dialog.create(builder ->
                builder.empty()
                        .base(DialogBase.builder(title)
                                .build()
                        )
                        .type(DialogType.confirmation(
                                ActionButton.create(
                                        Component.text("Yes", TextColor.color(0xAEFFC1)),
                                        Component.text("Click to confirm your input."),
                                        100,
                                        DialogAction.customClick(Key.key("chamoitemskins:" + fKey + "/yes"), null)
                                ),
                                ActionButton.create(
                                        Component.text("No", TextColor.color(0xFFA0B1)),
                                        Component.text("Click to discard your input."),
                                        100,
                                        DialogAction.customClick(Key.key("chamoitemskins:" + fKey + "/no"), null)
                                )
                        ))
        );

        player.showDialog(dialog);
    }

    @EventHandler
    public void onDialogClick(PlayerCustomClickEvent event) {
        Key id = event.getIdentifier();
        String path = id.value();

        if (!id.namespace().equals("chamoitemskins")) return;

        boolean isConfirm = path.endsWith("/confirm");
        boolean isDiscard = path.endsWith("/discard");
        boolean isYes = path.endsWith("/yes");
        boolean isNo = path.endsWith("/no");

        if (!isConfirm && !isDiscard && !isYes && !isNo) return;

        if (!(event.getCommonConnection() instanceof PlayerGameConnection conn)) return;
        Player player = conn.getPlayer();

        String key = path.substring(0, path.lastIndexOf('/'));

        Map<String, Consumer<String>> playerPending = pending.get(player.getUniqueId());
        if (playerPending == null) return;

        Consumer<String> callback = playerPending.remove(key);
        if (playerPending.isEmpty()) pending.remove(player.getUniqueId());
        if (callback == null) return;

        if (isConfirm) {
            DialogResponseView view = event.getDialogResponseView();
            String text = (view != null) ? view.getText(key) : null;
            callback.accept(text);
        } else if (isDiscard) {
            callback.accept(null);
        } else if (isYes) {
            callback.accept(String.valueOf(true));
        } else {
            callback.accept(null);
        }
    }

    /**
     * Opens a dialog without an input (informational / confirmation only).
     */
    public void open(Dialog dialog, Player player) {
        player.showDialog(dialog);
    }

    public boolean canUseDialogs() {
        try {
            Class.forName("io.papermc.paper.dialog.Dialog");
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
