package net.chamosmp.chamoitemskins.listener;

import net.chamosmp.chamoitemskins.manager.GrantManager;
import net.chamosmp.chamoitemskins.models.ModelService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Keeps equipped skin models in sync with the player's inventory.
 */
public final class SkinApplyListener implements Listener {
    private final GrantManager grantManager;
    private final Plugin plugin;
    private final ModelService modelService;

    public SkinApplyListener(@NotNull GrantManager grantManager, Plugin plugin, ModelService modelService) {
        this.grantManager = grantManager;
        this.plugin = plugin;
        this.modelService = modelService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        grantManager.refreshPlayerSkins(event.getPlayer());
    }
}
