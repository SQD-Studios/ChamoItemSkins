// --- plugin/src/main/java/net/chamosmp/chamoitemskins/bettermodel/BetterModelService.java ---
package net.chamosmp.chamoitemskins.bettermodel;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bukkit.platform.BukkitEntity;
import kr.toxicity.model.api.tracker.EntityTracker;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for interacting with BetterModel.
 */
public final class BetterModelService {
    private final Map<UUID, EntityTracker> trackers = new HashMap<>();

    public BetterModelService() {}

    /**
     * Applies a model to a player.
     * @param player  The player.
     * @param modelId The model ID (e.g., "namespace:model").
     */
    public void applyModel(@NotNull Player player, @NotNull String modelId) {
        BetterModel.model(modelId).ifPresent(model -> {
            removeModels(player);
            EntityTracker tracker = model.create(new BukkitEntity(player));
            trackers.put(player.getUniqueId(), tracker);
        });
    }

    /**
     * Removes all models from a player.
     * @param player The player.
     */
    public void removeModels(@NotNull Player player) {
        EntityTracker tracker = trackers.remove(player.getUniqueId());
        if (tracker != null) {
            tracker.close();
        }
    }

    public void playAnimation(@NotNull Player player, @NotNull String animation) {
        EntityTracker tracker = trackers.get(player.getUniqueId());
        if (tracker != null) {
            tracker.animate(animation);
        }
    }
}
