// --- plugin/src/main/java/net/chamosmp/chamoitemskins/gui/SkinEditorGui.java ---
package net.chamosmp.chamoitemskins.gui.editor;

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
        
        // Center the icons (up to 45 skins)
        int skinCount = skins.size();
        int startIndex = 0;
        if (skinCount < 9) {
            startIndex = (9 - skinCount) / 2;
        }

        for (int i = 0; i < skins.size() && i < 45; i++) {
            Skin skin = skins.get(i);
            int slot = i + startIndex;
            if (slot >= 45) slot = i; // Fallback if too many skins
            inventory.setItem(slot, createSkinIcon(skin));
        }
        
        // Add "New Skin" button at the bottom (centered)
        ItemStack newSkin = new ItemStack(Material.NETHER_STAR);
        var meta = newSkin.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse("<green><bold>Create New Skin"));
            newSkin.setItemMeta(meta);
        }
        inventory.setItem(49, newSkin);
    }

    private ItemStack createSkinIcon(Skin skin) {
        ItemStack item = new ItemStack(skin.displayItem().material());
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse(skin.name()));
            meta.lore(List.of(
                MessageUtil.parse("<gray>ID: <white>" + skin.id()),
                MessageUtil.parse("<gray>Categories: <white>" + String.join(", ", skin.categories())),
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
        
        // Find skin by slot
        int skinCount = skins.size();
        int startIndex = 0;
        if (skinCount < 9) {
            startIndex = (9 - skinCount) / 2;
        }

        int skinIdx = slot - startIndex;
        if (skinIdx >= 0 && skinIdx < skins.size() && slot < 45) {
            Skin skin = skins.get(skinIdx);
            handleSkinDetail(skin);
        }
    }

    private void handleNewSkin() {
        new SkinCreationGui(plugin, player, skinService).open();
    }

    private void handleSkinDetail(Skin skin) {
        new SkinEditDetailGui(plugin, player, skinService, skin).open();
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
