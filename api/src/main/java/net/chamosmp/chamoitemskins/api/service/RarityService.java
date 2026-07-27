package net.chamosmp.chamoitemskins.api.service;

import net.chamosmp.chamoitemskins.api.objects.Rarity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface RarityService {

    /**
     * Checks if rarity features are enabled.
     *
     * @return True if enabled.
     */
    boolean isEnabled();

    /**
     * Gets all loaded rarities, sorted by priority.
     *
     * @return A list of rarities.
     */
    @NotNull List<Rarity> getRarities();

    /**
     * Gets a rarity by its ID.
     *
     * @param id The rarity ID.
     * @return An optional containing the rarity if found.
     */
    @NotNull Optional<Rarity> getRarity(@NotNull String id);

    /**
     * Gets the default rarity (the one with the lowest priority).
     *
     * @return The default rarity.
     */
    @NotNull Rarity getDefaultRarity();

    /**
     * Resolves a rarity ID to a Rarity object, falling back to default if not found.
     *
     * @param id The rarity ID to resolve.
     * @return The resolved rarity.
     */
    @NotNull Rarity resolve(@Nullable String id);

    /**
     * Gets an unmodifiable map of all rarities.
     *
     * @return The rarity map.
     */
    @NotNull Map<String, Rarity> getRarityMap();
}
