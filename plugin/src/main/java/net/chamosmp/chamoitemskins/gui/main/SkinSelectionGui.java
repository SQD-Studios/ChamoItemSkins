// --- plugin/src/main/java/net/chamosmp/chamoitemskins/gui/main/SkinSelectionGui.java ---
package net.chamosmp.chamoitemskins.gui.main;

import net.chamosmp.chamoitemskins.api.model.Skin;
import net.chamosmp.chamoitemskins.api.service.GrantService;
import net.chamosmp.chamoitemskins.api.service.SkinService;
import net.chamosmp.chamoitemskins.models.ModelService;
import net.chamosmp.chamoitemskins.gui.GuiFillerUtil;
import net.chamosmp.chamoitemskins.gui.config.GuiSlotDef;
import net.chamosmp.chamoitemskins.gui.config.SlotType;
import net.chamosmp.chamoitemskins.listener.GuiListener;
import net.chamosmp.chamoitemskins.manager.RarityManager;
import net.chamosmp.chamoitemskins.scheduler.SchedulerUtil;
import net.chamosmp.chamoitemskins.util.ChatInputUtil;
import net.chamosmp.chamoitemskins.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * GUI for selecting a skin for a specific item type.
 */
public final class SkinSelectionGui implements GuiListener.ChamoGui {
    private final Plugin plugin;
    private final Player player;
    private final SkinService skinService;
    private final GrantService grantService;
    private final RarityManager rarityManager;
    private final ModelService modelService;
    private final Inventory inventory;
    private final List<GuiSlotDef> slots;
    private final List<Skin> pinnedSkins;
    private final Map<Integer, Skin> skinMap = new HashMap<>();
    private final ChatInputUtil chatInputUtil;

    private final Map<Integer, String> filterSlotCategories = new HashMap<>();
    private final String baseCategory;
    private String activeFilterCategory;
    private int activeFilterSlot = -1;

    private int activeSearchSlot = -1;
    private Set<Integer> searchSlotCategories = new HashSet<>();
    private String search;
    private boolean isSearching = false;

    private Map<Material, String> activeSkins = new HashMap<>();
    private Set<String> ownedSkinIds = new HashSet<>();

    public SkinSelectionGui(
            @NotNull Plugin plugin,
            @NotNull Player player,
            @NotNull String category,
            @NotNull SkinService skinService,
            @NotNull GrantService grantService,
            @NotNull RarityManager rarityManager,
            @NotNull ModelService modelService,
            @NotNull String title,
            int size,
            @NotNull List<GuiSlotDef> slots,
            ChatInputUtil chatInputUtil
    ) {
        this.plugin = plugin;
        this.player = player;
        this.skinService = skinService;
        this.grantService = grantService;
        this.rarityManager = rarityManager;
        this.modelService = modelService;
        this.slots = slots;
        this.baseCategory = category;
        this.activeFilterCategory = category;
        this.chatInputUtil = chatInputUtil;
        this.inventory = Bukkit.createInventory(this, size, MessageUtil.parse(player, title, Map.of("category", category, "material", category)));

        for (GuiSlotDef def : slots) {
            if (def.type() instanceof SlotType.FilterSlot filter) {
                filterSlotCategories.put(def.slot(), filter.category());
            } else if (def.type() instanceof SlotType.SearchSlot) {
                searchSlotCategories.add(def.slot());
            }
        }
        if (!filterSlotCategories.isEmpty()) {
            filterSlotCategories.entrySet().stream()
                    .filter(e -> e.getValue().equalsIgnoreCase(category))
                    .findFirst()
                    .ifPresent(e -> {
                        activeFilterSlot = e.getKey();
                        activeFilterCategory = e.getValue();
                    });
        }

        this.pinnedSkins = List.copyOf(skinService.getSkins().stream().filter(Skin::enabled).toList());
    }

    private boolean isBorderSlot(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        int totalRows = inventory.getSize() / 9;
        return row == 0 || row == totalRows - 1 || col == 0 || col == 8;
    }

    public void refresh() {
        inventory.clear();
        skinMap.clear();

        for (GuiSlotDef def : slots) {
            if (def.type() instanceof SlotType.SkinSlot) {
                continue;
            }
            if (def.type() instanceof SlotType.FilterSlot) {
                inventory.setItem(def.slot(), createFilterItem(def, def.slot() == activeFilterSlot));
            } else if (def.type() instanceof SlotType.SearchSlot) {
                inventory.setItem(def.slot(), createSearchItem(def));
            } else {
                inventory.setItem(def.slot(), createStaticItem(def));
            }
        }

        List<Skin> visibleSkins = filterSkins(pinnedSkins);

        int currentSlot = 10;
        for (Skin skin : visibleSkins) {
            if (currentSlot >= inventory.getSize()) break;

            while (currentSlot < inventory.getSize() && (isBorderSlot(currentSlot) || inventory.getItem(currentSlot) != null)) {
                currentSlot++;
            }

            if (currentSlot >= inventory.getSize()) break;

            skinMap.put(currentSlot, skin);
            inventory.setItem(currentSlot, createSkinItem(skin));
            currentSlot++;
        }

        GuiFillerUtil.apply(plugin, inventory, player);
    }

