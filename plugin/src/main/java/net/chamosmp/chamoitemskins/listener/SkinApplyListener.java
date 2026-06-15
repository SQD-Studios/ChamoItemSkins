// --- plugin/src/main/java/net/chamosmp/chamoitemskins/listener/SkinApplyListener.java ---
package net.chamosmp.chamoitemskins.listener;

import net.chamosmp.chamoitemskins.manager.GrantManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemHeld(@NotNull PlayerItemHeldEvent event) {
        grantManager.refreshPlayerSkins(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        grantManager.refreshPlayerSkins(player);
    }
}
