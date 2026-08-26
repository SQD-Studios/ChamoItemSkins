package net.chamosmp.chamoitemskins.manager;

import net.chamosmp.chamoitemskins.api.event.SkinEquipEvent;
import net.chamosmp.chamoitemskins.api.event.SkinGrantEvent;
import net.chamosmp.chamoitemskins.api.event.SkinRevokeEvent;
import net.chamosmp.chamoitemskins.api.event.SkinUnequipEvent;
import net.chamosmp.chamoitemskins.api.objects.Skin;
import net.chamosmp.chamoitemskins.api.objects.SkinBundle;
import net.chamosmp.chamoitemskins.api.objects.SkinGrant;
import net.chamosmp.chamoitemskins.api.service.GrantService;
import net.chamosmp.chamoitemskins.api.service.LogService;
import net.chamosmp.chamoitemskins.database.DatabaseManager;
import net.chamosmp.chamoitemskins.models.ModelService;
import net.chamosmp.chamoitemskins.scheduler.SchedulerUtil;
import net.chamosmp.chamoitemskins.util.LoggerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of GrantService.
 */
public final class GrantManager implements GrantService {
    private final Plugin plugin;
    private final DatabaseManager db;
    private final CacheManager cache;
    private final SkinManager skinManager;
    private final LogService logService;
    private final ModelService modelService;

    /**
     * Constructs a new GrantManager.
     *
     * @param plugin       The plugin instance.
     * @param db           The database manager.
     * @param cache        The cache manager.
     * @param skinManager  The skin manager.
     * @param logService   The log service.
     * @param modelService The model service.
     */
    public GrantManager(Plugin plugin, DatabaseManager db, CacheManager cache, SkinManager skinManager, LogService logService, ModelService modelService) {
        this.plugin = plugin;
        this.db = db;
        this.cache = cache;
        this.skinManager = skinManager;
        this.logService = logService;
        this.modelService = modelService;
    }

    @Override
    public @NotNull CompletableFuture<Collection<SkinGrant>> getGrants(@NotNull UUID playerUuid) {
        Collection<SkinGrant> cached = cache.getGrants(playerUuid);
        if (cached != null) return CompletableFuture.completedFuture(cached);

        return db.getGrants(playerUuid).thenApply(grants -> {
            cache.putGrants(playerUuid, grants);
            return grants;
        });
    }

    @Override
    public @NotNull CompletableFuture<Boolean> hasSkin(@NotNull UUID playerUuid, @NotNull String skinId) {
        return getGrants(playerUuid).thenApply(grants ->
                grants.stream().anyMatch(g -> g.skinId().equals(skinId))
        );
    }

    @Override
    public @NotNull CompletableFuture<Void> grantSkin(@NotNull UUID playerUuid, @NotNull String skinId, @NotNull String source) {
        return grantSkin(playerUuid, skinId, source, -1);
    }

