package net.chamosmp.chamoitemskins.api.service;

public interface MigrateService {

    /**
     * Migrate from HMCWarps
     */
    void migrateHMC();

    /**
     * Migrate from ItemSkins
     */
    void migrateItemSkins();


}
