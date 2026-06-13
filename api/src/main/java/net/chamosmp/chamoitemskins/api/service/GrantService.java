// --- api/src/main/java/net/chamosmp/chamoitemskins/api/service/GrantService.java ---
package net.chamosmp.chamoitemskins.api.service;

import net.chamosmp.chamoitemskins.api.model.SkinGrant;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing skin grants and active skins.
 */
public interface GrantService {
    /**
     * @param playerUuid The player's UUID.
     * @return All skin grants for this player.
     */
    @NotNull CompletableFuture<Collection<SkinGrant>> getGrants(@NotNull UUID playerUuid);

    /**
     * @param playerUuid The player's UUID.
     * @param skinId     The skin ID.
     * @return Whether the player owns the skin.
     */
    @NotNull CompletableFuture<Boolean> hasSkin(@NotNull UUID playerUuid, @NotNull String skinId);

    /**
     * Grants a skin to a player.
     * @param playerUuid The player's UUID.
     * @param skinId     The skin ID.
     * @param source     The source of the grant.
     * @return A future that completes when the grant is persisted.
     */
    @NotNull CompletableFuture<Void> grantSkin(@NotNull UUID playerUuid, @NotNull String skinId, @NotNull String source);

    /**
     * Revokes a skin from a player.
     * @param playerUuid The player's UUID.
     * @param skinId     The skin ID.
     * @return A future that completes when the revoke is persisted.
     */
    @NotNull CompletableFuture<Void> revokeSkin(@NotNull UUID playerUuid, @NotNull String skinId);

    /**
     * @param playerUuid The player's UUID.
     * @param material   The item material.
     * @return The ID of the active skin for this material, if any.
     */
    @NotNull CompletableFuture<Optional<String>> getActiveSkin(@NotNull UUID playerUuid, @NotNull Material material);

    /**
     * Sets the active skin for a material.
     * @param playerUuid The player's UUID.
     * @param material   The item material.
     * @param skinId     The skin ID, or null to remove.
     * @return A future that completes when the active skin is updated.
     */
    @NotNull CompletableFuture<Void> setActiveSkin(@NotNull UUID playerUuid, @NotNull Material material, @Nullable String skinId);
}
