package net.chamosmp.chamoitemskins.gui.editor.skin;

import com.sqdstudios.sqdlib.paper.chamogui.GuiFillerUtil;
import com.sqdstudios.sqdlib.paper.chamogui.listener.GuiListener;
import com.sqdstudios.sqdlib.paper.dialog.SimpleDialog;
import com.sqdstudios.sqdlib.paper.util.ColorUtil;
import com.sqdstudios.sqdlib.paper.util.SchedulerUtil;
import net.chamosmp.chamoitemskins.ChamoItemSkinsPlugin;
import net.chamosmp.chamoitemskins.api.model.Category;
import net.chamosmp.chamoitemskins.api.model.Rarity;
import net.chamosmp.chamoitemskins.api.model.Skin;
import net.chamosmp.chamoitemskins.api.service.SkinService;
import net.chamosmp.chamoitemskins.manager.CategoryManager;
import net.chamosmp.chamoitemskins.manager.RarityManager;
import net.chamosmp.chamoitemskins.models.ModelService;
import net.chamosmp.chamoitemskins.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI for editing an existing skin.
 */
public final class SkinEditDetailGui implements GuiListener.ChamoGui {
    private final ChamoItemSkinsPlugin plugin;
    private final Player player;
    private final SkinService skinService;
    private final RarityManager rarityManager;
    private final Inventory inventory;
    private final MessageUtil messageUtil;
    private final CategoryManager categoryManager;
    private final ModelService modelService;
    private final SimpleDialog chatInputUtil;
    private final List<Category> CATEGORIES;
    private Skin skin;
    private int categoryCycleIndex = -1;


    public SkinEditDetailGui(ChamoItemSkinsPlugin plugin, Player player, SkinService skinService, Skin skin, MessageUtil messageUtil, CategoryManager categoryManager, ModelService modelService, RarityManager rarityManager, SimpleDialog chatInputUtil) {
        this.plugin = plugin;
        this.player = player;
        this.skinService = skinService;
        this.rarityManager = rarityManager;
        this.skin = skin;
        this.inventory = Bukkit.createInventory(this, 27, ColorUtil.parse("<aqua><b>Edit Skin: " + skin.id()));
        this.messageUtil = messageUtil;
        this.categoryManager = categoryManager;
        this.modelService = modelService;
        this.CATEGORIES = categoryManager.getCategories();
        this.chatInputUtil = chatInputUtil;
        refresh();
    }

