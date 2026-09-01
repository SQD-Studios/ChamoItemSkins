package net.chamosmp.chamoitemskins.listener;

import net.chamosmp.chamoitemskins.manager.GrantManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Keeps equipped skin models in sync with the player's inventory.
 */
public final class SkinApplyListener implements Listener {
    private final GrantManager grantManager;

    public SkinApplyListener(@NotNull GrantManager grantManager) {
        this.grantManager = grantManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        grantManager.refreshPlayerSkins(event.getPlayer());
    }
}
