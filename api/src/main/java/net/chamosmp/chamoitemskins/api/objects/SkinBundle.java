package net.chamosmp.chamoitemskins.api.objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents a bundle of multiple skins.
 *
 * @param id      Unique bundle identifier.
 * @param name    MiniMessage display name.
 * @param skinIds List of skin IDs included in this bundle.
 */
public record SkinBundle(
        @NotNull String id,
        @NotNull String name,
        @Nullable List<String> skinIds
) {
}
