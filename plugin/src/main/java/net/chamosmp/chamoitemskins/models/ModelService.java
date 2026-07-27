package net.chamosmp.chamoitemskins.models;

import net.chamosmp.chamoitemskins.api.models.Model;
import net.chamosmp.chamoitemskins.api.objects.Skin;
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
public final class ModelService extends NexoService implements Model {

    private static final String NAMESPACE = "chamoitemskins";

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
            applyItemModel(item, skin.modelId());
        }
        return item;
    }

    @Override
    public void applyItemModel(@NotNull ItemStack item, @NotNull String modelId) {
        if (modelId.isBlank() || item.getType().isAir()) {
            return;
        }
        var meta = item.getItemMeta();
        if (meta == null) {
            return;
        }


        if (isNexo(modelId)) {
            applyNexoItem(item, modelId);
            return;
        }


        NamespacedKey key = resolveItemModelKey(modelId);
        meta.setItemModel(key);
        item.setItemMeta(meta);

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
    public void clearItemModel(@NotNull ItemStack item) {
        if (item.getType().isAir()) {
            return;
        }
        var meta = item.getItemMeta();
        if (meta == null || !meta.hasItemModel()) {
            return;
        }
        meta.setItemModel(null);
        item.setItemMeta(meta);
    }

    @Override
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

    @Override
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
        combined[offset] = inventory.getItemInOffHand();
        return java.util.Arrays.asList(combined);
    }
}
