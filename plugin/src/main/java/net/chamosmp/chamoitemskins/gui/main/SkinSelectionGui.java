// --- plugin/src/main/java/net/chamosmp/chamoitemskins/gui/SkinSelectionGui.java ---
package net.chamosmp.chamoitemskins.gui.main;

import net.chamosmp.chamoitemskins.api.model.Skin;
import net.chamosmp.chamoitemskins.api.service.GrantService;
import net.chamosmp.chamoitemskins.api.service.SkinService;
import net.chamosmp.chamoitemskins.gui.config.GuiSlotDef;
import net.chamosmp.chamoitemskins.gui.config.SlotType;
import net.chamosmp.chamoitemskins.listener.GuiListener;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GUI for selecting a skin for a specific item type.
 */
public final class SkinSelectionGui implements GuiListener.ChamoGui {
    private final Plugin plugin;
    private final Player player;
    private final String category;
    private final SkinService skinService;
    private final GrantService grantService;
    private final Inventory inventory;
    private final List<GuiSlotDef> slots;
    private final Map<Integer, Skin> skinMap = new HashMap<>();

    public SkinSelectionGui(Plugin plugin, Player player, String category, SkinService skinService, GrantService grantService, String title, int size, List<GuiSlotDef> slots) {
        this.plugin = plugin;
        this.player = player;
        this.category = category;
        this.skinService = skinService;
        this.grantService = grantService;
        this.slots = slots;
        this.inventory = Bukkit.createInventory(this, size, MessageUtil.parse(player, title, Map.of("category", category, "material", category)));

        refresh();
    }

    public void refresh() {
        inventory.clear();
        skinMap.clear();
        
        // Static and decorative slots first
        for (GuiSlotDef def : slots) {
            if (!(def.type() instanceof SlotType.SkinSlot)) {
                inventory.setItem(def.slot(), createStaticItem(def));
            }
        }

        List<Skin> categorySkins = (category == null || category.equalsIgnoreCase("ALL")) 
                ? skinService.getSkins().stream().filter(Skin::enabled).toList()
                : skinService.getSkins().stream()
                    .filter(Skin::enabled)
                    .filter(skin -> skin.categories().stream().anyMatch(cat -> cat.equalsIgnoreCase(category)))
                    .toList();

        // Place skins starting from slot 1 sequentially
        int currentSlot = 1;
        for (Skin skin : categorySkins) {
            if (currentSlot >= inventory.getSize()) break;
            
            // Skip if slot is already occupied by a static item
            while (currentSlot < inventory.getSize() && inventory.getItem(currentSlot) != null) {
                currentSlot++;
            }
            
            if (currentSlot >= inventory.getSize()) break;

            skinMap.put(currentSlot, skin);
            inventory.setItem(currentSlot, createSkinItem(skin));
            currentSlot++;
        }
    }

