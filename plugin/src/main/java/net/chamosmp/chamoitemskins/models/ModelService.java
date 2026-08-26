package net.chamosmp.chamoitemskins.models;

import net.chamosmp.chamoitemskins.api.models.Model;
import net.chamosmp.chamoitemskins.api.objects.Skin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for applying BetterModel item models to {@link ItemStack}s.
 */
public final class ModelService extends NexoService implements Model {

    private static final String NAMESPACE = "chamoitemskins";
    private static final NamespacedKey ORIGINAL_MATERIAL_KEY = new NamespacedKey(NAMESPACE, "original_material");

    /**
     * Constructs a new ModelService.
     */
    public ModelService() {

    }

    @Override
    public @NotNull ItemStack createPreviewItem(@NotNull Skin skin) {
        Material material = skin.displayItem() != null ? skin.displayItem().material() : Material.BARRIER;
        ItemStack item = new ItemStack(material);
        if (shouldApplyItemModel(skin.modelId())) {
            item = getItemModel(item, skin.modelId());
        }
        return item;
    }

    @Override
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

    @Override
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
                meta.getPersistentDataContainer().set(ORIGINAL_MATERIAL_KEY, org.bukkit.persistence.PersistentDataType.STRING, oldItem.getType().name());
                newItem.setItemMeta(meta);
            }
            inventory.setItem(slot, newItem);
        }
    }

    @Override
    public void clearItemModel(@NotNull ItemStack item, @NotNull Inventory inventory) {
        if (item.getType().isAir()) {
            return;
        }
        var meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        boolean changed = false;
        if (meta.hasItemModel()) {
            meta.setItemModel(null);
            changed = true;
        }

        String originalMaterialName = meta.getPersistentDataContainer().get(ORIGINAL_MATERIAL_KEY, org.bukkit.persistence.PersistentDataType.STRING);
        if (originalMaterialName != null) {
            Material originalMaterial = Material.matchMaterial(originalMaterialName);
            if (originalMaterial != null && originalMaterial != item.getType()) {
                meta.getPersistentDataContainer().remove(ORIGINAL_MATERIAL_KEY);
                item.setItemMeta(meta);
                item.setType(originalMaterial);
                return;
            }
            meta.getPersistentDataContainer().remove(ORIGINAL_MATERIAL_KEY);
            changed = true;
        }

        if (changed) {
            item.setItemMeta(meta);
        }
    }

    @Override
    public void refreshMaterial(@NotNull Player player, @NotNull Material material, @Nullable Skin skin) {
        for (ItemStack item : collectItems(player)) {
            if (item != null && item.getType() == material) {
                if (skin != null) {
                    swapItemStack(getItemModel(item, skin.modelId()), item, player.getInventory());
                } else {
                    clearItemModel(item, player.getInventory());
                }
            }
        }
    }

    @Override
    public void refreshInventory(@NotNull Player player, @NotNull Map<Material, Skin> activeSkins) {
        for (ItemStack item : collectItems(player)) {
            if (item == null || item.getType().isAir()) {
                continue;
            }
            Skin skin = activeSkins.get(item.getType());
            if (skin != null) {
                swapItemStack(getItemModel(item, skin.modelId()), item, player.getInventory());
            } else {
                clearItemModel(item, player.getInventory());
            }
        }
    }

    private boolean shouldApplyItemModel(@NotNull String modelId) {
        return !modelId.isBlank();
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
        // Use the fixed namespace; BetterModel registers models at bettermodel:<modelId>
        return new NamespacedKey(NAMESPACE, modelId.toLowerCase());
    }

}
