// --- api/src/main/java/net/chamosmp/chamoitemskins/api/service/LogService.java ---
package net.chamosmp.chamoitemskins.api.service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for logging skin-related actions.
 */
public interface LogService {
    /**
     * Logs an action.
     * @param playerUuid The player's UUID.
     * @param action     The action name (e.g., GRANT, REVOKE, EQUIP, UNEQUIP).
     * @param target     The target of the action (e.g., skin ID, bundle ID).
     * @param metadata   Additional metadata.
     * @return A future that completes when the log is persisted.
     */
    CompletableFuture<Void> log(UUID playerUuid, String action, String target, String metadata);
}
