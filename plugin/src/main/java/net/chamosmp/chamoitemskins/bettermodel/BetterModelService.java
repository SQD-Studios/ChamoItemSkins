package net.chamosmp.chamoitemskins.bettermodel;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.ModelRenderer;
import net.chamosmp.chamoitemskins.api.model.Skin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class BetterModelService {

    private static final Material BASE_MATERIAL = Material.LEATHER_HORSE_ARMOR;
    private static final NamespacedKey ORIGINAL_MATERIAL_KEY;

    private final Plugin plugin;

    public BetterModelService(@NotNull Plugin plugin) {
        this.plugin = plugin;
        ORIGINAL_MATERIAL_KEY = new NamespacedKey(plugin, "original_material");
    }

    // ... (keep hasRenderer, isAvailable unchanged) ...

    /**
     * Builds a GUI preview item using the skin's display material.
     * No model is applied – BetterModel only works on leather_horse_armor.
     */
    public @NotNull ItemStack createPreviewItem(@NotNull Skin skin) {
        Material material = skin.displayItem() != null ? skin.displayItem().material() : Material.BARRIER;
        return new ItemStack(material);
    }

    /**
     * Applies the BetterModel skin to an item via custom_model_data.
     * The item MUST be {@link Material#LEATHER_HORSE_ARMOR}.
     */
    public void applyItemModel(@NotNull ItemStack item, @NotNull String modelId) {
        if (modelId.isBlank() || item.getType().isAir() || item.getType() != BASE_MATERIAL) {
            return;
        }

        var meta = item.getItemMeta();
        if (meta == null) return;

        String rendererName = resolveRendererName(modelId);
        var optRenderer = BetterModel.model(rendererName);
        if (optRenderer.isEmpty()) {
            plugin.getLogger().warning("[ChamoItemSkins] BetterModel renderer not found: " + rendererName);
            return;
        }

        int cmd = optRenderer.get().customModelData();
        meta.setItemModel(null);
        meta.setCustomModelData(cmd);
        item.setItemMeta(meta);
    }

    /**
     * Converts an arbitrary item into a LEATHER_HORSE_ARMOR model item,
     * saving the original material in persistent data so it can be reverted.
     *
     * @param original the original item (e.g. DIAMOND_SWORD)
     * @param modelId  the skin model id
     * @return a new LEATHER_HORSE_ARMOR ItemStack with the model applied,
     *         or a clone of the original if conversion is not possible.
     */
    public @NotNull ItemStack convertToModelItem(@NotNull ItemStack original, @NotNull String modelId) {
        if (original.getType() == BASE_MATERIAL || !hasRenderer(modelId)) {
            // Already the base, just apply model
            ItemStack base = original.clone();
            applyItemModel(base, modelId);
            return base;
        }

        // Create new base item, copy all meta
        ItemStack modelItem = new ItemStack(BASE_MATERIAL, original.getAmount());
        ItemMeta originalMeta = original.getItemMeta();
        ItemMeta modelMeta = modelItem.getItemMeta();

        if (originalMeta != null && modelMeta != null) {
            // Copy display properties
            if (originalMeta.hasDisplayName()) {
                modelMeta.displayName(originalMeta.displayName());
            }
            if (originalMeta.hasLore()) {
                modelMeta.lore(originalMeta.lore());
            }
            if (originalMeta.hasEnchants()) {
                originalMeta.getEnchants().forEach((ench, level) -> modelMeta.addEnchant(ench, level, true));
            }
            modelMeta.setEnchantmentGlintOverride(originalMeta.hasEnchantmentGlintOverride());
            // Copy other attributes if needed (item flags, custom model data, etc.)
            // But we don't copy custom_model_data as we'll set our own

            // Save original material so we can revert later
            modelMeta.getPersistentDataContainer().set(
                    ORIGINAL_MATERIAL_KEY,
                    PersistentDataType.STRING,
                    original.getType().name()
            );

            modelItem.setItemMeta(modelMeta);
        }

        applyItemModel(modelItem, modelId);
        return modelItem;
    }

    /**
     * Reverts a model item back to its original material (if it was converted).
     *
     * @param modelItem a LEATHER_HORSE_ARMOR item that was previously converted
     * @return the reverted ItemStack, or the input unchanged if no original material is stored
     */
    public @NotNull ItemStack revertFromModelItem(@NotNull ItemStack modelItem) {
        if (modelItem.getType() != BASE_MATERIAL) return modelItem;

        ItemMeta meta = modelItem.getItemMeta();
        if (meta == null) return modelItem;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String originalMatName = pdc.get(ORIGINAL_MATERIAL_KEY, PersistentDataType.STRING);
        if (originalMatName == null) return modelItem;

        Material originalMaterial;
        try {
            originalMaterial = Material.valueOf(originalMatName);
        } catch (IllegalArgumentException e) {
            return modelItem;
        }

        // Remove the persistent data
        pdc.remove(ORIGINAL_MATERIAL_KEY);

        // Create the reverted item
        ItemStack original = new ItemStack(originalMaterial, modelItem.getAmount());
        // Copy display properties from the model item
        ItemMeta originalMeta = original.getItemMeta();
        if (originalMeta != null) {
            if (meta.hasDisplayName()) originalMeta.displayName(meta.displayName());
            if (meta.hasLore()) originalMeta.lore(meta.lore());
            if (meta.hasEnchants()) meta.getEnchants().forEach((ench, level) -> originalMeta.addEnchant(ench, level, true));
            originalMeta.setEnchantmentGlintOverride(meta.hasEnchantmentGlintOverride());
            original.setItemMeta(originalMeta);
        }

        return original;
    }

    /**
     * Removes the custom model data from a stack and reverts if necessary.
     */
    public void clearItemModel(@NotNull ItemStack item) {
        if (item.getType().isAir()) return;

        // If it was a converted model item, revert it first
        ItemStack reverted = revertFromModelItem(item);
        if (reverted != item) {
            // We can't replace the reference directly in the caller's inventory,
            // so we modify the original item's type and data.
            item.setType(reverted.getType());
            item.setItemMeta(reverted.getItemMeta());
            item.setAmount(reverted.getAmount());
            return;
        }

        var meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(0);
            meta.setItemModel(null);
            item.setItemMeta(meta);
        }
    }

    /**
     * Updates every inventory stack of {@code material} for the player,
     * converting them if a skin is active for that material.
     */
    public void refreshMaterial(@NotNull Player player, @NotNull Material material, @Nullable Skin skin) {
        for (ItemStack item : collectItems(player)) {
            if (item == null || item.getType().isAir()) continue;
            if (skin != null && item.getType() == material) {
                // Convert and apply
                ItemStack converted = convertToModelItem(item, skin.modelId());
                item.setType(converted.getType());
                item.setItemMeta(converted.getItemMeta());
                item.setAmount(converted.getAmount());
            } else if (skin == null && item.getType() == BASE_MATERIAL) {
                // Possibly a leftover model item – clear it
                clearItemModel(item);
            }
        }
    }

    /**
     * Re-applies active skin models across the player's entire inventory.
     */
    public void refreshInventory(@NotNull Player player, @NotNull Map<Material, Skin> activeSkins) {
        for (ItemStack item : collectItems(player)) {
            if (item == null || item.getType().isAir()) continue;

            // If the item is already a model item (BASE_MATERIAL), check if its
            // original material is in the active skins and reapply.
            if (item.getType() == BASE_MATERIAL) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    String orig = meta.getPersistentDataContainer().get(ORIGINAL_MATERIAL_KEY, PersistentDataType.STRING);
                    if (orig != null) {
                        Material originalMat = Material.valueOf(orig);
                        Skin skin = activeSkins.get(originalMat);
                        if (skin != null) {
                            applyItemModel(item, skin.modelId()); // just refresh the CMD
                        } else {
                            clearItemModel(item); // revert
                        }
                    }
                }
            } else {
                Skin skin = activeSkins.get(item.getType());
                if (skin != null) {
                    ItemStack converted = convertToModelItem(item, skin.modelId());
                    item.setType(converted.getType());
                    item.setItemMeta(converted.getItemMeta());
                    item.setAmount(converted.getAmount());
                }
            }
        }
    }

    private static @NotNull String resolveRendererName(@NotNull String modelId) {
        int separator = modelId.indexOf(':');
        if (separator >= 0 && separator < modelId.length() - 1) {
            return modelId.substring(separator + 1);
        }
        return modelId;
    }

    private static @NotNull Iterable<ItemStack> collectItems(@NotNull Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] combined = new ItemStack[inventory.getSize() + 5];
        ItemStack[] contents = inventory.getContents();
        System.arraycopy(contents, 0, combined, 0, contents.length);
        int offset = contents.length;
        combined[offset++] = inventory.getHelmet();
        combined[offset++] = inventory.getChestplate();
        combined[offset++] = inventory.getLeggings();
        combined[offset++] = inventory.getBoots();
        combined[offset]   = inventory.getItemInOffHand();
        return java.util.Arrays.asList(combined);
    }
}