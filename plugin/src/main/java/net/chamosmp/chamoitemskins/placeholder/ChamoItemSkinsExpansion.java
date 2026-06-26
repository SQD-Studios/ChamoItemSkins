// --- plugin/src/main/java/net/chamosmp/chamoitemskins/placeholder/ChamoItemSkinsExpansion.java ---
package net.chamosmp.chamoitemskins.placeholder;

import io.papermc.paper.plugin.configuration.PluginMeta;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.chamosmp.chamoitemskins.ChamoItemSkinsPlugin;
import net.chamosmp.chamoitemskins.api.model.Skin;
import net.chamosmp.chamoitemskins.api.service.GrantService;
import net.chamosmp.chamoitemskins.api.service.SkinService;
import net.chamosmp.chamoitemskins.manager.RarityManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI expansion for ChamoItemSkins.
 */
public final class ChamoItemSkinsExpansion extends PlaceholderExpansion {
    private final SkinService skinService;
    private final GrantService grantService;
    private final RarityManager rarityManager;
    private final ChamoItemSkinsPlugin plugin;

    public ChamoItemSkinsExpansion(SkinService skinService, GrantService grantService, RarityManager rarityManager, ChamoItemSkinsPlugin plugin) {
        this.skinService = skinService;
        this.grantService = grantService;
        this.rarityManager = rarityManager;
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "chamoitemskins";
    }

    @Override
    public @NotNull String getAuthor() {
        return "SQD Studios";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }


    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return null;

        if (params.startsWith("active_")) {
            String matName = params.substring(7);
            Material material = Material.matchMaterial(matName);
            if (material == null) return "";
            
            return grantService.getActiveSkin(player.getUniqueId(), material)
                    .thenApply(id -> id.flatMap(skinService::getSkin).map(Skin::name).orElse(""))
                    .join(); // join() is okay here as PAPI is usually called async or we are on VT
        }

        if (params.startsWith("owns_")) {
            String skinId = params.substring(5);
            return String.valueOf(grantService.hasSkin(player.getUniqueId(), skinId).join());
        }

        if (params.equals("total_owned")) {
            return String.valueOf(grantService.getGrants(player.getUniqueId()).join().size());
        }

        if (params.equals("total_skins")) {
            return String.valueOf(skinService.getSkins().stream().filter(Skin::enabled).count());
        }

        if (params.startsWith("rarity_")) {
            if (!rarityManager.isEnabled()) return "";
            String skinId = params.substring(7);
            return skinService.getSkin(skinId).map(s -> s.rarity().getDisplayName()).orElse("");
        }

        if (params.equals("total_bundles")) {
            return String.valueOf(skinService.getBundles().size());
        }

        if (params.startsWith("owns_bundle_")) {
             String bundleId = params.substring(12);
             return skinService.getBundle(bundleId).map(bundle -> {
                 for (String id : bundle.skinIds()) {
                     if (!grantService.hasSkin(player.getUniqueId(), id).join()) return "false";
                 }
                 return "true";
             }).orElse("false");
        }

        return null;
    }
}
