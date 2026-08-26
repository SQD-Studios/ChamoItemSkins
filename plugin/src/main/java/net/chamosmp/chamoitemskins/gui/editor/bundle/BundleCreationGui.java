package net.chamosmp.chamoitemskins.gui.editor.bundle;

import net.chamosmp.chamoitemskins.ChamoItemSkinsPlugin;
import net.chamosmp.chamoitemskins.api.objects.Skin;
import net.chamosmp.chamoitemskins.api.objects.SkinBundle;
import net.chamosmp.chamoitemskins.api.service.SkinService;
import net.chamosmp.chamoitemskins.gui.GuiFillerUtil;
import net.chamosmp.chamoitemskins.gui.editor.AdministratorSkinSelectionGui;
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
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI for creating a new skin.
 */
public final class BundleCreationGui implements GuiListener.ChamoGui {
    private final Plugin plugin;
    private final Player player;
    private final SkinService skinService;
    private final RarityManager rarityManager;
    private final MessageUtil messageUtil;
    private final ModelService modelService;
    private final CategoryManager categoryManager;
    private final ChatInputUtil chatInputUtil;

    private String id = "new_bundle";
    private String name = "New Bundle";

    private List<String> skins = new ArrayList<>();

    private final Inventory inventory;


    private final List<Skin> ALL_SKINS = new ArrayList<>();

    public BundleCreationGui(Plugin plugin, Player player, SkinService skinService, MessageUtil messageUtil, ModelService modelService, CategoryManager categoryManager, RarityManager rarityManager, ChatInputUtil chatInputUtil) {
        this.plugin = plugin;
        this.player = player;
        this.skinService = skinService;
        this.rarityManager = rarityManager;
        this.messageUtil = messageUtil;
        this.modelService = modelService;
        this.categoryManager = categoryManager;
        ALL_SKINS.addAll(skinService.getSkins());
        this.chatInputUtil = chatInputUtil;
        this.inventory = Bukkit.createInventory(this, 27, MessageUtil.parse("<yellow><b>Create New Bundle"));

        refresh();
    }

    public void refresh() {
        inventory.clear();

        // ID & Name
        inventory.setItem(10, createInfoItem(Material.NAME_TAG, "<yellow>ID: <white>" + id, "<gray>Click to set ID"));
        inventory.setItem(11, createInfoItem(Material.PAPER, "<yellow>Name: <white>" + name, "<gray>Click to set Name"));
        // Category Cycle Item
        List<String> lore = new ArrayList<>();
        lore.add("<dark_aqua>Click to open the skin selection");
        for (Skin cat : ALL_SKINS) {
            boolean contains = false;
            for (String cat2 : skins) {
                contains = cat2.equalsIgnoreCase(cat.id());
            }
            String prefix = contains ? "     <gray>> <green>" : "         <dark_gray>";
            lore.add("      " + prefix + cat.name());
        }

        inventory.setItem(12, createInfoItem(Material.BOOK, "<aqua><bold>Skins", lore));

        // Create Button (Bottom Right)
        ItemStack create = new ItemStack(Material.GREEN_CONCRETE);
        var meta = create.getItemMeta();
        if (meta != null) {
            meta.customName(MessageUtil.parse("<green><bold>CREATE"));
            create.setItemMeta(meta);
        }
        inventory.setItem(26, create);

        // Back Button
        ItemStack back = new ItemStack(Material.ARROW);
        var backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.customName(MessageUtil.parse("<gray>Back to Editor"));
            back.setItemMeta(backMeta);
        }
        inventory.setItem(25, back);
        GuiFillerUtil.apply(plugin, inventory, player);
    }


    private int categoryCycleIndex = 0;

    private ItemStack createInfoItem(Material mat, String name, String lore) {
        return createInfoItem(mat, name, List.of(lore));
    }

    private ItemStack createInfoItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.customName(MessageUtil.parse(name));
            meta.lore(lore.stream().map(MessageUtil::parse).toList());
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
        if (slot == 10) {
            chatInputUtil.getInput(player, MessageUtil.parse("<yellow>Enter skin ID:"), input -> {
                if (input == null) {
                    open();
                    return;
                }
                this.id = input.toLowerCase().replace(" ", "_");
                open();
                refresh();
            }, "editorcreateskinid", MessageUtil.parse("<aqua>Create Skin"));
        } else if (slot == 11) {
            chatInputUtil.getInput(player, MessageUtil.parse("<yellow>Enter skin Name:"), input -> {
                if (input == null) {
                    open();
                    return;
                }
                this.name = input;
                open();
                refresh();
            }, "editorcreateskinname", MessageUtil.parse("<aqua>Create Skin"));
        } else if (slot == 12) {
            new AdministratorSkinSelectionGui((ChamoItemSkinsPlugin) plugin, player, skinService, modelService).open(skins -> {
                List<String> skinIds = new ArrayList<>();
                for (Skin skin : skins) {
                    skinIds.add(skin.id());
                }
                this.skins = skinIds;
                open();
            }, null);
        } else if (slot == 25) {
            new BundleEditorGui(plugin, player, skinService, modelService, categoryManager, messageUtil, rarityManager, chatInputUtil).open(null);
        } else if (slot == 26) {
            SkinBundle bundle = new SkinBundle(id, name, skins);
            skinService.saveBundle(bundle);
            messageUtil.sendLangMessage(player, "<green>Bundle created!");
            new BundleEditorGui(plugin, player, skinService, modelService, categoryManager, messageUtil, rarityManager, chatInputUtil).open(null);
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
