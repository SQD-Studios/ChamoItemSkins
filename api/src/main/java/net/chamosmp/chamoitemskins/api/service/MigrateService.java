package net.chamosmp.chamoitemskins.api.service;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Service to migrate skin data from other plugins.
 * Currently supports HMCWarps and ItemSkins.
 */
public interface MigrateService {

    /**
     * Migrates data from ItemSkins.
     */
    void migrateItemSkins();

    /**
     * Migrates data from HMCWarps
     *
     * @param player The player to send notification to.
     */
    void migrateHMC(CommandSender player);

    /**
     * Does {@link MigrateService#migrateHMC(CommandSender)} without any player sender to reduce double messages
     */
    void migrateHMC();
}
