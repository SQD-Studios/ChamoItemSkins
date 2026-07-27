package net.chamosmp.chamoitemskins.gui.editor;

import net.chamosmp.chamoitemskins.ChamoItemSkinsPlugin;
import net.chamosmp.chamoitemskins.gui.GuiFillerUtil;
import net.chamosmp.chamoitemskins.gui.admin.AdminGui;
import net.chamosmp.chamoitemskins.gui.editor.skin.SkinEditorGui;
import net.chamosmp.chamoitemskins.listener.GuiListener;
import net.chamosmp.chamoitemskins.manager.CategoryManager;
import net.chamosmp.chamoitemskins.manager.RarityManager;
import net.chamosmp.chamoitemskins.models.ModelService;
import net.chamosmp.chamoitemskins.scheduler.SchedulerUtil;
import net.chamosmp.chamoitemskins.util.ChatInputUtil;
import net.chamosmp.chamoitemskins.util.MessageUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class EditorGui implements GuiListener.ChamoGui {

    private static final int BACK_SLOT = 18;

    private final ChamoItemSkinsPlugin plugin;
    private final Player player;
    private final Inventory inventory;

    private final MessageUtil messageUtil;
    private final CategoryManager categoryManager;
    private final ModelService modelService;
    private final RarityManager rarityManager;
    private final ChatInputUtil chatInputUtil;

    public EditorGui(Plugin plugin, Player player, MessageUtil messageUtil, CategoryManager categoryManager, ModelService modelService, RarityManager rarityManager, ChatInputUtil chatInputUtil) {
        this.plugin = (ChamoItemSkinsPlugin) plugin;
        this.player = player;
        this.messageUtil = messageUtil;
        this.categoryManager = categoryManager;
        this.modelService = modelService;
        this.rarityManager = rarityManager;
        this.chatInputUtil = chatInputUtil;
        this.inventory = Bukkit.createInventory(this, 27, MessageUtil.parse("<dark_aqua>Editor"));

        refresh();
    }

    public void refresh() {
        inventory.clear();

        ItemStack back = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta metaBack = back.getItemMeta();
        if (metaBack != null) {
            metaBack.displayName(MessageUtil.parse("<red><b>Go Back"));
            back.setItemMeta(metaBack);
        }
        inventory.setItem(BACK_SLOT, back);

        ItemStack skinEditor = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta metaSkinEditor = skinEditor.getItemMeta();
        if (metaSkinEditor != null) {
            metaSkinEditor.displayName(MessageUtil.parse("<aqua><b>Skin Editor"));
            skinEditor.setItemMeta(metaSkinEditor);
        }
        inventory.setItem(11, skinEditor);

        ItemStack bundleEditor = new ItemStack(Material.BLUE_BUNDLE);
        ItemMeta metaBundleEditor = bundleEditor.getItemMeta();
        if (metaBundleEditor != null) {
            metaBundleEditor.displayName(MessageUtil.parse("<yellow><b>Bundle Editor"));
            bundleEditor.setItemMeta(metaBundleEditor);
        }
        inventory.setItem(15, bundleEditor);

        GuiFillerUtil.apply(plugin, inventory, player);
    }

    public void open() {
        SchedulerUtil.runForEntity(plugin, player, () -> player.openInventory(inventory), () -> {
        });
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == BACK_SLOT) {
            new AdminGui(plugin, player, plugin.adminGuiTitle(), plugin.adminSize(), plugin.adminSlots(), messageUtil, categoryManager, modelService, rarityManager, chatInputUtil).open();
        }
        if (slot == 13) {
            new SkinEditorGui(plugin, player, plugin.getSkinService(), modelService, categoryManager, messageUtil, rarityManager, chatInputUtil).open();
        }
        if (slot == 15) {
            player.closeInventory();
            player.showTitle(Title.title(MessageUtil.parse("<dark_aqua>Not implemented, lol"), MessageUtil.parse("You have to wait (Hard isn't it)")));
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

}