// --- plugin/src/main/java/net/chamosmp/chamoitemskins/gui/editor/SkinEditorGui.java ---
package net.chamosmp.chamoitemskins.gui.editor;

import net.chamosmp.chamoitemskins.api.model.Skin;
import net.chamosmp.chamoitemskins.models.ModelService;
import net.chamosmp.chamoitemskins.api.service.SkinService;
import net.chamosmp.chamoitemskins.gui.GuiFillerUtil;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GUI for editing skins.
 */
public final class SkinEditorGui implements GuiListener.ChamoGui {
    private static final int NEW_SKIN_SLOT = 49;

    private final Plugin plugin;
    private final Player player;
    private final SkinService skinService;
    private final ModelService modelService;
    private final Inventory inventory;
    private final List<Skin> skins;
    private final List<Integer> skinSlots;
    private final Map<Integer, Skin> slotToSkin = new HashMap<>();

    public SkinEditorGui(Plugin plugin, Player player, SkinService skinService, ModelService modelService) {
        this.plugin = plugin;
        this.player = player;
        this.skinService = skinService;
        this.modelService = modelService;
        this.skins = new ArrayList<>(skinService.getSkins());
        this.inventory = Bukkit.createInventory(this, 54, MessageUtil.parse("<gold>Skin Editor"));
        this.skinSlots = computeSkinSlots(inventory.getSize());

        refresh();
    }

    /**
     * Row 2+ slots 1–8 (cols 0–7), skipping the rightmost column of each row for nav buttons.
     */
    static @NotNull List<Integer> computeSkinSlots(int inventorySize) {
        List<Integer> slots = new ArrayList<>();
        int rows = inventorySize / 9;
        for (int row = 1; row < rows - 1; row++) {
            for (int col = 1; col < 8; col++) { // skip col 0 and col 8 (border columns)
                slots.add(row * 9 + col);
            }
        }
        return slots;
    }

    public void refresh() {
        inventory.clear();
        slotToSkin.clear();

        int placed = 0;
        for (Skin skin : skins) {
            if (placed >= skinSlots.size()) break;
            //if (isBorderSlot(placed)) break;
            int slot = skinSlots.get(placed);
            slotToSkin.put(slot, skin);
            inventory.setItem(slot, createSkinIcon(skin));
            placed++;
        }

        ItemStack newSkin = new ItemStack(Material.NETHER_STAR);
        var meta = newSkin.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse("<green><bold>Create New Skin"));
            newSkin.setItemMeta(meta);
        }
        inventory.setItem(NEW_SKIN_SLOT, newSkin);
        GuiFillerUtil.apply(plugin, inventory, player);
    }

    private ItemStack createSkinIcon(Skin skin) {
        ItemStack item = modelService.createPreviewItem(skin);
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
        if (slot == NEW_SKIN_SLOT) {
            new SkinCreationGui(plugin, player, skinService).open();
            return;
        }

        Skin skin = slotToSkin.get(slot);
        if (skin != null) {
            new SkinEditDetailGui(plugin, player, skinService, skin).open();
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
