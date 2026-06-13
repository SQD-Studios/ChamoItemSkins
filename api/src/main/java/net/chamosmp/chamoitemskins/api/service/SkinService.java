// --- api/src/main/java/net/chamosmp/chamoitemskins/api/service/SkinService.java ---
package net.chamosmp.chamoitemskins.api.service;

import net.chamosmp.chamoitemskins.api.model.Skin;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;

/**
 * Service for managing skin definitions.
 */
public interface SkinService {
    /**
     * @return All registered skins.
     */
    @NotNull Collection<Skin> getSkins();

    /**
     * @return All registered bundles.
     */
    @NotNull Collection<net.chamosmp.chamoitemskins.api.model.SkinBundle> getBundles();

    /**
     * @param id The skin ID.
     * @return The skin if found.
     */
    @NotNull Optional<Skin> getSkin(@NotNull String id);

    /**
     * @param id The bundle ID.
     * @return The bundle if found.
     */
    @NotNull Optional<net.chamosmp.chamoitemskins.api.model.SkinBundle> getBundle(@NotNull String id);

    /**
     * @param material The item material.
     * @return All skins applicable to this material or its categories.
     */
    @NotNull Collection<Skin> getSkinsForMaterial(@NotNull Material material);

    /**
     * @param category The category name.
     * @return All skins in this category.
     */
    @NotNull Collection<Skin> getSkinsForCategory(@NotNull String category);

    /**
     * Saves a skin definition.
     * @param skin The skin to save.
     */
    void saveSkin(@NotNull Skin skin);

    /**
     * Saves a bundle definition.
     * @param bundle The bundle to save.
     */
    void saveBundle(@NotNull net.chamosmp.chamoitemskins.api.model.SkinBundle bundle);

    /**
     * Deletes a skin definition.
     * @param id The skin ID.
     */
    void deleteSkin(@NotNull String id);

    /**
     * Deletes a bundle definition.
     * @param id The bundle ID.
     */
    void deleteBundle(@NotNull String id);

    /**
     * Reloads all skins and bundles from configuration.
     */
    void reloadSkins();
}
