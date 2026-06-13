// --- plugin/src/main/java/net/chamosmp/chamoitemskins/manager/SkinManager.java ---
package net.chamosmp.chamoitemskins.manager;

import net.chamosmp.chamoitemskins.api.model.Skin;
import net.chamosmp.chamoitemskins.api.service.SkinService;
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
    private final Map<String, Skin> skins = new ConcurrentHashMap<>();

    public SkinManager(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull Collection<Skin> getSkins() {
        return Collections.unmodifiableCollection(skins.values());
    }

    @Override
    public @NotNull Optional<Skin> getSkin(@NotNull String id) {
        return Optional.ofNullable(skins.get(id));
    }

    @Override
    public @NotNull Collection<Skin> getSkinsForMaterial(@NotNull Material material) {
        return skins.values().stream()
                .filter(skin -> skin.itemType() == material)
                .toList();
    }

    @Override
    public void saveSkin(@NotNull Skin skin) {
        skins.put(skin.id(), skin);
        YamlUtil.saveSkin(plugin, skin);
    }

    @Override
    public void deleteSkin(@NotNull String id) {
        skins.remove(id);
        YamlUtil.deleteSkin(plugin, id);
    }

    @Override
    public void reloadSkins() {
        skins.clear();
        var config = ConfigUtil.loadOrAdapt(plugin, "skins.yml");
        YamlUtil.loadSkins(config).forEach(skin -> skins.put(skin.id(), skin));
    }
}
