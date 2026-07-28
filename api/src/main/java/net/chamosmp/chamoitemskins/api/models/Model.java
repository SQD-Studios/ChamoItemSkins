package net.chamosmp.chamoitemskins.api.models;

import net.chamosmp.chamoitemskins.api.objects.Skin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@ApiStatus.Internal
public interface Model extends Nexo {

    /**
     * Builds a GUI preview item using the skin model when available, otherwise the display material.
     *
     * @param skin The skin to apply
     */
    @NotNull ItemStack createPreviewItem(@NotNull Skin skin);

    /**
     * Applies a custom item model to an {@link ItemStack}.
     *
     * @param item    The item stack to apply the model to.
     * @param modelId The ID of the model to apply.
     * @apiNote {@link Model#getItemModel(ItemStack, String)} isn't fully implemented yet so you must use in the meantime
     * @deprecated This is deprecated in favor of {@link Model#getItemModel(ItemStack, String)}. This method may be unsafe when using Nexo Items. {@link Model#getItemModel(ItemStack, String)} is not a direct replacement. It returns an {@link ItemStack}, which you have to replace
     */
    @Deprecated(forRemoval = true)
    void applyItemModel(@NotNull ItemStack item, @NotNull String modelId);

    /**
     * Returns an item, with a model applied to it.
     *
     * @param item    The item stack to apply the model to.
     * @param modelId The ID of the model to apply.
     * @return The {@link ItemStack}
     */
    ItemStack getItemModel(@NotNull ItemStack item, @NotNull String modelId);

    /**
     *
     * Replaces an item in an inventory
     *
     * @param oldItem   The item to be replaced
     * @param newItem   The item that should replace {@code oldItem}
     * @param inventory The inventory which this is happening at
     */
    void swapItemStack(@NotNull ItemStack oldItem, @NotNull ItemStack newItem, @NotNull Inventory inventory);

    /**
     * Removes a custom item model from a stack.
     *
     */
    void clearItemModel(@NotNull ItemStack item, @NotNull Inventory inventory);

    /**
     * Updates every inventory stack of {@code material} for the player.
     *
     */
    void refreshMaterial(@NotNull Player player, @NotNull Material material, @Nullable Skin skin);

    /**
     * Re-applies active skin models across the player's entire inventory.
     *
     */
    void refreshInventory(@NotNull Player player, @NotNull Map<Material, Skin> activeSkins);
}
