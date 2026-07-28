package net.chamosmp.chamoitemskins.gui.editor;

import net.chamosmp.chamoitemskins.ChamoItemSkinsPlugin;
import net.chamosmp.chamoitemskins.api.objects.Skin;
import net.chamosmp.chamoitemskins.api.service.SkinService;
import net.chamosmp.chamoitemskins.gui.GuiFillerUtil;
import net.chamosmp.chamoitemskins.listener.GuiListener;
import net.chamosmp.chamoitemskins.models.ModelService;
import net.chamosmp.chamoitemskins.scheduler.SchedulerUtil;
import net.chamosmp.chamoitemskins.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class AdministratorSkinSelectionGui implements GuiListener.ChamoGui, Listener {

    private final ChamoItemSkinsPlugin plugin;
    private final Player player;
    private final SkinService skinService;
    private final ModelService modelService;

    private final Map<Integer, Skin> slotToSkin = new HashMap<>();
    private final List<Integer> skinSlots;
    private final List<Skin> skins;

    private final List<Skin> selectedSkinList = new ArrayList<>();

    private Map<UUID, Consumer<List<Skin>>> pending = new ConcurrentHashMap<>();

    private final Inventory inventory;

    public AdministratorSkinSelectionGui(ChamoItemSkinsPlugin plugin, Player player, SkinService skinService, ModelService modelService) {
        this.plugin = plugin;
        this.player = player;
        this.skinService = skinService;
        this.modelService = modelService;
        this.skins = new ArrayList<>(skinService.getSkins());
        this.inventory = Bukkit.createInventory(this, 54, MessageUtil.parse("<aqua><b>Select as skin"));
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
            int slot = skinSlots.get(placed);
            slotToSkin.put(slot, skin);
            boolean isSelected = false;
            for (Skin selectedSkin : selectedSkinList) {
                if (selectedSkin.equals(skin)) {
                    isSelected = true;
                    break;
                }
            }
            inventory.setItem(slot, createSkinIcon(skin, isSelected));
            placed++;
        }
        ItemStack itemStack = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            itemMeta.customName(MessageUtil.parse("<green><b>Confirm"));
            itemStack.setItemMeta(itemMeta);
        }
        inventory.setItem(53, itemStack);

        GuiFillerUtil.apply(plugin, inventory, player);
    }

    public void open(Consumer<List<Skin>> consumer, List<Skin> currentSkins) {
        if (consumer != null) {
            pending.put(player.getUniqueId(), consumer);
        }
        if (currentSkins != null && !currentSkins.isEmpty()) {
            selectedSkinList.addAll(currentSkins);
        }
        SchedulerUtil.runForEntity(plugin, player, () -> {
            player.openInventory(inventory);
        }, () -> {
        });
        refresh();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        Skin skin = slotToSkin.get(slot);
        if (skin != null) {
            if (!selectedSkinList.contains(skin)) {
                selectedSkinList.add(skin);
            } else {
                selectedSkinList.remove(skin);
            }
            refresh();
        }

        if (slot == 53) {
            UUID uuid = event.getWhoClicked().getUniqueId();
            if (pending.containsKey(uuid)) {
                pending.get(uuid).accept(selectedSkinList);
                pending.remove(uuid);
            }
        }
    }

    private ItemStack createSkinIcon(Skin skin, boolean isSelected) {

        ItemStack item = modelService.createPreviewItem(skin);
        var meta = item.getItemMeta();
        if (meta != null) {
            if (isSelected) {
                meta.customName(MessageUtil.parse("<dark_gray>[Selected] " + skin.name()));
            } else {
                meta.customName(MessageUtil.parse(skin.name()));
            }
            item.setItemMeta(meta);
        }
        return item;
    }



    @EventHandler
    public void handleClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        if (inv != this.inventory) {
            return;
        }
        UUID uuid = event.getPlayer().getUniqueId();
        pending.remove(uuid);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
