// --- api/src/main/java/net/chamosmp/chamoitemskins/api/ChamoItemSkinsApi.java ---
package net.chamosmp.chamoitemskins.api;

import net.chamosmp.chamoitemskins.api.service.GrantService;
import net.chamosmp.chamoitemskins.api.service.SkinService;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * Entry point for the ChamoItemSkins API.
 */
public interface ChamoItemSkinsApi {
    /**
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

    /**
     * @return The skin service.
     */
    @NotNull SkinService getSkinService();

    /**
     * @return The grant service.
     */
    @NotNull GrantService getGrantService();
}
