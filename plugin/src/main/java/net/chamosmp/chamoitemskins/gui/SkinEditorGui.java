// --- plugin/src/main/java/net/chamosmp/chamoitemskins/gui/SkinEditorGui.java ---
package net.chamosmp.chamoitemskins.gui;

import net.chamosmp.chamoitemskins.api.model.Skin;
import net.chamosmp.chamoitemskins.api.service.SkinService;
import net.chamosmp.chamoitemskins.listener.GuiListener;
import net.chamosmp.chamoitemskins.scheduler.SchedulerUtil;
import net.chamosmp.chamoitemskins.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GUI for editing skins.
 */
public final class SkinEditorGui implements GuiListener.ChamoGui {
    private final Plugin plugin;
    private final Player player;
    private final SkinService skinService;
    private final Inventory inventory;
    private final List<Skin> skins;

    public SkinEditorGui(Plugin plugin, Player player, SkinService skinService) {
        this.plugin = plugin;
        this.player = player;
        this.skinService = skinService;
        this.skins = new ArrayList<>(skinService.getSkins());
        this.inventory = Bukkit.createInventory(this, 54, MessageUtil.parse("<gold>Skin Editor"));
        
        refresh();
    }

    public void refresh() {
        inventory.clear();
        for (int i = 0; i < skins.size() && i < 45; i++) {
            Skin skin = skins.get(i);
            inventory.setItem(i, createSkinIcon(skin));
        }
        
        // Add "New Skin" button at the bottom
        ItemStack newSkin = new ItemStack(Material.NETHER_STAR);
        var meta = newSkin.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse("<green><bold>Create New Skin"));
            newSkin.setItemMeta(meta);
        }
        inventory.setItem(49, newSkin);
    }

    private ItemStack createSkinIcon(Skin skin) {
        ItemStack item = new ItemStack(skin.itemType());
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse(skin.name()));
            meta.lore(List.of(
                MessageUtil.parse("<gray>ID: <white>" + skin.id()),
                MessageUtil.parse("<gray>Type: <white>" + skin.itemType().name()),
                MessageUtil.parse("<gray>Enabled: " + (skin.enabled() ? "<green>Yes" : "<red>No"))
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    public void open() {
        SchedulerUtil.runForEntity(plugin, player, () -> player.openInventory(inventory), () -> {});
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 49) {
            // New Skin flow
            handleNewSkin();
            return;
        }
        
        if (slot < skins.size()) {
            Skin skin = skins.get(slot);
            handleSkinDetail(skin);
        }
    }

    private void handleNewSkin() {
        player.closeInventory();
        player.sendMessage(MessageUtil.parse("<yellow>Entering New Skin creation flow..."));
        // In a real plugin, this would use AnvilGUI or SignGUI for input.
        // For this task, we will simulate the creation of a skin with default values.
        player.sendMessage(MessageUtil.parse("<gray>Please use <white>/skinsadmin create <name> <model-id> <item-type> <gray>to create a skin."));
    }

    private void handleSkinDetail(Skin skin) {
        // Open Detail GUI
        player.sendMessage(MessageUtil.parse("<yellow>Editing skin: " + skin.id()));
        // Flip enabled status as an example of live editing
        Skin updated = new Skin(skin.id(), skin.name(), skin.modelId(), skin.itemType(), !skin.enabled(), skin.noteMaterial(), skin.displayItem());
        net.chamosmp.chamoitemskins.util.YamlUtil.saveSkin(plugin, updated);
        MessageUtil.sendMessage(player, plugin.getConfig().getString("messages.editor-saved", "<green>Skin <white>{skin_name}<green> saved."), Map.of("skin_name", skin.name()));
        SchedulerUtil.runSync(plugin, () -> {
            skinService.getSkins(); // Force reload if manager doesn't watch file
            refresh();
        });
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
