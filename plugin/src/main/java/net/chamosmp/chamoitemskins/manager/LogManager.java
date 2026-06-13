// --- plugin/src/main/java/net/chamosmp/chamoitemskins/manager/LogManager.java ---
package net.chamosmp.chamoitemskins.manager;

import net.chamosmp.chamoitemskins.api.service.LogService;
import net.chamosmp.chamoitemskins.database.DatabaseManager;
import net.chamosmp.chamoitemskins.scheduler.SchedulerUtil;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of LogService.
 */
public final class LogManager implements LogService {
    private final Plugin plugin;
    private final DatabaseManager databaseManager;

    public LogManager(Plugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    @Override
    public CompletableFuture<Void> log(UUID playerUuid, String action, String target, String metadata) {
        return CompletableFuture.runAsync(() -> {
            databaseManager.logAction(playerUuid, action, target, metadata);
        }, SchedulerUtil.getVirtualThreadExecutor());
    }
}
