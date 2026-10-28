package net.chamosmp.chamoitemskins.gui.editor.bundle;

import com.sqdstudios.sqdlib.paper.chamogui.GuiFillerUtil;
import com.sqdstudios.sqdlib.paper.chamogui.listener.GuiListener;
import com.sqdstudios.sqdlib.paper.dialog.SimpleDialog;
import com.sqdstudios.sqdlib.paper.util.ColorUtil;
import com.sqdstudios.sqdlib.paper.util.SchedulerUtil;
import net.chamosmp.chamoitemskins.ChamoItemSkinsPlugin;
import net.chamosmp.chamoitemskins.api.model.Skin;
import net.chamosmp.chamoitemskins.api.model.SkinBundle;
import net.chamosmp.chamoitemskins.api.service.SkinService;
import net.chamosmp.chamoitemskins.gui.editor.AdministratorSkinSelectionGui;
import net.chamosmp.chamoitemskins.manager.CategoryManager;
import net.chamosmp.chamoitemskins.manager.RarityManager;
import net.chamosmp.chamoitemskins.models.ModelService;
import net.chamosmp.chamoitemskins.util.MessageUtil;
import net.chamosmp.chamoitemskins.util.YamlUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * GUI for editing an existing skin.
 */
public final class BundleEditDetailGui implements GuiListener.ChamoGui {
    private final ChamoItemSkinsPlugin plugin;
    private final Player player;
    private final SkinService skinService;
    private final RarityManager rarityManager;
    private final Inventory inventory;
    private final MessageUtil messageUtil;
    private final CategoryManager categoryManager;
    private final ModelService modelService;
    private final SimpleDialog chatInputUtil;
    private final List<Skin> SKINS;
    private SkinBundle bundle;

    public BundleEditDetailGui(ChamoItemSkinsPlugin plugin, Player player, SkinService skinService, SkinBundle bundle, MessageUtil messageUtil, CategoryManager categoryManager, ModelService modelService, RarityManager rarityManager, SimpleDialog chatInputUtil) {
        this.plugin = plugin;
        this.player = player;
        this.skinService = skinService;
        this.rarityManager = rarityManager;
        this.bundle = bundle;
        this.inventory = Bukkit.createInventory(this, 27, ColorUtil.parse("<yellow><b>Edit Bundle: " + bundle.id()));
        this.messageUtil = messageUtil;
        this.categoryManager = categoryManager;
        this.modelService = modelService;
        this.SKINS = skinService.getSkins().stream().toList();
        this.chatInputUtil = chatInputUtil;
        refresh();
    }


    public void refresh() {
        inventory.clear();

        inventory.setItem(10, createInfoItem(Material.NAME_TAG, "<yellow>Name: <white>" + bundle.name(), "<gray>Click to set Name"));
        inventory.setItem(11, createInfoItem(Material.OAK_SIGN, "<yellow>ID: <white>" + bundle.id(), "<gray>Click to change Skin ID"));
        inventory.setItem(25, createInfoItem(Material.RED_CONCRETE, "<dark_red>Delete", "<red>Click to delete the skin"));

        List<String> lore = new ArrayList<>();
        lore.add("<dark_aqua>Click to open the skin selection");
        for (Skin skins : SKINS) {
            boolean contains = false;
            for (String bundleSkinId : bundle.skinIds()) {
                Optional<Skin> optionalCat = skinService.getSkin(bundleSkinId);
                Skin skinBundle = null;
                if (optionalCat.isPresent()) {
                    skinBundle = optionalCat.get();
                }
                if (skins.equals(skinBundle)) {
                    contains = true;
                }
            }
            String prefix = contains ? "     <gray>> <green>" : "         <dark_gray>";
            lore.add("      " + prefix + skins.name());
        }

        inventory.setItem(12, createInfoItem(Material.BOOK, "<aqua><bold>Skins", lore));

        ItemStack back = new ItemStack(Material.ARROW);
        var meta = back.getItemMeta();
        if (meta != null) {
            meta.customName(ColorUtil.parse("<gray>Back to Editor"));
            back.setItemMeta(meta);
        }
        inventory.setItem(26, back);
        GuiFillerUtil.apply(plugin, inventory, player);
    }

    private ItemStack createInfoItem(Material mat, String name, String lore) {
        return createInfoItem(mat, name, List.of(lore));
    }

    private ItemStack createInfoItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.customName(ColorUtil.parse(name));
            meta.lore(lore.stream().map(ColorUtil::parse).toList());
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
            chatInputUtil.getInput(ColorUtil.parse("<light_purple>" + bundle.id()), player, "editoreditskinnameb", ColorUtil.parse("<yellow>Enter bundle Name:"), bundle.name(),
                    input -> {
                        bundle = new SkinBundle(bundle.id(), input, bundle.skinIds());
                        saveAndRefresh();
                        open();
                    });
        } else if (slot == 11) {
            chatInputUtil.getInput(ColorUtil.parse("<light_purple>" + bundle.id()), player, "editoreditskinidb", ColorUtil.parse("<yellow>Enter NEW bundle ID:"), bundle.id(),
                    input -> {
                        if (input == null) {
                            return;
                        }
                        String oldId = bundle.id();
                        String newId = input.toLowerCase().replace(" ", "_");
                        if (oldId.equals(newId)) {
                            open();
                            return;
                        }

                        if (skinService.getBundle(newId).isPresent()) {
                            messageUtil.sendLangMessage(player, "<red>A bundle with ID " + newId + " already exists!");
                            open();
                            return;
                        }
                        YamlUtil.deleteBundle(plugin, oldId);
                        SkinBundle skinBundle = new SkinBundle(newId, bundle.name(), bundle.skinIds());
                        YamlUtil.saveBundle(plugin, skinBundle);
                        bundle = skinBundle;
                        saveAndRefresh();
                        open();
                    });
        } else if (slot == 12) {
            List<Skin> skins = new ArrayList<>();
            if (bundle.skinIds() != null) {
                for (String id : bundle.skinIds()) {
                    Optional<Skin> optionalSkin = skinService.getSkin(id);
                    optionalSkin.ifPresent(skins::add);
                }
            }
            new AdministratorSkinSelectionGui(plugin, player, skinService, modelService).open(newSkins -> {
                List<String> skinIds = new ArrayList<>();
                for (Skin skin : newSkins) {
                    skinIds.add(skin.id());
                }
                bundle = new SkinBundle(bundle.id(), bundle.name(), skinIds);
                saveAndRefresh();
                open();
            }, skins);
        } else if (slot == 25) {
            chatInputUtil.getYesNo(ColorUtil.parse("Are you sure you want to delete this bundle?"), player, input -> {
                if (input) {
                    skinService.deleteBundle(bundle.id());
                    plugin.reloadPlugin();
                    BundleEditorGui editorGui = new BundleEditorGui(plugin, player, skinService, modelService, categoryManager, messageUtil, rarityManager, chatInputUtil);
                    editorGui.open(() -> {
                        skinService.reloadSkins();
                        editorGui.refresh();
                        editorGui.open(null);
                    });
                }
            });
        } else if (slot == 26) {
            new BundleEditorGui(plugin, player, skinService, modelService, categoryManager, messageUtil, rarityManager, chatInputUtil).open(null);
        }
    }

    private void saveAndRefresh() {
        skinService.saveBundle(bundle);
        skinService.reloadSkins();
        refresh();
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
