package net.chamosmp.chamoitemskins.api.models;

import net.chamosmp.chamoitemskins.api.objects.Skin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

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
     * @apiNote This currently hasn't been implemented. It currently does nothing so you must use {@link Model#applyItemModel(ItemStack, String)}. I will update the API Note on there so you can see
     */
    ItemStack getItemModel(@NotNull ItemStack item, @NotNull String modelId);

    /**
     * Removes a custom item model from a stack.
     *
     * @apiNote This doesn't work with {@link Model#getItemModel(ItemStack, String)}
     */
    void clearItemModel(@NotNull ItemStack item);

    /**
     * Updates every inventory stack of {@code material} for the player.
     *
     * @apiNote This doesn't work with {@link Model#getItemModel(ItemStack, String)}
     */
    void refreshMaterial(@NotNull Player player, @NotNull Material material, @Nullable Skin skin);

    /**
     * Re-applies active skin models across the player's entire inventory.
     *
     * @apiNote This doesn't work with {@link Model#getItemModel(ItemStack, String)}
     */
    void refreshInventory(@NotNull Player player, @NotNull Map<Material, Skin> activeSkins);
}
