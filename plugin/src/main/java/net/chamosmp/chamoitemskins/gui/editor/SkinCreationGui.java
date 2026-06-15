// --- plugin/src/main/java/net/chamosmp/chamoitemskins/gui/editor/SkinCreationGui.java ---
package net.chamosmp.chamoitemskins.gui.editor;

import net.chamosmp.chamoitemskins.ChamoItemSkinsPlugin;
import net.chamosmp.chamoitemskins.api.model.Rarity;
import net.chamosmp.chamoitemskins.api.model.Skin;
import net.chamosmp.chamoitemskins.api.service.SkinService;
import net.chamosmp.chamoitemskins.gui.GuiFillerUtil;
import net.chamosmp.chamoitemskins.listener.GuiListener;
import net.chamosmp.chamoitemskins.manager.RarityManager;
import net.chamosmp.chamoitemskins.scheduler.SchedulerUtil;
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
import java.util.Map;

/**
 * GUI for creating a new skin.
 */
public final class SkinCreationGui implements GuiListener.ChamoGui {
    private final Plugin plugin;
    private final Player player;
    private final SkinService skinService;
    private final RarityManager rarityManager;

    private String id = "new_skin";
    private String name = "New Skin";
    private String modelId = "model_id";
    private Material itemType = Material.DIAMOND_SWORD;
    private List<String> categories = new ArrayList<>();

    private Rarity rarity;
    private final Inventory inventory;

    private static final List<String> ALL_CATEGORIES = List.of(
            "SWORD", "AXE", "SHIELD", "PICKAXE", "BOW", "CROSSBOW", "SHOVEL", "SPEAR", "MACE", "HOE"
    );

    public SkinCreationGui(Plugin plugin, Player player, SkinService skinService) {
        this.plugin = plugin;
        this.player = player;
        this.skinService = skinService;
        this.rarityManager = ((ChamoItemSkinsPlugin) plugin).getRarityManager();
        this.rarity = rarityManager.getDefaultRarity();
        this.inventory = Bukkit.createInventory(this, 27, MessageUtil.parse("<green>Create New Skin"));
        
        refresh();
    }

    public void refresh() {
        inventory.clear();
        
        // ID & Name
        inventory.setItem(10, createInfoItem(Material.NAME_TAG, "<yellow>ID: <white>" + id, "<gray>Click to set ID"));
        inventory.setItem(11, createInfoItem(Material.PAPER, "<yellow>Name: <white>" + name, "<gray>Click to set Name"));
        inventory.setItem(12, createInfoItem(Material.ARMOR_STAND, "<yellow>Model ID: <white>" + modelId, "<gray>Click to set Model ID"));
        
        // Category Cycle Item
        List<String> lore = new ArrayList<>();
        //lore.add("<gray>Selected Categories:");
        //if (categories.isEmpty()) lore.add(" <red>None");
        //else categories.forEach(c -> lore.add(" <green>• " + c));
        //lore.add("");
        lore.add("<yellow>Click to toggle categories in order:");
        for (String cat : ALL_CATEGORIES) {
            String prefix = categories.contains(cat) ? "<green>" : "<dark_gray>";
            lore.add("  " + prefix + cat);
        }
        inventory.setItem(13, createInfoItem(Material.BOOK, "<gold><bold>Categories", lore));

        // Rarities
        if (rarityManager.isEnabled()) {
            inventory.setItem(14, createInfoItem(Material.EMERALD, "<yellow>Rarity: " + rarity.getDisplayName(), "<gray>Click to cycle rarity"));
        }

        // Create Button (Bottom Right)
        ItemStack create = new ItemStack(Material.GREEN_CONCRETE);
        var meta = create.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse("<green><bold>CREATE"));
            create.setItemMeta(meta);
        }
        inventory.setItem(26, create);

        // Back Button
        ItemStack back = new ItemStack(Material.ARROW);
        var backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(MessageUtil.parse("<gray>Back to Editor"));
            back.setItemMeta(backMeta);
        }
        inventory.setItem(25, back);
        GuiFillerUtil.apply(plugin, inventory, player);
    }


    private int categoryCycleIndex = 0;

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

    public void open() {
        SchedulerUtil.runForEntity(plugin, player, () -> player.openInventory(inventory), () -> {});
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 10) {
            ((ChamoItemSkinsPlugin) plugin).getChatInputUtil().getInput(player, "<yellow>Enter skin ID:", input -> {
                this.id = input.toLowerCase().replace(" ", "_");
                open();
                refresh();
            });
        } else if (slot == 11) {
            ((ChamoItemSkinsPlugin) plugin).getChatInputUtil().getInput(player, "<yellow>Enter skin Name:", input -> {
                this.name = input;
                open();
                refresh();
            });
        } else if (slot == 12) {
            ((ChamoItemSkinsPlugin) plugin).getChatInputUtil().getInput(player, "<yellow>Enter Model ID:", () -> {
                try {
                    return kr.toxicity.model.api.BetterModel.modelKeys();
                } catch (Exception e) {
                    return java.util.Collections.emptyList();
                }
            }, input -> {
                this.modelId = input;
                open();
                refresh();
            });
        } else if (slot == 13) {
            String cat = ALL_CATEGORIES.get(categoryCycleIndex);
            if (categories.contains(cat)) {
                categories.remove(cat);
            } else {
                categories.add(cat);
            }
            categoryCycleIndex = (categoryCycleIndex + 1) % ALL_CATEGORIES.size();
            refresh();
        } else if (slot == 14 && rarityManager.isEnabled()) {
            rarity = nextRarity(rarity);
            refresh();
        } else if (slot == 25) {
            new SkinEditorGui(plugin, player, skinService, ((ChamoItemSkinsPlugin) plugin).getBetterModelService()).open();
        } else if (slot == 26) {
            Material displayMat = Material.PAPER;
            if (!categories.isEmpty()) {
                displayMat = switch (categories.get(0)) {
                    case "SWORD" -> Material.DIAMOND_SWORD;
                    case "AXE" -> Material.DIAMOND_AXE;
                    case "PICKAXE" -> Material.DIAMOND_PICKAXE;
                    case "SHOVEL" -> Material.DIAMOND_SHOVEL;
                    case "HOE" -> Material.DIAMOND_HOE;
                    case "SHIELD" -> Material.SHIELD;
                    case "BOW" -> Material.BOW;
                    case "CROSSBOW" -> Material.CROSSBOW;
                    case "MACE" -> Material.MACE;
                    case "SPEAR" -> Material.TRIDENT;
                    default -> Material.PAPER;
                };
            }
            Skin skin = new Skin(id, name, modelId, rarity, categories, true, null, 
                    new Skin.DisplayItem(displayMat, name, List.of("<gray>A new skin."), false), new ArrayList<>());
            skinService.saveSkin(skin);
            MessageUtil.sendMessage(player, "<green>Skin created!");
            new SkinEditorGui(plugin, player, skinService, ((ChamoItemSkinsPlugin) plugin).getBetterModelService()).open();
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
