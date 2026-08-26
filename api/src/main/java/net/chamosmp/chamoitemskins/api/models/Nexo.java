package net.chamosmp.chamoitemskins.api.models;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/// @apiNote Please do not use this directly. This is internal. Instead use {@link Model}
@ApiStatus.Internal
public interface Nexo {

    /// Is Nexo Enabled?
    ///
    /// @return true = Nexo is enabled, false if Nexo is not found
    boolean isNexoEnabled();

    /// Gets the nexo item id from the ChamoItemSkins format (If it starts with nexo: it will parse that)
    ///
    /// @param modelId The model id, to get the nexo item from
    /// @return The nexo item id from the model id
    String getNexoId(@NotNull String modelId);

    /// It makes a Nexo Item {@link ItemStack} with a current {@link ItemStack}
    ///
    /// @param itemStack The current itemStack to have as a base
    /// @param nexoId    The Model ID
    /// @return The new {@link ItemStack}
    ItemStack getNexoItem(@NotNull ItemStack itemStack, @NotNull String nexoId);

    /// Is the modelId nexo?
    ///
    /// @param modelId The modelId to check it on
    /// @return If the modelId has a nexo: prefix
    boolean isNexo(@NotNull String modelId);
}
