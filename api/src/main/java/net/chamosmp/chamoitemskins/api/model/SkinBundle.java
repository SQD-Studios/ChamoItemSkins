// --- api/src/main/java/net/chamosmp/chamoitemskins/api/model/SkinBundle.java ---
package net.chamosmp.chamoitemskins.api.model;

import java.util.List;

/**
 * Represents a bundle of multiple skins.
 *
 * @param id      Unique bundle identifier.
 * @param name    MiniMessage display name.
 * @param skinIds List of skin IDs included in this bundle.
 */
public record SkinBundle(
        String id,
        String name,
        List<String> skinIds
) {}
