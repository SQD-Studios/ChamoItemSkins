// --- api/src/main/java/net/chamosmp/chamoitemskins/api/event/SkinRevokeEvent.java ---
package net.chamosmp.chamoitemskins.api.event;

import net.chamosmp.chamoitemskins.api.model.Skin;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player's skin is about to be revoked.
 */
public class SkinRevokeEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Skin skin;
    private boolean cancelled;

    public SkinRevokeEvent(@NotNull Player player, @NotNull Skin skin) {
        super(player);
        this.skin = skin;
    }

    /**
     * Gets the skin being revoked.
     * @return The skin.
     */
    public @NotNull Skin getSkin() {
        return skin;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