    @Override
    public @NotNull CompletableFuture<Void> grantSkin(@NotNull UUID playerUuid, @NotNull String skinId, @NotNull String source, int days) {

        Player player = Bukkit.getPlayer(playerUuid);
        Skin skin = skinManager.getSkin(skinId).orElse(null);

        if (player != null && skin != null) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            SchedulerUtil.runSync(plugin, () -> {
                SkinGrantEvent event = new SkinGrantEvent(player, skin, source);
                Bukkit.getPluginManager().callEvent(event);

                if (!event.isCancelled()) {
                    // Calculate expiration. If days <= 0, set to null (never expires) or handle accordingly.
                    LocalDateTime expiresAt = days > 0 ? LocalDateTime.now().plusDays(days) : null;
                    db.grantSkinWithExpiry(playerUuid, skinId, source, expiresAt)
                            .thenRun(() -> {
                                cache.invalidate(playerUuid);
                                logService.log(playerUuid, "GRANT", skinId, source);
                                future.complete(null);
                            })
                            .exceptionally(ex -> {
                                future.completeExceptionally(ex);
                                return null;
                            });
                } else {
                    future.completeExceptionally(new RuntimeException("Grant cancelled by event"));
                }
            });
            return future;
        }
        LocalDateTime expiresAt = days > 0 ? LocalDateTime.now().plusDays(days) : null;
        return db.grantSkinWithExpiry(playerUuid, skinId, source, expiresAt)
                .thenRun(() -> cache.invalidate(playerUuid));
    }

    @Override
    public void checkAndRevokeExpiredGrants() {
        db.getExpiredGrants().thenAccept(expiredGrants -> {
            if (expiredGrants.isEmpty()) return;

            for (DatabaseManager.ExpiredGrant entry : expiredGrants) {
                revokeSkin(entry.playerUuid(), entry.skinId());
            }
        }).exceptionally(ex -> {
            LoggerUtil.log(LoggerUtil.LogType.WARNING, "Failed to check expired skins: " + ex);
            return null;
        });
    }

    @Override
    public @NotNull CompletableFuture<Void> revokeSkin(@NotNull UUID playerUuid, @NotNull String skinId) {
        Player player = Bukkit.getPlayer(playerUuid);
        Skin skin = skinManager.getSkin(skinId).orElse(null);

        if (player != null && skin != null) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            SchedulerUtil.runSync(plugin, () -> {
                SkinRevokeEvent event = new SkinRevokeEvent(player, skin);
                Bukkit.getPluginManager().callEvent(event);

                if (!event.isCancelled()) {
                    db.revokeSkin(playerUuid, skinId).thenRun(() -> {
                        cache.invalidate(playerUuid);
                        logService.log(playerUuid, "REVOKE", skinId, null);
                        future.complete(null);
                    });
                } else {
                    future.completeExceptionally(new RuntimeException("Revoke cancelled by event"));
                }
            });
            return future;
        }

        return db.revokeSkin(playerUuid, skinId).thenRun(() -> cache.invalidate(playerUuid));
    }

    @Override
    public @NotNull CompletableFuture<Optional<String>> getActiveSkin(@NotNull UUID playerUuid, @NotNull Material material) {
        return db.getActiveSkin(playerUuid, material);
    }

    @Override
    public @NotNull CompletableFuture<Map<Material, String>> getAllActiveSkins(@NotNull UUID playerUuid) {
        return db.getAllActiveSkins(playerUuid);
    }

    @Override
    public @NotNull CompletableFuture<Void> setActiveSkin(@NotNull UUID playerUuid, @NotNull Material material, @Nullable String skinId) {
        Player player = Bukkit.getPlayer(playerUuid);
        Skin newSkin = skinId != null ? skinManager.getSkin(skinId).orElse(null) : null;

        if (player != null) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            db.getActiveSkin(playerUuid, material).thenAccept(previousSkinId -> {
                Skin previousSkin = previousSkinId.flatMap(skinManager::getSkin).orElse(null);

                Runnable equipLogic = () -> {
                    if (newSkin != null) {
                        SkinEquipEvent event = new SkinEquipEvent(player, newSkin);
                        Bukkit.getPluginManager().callEvent(event);
                        if (event.isCancelled()) {
                            future.completeExceptionally(new RuntimeException("Equip cancelled"));
                            return;
                        }
                    } else if (previousSkin != null) {
                        SkinUnequipEvent event = new SkinUnequipEvent(player, previousSkin);
                        Bukkit.getPluginManager().callEvent(event);
                        if (event.isCancelled()) {
                            future.completeExceptionally(new RuntimeException("Unequip cancelled"));
                            return;
                        }
                    }
                    db.upsertActiveSkin(playerUuid, material, skinId)
                            .thenRun(() -> {
                                logService.log(playerUuid, newSkin != null ? "EQUIP" : "UNEQUIP", skinId, material.name());
                                SchedulerUtil.runForEntity(plugin, player, () -> {
                                    modelService.refreshMaterial(player, material, newSkin);
                                    future.complete(null);
                                }, () -> future.complete(null));
                            })
                            .exceptionally(ex -> {
                                future.completeExceptionally(ex);
                                return null;
                            });
                };


                if (Bukkit.isPrimaryThread()) {
                    equipLogic.run();
                } else {
                    SchedulerUtil.runSync(plugin, equipLogic);
                }

            }).exceptionally(ex -> {
                future.completeExceptionally(ex);
                return null;
            });

            return future;
        }
        return db.upsertActiveSkin(playerUuid, material, skinId);
    }

    /**
     * Refreshes all skins in a player's inventory based on their active skins in the database.
     *
     * @param player The player to refresh.
     */
    public void refreshPlayerSkins(@NotNull Player player) {
        db.getAllActiveSkins(player.getUniqueId()).thenAccept(skinIdMap -> {
            Map<Material, Skin> activeSkins = new HashMap<>();
            for (Map.Entry<Material, String> entry : skinIdMap.entrySet()) {
                Material material = entry.getKey();
                String skinId = entry.getValue();
                if (material != null && !material.isAir() && material.isItem()) {
                    skinManager.getSkin(skinId).ifPresent(skin -> activeSkins.put(material, skin));
                }
            }
            SchedulerUtil.runForEntity(plugin, player, () ->
                    modelService.refreshInventory(player, Map.copyOf(activeSkins)), () -> {
            });
        }).exceptionally(ex -> {
            LoggerUtil.log(LoggerUtil.LogType.WARNING, "Failed to refresh player skins: " + ex.getMessage());
            return null;
        });
    }

    @Override
    public @NotNull CompletableFuture<Void> grantBundle(@NotNull UUID playerUuid, @NotNull String bundleId, @NotNull String source) {
        Optional<SkinBundle> bundleOpt = skinManager.getBundle(bundleId);
        if (bundleOpt.isEmpty())
            return CompletableFuture.failedFuture(new IllegalArgumentException("Bundle not found"));
        SkinBundle bundle = bundleOpt.get();

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (String skinId : bundle.skinIds()) {
            futures.add(grantSkin(playerUuid, skinId, source + " (Bundle: " + bundleId + ")"));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> logService.log(playerUuid, "GRANT_BUNDLE", bundleId, source));
    }

    @Override
    public @NotNull CompletableFuture<Void> revokeBundle(@NotNull UUID playerUuid, @NotNull String bundleId) {
        Optional<SkinBundle> bundleOpt = skinManager.getBundle(bundleId);
        if (bundleOpt.isEmpty())
            return CompletableFuture.failedFuture(new IllegalArgumentException("Bundle not found"));
        SkinBundle bundle = bundleOpt.get();

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (String skinId : bundle.skinIds()) {
            futures.add(revokeSkin(playerUuid, skinId));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> logService.log(playerUuid, "REVOKE_BUNDLE", bundleId, null));
    }

    @Override
    public @NotNull CompletableFuture<Boolean> hasBundle(@NotNull UUID playerUuid, @NotNull String bundleId) {
        Optional<SkinBundle> bundleOpt = skinManager.getBundle(bundleId);
        if (bundleOpt.isEmpty())
            return CompletableFuture.failedFuture(new IllegalArgumentException("Bundle not found"));
        SkinBundle bundle = bundleOpt.get();

        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        for (String skinId : bundle.skinIds()) {
            futures.add(hasSkin(playerUuid, skinId));
        }
        for (CompletableFuture<Boolean> future : futures) {
            if (!future.join()) {
                return CompletableFuture.completedFuture(false);
            }
        }

        return CompletableFuture.completedFuture(true);
    }

}
