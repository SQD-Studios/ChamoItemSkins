// --- plugin/src/main/java/net/chamosmp/chamoitemskins/listener/NoteListener.java ---
package net.chamosmp.chamoitemskins.listener;

import net.chamosmp.chamoitemskins.api.model.Skin;
import net.chamosmp.chamoitemskins.api.service.GrantService;
import net.chamosmp.chamoitemskins.api.service.SkinService;
import net.chamosmp.chamoitemskins.scheduler.SchedulerUtil;
import net.chamosmp.chamoitemskins.util.MessageUtil;
import net.chamosmp.chamoitemskins.util.NoteUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static net.chamosmp.chamoitemskins.util.NoteUtil.EXPIRATION_KEY;

/**
 * Handles skin note consumption.
 */
public final class NoteListener implements Listener {
    private static final Logger log = LoggerFactory.getLogger(NoteListener.class);
    private final Plugin plugin;
    private final SkinService skinService;
    private final GrantService grantService;
    private final FileConfiguration config;
    private final MessageUtil messageUtil;

    public NoteListener(Plugin plugin, SkinService skinService, GrantService grantService, FileConfiguration config, MessageUtil messageUtil) {
        this.plugin = plugin;
        this.skinService = skinService;
        this.grantService = grantService;
        this.config = config;
        this.messageUtil = messageUtil;
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
                    messageUtil.sendLangMessage(player, config.getString("messages.already-owned", "<red>You already own this skin!"));
                    return;
                }
                try {
                    grantService.grantSkin(player.getUniqueId(), skinId, "NOTE", item.getPersistentDataContainer().get(EXPIRATION_KEY, PersistentDataType.INTEGER)).thenRun(() -> {
                        // Re-check amount in sync to avoid race conditions as much as possible with item reduction
                        SchedulerUtil.runSync(plugin, () -> {
                            item.setAmount(item.getAmount() - 1);
                            messageUtil.sendLangMessage(player, config.getString("messages.grant-received", "<green>✔ You unlocked <white>{skin_name}<green>!"),
                                    Map.of("skin_name", skin.name()));
                        });
                    }).exceptionally(ex -> {
                        player.sendRichMessage("Failed to grant skin: " + ex.getMessage());
                        return null;
                    });
                } catch (NullPointerException e) {
                    log.error("Exception happened. Failed to unbox the persistent container: ", e);
                }
            }).exceptionally(ex -> {
                if (ex.getMessage() != null && ex.getMessage().contains("closed")) {
                    messageUtil.sendLangMessage(player, "<red>Database is currently busy or reloading. Please try again in a moment.");
                } else {
                    player.sendRichMessage("Failed to check skin ownership: " + ex.getMessage());
                }
                return null;
            });
        });
    }
}
