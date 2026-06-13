// --- plugin/src/main/java/net/chamosmp/chamoitemskins/listener/NoteListener.java ---
package net.chamosmp.chamoitemskins.listener;

import net.chamosmp.chamoitemskins.api.model.Skin;
import net.chamosmp.chamoitemskins.api.service.GrantService;
import net.chamosmp.chamoitemskins.api.service.SkinService;
import net.chamosmp.chamoitemskins.util.MessageUtil;
import net.chamosmp.chamoitemskins.util.NoteUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Handles skin note consumption.
 */
public final class NoteListener implements Listener {
    private final Plugin plugin;
    private final SkinService skinService;
    private final GrantService grantService;
    private final FileConfiguration config;

    public NoteListener(Plugin plugin, SkinService skinService, GrantService grantService, FileConfiguration config) {
        this.plugin = plugin;
        this.skinService = skinService;
        this.grantService = grantService;
        this.config = config;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        ItemStack item = event.getItem();
        if (item == null || !NoteUtil.isNote(item)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        String skinId = NoteUtil.getSkinId(item);
        
        skinService.getSkin(skinId).ifPresent(skin -> {
            grantService.hasSkin(player.getUniqueId(), skinId).thenAccept(has -> {
                if (has) {
                    MessageUtil.sendMessage(player, config.getString("messages.already-owned", "<red>You already own this skin!"));
                    return;
                }

                grantService.grantSkin(player.getUniqueId(), skinId, "NOTE").thenRun(() -> {
                    item.setAmount(item.getAmount() - 1);
                    MessageUtil.sendMessage(player, config.getString("messages.grant-received", "<green>✔ You unlocked <white>{skin_name}<green>!"),
                            Map.of("skin_name", skin.name()));
                }).exceptionally(ex -> {
                    player.sendMessage("Failed to grant skin: " + ex.getMessage());
                    return null;
                });
            });
        });
    }
}
