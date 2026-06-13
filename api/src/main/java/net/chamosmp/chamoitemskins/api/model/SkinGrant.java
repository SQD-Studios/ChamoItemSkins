// --- api/src/main/java/net/chamosmp/chamoitemskins/api/model/SkinGrant.java ---
package net.chamosmp.chamoitemskins.api.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a player's ownership of a specific skin.
 *
 * @param grantId    Unique ID of the grant.
 * @param playerUuid UUID of the player who owns the skin.
 * @param skinId     ID of the skin owned.
 * @param grantedAt  Timestamp when the skin was granted.
 * @param source     Source of the grant (e.g., "NOTE", "ADMIN").
 */
public record SkinGrant(
        UUID grantId,
        UUID playerUuid,
        String skinId,
        Instant grantedAt,
        String source
) {}
