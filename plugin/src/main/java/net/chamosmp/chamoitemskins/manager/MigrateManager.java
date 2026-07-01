package net.chamosmp.chamoitemskins.manager;

import de.skyslycer.hmcwraps.HMCWraps;
import net.chamosmp.chamoitemskins.api.service.GrantService;
import net.chamosmp.chamoitemskins.api.service.MigrateService;
import org.bukkit.Bukkit;

public class MigrateManager implements MigrateService {

    // HMC Warps Arrays
    private String[] hmcIds;
    private String[] hmcModels;
    private String[] hmcNames;
    private String[] hmcItems;

    // ItemSkins Arrays
    private String[] itemIds;
    private String[] itemModels;
    private String[] itemNames;
    private String[] itemItems;


    @Override
    public void migrateItemSkins() {}

    @Override
    public void migrateHMC() {
        if (!Bukkit.getPluginManager().isPluginEnabled("HMCWraps")) {
            return;
        }
        //var wraps = (HMCWraps) Bukkit.getPluginManager().getPlugin(HMCWraps.class);
    }
}
