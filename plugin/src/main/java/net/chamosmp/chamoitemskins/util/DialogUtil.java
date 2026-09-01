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
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.intellij.lang.annotations.Subst;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Utility for opening Paper Dialog API inputs and returning results via callbacks.
 */
public class DialogUtil implements Listener {

    private final Map<UUID, Map<String, Consumer<String>>> pending = new ConcurrentHashMap<>();

    public DialogUtil() {

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
        final String safeKey = key.toLowerCase().replaceAll("[^a-z0-9_\\-.]", "_");

        pending.computeIfAbsent(player.getUniqueId(), _ -> new ConcurrentHashMap<>())
                .put(safeKey, callback);

        Dialog dialog = Dialog.create(builder ->
                builder.empty()
                        .base(DialogBase.builder(title)
                                .inputs(List.of(
                                        DialogInput.text(safeKey, content)
                                                .initial(defaultValue != null ? defaultValue : "")
                                                .build()
                                ))
                                .build()
                        )
                        .type(DialogType.confirmation(
                                ActionButton.create(
                                        MessageUtil.parse("<green>Confirm"),
                                        MessageUtil.parse("Click to confirm your input."),
                                        100,
                                        DialogAction.customClick(Key.key("chamoitemskins:" + safeKey + "/confirm"), null)
                                ),
                                ActionButton.create(
                                        MessageUtil.parse("<red>Discard"),
                                        MessageUtil.parse("Click to discard your input."),
                                        100,
                                        DialogAction.staticAction(ClickEvent.callback(_ -> {
                                            pending.remove(player.getUniqueId());
                                            callback.accept(null);
                                        }))
                                )
                        ))
        );

        player.showDialog(dialog);
    }

    /**
     * Opens a text input dialog for a player and calls {@code callback} with the
     * result when the player confirms, or with {@code null} if they discard.
     *
     * @param title    The title of the dialog
     * @param player   The player to open the dialog to
     * @param callback Called with the player's input, or null on discard
     */
    public void getYesNo(Component title, Player player, Consumer<Boolean> callback) {
        Dialog dialog = Dialog.create(builder ->
                builder.empty()
                        .base(DialogBase.builder(title)
                                .build()
                        )
                        .type(DialogType.confirmation(
                                ActionButton.create(
                                        MessageUtil.parse("<green>Yes"),
                                        MessageUtil.parse("Click to confirm your input."),
                                        100,
                                        DialogAction.staticAction(ClickEvent.callback(_ -> {
                                            callback.accept(true);
                                        }))
                                ),
                                ActionButton.create(
                                        MessageUtil.parse("<red>No"),
                                        MessageUtil.parse("Click to discard your input."),
                                        100,
                                        DialogAction.staticAction(ClickEvent.callback(_ -> {
                                            callback.accept(false);
                                        })))
                        ))
        );

        player.showDialog(dialog);
    }

    /**
     * Code logic for the {@link #getInput(Component, Player, String, Component, String, Consumer)}, when the value is confirmed
     * @param event The event
     */
    @EventHandler
    public void onDialogClick(PlayerCustomClickEvent event) {
        Key id = event.getIdentifier();
        String path = id.value();

        if (!id.namespace().equals("chamoitemskins")) return;

        boolean isConfirm = path.endsWith("/confirm");

        if (!isConfirm) return;

        if (!(event.getCommonConnection() instanceof PlayerGameConnection conn)) return;
        Player player = conn.getPlayer();

        String key = path.substring(0, path.lastIndexOf('/'));

        Map<String, Consumer<String>> playerPending = pending.get(player.getUniqueId());
        if (playerPending == null) return;

        Consumer<String> callback = playerPending.remove(key);
        if (playerPending.isEmpty()) pending.remove(player.getUniqueId());
        if (callback == null) return;

        DialogResponseView view = event.getDialogResponseView();
        String text = (view != null) ? view.getText(key) : null;
        callback.accept(text);
    }
}