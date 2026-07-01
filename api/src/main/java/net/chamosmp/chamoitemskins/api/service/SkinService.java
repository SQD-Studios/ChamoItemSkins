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
     * Gets all registered skins.
     * @return A collection of all skins.
     */
    @NotNull Collection<Skin> getSkins();

    /**
     * Gets all registered bundles.
     * @return A collection of all bundles.
     */
    @NotNull Collection<net.chamosmp.chamoitemskins.api.model.SkinBundle> getBundles();

    /**
     * Gets a skin by its ID.
     * @param id The skin ID.
     * @return An optional containing the skin if found.
     */
    @NotNull Optional<Skin> getSkin(@NotNull String id);

    /**
     * Gets a bundle by its ID.
     * @param id The bundle ID.
     * @return An optional containing the bundle if found.
     */
    @NotNull Optional<net.chamosmp.chamoitemskins.api.model.SkinBundle> getBundle(@NotNull String id);

    /**
     * Gets all skins applicable to a specific material or its categories.
     * @param material The item material.
     * @return A collection of applicable skins.
     */
    @NotNull Collection<Skin> getSkinsForMaterial(@NotNull Material material);

    /**
     * Gets all skins belonging to a specific category.
     * @param category The category name.
     * @return A collection of skins in the category.
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

    /**
     * This is meant for the gui, after changing the skinid, to change in the database too.
     * It deletes the old skin, and you must define it again like this:
     * {@code new Skin(id, name, modelId, rarity, categories, enabled, noteMaterial, displayItem, animations)}
     * @param oldId The old id of the skin
     * @param newSkin The new skin definition
     */
    void changeId(@NotNull String oldId, Skin newSkin);
}
