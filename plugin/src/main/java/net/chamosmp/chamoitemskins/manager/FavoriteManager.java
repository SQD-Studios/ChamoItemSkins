package net.chamosmp.chamoitemskins.manager;

import net.chamosmp.chamoitemskins.ChamoItemSkinsPlugin;
import net.chamosmp.chamoitemskins.api.objects.Skin;
import net.chamosmp.chamoitemskins.api.service.FavoriteService;
import net.chamosmp.chamoitemskins.database.DatabaseManager;
import net.chamosmp.chamoitemskins.scheduler.SchedulerUtil;
import net.chamosmp.chamoitemskins.util.LoggerUtil;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

public class FavoriteManager implements FavoriteService {

    private final DatabaseManager db;
    private final SkinManager skinManager;

    public boolean isEnabled;

    public FavoriteManager(ChamoItemSkinsPlugin plugin, DatabaseManager databaseManager, SkinManager skinManager) {
        this.db = databaseManager;
        this.skinManager = skinManager;

        this.isEnabled = plugin.getConfig().getBoolean("favorites.enabled");
    }

    @Override
    public CompletableFuture<Void> addFavoriteSkinToPlayer(@NotNull Player player, @NotNull Skin skin) {
        return db.addFavoriteSkinToPlayer(player.getUniqueId(), skin);
    }

    @Override
    public CompletableFuture<Void> removeFavoriteSkinFromPlayer(@NotNull Player player, @NotNull Skin skin) {
        return db.removeFavoriteSkinFromPlayer(player.getUniqueId(), skin);
    }

    @Override
    public @NotNull CompletableFuture<Collection<Skin>> getFavoriteSkinsFromPlayer(@NotNull Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Collection<Skin> skins = new ArrayList<>();
                Collection<String> ids;
                ids = db.getFavoriteSkinsFromPlayer(player.getUniqueId()).get();

                if (ids != null) {
                    ids.forEach(id -> {
                        Optional<Skin> optionalSkin = skinManager.getSkin(id);
                        optionalSkin.ifPresent(skins::add);
                    });
                }
                return skins;
            } catch (CancellationException | ExecutionException | InterruptedException e) {
                LoggerUtil.log(LoggerUtil.LogType.SEVERE, "Error getting favorite skins from database." + e);
                return Collections.emptyList();
            }
        }, SchedulerUtil.getVirtualThreadExecutor());
    }

    @Override
    public void changeFavoriteSkin(@NotNull Player player, @NotNull Skin skin) {
        CompletableFuture.runAsync(() -> {
            Collection<Skin> skins;
            try {
                skins = getFavoriteSkinsFromPlayer(player).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }

            if (skins.isEmpty() || !skins.contains(skin)) {
                addFavoriteSkinToPlayer(player, skin);
            } else {
                    removeFavoriteSkinFromPlayer(player, skin);
            }
        });
    }

    public enum ClickType {
        SHIFTCLICK,
        RIGHTCLICK;
    }

    public @Nullable ClickType getClickType(String s) {
        try {
            return ClickType.valueOf(s);
        } catch (IllegalArgumentException e) {
            LoggerUtil.log(LoggerUtil.LogType.SEVERE, "Invalid click type in the config: " + s);
            return null;
        }
    }
}