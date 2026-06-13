// --- plugin/src/main/java/net/chamosmp/chamoitemskins/database/DatabaseManager.java ---
package net.chamosmp.chamoitemskins.database;

import net.chamosmp.chamoitemskins.api.model.SkinGrant;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
}