    public void refresh() {
        inventory.clear();

        inventory.setItem(10, createInfoItem(Material.NAME_TAG, "<yellow>Name: <white>" + skin.name(), "<gray>Click to set Name"));
        inventory.setItem(11, createInfoItem(Material.LEVER, "<yellow>Enabled: " + (skin.enabled() ? "<green>Yes" : "<red>No"), "<gray>Click to toggle"));
        inventory.setItem(12, createInfoItem(Material.ARMOR_STAND, "<yellow>Model ID: <white>" + skin.modelId(), "<gray>Click to set Model ID"));
        inventory.setItem(13, createInfoItem(Material.OAK_SIGN, "<yellow>ID: <white>" + skin.id(), "<gray>Click to change Skin ID"));
        inventory.setItem(25, createInfoItem(Material.RED_CONCRETE, "<dark_red>Delete", "<red>Click to delete the skin"));

        List<String> lore = new ArrayList<>();
        lore.add("<yellow>Click to toggle categories in order:");
        for (Category cat : CATEGORIES) {
            boolean contains = false;
            if (skin.categories() != null) {
                for (Category cat2 : skin.categories()) {
                    contains = cat2.name().equalsIgnoreCase(cat.name());
                }
            }
            String prefix = contains ? "     <gray>> <green>" : "         <dark_gray>";
            lore.add("      " + prefix + cat.name());
        }

        inventory.setItem(14, createInfoItem(Material.BOOK, "<gold><bold>Categories", lore));

        if (rarityManager.isEnabled()) {
            inventory.setItem(15, createInfoItem(Material.EMERALD, "<yellow>Rarity: " + skin.rarity().getDisplayName(), "<gray>Click to cycle rarity"));
        }

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

    private Rarity nextRarity(Rarity current) {
        var rarities = rarityManager.getRarities();
        if (rarities.isEmpty()) return current;
        int idx = 0;
        for (int i = 0; i < rarities.size(); i++) {
            if (rarities.get(i).id().equals(current.id())) {
                idx = i;
                break;
            }
        }
        return rarities.get((idx + 1) % rarities.size());
    }

    public void open() {
        SchedulerUtil.runForEntity(plugin, player, () -> player.openInventory(inventory), () -> {
        });
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 10) {
            chatInputUtil.getInput(ColorUtil.parse("<light_purple>" + skin.id()), player, "editoreditskinname", ColorUtil.parse("<yellow>Enter skin Name:"), skin.name(),
                    input -> {
                        skin = new Skin(skin.id(), input, skin.modelId(), skin.rarity(), skin.categories(), skin.enabled(), skin.noteMaterial(), skin.displayItem());
                        saveAndRefresh();
                        open();
                    });
        } else if (slot == 11) {
            skin = new Skin(skin.id(), skin.name(), skin.modelId(), skin.rarity(), skin.categories(), !skin.enabled(), skin.noteMaterial(), skin.displayItem());
            saveAndRefresh();
        } else if (slot == 12) {
            chatInputUtil.getInput(ColorUtil.parse("<light_purple>" + skin.id()), player, "editoreditmodelid", ColorUtil.parse("<yellow>Enter Model ID:"), skin.modelId(),
                    input -> {
                        skin = new Skin(skin.id(), skin.name(), input, skin.rarity(), skin.categories(), skin.enabled(), skin.noteMaterial(), skin.displayItem());
                        saveAndRefresh();
                        open();
                    });
        } else if (slot == 13) {
            chatInputUtil.getInput(ColorUtil.parse("<light_purple>" + skin.id()), player, "editoreditskinid", ColorUtil.parse("<yellow>Enter NEW skin ID:"), skin.id(),
                    input -> {
                        String oldId = skin.id();
                        String newId = input.toLowerCase().replace(" ", "_");
                        if (oldId.equals(newId)) {
                            open();
                            return;
                        }

                        if (skinService.getSkin(newId).isPresent()) {
                            messageUtil.sendLangMessage(player, "<red>A skin with ID " + newId + " already exists!");
                            open();
                            return;
                        }

                        Skin newSkin = new Skin(newId, skin.name(), skin.modelId(), skin.rarity(), skin.categories(), skin.enabled(), skin.noteMaterial(), skin.displayItem());
                        skinService.changeId(oldId, newSkin);
                        skin = newSkin;
                        saveAndRefresh();
                        open();
                    });
        } else if (slot == 14) {
            categoryCycleIndex = (categoryCycleIndex + 1) % CATEGORIES.size();
            Category cat = CATEGORIES.get(categoryCycleIndex);
            skin = new Skin(skin.id(), skin.name(), skin.modelId(), skin.rarity(),
                    List.of(cat),
                    skin.enabled(), skin.noteMaterial(), skin.displayItem());
            saveAndRefresh();
        } else if (slot == 15 && rarityManager.isEnabled()) {
            Rarity nextRarity = nextRarity(skin.rarity());
            skin = new Skin(skin.id(), skin.name(), skin.modelId(), nextRarity, skin.categories(), skin.enabled(), skin.noteMaterial(), skin.displayItem());
            saveAndRefresh();
        } else if (slot == 25) {
            chatInputUtil.getYesNo(ColorUtil.parse("Are you sure you want to delete this skin?"), player,
                    input -> {
                        if (input) {
                            skinService.deleteSkin(skin.id());
                            plugin.reloadPlugin();
                            SkinEditorGui editorGui = new SkinEditorGui(plugin, player, skinService, modelService, categoryManager, messageUtil, rarityManager, chatInputUtil);
                            editorGui.open(() -> {
                                skinService.reloadSkins();
                                editorGui.refresh();
                                editorGui.open(null);
                            });

                        }
                    });
        } else if (slot == 26) {
            new SkinEditorGui(plugin, player, skinService, modelService, categoryManager, messageUtil, rarityManager, chatInputUtil).open(null);
        }
    }

    private void saveAndRefresh() {
        skinService.saveSkin(skin);
        refresh();
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
