// --- plugin/src/main/java/net/chamosmp/chamoitemskins/manager/SkinManager.java ---
package net.chamosmp.chamoitemskins.manager;

import net.chamosmp.chamoitemskins.api.model.Category;
import net.chamosmp.chamoitemskins.api.model.Skin;
import net.chamosmp.chamoitemskins.api.model.SkinBundle;
import net.chamosmp.chamoitemskins.api.service.SkinService;
import net.chamosmp.chamoitemskins.database.DatabaseManager;
import net.chamosmp.chamoitemskins.util.ConfigUtil;
import net.chamosmp.chamoitemskins.util.YamlUtil;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of SkinService.
 */
public final class SkinManager implements SkinService {
    private final Plugin plugin;
    private final RarityManager rarityManager;
    private final Map<String, Skin> skins = new ConcurrentHashMap<>();
    private final Map<String, SkinBundle> bundles = new ConcurrentHashMap<>();
    private final DatabaseManager databaseManager;

    /**
     * Constructs a new SkinManager.
     * @param plugin         The plugin instance.
     * @param rarityManager  The rarity manager.
     * @param databaseManager The database manager.
     */
    public SkinManager(@NotNull Plugin plugin, @NotNull RarityManager rarityManager, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.rarityManager = rarityManager;
        this.databaseManager = databaseManager;
    }

    @Override
    public @NotNull Collection<Skin> getSkins() {
        return Collections.unmodifiableCollection(skins.values());
    }

    @Override
    public @NotNull Collection<SkinBundle> getBundles() {
        return Collections.unmodifiableCollection(bundles.values());
    }

    @Override
    public @NotNull Optional<Skin> getSkin(@NotNull String id) {
        return Optional.ofNullable(skins.get(id));
    }

    @Override
    public @NotNull Optional<SkinBundle> getBundle(@NotNull String id) {
        return Optional.ofNullable(bundles.get(id));
    }

    @Override
    public @NotNull Collection<Skin> getSkinsForMaterial(@NotNull Material material) {
        return skins.values().stream()
                .filter(skin -> skin.categories().stream().anyMatch(cat -> isMaterialInCategory(material, cat)))
                .toList();
    }

    @Override
    public @NotNull Collection<Skin> getSkinsForCategory(@NotNull Category category) {
        return skins.values().stream()
                .filter(skin -> skin.categories().contains(category))
                .toList();
    }

    /**
     * Determines if a material belongs to a specific category.
     * @param materialName The name of the material.
     * @param category     The category to check.
     * @return True if the material is in the category.
     */
    private boolean isMaterialInCategory(Material materialName, Category category) {
        return category.isAllowed(materialName);
    }

    @Override
    public void saveSkin(@NotNull Skin skin) {
        skins.put(skin.id(), skin);
        YamlUtil.saveSkin(plugin, skin);
    }

    @Override
    public void saveBundle(@NotNull SkinBundle bundle) {
        bundles.put(bundle.id(), bundle);
        YamlUtil.saveBundle(plugin, bundle);
    }

    @Override
    public void deleteSkin(@NotNull String id) {
        skins.remove(id);
        YamlUtil.deleteSkin(plugin, id);
    }

    @Override
    public void changeId(@NotNull String oldId, Skin newSkin) {
        databaseManager.migrateSkinId(oldId, newSkin.id());
        deleteSkin(oldId);
        saveSkin(newSkin);
    }

    @Override
    public void deleteBundle(@NotNull String id) {
        bundles.remove(id);
        YamlUtil.deleteBundle(plugin, id);
    }

    @Override
    public void reloadSkins() {
        skins.clear();
        bundles.clear();
        var config = ConfigUtil.loadDataFile(plugin, "skins.yml");
        YamlUtil.loadSkins(config, rarityManager, new CategoryManager(plugin)).forEach(skin -> skins.put(skin.id(), skin));
        YamlUtil.loadBundles(config).forEach(bundle -> bundles.put(bundle.id(), bundle));
    }
}
