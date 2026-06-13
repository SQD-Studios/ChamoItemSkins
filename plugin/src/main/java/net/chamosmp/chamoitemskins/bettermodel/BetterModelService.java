// --- plugin/src/main/java/net/chamosmp/chamoitemskins/bettermodel/BetterModelService.java ---
package net.chamosmp.chamoitemskins.bettermodel;

import kr.toxicity.model.api.BetterModel;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Service for interacting with BetterModel.
 */
public final class BetterModelService {

    public BetterModelService() {}

    /**
     * Applies a model to a player.
     * @param player  The player.
     * @param modelId The model ID (e.g., "namespace:model").
     */
    public void applyModel(@NotNull Player player, @NotNull String modelId) {
    }

    /**
     * Removes all models from a player.
     * @param player The player.
     */
    public void removeModels(@NotNull Player player) {
    }
}
