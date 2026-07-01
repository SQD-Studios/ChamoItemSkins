package net.chamosmp.chamoitemskins.manager;

import de.skyslycer.hmcwraps.HMCWraps;
import de.skyslycer.hmcwraps.serialization.wrap.Wrap;
import net.chamosmp.chamoitemskins.api.model.Skin;
import net.chamosmp.chamoitemskins.api.service.MigrateService;
import net.chamosmp.chamoitemskins.api.service.SkinService;
import net.chamosmp.chamoitemskins.util.ConfigUtil;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MigrateManager implements MigrateService {

    private final Plugin plugin;
    private final SkinService skinService;
    private HMCWraps hmcWraps;

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

    public MigrateManager(Plugin plugin, SkinService skinService) {
        this.plugin = plugin;
        this.skinService = skinService;
    }


    @Override
    public void migrateItemSkins() {

    }


    // HMCWarps Section
    public void migrateHMC() {
        plugin.getLogger().info("Started migration process");
        if (!Bukkit.getPluginManager().isPluginEnabled("HMCWraps")) {
            plugin.getLogger().warning("HMCWraps is not enabled. Migration aborted.");
            return;
        }

        // Get HMCWraps instance – using ServicesManager or plugin manager
        hmcWraps = Bukkit.getServicesManager().load(HMCWraps.class);
        if (hmcWraps == null) {
            plugin.getLogger().severe("Failed to get HMCWraps instance.");
            return;
        }

        // Disable the rarities

        YamlConfiguration config = ConfigUtil.loadOrAdapt(plugin, "config.yml");
        config.set("rarities.enabled", false);
        try {
            config.save(new File(plugin.getDataFolder(), "config.yml"));
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config: " + e.getMessage());
        }

        // Access wraps via WrapHandler (or WrapRepository)
        Map<String, Wrap> wraps = hmcWraps.getWrapsLoader().getWraps();
        if (wraps == null || wraps.isEmpty()) {
            plugin.getLogger().info("No wraps found in HMCWraps to migrate.");
            return;
        }
        plugin.getLogger().info("Starting migration of " + wraps.size() + " wraps from HMCWraps...");

        int migrated = 0;
        int failed = 0;

        for (Map.Entry<String, Wrap> entry : wraps.entrySet()) {
            String wrapId = entry.getKey();
            Wrap wrap = entry.getValue();

            try {
                Skin skin = convertWrapToSkin(wrapId, wrap);
                if (skin != null) {
                    skinService.saveSkin(skin);
                    migrated++;
                    plugin.getLogger().info("Successfully migrated wrap: " + wrapId);
                } else {
                    failed++;
                    plugin.getLogger().warning("Failed to convert wrap: " + wrapId);
                }
            } catch (Exception e) {
                failed++;
                plugin.getLogger().severe("Error migrating wrap " + wrapId + ": " + e.getMessage());
            }
        }
        plugin.getLogger().info("Migration completed. Migrated: " + migrated + ", Failed: " + failed);
    }


    @Override
    public void migrateHMC(CommandSender player) {
        if (player instanceof ConsoleCommandSender) {
            migrateHMC();
            return;
        }
        player.sendRichMessage("<green>Started migration process");
        plugin.getLogger().info("Started migration process");
        if (!Bukkit.getPluginManager().isPluginEnabled("HMCWraps")) {
            player.sendRichMessage("<red>HMCWraps not enabled. Migration aborted.");
            plugin.getLogger().warning("HMCWraps is not enabled. Migration aborted.");
            return;
        }

        // Get HMCWraps instance – using ServicesManager or plugin manager
        hmcWraps = Bukkit.getServicesManager().load(HMCWraps.class);
        if (hmcWraps == null) {
            player.sendRichMessage("<red>Failed to load HMCWraps instance.");
            plugin.getLogger().severe("Failed to get HMCWraps instance.");
            return;
        }

        // Disable the rarities

        YamlConfiguration config = ConfigUtil.loadOrAdapt(plugin, "config.yml");
        config.set("rarities.enabled", false);
        try {
            config.save(new File(plugin.getDataFolder(), "config.yml"));
        } catch (IOException e) {
            player.sendRichMessage("<red>Could not save config. Please check the console and report!");
            plugin.getLogger().severe("Could not save config: " + e.getMessage());
        }

        // Access wraps via WrapHandler (or WrapRepository)
        Map<String, Wrap> wraps = hmcWraps.getWrapsLoader().getWraps();
        if (wraps == null || wraps.isEmpty()) {
            player.sendRichMessage("<red>No wraps found to migrate.");
            plugin.getLogger().info("No wraps found in HMCWraps to migrate.");
            return;
        }

        player.sendRichMessage("<green>Starting migration of " + wraps.size() + " wraps from HMCWraps...");
        plugin.getLogger().info("Starting migration of " + wraps.size() + " wraps from HMCWraps...");

        int migrated = 0;
        int failed = 0;

        for (Map.Entry<String, Wrap> entry : wraps.entrySet()) {
            String wrapId = entry.getKey();
            Wrap wrap = entry.getValue();

            try {
                Skin skin = convertWrapToSkin(wrapId, wrap);
                if (skin != null) {
                    skinService.saveSkin(skin);
                    migrated++;
                    plugin.getLogger().info("Successfully migrated wrap: " + wrapId);
                } else {
                    failed++;
                    plugin.getLogger().warning("Failed to convert wrap: " + wrapId);
                }
            } catch (Exception e) {
                failed++;
                plugin.getLogger().severe("Error migrating wrap " + wrapId + ": " + e.getMessage());
            }
        }
        player.sendRichMessage("<green>Migration completed. Migrated: " + migrated + ", <red>Failed: " + failed);
        plugin.getLogger().info("Migration completed. Migrated: " + migrated + ", Failed: " + failed);
    }

    /**
     * Converts an HMCWraps Wrap to a ChamoItemSkins Skin.
     *
     * @param wrapId The UUID of the wrap
     * @param wrap   The Wrap object from HMCWraps
     * @return A Skin object or null if conversion fails
     */
    private Skin convertWrapToSkin(String wrapId, Wrap wrap) {
        try {
            String displayName = wrap.getName() != null ? wrap.getName() : "Migrated " + wrapId;

            NamespacedKey modelData = wrap.getItemModel();



            List<String> categories = determineCategories(wrap);

            // Rarity: Since HMCWraps has no rarity, set to null or a default
            // If you have a RarityManager, you can get "common" or leave null
            //Rarity rarity = rarityManager.getRarity("common").orElse(null);


            // Skin constructor: (id, name, model, rarity, categories, enabled, note, display, animations)
            return new Skin(
                    wrapId,
                    displayName,
                    String.valueOf(modelData),
                    null,
                    categories,
                    true,
                    null,
                    null,
                    Collections.emptyList()
            );

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to convert wrap " + wrapId + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Determines the categories
     *
     * @param wrap The wrap to determine the categories of.
     * @return The categorized
     */
    private List<String> determineCategories(Wrap wrap) {
        List<String> categories = new ArrayList<>();
        if (hmcWraps.getCollectionHelper().getCollection(wrap) != null && !hmcWraps.getCollectionHelper().getCollection(wrap).isEmpty()) {
            categories.add(hmcWraps.getCollectionHelper().getCollection(wrap));
        }

        if (categories.isEmpty()) {
            categories.add("ALL");
        }

        return categories;
    }

    /**
     * Maps an HMCWraps item key to a ChamoItemSkins category.
     */
    private String mapItemToCategory(String itemKey) {
        if (itemKey == null) return null;
        String upper = itemKey.toUpperCase();

        switch (upper) {
            case "SWORD" -> {
                return "SWORD";
            }
            case "AXE" -> {
                return "AXE";
            }
            case "PICKAXE" -> {
                return "PICKAXE";
            }
            case "SHOVEL" -> {
                return "SHOVEL";
            }
            case "HOE" -> {
                return "HOE";
            }
            case "SHIELD" -> {
                return "SHIELD";
            }
            case "BOW" -> {
                return "BOW";
            }
            case "CROSSBOW" -> {
                return "CROSSBOW";
            }
            case "TRIDENT" -> {
                return "TRIDENT";
            }
            case "HELMET" -> {
                return "HELMET";
            }
            case "CHESTPLATE" -> {
                return "CHESTPLATE";
            }
            case "LEGGINGS" -> {
                return "LEGGINGS";
            }
            case "BOOTS" -> {
                return "BOOTS";
            }
            case "MACE" -> {
                return "MACE";
            }
        }
        return null;
    }
}