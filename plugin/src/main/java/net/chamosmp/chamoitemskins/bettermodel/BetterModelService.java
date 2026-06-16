// --- plugin/src/main/java/net/chamosmp/chamoitemskins/bettermodel/BetterModelService.java ---
package net.chamosmp.chamoitemskins.bettermodel;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bukkit.BetterModelBukkit;
import kr.toxicity.model.api.bukkit.platform.BukkitItemStack;
import kr.toxicity.model.api.nms.NMS;
import kr.toxicity.model.api.platform.PlatformEntity;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.tracker.EntityTrackerRegistry;
import net.chamosmp.chamoitemskins.api.model.Skin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Service for applying BetterModel item models to {@link ItemStack}s.
 */
public final class BetterModelService {

    private static final String NAMESPACE = "bettermodel";

    public BetterModelService() { }

    /**
     * Returns whether a BetterModel renderer exists for the given model id.
     *
     * @param modelId Skin model id (bare name or {@code namespace:path}).
     */
    public boolean hasRenderer(@NotNull String modelId) {
        if (modelId.isBlank()) {
            return false;
        }
        try {
            return BetterModel.model(resolveRendererName(modelId)).isPresent();
        } catch (NullPointerException e) {
            return false;
        }
    }

    /**
     * Returns whether BetterModel is initialized and ready to use.
     */
    public boolean isAvailable() {
        try {
            return BetterModel.platform() != null;
        } catch (NullPointerException e) {
            return false;
        }
    }

    /**
     * Builds a GUI preview item using the skin model when available, otherwise the display material.
     */
    public @NotNull ItemStack createPreviewItem(@NotNull Skin skin) {
        Material material = skin.displayItem() != null ? skin.displayItem().material() : Material.BARRIER;
        ItemStack item = new ItemStack(material);
        if (shouldApplyItemModel(skin.modelId())) {
            applyItemModel(item, skin.modelId());
        }
        return item;
    }

    public void applyItemModel(@NotNull ItemStack item, @NotNull String modelId) {
        if (modelId.isBlank() || item.getType().isAir()) {
            return;
        }
        var meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        NamespacedKey key = resolveItemModelKey(modelId);
        Bukkit.getLogger().info("[ChamoItemSkins] Applying item model: " + key);
        //meta.setItemModel(key);
        //item.setItemMeta(meta);
        //BukkitItemStack item2 = valueOf(item);
        //NMS.createSkinItem();
    }

    /**
     * Removes a custom item model from a stack.
     */
    public void clearItemModel(@NotNull ItemStack item) {
        if (item.getType().isAir()) {
            return;
        }
        //var meta = item.getItemMeta();
        //if (meta == null || !meta.hasItemModel()) {
        //    return;
        //}
        //meta.setItemModel(null);
        //item.setItemMeta(meta);

    }

    /**
     * Updates every inventory stack of {@code material} for the player.
     */
    public void refreshMaterial(@NotNull Player player, @NotNull Material material, @Nullable Skin skin) {
        for (ItemStack item : collectItems(player)) {
            if (item != null && item.getType() == material) {
                if (skin != null) {
                    applyItemModel(item, skin.modelId());
                } else {
                    clearItemModel(item);
                }
            }
        }
    }

    /**
     * Re-applies active skin models across the player's entire inventory.
     */
    public void refreshInventory(@NotNull Player player, @NotNull Map<Material, Skin> activeSkins) {
        for (ItemStack item : collectItems(player)) {
            if (item == null || item.getType().isAir()) {
                continue;
            }
            Skin skin = activeSkins.get(item.getType());
            if (skin != null) {
                applyItemModel(item, skin.modelId());
            } else {
                clearItemModel(item);
            }
        }
    }

    private boolean shouldApplyItemModel(@NotNull String modelId) {
        return !modelId.isBlank();
    }

    private static @NotNull String resolveRendererName(@NotNull String modelId) {
        int separator = modelId.indexOf(':');
        if (separator >= 0 && separator < modelId.length() - 1) {
            return modelId.substring(separator + 1);
        }
        return modelId;
    }

    /**
     * Resolves the NamespacedKey for a model ID.
     * <p>
     * If the modelId already contains a colon (e.g. "mynamespace:mymodel"),
     * it is used as-is. Otherwise, the hardcoded "bettermodel" namespace is used
     * with the bare model name as the path — no subfolder prefix is added.
     */
    private static @NotNull NamespacedKey resolveItemModelKey(@NotNull String modelId) {
        int separator = modelId.indexOf(':');
        if (separator >= 0 && separator < modelId.length() - 1) {
            return new NamespacedKey(
                    modelId.substring(0, separator).toLowerCase(),
                    modelId.substring(separator + 1).toLowerCase()
            );
        }
        // Use the fixed namespace; BetterModel registers models at bettermodel:<modelId>
        return new NamespacedKey(NAMESPACE, modelId.toLowerCase());
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