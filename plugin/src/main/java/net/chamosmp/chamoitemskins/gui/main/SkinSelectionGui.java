package net.chamosmp.chamoitemskins.gui.main;

import net.chamosmp.chamoitemskins.api.objects.Category;
import net.chamosmp.chamoitemskins.api.objects.Skin;
import net.chamosmp.chamoitemskins.api.service.GrantService;
import net.chamosmp.chamoitemskins.api.service.SkinService;
import net.chamosmp.chamoitemskins.gui.GuiFillerUtil;
import net.chamosmp.chamoitemskins.gui.GuiMultiPageUtil;
import net.chamosmp.chamoitemskins.gui.config.GuiSlotDef;
import net.chamosmp.chamoitemskins.gui.config.SlotType;
import net.chamosmp.chamoitemskins.listener.GuiListener;
import net.chamosmp.chamoitemskins.manager.RarityManager;
import net.chamosmp.chamoitemskins.models.ModelService;
import net.chamosmp.chamoitemskins.scheduler.SchedulerUtil;
import net.chamosmp.chamoitemskins.util.ChatInputUtil;
import net.chamosmp.chamoitemskins.util.ConfigUtil;
import net.chamosmp.chamoitemskins.util.LoggerUtil;
import net.chamosmp.chamoitemskins.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
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
    private final MessageUtil messageUtil;

    private final Map<Integer, String> filterSlotCategories = new HashMap<>();
    private final String baseCategory;

    private final GuiMultiPageUtil<Skin> pagination;

    private int PAGE_NEXT;
    private int PAGE_PRE;


    /**
     * I need to Javadoc this because I WON'T REMEMBER IT<br>
     * {@code 1} = Owned<br>
     * {@code 2} = All<br>
     * {@code Default} = 2<br>
     */
    private int filterLoreCycle = 2;


    private String search;
    private boolean isSearching = false;
    private int searchSlot = 0;

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
            ChatInputUtil chatInputUtil, MessageUtil messageUtil
    ) {
        this.plugin = plugin;
        this.player = player;
        this.skinService = skinService;
        this.grantService = grantService;
        this.rarityManager = rarityManager;
        this.modelService = modelService;
        this.slots = slots;
        this.baseCategory = category;
        this.chatInputUtil = chatInputUtil;
        this.messageUtil = messageUtil;
        this.inventory = Bukkit.createInventory(this, size, MessageUtil.parse(player, title, Map.of("category", category, "material", category)));

        Set<Integer> reserved = new HashSet<>();
        for (GuiSlotDef def : slots) {
            if (def.type() instanceof SlotType.FilterSlot) {
                filterSlotCategories.put(def.slot(), "category");
            }
            if (!(def.type() instanceof SlotType.SkinSlot)) {
                reserved.add(def.slot());
            }
        }

        this.pagination = new GuiMultiPageUtil<>(
                inventory.getSize(),
                this::isBorderSlot,
                reserved
        );

        this.pinnedSkins = List.copyOf(skinService.getSkins().stream().filter(Skin::enabled).toList());

        List<Skin> visibleSkins = filterSkins(pinnedSkins);
        pagination.setItems(visibleSkins);

        this.PAGE_NEXT = inventory.getSize() - 1;
        this.PAGE_PRE = inventory.getSize() - 2;

        refresh();
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

        List<Skin> pageSkins = pagination.getCurrentPageItems();
        List<Integer> available = pagination.getAvailableSlots();

        int index = 0;
        for (Skin skin : pageSkins) {
            if (index >= available.size()) break;
            int slot = available.get(index);
            skinMap.put(slot, skin);
            inventory.setItem(slot, createSkinItem(skin));
            index++;
        }

        for (GuiSlotDef def : slots) {
            switch (def.type()) {
                case SlotType.SkinSlot _ -> {
                }
                case SlotType.FilterSlot _ -> {
                    inventory.setItem(def.slot(), createFilterItem(def));
                }
                case SlotType.SearchSlot _ -> {
                    inventory.setItem(def.slot(), createSearchItem(def));
                }
                case SlotType.NextPage _ -> {
                    if (pagination.hasNext()) {
                        inventory.setItem(def.slot(), createNavigationItem(def));
                        PAGE_NEXT = def.slot();
                    }
                }
                case SlotType.PreviousPage _ -> {
                    if (pagination.hasPrev()) {
                        inventory.setItem(def.slot(), createNavigationItem(def));
                        PAGE_PRE = def.slot();
                    }
                }
                default -> {
                    inventory.setItem(def.slot(), createStaticItem(def));
                }
            }
        }

        GuiFillerUtil.apply(plugin, inventory, player);
    }

    private @NotNull List<Skin> filterSkins(@NotNull List<Skin> source) {
        String filter = filterLoreCycle == 1 ? "OWNED" : "ALL";
        return source.stream()
                .filter(skin -> baseCategory == null
                        || baseCategory.equalsIgnoreCase("ALL")
                        || skin.categories().stream().anyMatch(cat -> cat.name().equalsIgnoreCase(baseCategory)))
                .filter(skin -> matchesCategoryFilter(skin, filter))
                .filter(skin -> matchesOwnershipFilter(skin, filter))
                .filter(this::matchesSearchFilter)
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
        return skin.categories().stream().anyMatch(cat -> cat.name().equalsIgnoreCase(filter));
    }

    private boolean matchesOwnershipFilter(@NotNull Skin skin, @NotNull String filter) {
        if (filter.equalsIgnoreCase("OWNED")) {
            return ownedSkinIds.contains(skin.id());
        }
        return true;
    }

    private static final List<String> ALL_FILTERS = List.of("OWNED", "ALL");

    private @NotNull ItemStack createFilterItem(@NotNull GuiSlotDef def) {
        ItemStack item = new ItemStack(def.material());
        var meta = item.getItemMeta();
        if (meta != null) {

            meta.customName(MessageUtil.parse(player, def.name(), Map.of()));


            YamlConfiguration config = ConfigUtil.loadOrAdapt(plugin, "config.yml");
            String character = config.getString("filter.chosen-character");

            Map<String, String> placeholders;

            if (character == null) return item;
            if (filterLoreCycle == 1) {
                placeholders = Map.of("owned", MessageUtil.placeholder(character, Map.of("category", "Owned")), "all", "All Skins");
            } else {
                placeholders = Map.of("owned", "Owned", "all", MessageUtil.placeholder(character, Map.of("category", "All Skins")));
            }
            List<String> lore = new ArrayList<>(MessageUtil.placeholder(def.lore(), placeholders));
            meta.lore(lore.stream().map(l -> MessageUtil.parse(player, l, Map.of())).toList());


            meta.setEnchantmentGlintOverride(def.glow());

            item.setItemMeta(meta);
        }
        return item;
    }

    private @NotNull ItemStack createNavigationItem(@NotNull GuiSlotDef def) {
        ItemStack item = new ItemStack(def.material());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.customName(MessageUtil.parse(def.name()));
            List<Component> loreList = new ArrayList<>();
            for (String lore : def.lore()) {
                loreList.add(MessageUtil.parse(lore));
            }
            meta.lore(loreList);
            meta.setEnchantmentGlintOverride(def.glow());
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
            String customName = skin.name();
            if (active) {
                customName = "<dark_gray><i>[EQUIPPED] <white>" + customName;
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

            meta.customName(MessageUtil.parse(player, customName, Map.of()));
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
            String safeSearch = search == null ? "Nothing" : search;
            Map<String, String> placeholders = Map.of("search", safeSearch);

            meta.customName(MessageUtil.parse(player, def.name(), Map.of()));
            List<String> lore = new ArrayList<>(MessageUtil.placeholder(def.lore(), placeholders));

            meta.lore(lore.stream().map(l -> MessageUtil.parse(player, l, Map.of())).toList());
            item.setItemMeta(meta);
        }
        searchSlot = def.slot();
        return item;
    }

    private boolean isMaterialInCategory(Material materialName, Category category) {
        return category.isAllowed(materialName);
    }

    private @NotNull ItemStack createStaticItem(@NotNull GuiSlotDef def) {
        if (def.material() != null) {
            ItemStack item = new ItemStack(def.material());
            var meta = item.getItemMeta();
            if (meta != null) {
                meta.customName(MessageUtil.parse(player, def.name(), Map.of()));
                meta.lore(def.lore().stream().map(l -> MessageUtil.parse(player, l, Map.of())).toList());
                if (def.glow()) {
                    meta.setEnchantmentGlintOverride(true);
                }
                item.setItemMeta(meta);
            }
            return item;
        }
        return new ItemStack(Material.BARRIER);
    }

    public void open() {
        loadPlayerData(true);
    }

    private void loadPlayerData(boolean openAfter) {
        grantService.getAllActiveSkins(player.getUniqueId()).thenAccept(loadedActive -> {
            grantService.getGrants(player.getUniqueId()).thenAccept(grants -> {
                Set<String> loadedOwned = grants.stream()
                        .map(grant -> grant.skinId())
                        .collect(Collectors.toSet());

                SchedulerUtil.runForEntity(plugin, player, () -> {
                    activeSkins = loadedActive;
                    ownedSkinIds = loadedOwned;
                    refresh();
                    if (openAfter) {
                        player.openInventory(inventory);
                    }
                }, () -> {
                });
            });
        }).exceptionally(ex -> {
            LoggerUtil.log(LoggerUtil.LogType.SEVERE, "Failed to load player data: " + ex.getMessage());
            return null;
        });
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
            if (filterLoreCycle == 1) {
                filterLoreCycle = 2;
            } else {
                filterLoreCycle = 1;
            }
            refresh();
        }

        if (searchSlot == slot) {
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
                    SchedulerUtil.runForEntity(plugin, player, () -> player.openInventory(inventory), () -> {
                    });
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
                messageUtil.sendLangMessage(player, "skin-not-owned", Map.of("skin_name", skin.name()));
                return;
            }

            ItemStack handItem = player.getInventory().getItemInMainHand();
            if (handItem.getType() == Material.AIR) {
                messageUtil.sendLangMessage(player, "holding");
                return;
            }
            Material targetMat = handItem.getType();

            if (skin.categories().stream().noneMatch(cat -> isMaterialInCategory(targetMat, cat))) {
                messageUtil.sendLangMessage(player, "cannot-item");
                return;
            }

            String activeId = activeSkins.get(targetMat);
            if (skin.id().equals(activeId)) {
                grantService.setActiveSkin(player.getUniqueId(), targetMat, null).thenRun(() ->
                        SchedulerUtil.runForEntity(plugin, player, () -> {
                            messageUtil.sendLangMessage(player, "skin-unequipped", Map.of("skin_name", skin.name()));
                            updateAfterEquip(targetMat, null);
                        }, () -> {
                        })
                );
            } else {
                grantService.setActiveSkin(player.getUniqueId(), targetMat, skin.id()).thenRun(() ->
                        SchedulerUtil.runForEntity(plugin, player, () -> {
                            messageUtil.sendLangMessage(player, "skin-equipped", Map.of("skin_name", skin.name()));
                            updateAfterEquip(targetMat, skin.id());
                        }, () -> {
                        })
                );
            }
            return;
        }

        slots.stream().filter(s -> s.slot() == slot).findFirst().ifPresent(def -> {
            if (Objects.requireNonNull(def.type()) instanceof SlotType.BackSlot) {
                player.performCommand("skins");
            }
        });

        // Navigation items
        if (slot == PAGE_PRE && pagination.hasPrev()) {
            pagination.prevPage();
            refresh();
        } else if (slot == PAGE_NEXT && pagination.hasNext()) {
            pagination.nextPage();
            refresh();
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}