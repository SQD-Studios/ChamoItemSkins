package net.chamosmp.chamoitemskins.manager;

import net.chamosmp.chamoitemskins.api.service.LogService;
import net.chamosmp.chamoitemskins.database.DatabaseManager;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of LogService.
 */
public final class LogManager implements LogService {
    private final DatabaseManager databaseManager;

    public LogManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public CompletableFuture<Void> log(UUID playerUuid, String action, String target, String metadata) {
        if (target == null) {
            return CompletableFuture.completedFuture(null);
        }
        return databaseManager.logAction(playerUuid, action, target, metadata);
    }
}
