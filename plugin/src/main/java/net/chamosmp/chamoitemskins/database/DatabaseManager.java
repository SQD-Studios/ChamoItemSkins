// --- plugin/src/main/java/net/chamosmp/chamoitemskins/database/DatabaseManager.java ---
package net.chamosmp.chamoitemskins.database;

import net.chamosmp.chamoitemskins.api.model.Skin;
import net.chamosmp.chamoitemskins.api.model.SkinGrant;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Sealed interface for database management.
 */
public sealed interface DatabaseManager permits MySQLDatabase, SQLiteDatabase {
    /**
     * Initializes the database connection and creates tables.
     */
    void init();

    /**
     * Closes the database connection.
     */
    void close();

    /**
     * Grants a skin to a player.
     */
    @NotNull CompletableFuture<Void> grantSkin(@NotNull UUID playerUuid, @NotNull String skinId, @NotNull String source);

    /**
     * Revokes a skin from a player.
     */
    @NotNull CompletableFuture<Void> revokeSkin(@NotNull UUID playerUuid, @NotNull String skinId);

    /**
     * Gets all skin grants for a player.
     */
    @NotNull CompletableFuture<Collection<SkinGrant>> getGrants(@NotNull UUID playerUuid);

    /**
     * Gets the active skin for a specific item type.
     */
    @NotNull CompletableFuture<Optional<String>> getActiveSkin(@NotNull UUID playerUuid, @NotNull Material material);

    /**
     * Sets the active skin for a specific item type.
     */
    @NotNull CompletableFuture<Void> setActiveSkin(@NotNull UUID playerUuid, @NotNull Material material, @Nullable String skinId);

    /**
     * Logs an action to the database.
     */
    void logAction(@NotNull UUID playerUuid, @NotNull String action, @NotNull String target, @Nullable String metadata);

    @NotNull CompletableFuture<Void> migrateSkinId(String oldSkinId, @NotNull String newSkinId);

    /**
     * Grants a skin with an optional expiration date.
     * @param playerUuid the player UUID
     * @param skinId     the skin ID
     * @param source     the grant source
     * @param expiresAt  the expiration timestamp, or null for permanent
     * @return a CompletableFuture that completes when the grant is stored
     */
    @NotNull CompletableFuture<Void> grantSkinWithExpiry(
            @NotNull UUID playerUuid,
            @NotNull String skinId,
            @NotNull String source,
            @Nullable LocalDateTime expiresAt
    );

    /**
     * Retrieves all grants that have expired (expires_at is not null and <= now).
     * @return a CompletableFuture containing a collection of expired grant entries
     */
    @NotNull CompletableFuture<Collection<ExpiredGrant>> getExpiredGrants();

    /**
     * Simple record representing an expired grant entry.
     */
    record ExpiredGrant(@NotNull UUID playerUuid, @NotNull String skinId) {}
}