    private ItemStack createSkinItem(Skin skin) {
        boolean owned = grantService.hasSkin(player.getUniqueId(), skin.id()).join();
        Material mat = skin.displayItem().material(); // Always show the material if enabled, or maybe barrier if not owned?
        // User said "The enabled skin doesnt show", maybe they mean the ones they OWN?
        // Let's ensure it shows if enabled.
        
        ItemStack item = new ItemStack(mat);
        var meta = item.getItemMeta();
        if (meta != null) {
            boolean active = false;
            // Check if active for ANY material in this category
            for (Material playerMat : Material.values()) {
                if (playerMat.isAir() || !playerMat.isItem()) continue;
                String playerMatName = playerMat.name();
                if (skin.categories().stream().anyMatch(cat -> isMaterialInCategory(playerMatName, cat))) {
                     if (grantService.getActiveSkin(player.getUniqueId(), playerMat).join().filter(id -> id.equals(skin.id())).isPresent()) {
                         active = true;
                         break;
                     }
                }
            }

            String displayName = skin.name();
            if (active) {
                displayName = "<green>[EQUIPPED] <white>" + displayName;
            }
            
            // Show rarity in lore
            List<String> lore = new ArrayList<>();
            lore.add(skin.rarity().getDisplayName());
            lore.add("");
            lore.addAll(skin.displayItem().lore());
            
            if (!owned) {
                lore.add("");
                lore.add("<red>✘ Locked");
            }

            meta.displayName(MessageUtil.parse(player, displayName, Map.of()));
            meta.lore(lore.stream().map(l -> MessageUtil.parse(player, l, Map.of())).toList());
            
            if (active || skin.displayItem().glow()) {
                meta.setEnchantmentGlintOverride(true);
            }
            
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isMaterialInCategory(String materialName, String category) {
        return switch (category.toUpperCase()) {
            case "SWORD", "SWORDS" -> materialName.contains("SWORD") || materialName.equals("MACE");
            case "AXE", "AXES" -> materialName.contains("_AXE");
            case "PICKAXE", "PICKAXES" -> materialName.contains("PICKAXE");
            case "SHOVEL", "SHOVELS" -> materialName.contains("SHOVEL");
            case "HOE", "HOES" -> materialName.contains("_HOE");
            case "SHIELD" -> materialName.equals("SHIELD");
            case "BOW" -> materialName.equals("BOW");
            case "CROSSBOW" -> materialName.equals("CROSSBOW");
            case "MACE" -> materialName.equals("MACE");
            case "SPEAR", "TRIDENT" -> materialName.contains("TRIDENT");
            case "HELMET", "HELMETS" -> materialName.contains("HELMET");
            case "CHESTPLATE", "CHESTPLATES" -> materialName.contains("CHESTPLATE");
            case "LEGGINGS" -> materialName.contains("LEGGINGS");
            case "BOOTS" -> materialName.contains("BOOTS");
            default -> false;
        };
    }

    private ItemStack createFillerItem(GuiSlotDef def) {
        return new ItemStack(Material.AIR);
    }

    private ItemStack createStaticItem(GuiSlotDef def) {
        ItemStack item = new ItemStack(def.material());
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse(player, def.name(), Map.of()));
            meta.lore(def.lore().stream().map(l -> MessageUtil.parse(player, l, Map.of())).toList());
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
        Skin skin = skinMap.get(slot);
        if (skin != null) {
            grantService.hasSkin(player.getUniqueId(), skin.id()).thenAccept(owned -> {
                if (owned) {
                    // When clicking in a category, we might want to equip it for the main hand item if it matches
                    ItemStack handItem = player.getInventory().getItemInMainHand();
                    if (handItem == null || handItem.getType() == Material.AIR) {
                        MessageUtil.sendMessage(player, "<red>You must be holding an item to apply this skin!");
                        return;
                    }
                    Material targetMat = handItem.getType();
                    
                    if (skin.categories().stream().noneMatch(cat -> isMaterialInCategory(targetMat.name(), cat))) {
                        MessageUtil.sendMessage(player, "<red>This skin cannot be applied to this item!");
                        return;
                    }
                    
                    grantService.getActiveSkin(player.getUniqueId(), targetMat).thenAccept(active -> {
                        if (active.isPresent() && active.get().equals(skin.id())) {
                            grantService.setActiveSkin(player.getUniqueId(), targetMat, null).thenRun(() -> {
                                MessageUtil.sendMessage(player, plugin.getConfig().getString("messages.skin-unequipped", "<yellow>Skin <white>{skin_name}<yellow> removed."), Map.of("skin_name", skin.name()));
                                SchedulerUtil.runForEntity(plugin, player, this::refresh, () -> {});
                            });
                        } else {
                            grantService.setActiveSkin(player.getUniqueId(), targetMat, skin.id()).thenRun(() -> {
                                MessageUtil.sendMessage(player, plugin.getConfig().getString("messages.skin-equipped", "<green>✔ Equipped <white>{skin_name}<green>."), Map.of("skin_name", skin.name()));
                                SchedulerUtil.runForEntity(plugin, player, this::refresh, () -> {});
                            });
                        }
                    });
                } else {
                    MessageUtil.sendMessage(player, plugin.getConfig().getString("messages.skin-not-owned", "<red>✘ You don't own <white>{skin_name}<red>."), Map.of("skin_name", skin.name()));
                }
            });
        } else {
            slots.stream().filter(s -> s.slot() == slot).findFirst().ifPresent(def -> {
                if (def.type() instanceof SlotType.BackSlot) {
                    // Navigate back to MainSkinsGui
                    player.performCommand("skins");
                }
            });
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
