package net.chamosmp.chamoitemskins.gui.editor.skin;

import net.chamosmp.chamoitemskins.ChamoItemSkinsPlugin;
import net.chamosmp.chamoitemskins.api.objects.Category;
import net.chamosmp.chamoitemskins.api.objects.Skin;
import net.chamosmp.chamoitemskins.gui.admin.AdminGui;
import net.chamosmp.chamoitemskins.gui.editor.EditorGui;
import net.chamosmp.chamoitemskins.manager.CategoryManager;
import net.chamosmp.chamoitemskins.manager.RarityManager;
import net.chamosmp.chamoitemskins.models.ModelService;
import net.chamosmp.chamoitemskins.api.service.SkinService;
import net.chamosmp.chamoitemskins.gui.GuiFillerUtil;
import net.chamosmp.chamoitemskins.listener.GuiListener;
import net.chamosmp.chamoitemskins.scheduler.SchedulerUtil;
import net.chamosmp.chamoitemskins.util.ChatInputUtil;
import net.chamosmp.chamoitemskins.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
    private static final int NEW_SKIN_SLOT = 46;
    private static final int BACK_SLOT = 45;

    private final ChamoItemSkinsPlugin plugin;
    private final Player player;
    private final SkinService skinService;
    private final ModelService modelService;
    private final Inventory inventory;
    private final List<Skin> skins;
    private final List<Integer> skinSlots;
    private final Map<Integer, Skin> slotToSkin = new HashMap<>();
    private final CategoryManager categoryManager;
    private final MessageUtil messageUtil;
    private final RarityManager rarityManager;
    private final ChatInputUtil chatInputUtil;

    public SkinEditorGui(Plugin plugin, Player player, SkinService skinService, ModelService modelService, CategoryManager categoryManager, MessageUtil messageUtil, RarityManager rarityManager, ChatInputUtil chatInputUtil) {
        this.plugin = (ChamoItemSkinsPlugin) plugin;
        this.player = player;
        this.skinService = skinService;
        this.modelService = modelService;
        this.skins = new ArrayList<>(skinService.getSkins());
        this.categoryManager = categoryManager;
        this.messageUtil = messageUtil;
        this.rarityManager = rarityManager;
        this.chatInputUtil = chatInputUtil;
        this.inventory = Bukkit.createInventory(this, 54, MessageUtil.parse("<aqua><b>Skin Editor"));
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

        ItemStack newSkin = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        var meta = newSkin.getItemMeta();
        if (meta != null) {
            meta.customName(MessageUtil.parse("<green><bold>Create New Skin"));
            newSkin.setItemMeta(meta);
        }
        inventory.setItem(NEW_SKIN_SLOT, newSkin);

        ItemStack back = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta metaBack = back.getItemMeta();
        if (metaBack != null) {
            metaBack.customName(MessageUtil.parse("<red><b>Go Back"));
            back.setItemMeta(metaBack);
        }
        inventory.setItem(BACK_SLOT, back);
        GuiFillerUtil.apply(plugin, inventory, player);
    }

    private ItemStack createSkinIcon(Skin skin) {
        ItemStack item = modelService.createPreviewItem(skin);
        var meta = item.getItemMeta();
        if (meta != null) {
            List<String> categoryNames = new ArrayList<>();
            for (Category c : skin.categories()) {
                categoryNames.add(c.name());
            }
            meta.customName(MessageUtil.parse(skin.name()));
            meta.lore(List.of(
                    MessageUtil.parse("<gray>ID: <white>" + skin.id()),

                    MessageUtil.parse("<gray>Categories: <white>" + categoryNames),
                    MessageUtil.parse("<gray>Enabled: " + (skin.enabled() ? "<green>Yes" : "<red>No"))
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    public void open(Runnable afterOpen) {
        SchedulerUtil.runForEntity(plugin, player, () -> player.openInventory(inventory), () -> {
        });
        if (afterOpen != null) afterOpen.run();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == NEW_SKIN_SLOT) {
            new SkinCreationGui(plugin, player, skinService, messageUtil, modelService, categoryManager, rarityManager, chatInputUtil).open();
            return;
        }
        if (slot == BACK_SLOT) {
            new EditorGui(plugin, player, messageUtil, categoryManager, modelService, rarityManager, chatInputUtil).open();
        }

        Skin skin = slotToSkin.get(slot);
        if (skin != null) {
            new SkinEditDetailGui(plugin, player, skinService, skin, messageUtil, categoryManager, modelService, rarityManager, chatInputUtil).open();
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
