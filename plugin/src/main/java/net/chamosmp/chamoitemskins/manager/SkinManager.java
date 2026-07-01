// --- plugin/src/main/java/net/chamosmp/chamoitemskins/manager/SkinManager.java ---
package net.chamosmp.chamoitemskins.manager;

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
        String matName = material.name().toUpperCase();
        return skins.values().stream()
                .filter(skin -> skin.categories().stream().anyMatch(cat -> isMaterialInCategory(matName, cat)))
                .toList();
    }

    @Override
    public @NotNull Collection<Skin> getSkinsForCategory(@NotNull String category) {
        return skins.values().stream()
                .filter(skin -> skin.categories().contains(category))
                .toList();
    }

    private boolean isMaterialInCategory(String materialName, String category) {
        return switch (category.toUpperCase()) {
            case "SWORD", "SWORDS" -> materialName.contains("SWORD") || materialName.equals("MACE");
            case "AXE", "AXES" -> materialName.contains("_AXE");
            case "PICKAXE", "PICKAXES" -> materialName.contains("PICKAXE");
            case "SHOVEL", "SHOVELS" -> materialName.contains("SHOVEL");
            case "HOE", "HOES" -> materialName.contains("_HOE");
            case "SHIELD" -> materialName.equals("SHIELD");
            case "BOW" -> materialName.equals("BOW");
            case "CROSSBOW" -> materialName.equals("CROSSBOW");
            case "MACE" -> materialName.equals("MACE");
            case "SPEAR", "TRIDENT" -> materialName.contains("TRIDENT");
            case "HELMET", "HELMETS" -> materialName.contains("HELMET");
            case "CHESTPLATE", "CHESTPLATES" -> materialName.contains("CHESTPLATE");
            case "LEGGINGS" -> materialName.contains("LEGGINGS");
            case "BOOTS" -> materialName.contains("BOOTS");
            case "TOOLS" -> materialName.contains("SWORD") || materialName.contains("_AXE") ||
                             materialName.contains("PICKAXE") || materialName.contains("SHOVEL") ||
                             materialName.contains("_HOE");
            case "ARMOR" -> materialName.contains("HELMET") || materialName.contains("CHESTPLATE") ||
                             materialName.contains("LEGGINGS") || materialName.contains("BOOTS");
            case "ALL", "GENERAL" -> true;
            default -> false;
        };
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
        YamlUtil.loadSkins(config, rarityManager).forEach(skin -> skins.put(skin.id(), skin));
        YamlUtil.loadBundles(config).forEach(bundle -> bundles.put(bundle.id(), bundle));
    }
}
