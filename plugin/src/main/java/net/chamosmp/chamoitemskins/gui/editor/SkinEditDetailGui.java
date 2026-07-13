// --- plugin/src/main/java/net/chamosmp/chamoitemskins/gui/editor/SkinEditDetailGui.java ---
package net.chamosmp.chamoitemskins.gui.editor;

import net.chamosmp.chamoitemskins.ChamoItemSkinsPlugin;
import net.chamosmp.chamoitemskins.api.model.Rarity;
import net.chamosmp.chamoitemskins.api.model.Skin;
import net.chamosmp.chamoitemskins.api.service.SkinService;
import net.chamosmp.chamoitemskins.gui.GuiFillerUtil;
import net.chamosmp.chamoitemskins.listener.GuiListener;
import net.chamosmp.chamoitemskins.manager.RarityManager;
import net.chamosmp.chamoitemskins.manager.SkinManager;
import net.chamosmp.chamoitemskins.models.ModelService;
import net.chamosmp.chamoitemskins.scheduler.SchedulerUtil;
import net.chamosmp.chamoitemskins.util.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import javax.naming.Name;
import java.util.ArrayList;
import java.util.List;

/**
 * GUI for editing an existing skin.
 */
public final class SkinEditDetailGui implements GuiListener.ChamoGui {
    private final Plugin plugin;
    private final Player player;
    private final SkinService skinService;
    private final RarityManager rarityManager;
    private Skin skin;
    private final Inventory inventory;
    private final MessageUtil messageUtil;

    private static final List<String> ALL_CATEGORIES = List.of(
            "SWORD", "AXE", "SHIELD", "PICKAXE", "BOW", "CROSSBOW", "SHOVEL", "SPEAR", "MACE", "HOE"
    );

    public SkinEditDetailGui(Plugin plugin, Player player, SkinService skinService, Skin skin, MessageUtil messageUtil) {
        this.plugin = plugin;
        this.player = player;
        this.skinService = skinService;
        this.rarityManager = ((ChamoItemSkinsPlugin) plugin).getRarityManager();
        this.skin = skin;
        this.inventory = Bukkit.createInventory(this, 27, MessageUtil.parse("<gold>Edit Skin: " + skin.id()));
        this.messageUtil = messageUtil;

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
        for (String cat : ALL_CATEGORIES) {
            String prefix = skin.categories().contains(cat) ? "     <gray>> <green>" : "         <dark_gray>";
            lore.add("      " + prefix + cat);
        }

        inventory.setItem(14, createInfoItem(Material.BOOK, "<gold><bold>Categories", lore));

        if (rarityManager.isEnabled()) {
            inventory.setItem(15, createInfoItem(Material.EMERALD, "<yellow>Rarity: " + skin.rarity().getDisplayName(), "<gray>Click to cycle rarity"));
        }

        ItemStack back = new ItemStack(Material.ARROW);
        var meta = back.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse("<gray>Back to Editor"));
            back.setItemMeta(meta);
        }
        inventory.setItem(26, back);
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
            meta.displayName(MessageUtil.parse(name));
            meta.lore(lore.stream().map(MessageUtil::parse).toList());
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
        SchedulerUtil.runForEntity(plugin, player, () -> player.openInventory(inventory), () -> {});
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 10) {
            ((ChamoItemSkinsPlugin) plugin).getChatInputUtil().getInput(player, Component.text("Enter skin Name:", NamedTextColor.YELLOW), input -> {
                skin = new Skin(skin.id(), input, skin.modelId(), skin.rarity(), skin.categories(), skin.enabled(), skin.noteMaterial(), skin.displayItem(), skin.animations());
                saveAndRefresh();
                open();
            }, "editoreditskinname", Component.text(skin.id(), NamedTextColor.LIGHT_PURPLE), skin.name());
        } else if (slot == 11) {
            skin = new Skin(skin.id(), skin.name(), skin.modelId(), skin.rarity(), skin.categories(), !skin.enabled(), skin.noteMaterial(), skin.displayItem(), skin.animations());
            saveAndRefresh();
        } else if (slot == 12) {
            ((ChamoItemSkinsPlugin) plugin).getChatInputUtil().getInput(player, Component.text("Enter Model ID:", NamedTextColor.YELLOW), input -> {
                skin = new Skin(skin.id(), skin.name(), input, skin.rarity(), skin.categories(), skin.enabled(), skin.noteMaterial(), skin.displayItem(), skin.animations());
                saveAndRefresh();
                open();
            }, "editoreditmodelid", Component.text(skin.id(), NamedTextColor.LIGHT_PURPLE), skin.modelId());
        } else if (slot == 13) {
            ((ChamoItemSkinsPlugin) plugin).getChatInputUtil().getInput(player, Component.text("Enter NEW skin ID:", NamedTextColor.YELLOW), input -> {
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

                Skin newSkin = new Skin(newId, skin.name(), skin.modelId(), skin.rarity(), skin.categories(), skin.enabled(), skin.noteMaterial(), skin.displayItem(), skin.animations());
                skinService.changeId(oldId, newSkin);
                skin = newSkin;
                saveAndRefresh();
                open();
            }, "editoreditskinid", Component.text(skin.id(), NamedTextColor.LIGHT_PURPLE), skin.id());
        } else if (slot == 14) {
            categoryCycleIndex = (categoryCycleIndex + 1) % ALL_CATEGORIES.size();
            String cat = ALL_CATEGORIES.get(categoryCycleIndex);
            skin = new Skin(skin.id(), skin.name(), skin.modelId(), skin.rarity(),
                    List.of(cat),  // always exactly one category
                    skin.enabled(), skin.noteMaterial(), skin.displayItem(), skin.animations());
            saveAndRefresh();
        } else if (slot == 15 && rarityManager.isEnabled()) {
            Rarity nextRarity = nextRarity(skin.rarity());
            skin = new Skin(skin.id(), skin.name(), skin.modelId(), nextRarity, skin.categories(), skin.enabled(), skin.noteMaterial(), skin.displayItem(), skin.animations());
            saveAndRefresh();
        } else if (slot == 25) {
            ((ChamoItemSkinsPlugin) plugin).getChatInputUtil().getYesNo(player, input -> {
                if (input.equalsIgnoreCase("true")) {
                    skinService.deleteSkin(skin.id());
                    ((ChamoItemSkinsPlugin) plugin).reloadPlugin();
                    new SkinEditorGui(plugin, player, skinService, ((ChamoItemSkinsPlugin) plugin).getModelService()).open();

                } else {
                    return;
                }
            }, "editoreditdeleteconf", Component.text("Are you sure you want to delete this skin?"));
        } else if (slot == 26) {
            new SkinEditorGui(plugin, player, skinService, ((ChamoItemSkinsPlugin) plugin).getModelService()).open();
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