    private @NotNull List<Skin> filterSkins(@NotNull List<Skin> source) {
        String filter = activeFilterCategory == null ? "ALL" : activeFilterCategory;
        return source.stream()
                .filter(skin -> baseCategory == null
                        || baseCategory.equalsIgnoreCase("ALL")
                        || skin.categories().stream().anyMatch(cat -> cat.equalsIgnoreCase(baseCategory)))
                .filter(skin -> matchesCategoryFilter(skin, filter))
                .filter(skin -> matchesOwnershipFilter(skin, filter))
                .filter(skin -> matchesSearchFilter(skin))
                .toList();
    }

    private boolean matchesSearchFilter(@NotNull Skin skin) {
        if (search == null || search.isBlank() || !isSearching) return true;
        String q = search.toLowerCase();
        return skin.id().toLowerCase().contains(q) || skin.name().toLowerCase().contains(q);
    }

    private boolean matchesCategoryFilter(@NotNull Skin skin, @NotNull String filter) {
        if (filter.equalsIgnoreCase("ALL") || filter.equalsIgnoreCase("OWNED")) {
            return true;
        }
        return skin.categories().stream().anyMatch(cat -> cat.equalsIgnoreCase(filter));
    }

    private boolean matchesOwnershipFilter(@NotNull Skin skin, @NotNull String filter) {
        if (filter.equalsIgnoreCase("OWNED")) {
            return ownedSkinIds.contains(skin.id());
        }
        return true;
    }

    private static final List<String> ALL_FILTERS = List.of ("OWNED", "ALL");

