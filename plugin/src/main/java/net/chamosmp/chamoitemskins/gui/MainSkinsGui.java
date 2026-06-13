// --- plugin/src/main/java/net/chamosmp/chamoitemskins/gui/MainSkinsGui.java ---
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

import net.chamosmp.chamoitemskins.util.ConfigUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main GUI for browsing skin categories.
 */
public final class MainSkinsGui implements GuiListener.ChamoGui {
    private final Plugin plugin;
    private final Player player;
    private final SkinService skinService;
    private final GrantService grantService;
    private final Inventory inventory;
    private final List<GuiSlotDef> slots;
    private final Map<Integer, Material> categorySlots = new HashMap<>();

    public MainSkinsGui(Plugin plugin, Player player, SkinService skinService, GrantService grantService, String title, int size, List<GuiSlotDef> slots) {
        this.plugin = plugin;
        this.player = player;
        this.skinService = skinService;
        this.grantService = grantService;
        this.slots = slots;
        this.inventory = Bukkit.createInventory(this, size, MessageUtil.parse(player, title, Map.of()));
        
        setupInventory();
    }

    private void setupInventory() {
        for (GuiSlotDef def : slots) {
            ItemStack item = new ItemStack(def.material());
            var meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(MessageUtil.parse(player, def.name(), Map.of()));
                meta.lore(def.lore().stream().map(l -> MessageUtil.parse(player, l, Map.of())).toList());
                if (def.glow()) meta.setEnchantmentGlintOverride(true);
                item.setItemMeta(meta);
            }
            inventory.setItem(def.slot(), item);

            if (def.type() instanceof SlotType.ActionSlot action && action.action().startsWith("CATEGORY_")) {
                Material mat = Material.matchMaterial(action.action().substring(9));
                if (mat != null) categorySlots.put(def.slot(), mat);
            }
        }
    }

    public void open() {
        SchedulerUtil.runForEntity(plugin, player, () -> player.openInventory(inventory), () -> {});
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slotIdx = event.getRawSlot();
        if (categorySlots.containsKey(slotIdx)) {
            Material categoryMaterial = categorySlots.get(slotIdx);
            YamlConfiguration selectionConfig = ConfigUtil.loadOrAdapt(plugin, "gui.yml");
            ConfigurationSection selectionSlotsSection = selectionConfig.getConfigurationSection("selection-slots");
            List<GuiSlotDef> selectionSlots = parseSlots(selectionSlotsSection);
            String selectionTitle = selectionConfig.getString("selection-title", "Select Skin");
            int selectionSize = selectionConfig.getInt("selection-size", 54);
            
            new SkinSelectionGui(plugin, player, categoryMaterial, skinService, grantService, selectionTitle, selectionSize, selectionSlots).open();
        } else {
            slots.stream().filter(s -> s.slot() == slotIdx).findFirst().ifPresent(def -> {
                if (def.type() instanceof SlotType.ActionSlot action) {
                    if (action.action().equals("ADMIN_GUI") && player.hasPermission("chamoitemskins.admin")) {
                        player.performCommand("skinsadmin gui");
                    }
                }
            });
        }
    }

    private List<GuiSlotDef> parseSlots(ConfigurationSection section) {
        List<GuiSlotDef> slotsList = new ArrayList<>();
        if (section == null) return slotsList;
        for (String key : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s == null) continue;
            
            SlotType type = parseSlotType(s.getString("type", "Decorative"), s);
            slotsList.add(new GuiSlotDef(
                    type,
                    s.getInt("slot"),
                    Material.matchMaterial(s.getString("material", "STONE")),
                    s.getString("name", ""),
                    s.getStringList("lore"),
                    s.getBoolean("glow", false)
            ));
        }
        return slotsList;
    }

    private SlotType parseSlotType(String typeStr, ConfigurationSection section) {
        return switch (typeStr.toUpperCase()) {
            case "SKINSLOT" -> new SlotType.SkinSlot(section.getInt("index", 0));
            case "FILTERSLOT" -> new SlotType.FilterSlot();
            case "BACKSLOT" -> new SlotType.BackSlot();
            case "ACTIONSLOT" -> new SlotType.ActionSlot(section.getString("action", ""));
            default -> new SlotType.Decorative();
        };
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
