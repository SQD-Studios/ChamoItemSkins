package net.chamosmp.chamoitemskins.gui.editor.skin;

import net.chamosmp.chamoitemskins.ChamoItemSkinsPlugin;
import net.chamosmp.chamoitemskins.api.objects.Category;
import net.chamosmp.chamoitemskins.api.objects.Skin;
import net.chamosmp.chamoitemskins.api.service.SkinService;
import net.chamosmp.chamoitemskins.gui.GuiFillerUtil;
import net.chamosmp.chamoitemskins.gui.GuiMultiPageUtil;
import net.chamosmp.chamoitemskins.gui.editor.EditorGui;
import net.chamosmp.chamoitemskins.listener.GuiListener;
import net.chamosmp.chamoitemskins.manager.CategoryManager;
import net.chamosmp.chamoitemskins.manager.RarityManager;
import net.chamosmp.chamoitemskins.models.ModelService;
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

import java.util.*;

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
    private final Map<Integer, Skin> slotToSkin = new HashMap<>();
    private final CategoryManager categoryManager;
    private final MessageUtil messageUtil;
    private final RarityManager rarityManager;
    private final ChatInputUtil chatInputUtil;

    private final GuiMultiPageUtil<Skin> pagination;

    private static final int PAGE_PRE = 52;
    private static final int PAGE_NEXT = 53;


    public SkinEditorGui(Plugin plugin, Player player, SkinService skinService, ModelService modelService, CategoryManager categoryManager, MessageUtil messageUtil, RarityManager rarityManager, ChatInputUtil chatInputUtil) {
        this.plugin = (ChamoItemSkinsPlugin) plugin;
        this.player = player;
        this.skinService = skinService;
        this.modelService = modelService;
        List<Skin> skins = new ArrayList<>(skinService.getSkins());
        this.categoryManager = categoryManager;
        this.messageUtil = messageUtil;
        this.rarityManager = rarityManager;
        this.chatInputUtil = chatInputUtil;
        this.inventory = Bukkit.createInventory(this, 54, MessageUtil.parse("<aqua><b>Skin Editor"));
        List<Integer> skinSlots = computeSkinSlots(inventory.getSize());

        Set<Integer> reserved = new HashSet<>();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (!skinSlots.contains(slot)) {
                reserved.add(slot);
            }
        }

        this.pagination = new GuiMultiPageUtil<>(
                inventory.getSize(),
                this::isBorderSlot,
                reserved
        );
        pagination.setItems(skins);

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

    private boolean isBorderSlot(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        int totalRows = inventory.getSize() / 9;
        return row == 0 || row == totalRows - 1 || col == 0 || col == 8;
    }

    public void refresh() {
        inventory.clear();
        slotToSkin.clear();

        List<Skin> pageSkins = pagination.getCurrentPageItems();
        List<Integer> available = pagination.getAvailableSlots();

        for (int i = 0; i < pageSkins.size() && i < available.size(); i++) {
            int slot = available.get(i);
            Skin skin = pageSkins.get(i);
            slotToSkin.put(slot, skin);
            inventory.setItem(slot, createSkinIcon(skin));
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

        ItemStack next = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta metaNext = next.getItemMeta();
        if (metaNext != null) {
            metaNext.customName(MessageUtil.parse("<green><b>Next Page"));
            next.setItemMeta(metaNext);
        }
        if (pagination.hasNext()) {
            inventory.setItem(PAGE_NEXT, next);
        }

        ItemStack prev = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta metaPrev = next.getItemMeta();
        if (metaNext != null) {
            metaPrev.customName(MessageUtil.parse("<red><b>Previous Page"));
            prev.setItemMeta(metaPrev);
        }
        if (pagination.hasPrev()) {
            inventory.setItem(PAGE_PRE, prev);
        }

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
        if (slot == PAGE_PRE && pagination.hasPrev()) {
            pagination.prevPage();
            refresh();
        } else if (slot == PAGE_NEXT && pagination.hasNext()) {
            pagination.nextPage();
            refresh();
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
