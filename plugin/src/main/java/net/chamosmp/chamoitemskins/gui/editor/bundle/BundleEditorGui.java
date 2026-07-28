package net.chamosmp.chamoitemskins.gui.editor.bundle;

import net.chamosmp.chamoitemskins.ChamoItemSkinsPlugin;
import net.chamosmp.chamoitemskins.api.objects.Skin;
import net.chamosmp.chamoitemskins.api.objects.SkinBundle;
import net.chamosmp.chamoitemskins.api.service.SkinService;
import net.chamosmp.chamoitemskins.gui.GuiFillerUtil;
import net.chamosmp.chamoitemskins.gui.editor.EditorGui;
import net.chamosmp.chamoitemskins.gui.editor.skin.SkinCreationGui;
import net.chamosmp.chamoitemskins.gui.editor.skin.SkinEditDetailGui;
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
public final class BundleEditorGui implements GuiListener.ChamoGui {
    private static final int NEW_SKIN_SLOT = 46;
    private static final int BACK_SLOT = 45;

    private final ChamoItemSkinsPlugin plugin;
    private final Player player;
    private final SkinService skinService;
    private final ModelService modelService;
    private final Inventory inventory;
    private final List<SkinBundle> skins;
    private final List<Integer> skinSlots;
    private final Map<Integer, SkinBundle> slotToSkin = new HashMap<>();
    private final CategoryManager categoryManager;
    private final MessageUtil messageUtil;
    private final RarityManager rarityManager;
    private final ChatInputUtil chatInputUtil;

    public BundleEditorGui(Plugin plugin, Player player, SkinService skinService, ModelService modelService, CategoryManager categoryManager, MessageUtil messageUtil, RarityManager rarityManager, ChatInputUtil chatInputUtil) {
        skinService.reloadSkins();
        this.plugin = (ChamoItemSkinsPlugin) plugin;
        this.player = player;
        this.skinService = skinService;
        this.modelService = modelService;
        this.skins = new ArrayList<>(skinService.getBundles());
        this.categoryManager = categoryManager;
        this.messageUtil = messageUtil;
        this.rarityManager = rarityManager;
        this.chatInputUtil = chatInputUtil;
        this.inventory = Bukkit.createInventory(this, 54, MessageUtil.parse("<yellow><b>Bundle Editor"));
        this.skinSlots = computeSlots(inventory.getSize());

        refresh();
    }

    /**
     * Row 2+ slots 1–8 (cols 0–7), skipping the rightmost column of each row for nav buttons.
     */
    static @NotNull List<Integer> computeSlots(int inventorySize) {
        List<Integer> slots = new ArrayList<>();
        int rows = inventorySize / 9;
        for (int row = 1; row < rows - 1; row++) {
            for (int col = 1; col < 8; col++) {
                slots.add(row * 9 + col);
            }
        }
        return slots;
    }

    public void refresh() {
        inventory.clear();
        slotToSkin.clear();

        int placed = 0;
        for (SkinBundle skin : skins) {
            if (placed >= skinSlots.size()) break;
            int slot = skinSlots.get(placed);
            slotToSkin.put(slot, skin);
            inventory.setItem(slot, createIcon(skin));
            placed++;
        }

        ItemStack newSkin = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        var meta = newSkin.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse("<green><bold>Create a New Bundle"));
            newSkin.setItemMeta(meta);
        }
        inventory.setItem(NEW_SKIN_SLOT, newSkin);

        ItemStack back = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta metaBack = back.getItemMeta();
        if (metaBack != null) {
            metaBack.displayName(MessageUtil.parse("<red><b>Go Back"));
            back.setItemMeta(metaBack);
        }
        inventory.setItem(BACK_SLOT, back);
        GuiFillerUtil.apply(plugin, inventory, player);
    }

    private ItemStack createIcon(SkinBundle bundle) {
        Optional<Skin> optionalSkin = skinService.getSkin(bundle.skinIds().getFirst());
        Skin displaySkin = null;
        if (optionalSkin.isPresent()) {
            displaySkin = optionalSkin.get();
        }
        ItemStack item;
        if (displaySkin != null) {
            item = modelService.createPreviewItem(displaySkin);
        } else {
            item = new ItemStack(Material.BUNDLE);
        }
        var meta = item.getItemMeta();
        if (meta != null) {
            List<String> skinNames = new ArrayList<>(bundle.skinIds());
            meta.displayName(MessageUtil.parse(bundle.name()));
            meta.lore(List.of(
                    MessageUtil.parse("<gray>ID: <white>" + bundle.id()),

                    MessageUtil.parse("<gray>Skins: <white>" + skinNames)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    public void open() {
        SchedulerUtil.runForEntity(plugin, player, () -> player.openInventory(inventory), () -> {
        });
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == NEW_SKIN_SLOT) {
            new BundleCreationGui(plugin, player, skinService, messageUtil, modelService, categoryManager, rarityManager, chatInputUtil).open();
            return;
        }
        if (slot == BACK_SLOT) {
            new EditorGui(plugin, player, messageUtil, categoryManager, modelService, rarityManager, chatInputUtil).open();
        }

        SkinBundle skin = slotToSkin.get(slot);
        if (skin != null) {
            new BundleEditDetailGui(plugin, player, skinService, skin, messageUtil, categoryManager, modelService, rarityManager, chatInputUtil).open();
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
