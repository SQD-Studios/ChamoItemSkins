// --- api/src/main/java/net/chamosmp/chamoitemskins/api/event/SkinGrantEvent.java ---
package net.chamosmp.chamoitemskins.api.event;

import net.chamosmp.chamoitemskins.api.model.Skin;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player is about to be granted a skin (e.g., via note or admin).
 */
public class SkinGrantEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Skin skin;
    private final String source;
    private boolean cancelled;

    public SkinGrantEvent(@NotNull Player player, @NotNull Skin skin, @NotNull String source) {
        super(player);
        this.skin = skin;
        this.source = source;
    }

    /**
     * Gets the skin being granted.
     * @return The skin.
     */
    public @NotNull Skin getSkin() {
        return skin;
    }

    /**
     * Gets the source of the grant.
     * @return The source.
     */
    public @NotNull String getSource() {
        return source;
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
