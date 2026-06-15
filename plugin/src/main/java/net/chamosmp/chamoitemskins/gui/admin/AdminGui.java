// --- plugin/src/main/java/net/chamosmp/chamoitemskins/gui/AdminGui.java ---
package net.chamosmp.chamoitemskins.gui.admin;

import net.chamosmp.chamoitemskins.api.service.GrantService;
import net.chamosmp.chamoitemskins.api.service.SkinService;
import net.chamosmp.chamoitemskins.gui.GuiFillerUtil;
import net.chamosmp.chamoitemskins.gui.config.GuiSlotDef;
import net.chamosmp.chamoitemskins.gui.config.SlotType;
import net.chamosmp.chamoitemskins.gui.editor.SkinEditorGui;
import net.chamosmp.chamoitemskins.listener.GuiListener;
import net.chamosmp.chamoitemskins.scheduler.SchedulerUtil;
import net.chamosmp.chamoitemskins.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Admin control panel GUI.
 */
public final class AdminGui implements GuiListener.ChamoGui {
    private final Plugin plugin;
    private final Player player;
    private final Inventory inventory;
    private final List<GuiSlotDef> slots;

    public AdminGui(Plugin plugin, Player player, String title, int size, List<GuiSlotDef> slots) {
        this.plugin = plugin;
        this.player = player;
        this.slots = slots;
        this.inventory = Bukkit.createInventory(this, size, MessageUtil.parse(player, title, Map.of()));
        
        setupInventory();
    }

    private void setupInventory() {
        for (GuiSlotDef def : slots) {
            ItemStack item = new ItemStack(def.material());
            var meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(MessageUtil.parse(player, def.name(), Map.of()));
                meta.lore(def.lore().stream().map(l -> MessageUtil.parse(player, l, Map.of())).toList());
                item.setItemMeta(meta);
            }
            inventory.setItem(def.slot(), item);
        }
        GuiFillerUtil.apply(plugin, inventory, player);
    }

    public void open() {
        SchedulerUtil.runForEntity(plugin, player, () -> player.openInventory(inventory), () -> {});
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        slots.stream().filter(s -> s.slot() == slot).findFirst().ifPresent(def -> {
            if (def.type() instanceof SlotType.ActionSlot action) {
                handleAction(action.action());
            }
        });
    }

    private void handleAction(String action) {
        switch (action) {
            case "RELOAD" -> {
                SchedulerUtil.runSync(plugin, () -> {
                    if (plugin instanceof net.chamosmp.chamoitemskins.ChamoItemSkinsPlugin chamoPlugin) {
                        chamoPlugin.reloadPlugin();
                    }
                    MessageUtil.sendMessage(player, plugin.getConfig().getString("messages.reload-success", "<green>ChamoItemSkins reloaded."));
                    player.closeInventory();
                });
            }
            case "EDITOR" -> {
                net.chamosmp.chamoitemskins.api.service.SkinService service = Bukkit.getServicesManager().load(net.chamosmp.chamoitemskins.api.service.SkinService.class);
                if (service == null) {
                    service = net.chamosmp.chamoitemskins.api.ChamoItemSkinsApi.get().getSkinService();
                }
                new SkinEditorGui(plugin, player, service, ((net.chamosmp.chamoitemskins.ChamoItemSkinsPlugin) plugin).getBetterModelService()).open();
            }
            case "GIVE" -> {
                ((net.chamosmp.chamoitemskins.ChamoItemSkinsPlugin) plugin).getChatInputUtil().getInput(player, "<yellow>Enter skin ID to GIVE note:", skinId -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "skinsadmin give " + player.getName() + " " + skinId);
                    open();
                });
            }
            case "GRANT" -> {
                ((net.chamosmp.chamoitemskins.ChamoItemSkinsPlugin) plugin).getChatInputUtil().getInput(player, "<yellow>Enter skin ID to GRANT access:", skinId -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "skinsadmin access give " + player.getName() + " " + skinId);
                    open();
                });
            }
            case "REVOKE" -> {
                ((net.chamosmp.chamoitemskins.ChamoItemSkinsPlugin) plugin).getChatInputUtil().getInput(player, "<yellow>Enter skin ID to REVOKE access:", skinId -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "skinsadmin access revoke " + player.getName() + " " + skinId);
                    open();
                });
            }
            default -> player.sendMessage("Action " + action + " not implemented yet.");
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