    private @NotNull ItemStack createFilterItem(@NotNull GuiSlotDef def, boolean active) {
        ItemStack item = new ItemStack(def.material());
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse(player, def.name(), Map.of()));
            meta.lore(def.lore().stream().map(l -> MessageUtil.parse(player, l, Map.of())).toList());
            meta.setEnchantmentGlintOverride(active || def.glow());
            item.setItemMeta(meta);
        }
        return item;
    }

    private @NotNull ItemStack createSkinItem(@NotNull Skin skin) {
        boolean owned = ownedSkinIds.contains(skin.id());
        boolean active = activeSkins.values().stream().anyMatch(id -> id.equals(skin.id()));

        ItemStack item = modelService.createPreviewItem(skin);
        var meta = item.getItemMeta();
        if (meta != null) {
            String displayName = skin.name();
            if (active) {
                displayName = "<dark_gray><i>[EQUIPPED] <white>" + displayName;
            }

            List<String> lore = new ArrayList<>();
            if (rarityManager.isEnabled()) {
                lore.add(skin.rarity().getDisplayName());
                lore.add("");
            }
            if (skin.displayItem() != null) {
                lore.addAll(skin.displayItem().lore());
            }

            if (!owned) {
                lore.add("");
                lore.add("<red>Locked");
            }

            meta.displayName(MessageUtil.parse(player, displayName, Map.of()));
            meta.lore(lore.stream().map(l -> MessageUtil.parse(player, l, Map.of())).toList());

            if (active || (skin.displayItem() != null && skin.displayItem().glow())) {
                meta.setEnchantmentGlintOverride(true);
            }

            item.setItemMeta(meta);
        }
        return item;
    }

    private @NotNull ItemStack createSearchItem(@NotNull GuiSlotDef def) {
        ItemStack item = new ItemStack(def.material());
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse(player, def.name(), Map.of()));

            List<String> lore = new ArrayList<>(def.lore());

            String safeSearch = search == null ? "Nothing" : search;
            lore.add("<white>Searching for: <dark_red>" + safeSearch);

            meta.lore(lore.stream().map(l -> MessageUtil.parse(player, l, Map.of())).toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isMaterialInCategory(String materialName, String category) {
        return switch (category.toUpperCase()) {
            case "SWORD", "SWORDS" -> materialName.contains("SWORD");
            case "AXE", "AXES" -> materialName.contains("_AXE");
            case "PICKAXE", "PICKAXES" -> materialName.contains("PICKAXE");
            case "SHOVEL", "SHOVELS" -> materialName.contains("SHOVEL");
            case "HOE", "HOES" -> materialName.contains("_HOE");
            case "SHIELD" -> materialName.equals("SHIELD");
            case "BOW" -> materialName.equals("BOW");
            case "CROSSBOW" -> materialName.equals("CROSSBOW");
            case "MACE" -> materialName.equals("MACE");
            case "TRIDENT", "TRIDENTS" -> materialName.contains("TRIDENT");
            case "HELMET", "HELMETS" -> materialName.contains("HELMET");
            case "CHESTPLATE", "CHESTPLATES" -> materialName.contains("CHESTPLATE");
            case "LEGGINGS" -> materialName.contains("LEGGINGS");
            case "BOOTS" -> materialName.contains("BOOTS");
            case "SPEAR", "SPEARS" -> materialName.contains("SPEAR");
            default -> false;
        };
    }

    private @NotNull ItemStack createStaticItem(@NotNull GuiSlotDef def) {
        ItemStack item = new ItemStack(def.material());
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse(player, def.name(), Map.of()));
            meta.lore(def.lore().stream().map(l -> MessageUtil.parse(player, l, Map.of())).toList());
            if (def.glow()) {
                meta.setEnchantmentGlintOverride(true);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public void open() {
        loadPlayerData(true);
    }

    private void loadPlayerData(boolean openAfter) {
        CompletableFuture.runAsync(() -> {
            Map<Material, String> loadedActive = new HashMap<>();
            for (Material material : Material.values()) {
                if (material.isAir() || !material.isItem()) continue;
                grantService.getActiveSkin(player.getUniqueId(), material).join()
                        .ifPresent(id -> loadedActive.put(material, id));
            }

            Set<String> loadedOwned = grantService.getGrants(player.getUniqueId()).join().stream()
                    .map(grant -> grant.skinId())
                    .collect(Collectors.toSet());

            SchedulerUtil.runForEntity(plugin, player, () -> {
                activeSkins = loadedActive;
                ownedSkinIds = loadedOwned;
                refresh();
                if (openAfter) {
                    player.openInventory(inventory);
                }
            }, () -> {});
        }, SchedulerUtil.getVirtualThreadExecutor());
    }

    private void updateAfterEquip(@NotNull Material targetMat, @Nullable String skinId) {
        if (skinId == null) {
            activeSkins.remove(targetMat);
        } else {
            activeSkins.put(targetMat, skinId);
        }
        refresh();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        if (filterSlotCategories.containsKey(slot)) {
            if (slot != activeFilterSlot) {
                activeFilterSlot = slot;
                activeFilterCategory = filterSlotCategories.get(slot);
                refresh();
            }
            return;
        }

        if (48 == slot) {
            activeSearchSlot = slot;
            refresh();
            if (!isSearching) {
                chatInputUtil.getInput(player, Component.text("Search:"), input -> {
                    if (input == null) {
                        isSearching = false;
                        return;
                    }
                    search = input;

                    isSearching = true;
                    refresh();
                    SchedulerUtil.runForEntity(plugin, player, () -> player.openInventory(inventory), () -> {});
                    }, "selectionsearch", Component.text("Search for a skin"));
            } else {
                isSearching = false;
                search = null;
                refresh();
                return;
            }
        }

        Skin skin = skinMap.get(slot);
        if (skin != null) {
            if (!ownedSkinIds.contains(skin.id())) {
                MessageUtil.sendMessage(player, plugin.getConfig().getString("messages.skin-not-owned", "<red>✘ You don't own <white>{skin_name}<red>."), Map.of("skin_name", skin.name()));
                return;
            }

            ItemStack handItem = player.getInventory().getItemInMainHand();
            if (handItem.getType() == Material.AIR) {
                MessageUtil.sendMessage(player, "<red>You must be holding an item to apply this skin!");
                return;
            }
            Material targetMat = handItem.getType();

            if (skin.categories().stream().noneMatch(cat -> isMaterialInCategory(targetMat.name(), cat))) {
                MessageUtil.sendMessage(player, "<red>This skin cannot be applied to this item!");
                return;
            }

            String activeId = activeSkins.get(targetMat);
            if (skin.id().equals(activeId)) {
                grantService.setActiveSkin(player.getUniqueId(), targetMat, null).thenRun(() ->
                        SchedulerUtil.runForEntity(plugin, player, () -> {
                            MessageUtil.sendMessage(player, plugin.getConfig().getString("messages.skin-unequipped", "<yellow>Skin <white>{skin_name}<yellow> removed."), Map.of("skin_name", skin.name()));
                            updateAfterEquip(targetMat, null);
                        }, () -> {})
                );
            } else {
                grantService.setActiveSkin(player.getUniqueId(), targetMat, skin.id()).thenRun(() ->
                        SchedulerUtil.runForEntity(plugin, player, () -> {
                            MessageUtil.sendMessage(player, plugin.getConfig().getString("messages.skin-equipped", "<green>✔ Equipped <white>{skin_name}<green>."), Map.of("skin_name", skin.name()));
                            updateAfterEquip(targetMat, skin.id());
                        }, () -> {})
                );
            }
            return;
        }

        slots.stream().filter(s -> s.slot() == slot).findFirst().ifPresent(def -> {
            if (def.type() instanceof SlotType.BackSlot) {
                player.performCommand("skins");
            }
        });
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}