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
     * @param id The skin ID.
     * @return The skin if found.
     */
    @NotNull Optional<Skin> getSkin(@NotNull String id);

    /**
     * @param material The item material.
     * @return All skins applicable to this material.
     */
    @NotNull Collection<Skin> getSkinsForMaterial(@NotNull Material material);

    /**
     * Saves a skin definition.
     * @param skin The skin to save.
     */
    void saveSkin(@NotNull Skin skin);

    /**
     * Deletes a skin definition.
     * @param id The skin ID.
     */
    void deleteSkin(@NotNull String id);

    /**
     * Reloads all skins from configuration.
     */
    void reloadSkins();
}
