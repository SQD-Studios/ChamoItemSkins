package net.chamosmp.chamoitemskins.api.service;

/**
 * Service to migrate skin data from other plugins.
 * Currently supports HMCWarps and ItemSkins.
 */
public interface MigrateService {

    /**
     * Migrates data from HMCWarps.
     */
    void migrateHMC();

    /**
     * Migrates data from ItemSkins.
     */
    void migrateItemSkins();
}
