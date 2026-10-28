package net.chamosmp.chamoitemskins.models;

import net.chamosmp.chamoitemskins.api.model.Skin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Service for applying BetterModel item models to {@link ItemStack}s.
 */
public final class ModelService extends NexoService {

    private static final String NAMESPACE = "chamoitemskins";

    public ModelService() {
    }

    private static @NotNull Iterable<ItemStack> collectItems(@NotNull Player player) {
        PlayerInventory inventory = player.getInventory();
        return java.util.Arrays.asList(inventory.getContents());
    }

    /**
     * Resolves the NamespacedKey for a model ID.
     * <p>
     * If the modelId already contains a colon (e.g. "mynamespace:mymodel"),
     * it is used as-is. Otherwise, the hardcoded "bettermodel" namespace is used
     * with the bare model name as the path - no subfolder prefix is added.
     */
    private static @NotNull NamespacedKey resolveItemModelKey(@NotNull String modelId) {
        int separator = modelId.indexOf(':');
        if (separator >= 0 && separator < modelId.length() - 1) {
            return new NamespacedKey(
                    modelId.substring(0, separator).toLowerCase(),
                    modelId.substring(separator + 1).toLowerCase()
            );
        }
        return new NamespacedKey(NAMESPACE, modelId.toLowerCase());
    }

    public @NotNull ItemStack createPreviewItem(@NotNull Skin skin) {
        Material material = skin.displayItem() != null ? skin.displayItem().material() : Material.BARRIER;
        ItemStack item = new ItemStack(material);
        if (shouldApplyItemModel(skin.modelId())) {
            item = getItemModel(item, skin.modelId());
        }
        return item;
    }

    public ItemStack getItemModel(@NotNull ItemStack item, @NotNull String modelId) {
        if (modelId.isBlank() || item.getType().isAir()) {
            return item;
        }

        if (isNexo(modelId)) return getNexoItem(item, modelId);

        var meta = item.getItemMeta();
        if (meta == null) return item;

        NamespacedKey key = resolveItemModelKey(modelId);
        meta.setItemModel(key);
        item.setItemMeta(meta);

        return item;
    }

    public void swapItemStack(@NotNull ItemStack oldItem, @NotNull ItemStack newItem, @NotNull Inventory inventory) {
        int slot = -1;
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] == oldItem) {
                slot = i;
                break;
            }
        }

        if (slot != -1) {
            var meta = newItem.getItemMeta();
            if (meta != null) {
                newItem.setItemMeta(meta);
            }
            inventory.setItem(slot, newItem);
        }
    }

    public void clearItemModel(@NotNull ItemStack item) {
        if (item.getType().isAir()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        boolean changed = false;
        if (meta.hasItemModel()) {
            meta.setItemModel(null);
            changed = true;
        }

        if (changed) {
            item.setItemMeta(meta);
        }
    }

    public void refreshMaterial(@NotNull Player player, @NotNull Material material, @Nullable Skin skin) {
        for (ItemStack item : collectItems(player)) {
            if (item != null && item.getType() == material) {
                if (skin != null) {
                    swapItemStack(getItemModel(item, skin.modelId()), item, player.getInventory());
                } else {
                    clearItemModel(item);
                }
            }
        }
    }

    public void refreshInventory(@NotNull Player player, @NotNull Map<Material, Skin> activeSkins) {
        for (ItemStack item : collectItems(player)) {
            if (item == null || item.getType().isAir()) {
                continue;
            }
            Skin skin = activeSkins.get(item.getType());
            if (skin != null) {
                swapItemStack(getItemModel(item, skin.modelId()), item, player.getInventory());
            } else {
                clearItemModel(item);
            }
        }
    }

    private boolean shouldApplyItemModel(@NotNull String modelId) {
        return !modelId.isBlank();
    }

}
