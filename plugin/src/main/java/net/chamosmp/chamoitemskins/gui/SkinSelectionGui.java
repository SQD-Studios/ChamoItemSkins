// --- plugin/src/main/java/net/chamosmp/chamoitemskins/gui/SkinSelectionGui.java ---
package net.chamosmp.chamoitemskins.gui;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GUI for selecting a skin for a specific item type.
 */
public final class SkinSelectionGui implements GuiListener.ChamoGui {
    private final Plugin plugin;
    private final Player player;
    private final Material material;
    private final SkinService skinService;
    private final GrantService grantService;
    private final Inventory inventory;
    private final List<GuiSlotDef> slots;
    private final Map<Integer, Skin> skinMap = new HashMap<>();

    public SkinSelectionGui(Plugin plugin, Player player, Material material, SkinService skinService, GrantService grantService, String title, int size, List<GuiSlotDef> slots) {
        this.plugin = plugin;
        this.player = player;
        this.material = material;
        this.skinService = skinService;
        this.grantService = grantService;
        this.slots = slots;
        this.inventory = Bukkit.createInventory(this, size, MessageUtil.parse(player, title, Map.of("material", material.name())));

        refresh();
    }

    public void refresh() {
        inventory.clear();
        skinMap.clear();
        
        List<Skin> materialSkins = skinService.getSkinsForMaterial(material).stream().filter(Skin::enabled).toList();
        int skinIndex = 0;

        for (GuiSlotDef def : slots) {
            if (def.type() instanceof SlotType.SkinSlot) {
                if (skinIndex < materialSkins.size()) {
                    Skin skin = materialSkins.get(skinIndex++);
                    skinMap.put(def.slot(), skin);
                    inventory.setItem(def.slot(), createSkinItem(skin));
                } else {
                    inventory.setItem(def.slot(), createFillerItem(def));
                }
            } else {
                inventory.setItem(def.slot(), createStaticItem(def));
            }
        }
    }

    private ItemStack createSkinItem(Skin skin) {
        boolean owned = grantService.hasSkin(player.getUniqueId(), skin.id()).join();
        Material mat = owned ? skin.displayItem().material() : Material.BARRIER;
        ItemStack item = new ItemStack(mat);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse(player, skin.name(), Map.of()));
            meta.lore(skin.displayItem().lore().stream().map(l -> MessageUtil.parse(player, l, Map.of())).toList());
            
            boolean active = grantService.getActiveSkin(player.getUniqueId(), material).join()
                    .map(id -> id.equals(skin.id())).orElse(false);
            
            if (active || skin.displayItem().glow()) {
                meta.setEnchantmentGlintOverride(true);
            }
            
            item.setItemMeta(meta);
        }
        return item;
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
                    grantService.getActiveSkin(player.getUniqueId(), material).thenAccept(active -> {
                        if (active.isPresent() && active.get().equals(skin.id())) {
                            grantService.setActiveSkin(player.getUniqueId(), material, null).thenRun(() -> {
                                MessageUtil.sendMessage(player, plugin.getConfig().getString("messages.skin-unequipped", "<yellow>Skin <white>{skin_name}<yellow> removed."), Map.of("skin_name", skin.name()));
                                SchedulerUtil.runForEntity(plugin, player, this::refresh, () -> {});
                            });
                        } else {
                            grantService.setActiveSkin(player.getUniqueId(), material, skin.id()).thenRun(() -> {
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
