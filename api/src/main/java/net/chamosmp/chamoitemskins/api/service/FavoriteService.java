package net.chamosmp.chamoitemskins.api.service;

import net.chamosmp.chamoitemskins.api.objects.Skin;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public interface FavoriteService {

    /**
     * Add a skin for a player as their favorite
     *
     * @param player The player this action takes on
     * @param skin   The skin to add as a favorite
     * @return The {@link CompletableFuture}
     */
    CompletableFuture<Void> addFavoriteSkinToPlayer(@NotNull Player player, @NotNull Skin skin);

    /**
     * Remove a skin from a player as their favourite
     *
     * @param player The player this action takes on
     * @param skin   The skin to remove
     * @return The {@link CompletableFuture}
     */
    CompletableFuture<Void> removeFavoriteSkinFromPlayer(@NotNull Player player, @NotNull Skin skin);

    /**
     * Get a collection of skins, that a player has favorite
     *
     * @param player The player this action takes on
     * @return The collection of skins
     */
    @NotNull CompletableFuture<Collection<Skin>> getFavoriteSkinsFromPlayer(@NotNull Player player);

    /**
     * Change the favorite status for the skin, for the specified player
     *
     * @param player
     * @param skin
     */
    void changeFavoriteSkin(@NotNull Player player, @NotNull Skin skin);
}
