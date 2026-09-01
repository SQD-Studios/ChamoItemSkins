package net.chamosmp.chamoitemskins.util;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Consumer;

/**
 * Wrapper class of {@link DialogUtil}
 *
 * This is kept for backwards combatability in every part of the plugin
 *
 * @apiNote Use {@link DialogUtil} directly instead of this
 */
@ApiStatus.Obsolete
public final class ChatInputUtil {
    private final DialogUtil dialogUtil;

    public ChatInputUtil(DialogUtil dialogUtil) {
        this.dialogUtil = dialogUtil;
    }

    /**
     * "The util means, the dialog/chat thingy, not to be confused"
     *
     * @param player       The player to open the util to
     * @param prompt       The prompt to give the player
     * @param callback     Callback is the piece of code to be executed
     * @param key          The key, the buttons/dialog should have
     * @param title        The title in the dialog
     * @param defaultValue The value the text prompt should have, useful for editing already defined configs.
     */
    public void getInput(Player player, Component prompt, Consumer<String> callback, String key, Component title, String defaultValue) {
        dialogUtil.getInput(title, player, key, prompt, defaultValue, callback);
    }

    /**
     * "The util means, the dialog/chat thingy, not to be confused"
     *
     * @param player   The player to open the util to
     * @param prompt   The prompt to give the player
     * @param callback Callback is the piece of code to be executed
     * @param key      The key, the buttons/dialog should have
     * @param title    The title in the dialog
     */
    public void getInput(Player player, Component prompt, Consumer<String> callback, String key, Component title) {
        getInput(player, prompt, callback, key, title, null);
    }

    /**
     * The chat thingy doesn't really work. It works only on the dialogs
     *
     * @param player   The player to open the util to
     * @param callback Callback is the piece of code to be executed
     * @param title    The title in the dialog
     */
    public void getYesNo(Player player, Consumer<Boolean> callback, Component title) {
        dialogUtil.getYesNo(title, player, callback);
    }
}