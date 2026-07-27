package net.chamosmp.chamoitemskins.api.service;

import net.kyori.adventure.audience.Audience;

/**
 * Service to migrate skin data from other plugins.
 * Currently, supports HMCWarps and ItemSkins.
 */
public interface MigrateService {

    /**
     * Migrates data from ItemSkins.
     */
    void migrateItemSkins();

    /**
     * Migrates data from HMCWarps
     *
     * @param audience The audience to send messages to
     */
    void migrateHMC(Audience audience);
}
