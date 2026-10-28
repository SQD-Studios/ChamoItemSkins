package net.chamosmp.chamoitemskins.gui.admin;

import com.sqdstudios.sqdlib.paper.chamogui.GuiFillerUtil;
import com.sqdstudios.sqdlib.paper.chamogui.config.GuiSlot;
import com.sqdstudios.sqdlib.paper.chamogui.config.SlotType;
import com.sqdstudios.sqdlib.paper.chamogui.listener.GuiListener;
import com.sqdstudios.sqdlib.paper.dialog.SimpleDialog;
import com.sqdstudios.sqdlib.paper.util.ColorUtil;
import com.sqdstudios.sqdlib.paper.util.SchedulerUtil;
import net.chamosmp.chamoitemskins.ChamoItemSkinsPlugin;
import net.chamosmp.chamoitemskins.api.service.SkinService;
import net.chamosmp.chamoitemskins.gui.editor.EditorGui;
import net.chamosmp.chamoitemskins.manager.CategoryManager;
import net.chamosmp.chamoitemskins.manager.RarityManager;
import net.chamosmp.chamoitemskins.models.ModelService;
import net.chamosmp.chamoitemskins.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Admin control panel GUI.
 */
public final class AdminGui implements GuiListener.ChamoGui {
    private final ChamoItemSkinsPlugin plugin;
    private final Player player;
    private final Inventory inventory;
    private final List<GuiSlot> slots;
    private final MessageUtil messageUtil;
    private final CategoryManager categoryManager;
    private final ModelService modelService;
    private final RarityManager rarityManager;
    private final SimpleDialog chatInputUtil;
    private final SkinService skinService;

    public AdminGui(ChamoItemSkinsPlugin plugin, Player player, String title, int size, List<GuiSlot> slots, MessageUtil messageUtil, CategoryManager categoryManager, ModelService modelService, RarityManager rarityManager, SimpleDialog chatInputUtil, SkinService skinService) {
        this.plugin = plugin;
        this.player = player;
        this.slots = slots;
        this.messageUtil = messageUtil;
        this.categoryManager = categoryManager;
        this.modelService = modelService;
        this.rarityManager = rarityManager;
        this.chatInputUtil = chatInputUtil;
        this.skinService = skinService;
        this.inventory = Bukkit.createInventory(this, size, ColorUtil.parse(player, title, Map.of()));

        setupInventory();
    }

    private void setupInventory() {
        for (GuiSlot def : slots) {
            ItemStack item = new ItemStack(def.material());
            var meta = item.getItemMeta();
            if (meta != null) {
                meta.customName(ColorUtil.parse(player, def.name(), Map.of()));
                meta.lore(def.lore().stream().map(l -> ColorUtil.parse(player, l, Map.of())).toList());
                item.setItemMeta(meta);
            }
            inventory.setItem(def.slot(), item);
        }
        GuiFillerUtil.apply(plugin, inventory, player);
    }

    public void open() {
        SchedulerUtil.runForEntity(plugin, player, () -> player.openInventory(inventory), () -> {
        });
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        slots.stream().filter(s -> s.slot() == slot).findFirst().ifPresent(def -> {
            if (def.type() instanceof SlotType.ActionSlot(String action)) {
                handleAction(action);
            }
        });
    }

    private void handleAction(String action) {
        switch (action) {
            case "RELOAD" -> SchedulerUtil.runSync(plugin, () -> {
                plugin.reloadConfiguration();
                skinService.reloadSkins();
                messageUtil.sendLangMessage(player, "reload-success");
            });
            case "EDITOR" ->
                    new EditorGui(plugin, player, messageUtil, categoryManager, modelService, rarityManager, chatInputUtil).open();
            default -> player.sendRichMessage("Action " + action + " not implemented yet.");
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
