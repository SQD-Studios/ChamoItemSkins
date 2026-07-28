package net.chamosmp.chamoitemskins.manager;

import de.skyslycer.hmcwraps.HMCWraps;
import de.skyslycer.hmcwraps.serialization.wrap.Wrap;
import net.chamosmp.chamoitemskins.api.objects.Category;
import net.chamosmp.chamoitemskins.api.objects.Skin;
import net.chamosmp.chamoitemskins.api.service.MigrateService;
import net.chamosmp.chamoitemskins.api.service.SkinService;
import net.chamosmp.chamoitemskins.util.ConfigUtil;
import net.chamosmp.chamoitemskins.util.LoggerUtil;
import net.chamosmp.chamoitemskins.util.MessageUtil;
import net.chamosmp.chamoitemskins.util.YamlUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class MigrateManager implements MigrateService {

    private static final Logger log = LoggerFactory.getLogger(MigrateManager.class);
    private final Plugin plugin;
    private final SkinService skinService;
    private HMCWraps hmcWraps;

    public MigrateManager(Plugin plugin, SkinService skinService) {
        this.plugin = plugin;
        this.skinService = skinService;
    }


    @Override
    public void migrateItemSkins() {
        throw new UnsupportedOperationException("migrateItemSkins() is not implemented yet in MigrateManager.");
    }


    // HMCWarps Section
    @Override
    public void migrateHMC(Audience audience) {
        // Weird trickery hehe
        Audience a;
        boolean isConsole = audience instanceof ConsoleCommandSender;
        if (!isConsole) {
            a = Audience.audience(audience, Bukkit.getConsoleSender());
        } else {
            a = audience;
        }
        a.sendMessage(MessageUtil.parse("<green>Started migration process"));
        if (!Bukkit.getPluginManager().isPluginEnabled("HMCWraps")) {
            a.sendMessage(MessageUtil.parse("<red>HMCWraps not enabled. Migration aborted."));
            return;
        }

        // Get HMCWraps instance – using ServicesManager or plugin manager
        hmcWraps = Bukkit.getServicesManager().load(HMCWraps.class);
        if (hmcWraps == null) {
            a.sendMessage(MessageUtil.parse("<red>Failed to load HMCWraps instance."));
            return;
        }

        // Disable the rarities

        YamlConfiguration config = ConfigUtil.loadOrAdapt(plugin, "config.yml");
        config.set("rarities.enabled", false);
        try {
            config.save(new File(plugin.getDataFolder(), "config.yml"));
        } catch (IOException e) {
            a.sendMessage(MessageUtil.parse("<dark_red>Could not save config: " + e.getMessage()));
        }

        // Access wraps via WrapHandler (or WrapRepository)
        Map<String, Wrap> wraps = hmcWraps.getWrapsLoader().getWraps();
        if (wraps == null || wraps.isEmpty()) {
            a.sendMessage(MessageUtil.parse("<yellow>No wraps found in HMCWarps to migrate"));
            return;
        }

        a.sendMessage(MessageUtil.parse("<green>Starting migration of " + wraps.size() + " wraps from HMCWarps"));

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
                    a.sendMessage(MessageUtil.parse("<green>Successfully migrated wrap: " + wrapId));
                } else {
                    failed++;
                    a.sendMessage(MessageUtil.parse("<red>Failed to convert wrap: " + wrapId));
                }
            } catch (Exception e) {
                failed++;
                a.sendMessage(MessageUtil.parse("<dark_red>Error migrating wrap " + wrapId + ": " + e.getMessage()));
            }
        }
        a.sendMessage(MessageUtil.parse("<green>Migration completed. Migrated: " + migrated + ", <red>Failed: " + failed));
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

            List<Category> categories = determineCategories(wrap);

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
            LoggerUtil.log(LoggerUtil.LogType.WARNING, "Failed to convert wrap " + wrapId + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Determines the categories
     *
     * @param wrap The wrap to determine the categories of.
     * @return The categorized
     */
    private List<Category> determineCategories(Wrap wrap) {
        List<Category> categories = new ArrayList<>();
        if (hmcWraps.getCollectionHelper().getCollection(wrap) != null && !hmcWraps.getCollectionHelper().getCollection(wrap).isEmpty()) {
            String collection = hmcWraps.getCollectionHelper().getCollection(wrap);
            List<Material> allowedItems = hmcWraps.getCollectionHelper().getMaterials(collection);
            List<String> materialNames = allowedItems.stream()
                    .map(Material::toString)
                    .collect(Collectors.toList());
            Category category = new Category(collection, materialNames, collection.toLowerCase());
            YamlUtil.saveCategory(plugin, category);
            categories.add(category);
        }

        if (categories.isEmpty()) {
            categories.add(new Category("ALL", List.of(""), "all"));
        }

        return categories;
    }

}