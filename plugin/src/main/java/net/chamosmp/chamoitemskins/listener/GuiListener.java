// --- plugin/src/main/java/net/chamosmp/chamoitemskins/listener/GuiListener.java ---
package net.chamosmp.chamoitemskins.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Handles GUI interactions by delegating to custom InventoryHolders.
 */
public final class GuiListener implements Listener {

    /**
     * Interface for GUIs that handle their own clicks.
     */
    public interface ChamoGui extends InventoryHolder {
        void handleClick(InventoryClickEvent event);
        default void handleClose(InventoryCloseEvent event) {}
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof ChamoGui gui) {
            event.setCancelled(true);
            gui.handleClick(event);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof ChamoGui gui) {
            gui.handleClose(event);
        }
    }
}
