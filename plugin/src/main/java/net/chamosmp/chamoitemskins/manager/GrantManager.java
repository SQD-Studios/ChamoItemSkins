// --- plugin/src/main/java/net/chamosmp/chamoitemskins/manager/GrantManager.java ---
package net.chamosmp.chamoitemskins.manager;

import net.chamosmp.chamoitemskins.api.event.SkinEquipEvent;
import net.chamosmp.chamoitemskins.api.event.SkinGrantEvent;
import net.chamosmp.chamoitemskins.api.event.SkinRevokeEvent;
import net.chamosmp.chamoitemskins.api.event.SkinUnequipEvent;
import net.chamosmp.chamoitemskins.api.model.Skin;
import net.chamosmp.chamoitemskins.api.model.SkinGrant;
import net.chamosmp.chamoitemskins.api.service.GrantService;
import net.chamosmp.chamoitemskins.database.DatabaseManager;
import net.chamosmp.chamoitemskins.scheduler.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of GrantService.
 */
public final class GrantManager implements GrantService {
    private final Plugin plugin;
    private final DatabaseManager db;
    private final CacheManager cache;
    private final SkinManager skinManager;

    public GrantManager(Plugin plugin, DatabaseManager db, CacheManager cache, SkinManager skinManager) {
        this.plugin = plugin;
        this.db = db;
        this.cache = cache;
        this.skinManager = skinManager;
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
        Player player = Bukkit.getPlayer(playerUuid);
        Skin skin = skinManager.getSkin(skinId).orElse(null);
        
        if (player != null && skin != null) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            SchedulerUtil.runSync(plugin, () -> {
                SkinGrantEvent event = new SkinGrantEvent(player, skin, source);
                Bukkit.getPluginManager().callEvent(event);
                
                if (!event.isCancelled()) {
                    db.grantSkin(playerUuid, skinId, source).thenRun(() -> {
                        cache.invalidate(playerUuid);
                        future.complete(null);
                    });
                } else {
                    future.completeExceptionally(new RuntimeException("Grant cancelled by event"));
                }
            });
            return future;
        }
        
        return db.grantSkin(playerUuid, skinId, source).thenRun(() -> cache.invalidate(playerUuid));
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
    public @NotNull CompletableFuture<Void> setActiveSkin(@NotNull UUID playerUuid, @NotNull Material material, @Nullable String skinId) {
        Player player = Bukkit.getPlayer(playerUuid);
        Skin skin = skinId != null ? skinManager.getSkin(skinId).orElse(null) : null;

        if (player != null) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            SchedulerUtil.runSync(plugin, () -> {
                if (skin != null) {
                    SkinEquipEvent event = new SkinEquipEvent(player, skin);
                    Bukkit.getPluginManager().callEvent(event);
                    if (event.isCancelled()) {
                        future.completeExceptionally(new RuntimeException("Equip cancelled"));
                        return;
                    }
                } else {
                    // Unequipping
                    // We might need to know WHICH skin is being unequipped to fire the event correctly
                    // For now, let's just proceed or fetch active skin first
                    getActiveSkin(playerUuid, material).thenAccept(activeId -> {
                        if (activeId.isPresent()) {
                            skinManager.getSkin(activeId.get()).ifPresent(s -> {
                                SchedulerUtil.runSync(plugin, () -> {
                                    SkinUnequipEvent unequipEvent = new SkinUnequipEvent(player, s);
                                    Bukkit.getPluginManager().callEvent(unequipEvent);
                                    if (unequipEvent.isCancelled()) {
                                        future.completeExceptionally(new RuntimeException("Unequip cancelled"));
                                        return;
                                    }
                                    db.setActiveSkin(playerUuid, material, null).thenRun(() -> future.complete(null));
                                });
                            });
                        } else {
                            db.setActiveSkin(playerUuid, material, null).thenRun(() -> future.complete(null));
                        }
                    });
                    return;
                }
                
                db.setActiveSkin(playerUuid, material, skinId).thenRun(() -> future.complete(null));
            });
            return future;
        }

        return db.setActiveSkin(playerUuid, material, skinId);
    }
}
