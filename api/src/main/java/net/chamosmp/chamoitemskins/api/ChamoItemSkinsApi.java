package net.chamosmp.chamoitemskins.api;

import net.chamosmp.chamoitemskins.api.models.Model;
import net.chamosmp.chamoitemskins.api.service.*;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Entry point for the ChamoItemSkins API.
 */
public interface ChamoItemSkinsApi {
    /**
     * Get the ChamoItemSkins API
     *
     * @return The singleton instance of the API.
     * @throws IllegalStateException if the plugin is not loaded.
     */
    static @NotNull ChamoItemSkinsApi get() {
        var instance = Bukkit.getServicesManager().load(ChamoItemSkinsApi.class);
        if (instance == null) {
            throw new IllegalStateException("ChamoItemSkinsApi is not loaded!");
        }
        return instance;
    }

    /// Get the {@link CacheService} class
    @ApiStatus.Internal
    @NotNull CacheService getCacheService();

    /// Get the {@link CategoryService} class
    @NotNull CategoryService getCategoryService();

    /// Get the {@link GrantService} class
    @NotNull GrantService getGrantService();

    /// Get the {@link LanguageService} class
    @ApiStatus.Internal
    @NotNull LanguageService getLanguageService();

    /// Get the {@link LogService} class
    @ApiStatus.Internal
    @NotNull LogService getLogService();

    /// Get the {@link MigrateService} class
    @NotNull MigrateService getMigrateService();

    /// Get the {@link RarityService} class
    @NotNull RarityService getRarityService();

    /// Get the {@link SkinService} class
    @NotNull SkinService getSkinService();

    /// Get the {@link Model} class
    @NotNull Model getModelClass();

    @NotNull FavoriteService getFavoriteService();
}
