package net.chamosmp.chamoitemskins.models;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import net.chamosmp.chamoitemskins.api.models.Nexo;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class NexoService implements Nexo {

    @Override
    public boolean isNexo(@NotNull String modelId) {
        return modelId.contains("nexo:");
    }

    @Override
    public ItemStack getNexoItem(@NotNull ItemStack itemStack, @NotNull String nexoId) {
        if (!isNexoEnabled()) return itemStack;

        ItemBuilder builder = NexoItems.itemFromId(getNexoId(nexoId));
        if (builder == null) return itemStack;

        ItemStack newItem = builder.build();
        ItemMeta originalMeta = itemStack.getItemMeta();
        ItemMeta nexoMeta = newItem.getItemMeta();

        if (originalMeta != null && nexoMeta != null) {
            mergeMeta(originalMeta, nexoMeta);
        }

        itemStack.setItemMeta(nexoMeta);
        return itemStack.withType(newItem.getType());
    }

    private void mergeMeta(ItemMeta source, ItemMeta target) {
        source.getEnchants().forEach((enchant, level) -> target.addEnchant(enchant, level, true));
        if (source.hasCustomName()) target.customName(source.customName());
        if (source.hasLore()) target.lore(source.lore());
        if (source.hasCustomModelDataComponent()) target.setCustomModelDataComponent(source.getCustomModelDataComponent());
        source.getPersistentDataContainer().copyTo(target.getPersistentDataContainer(), true);
    }

    @Override
    public String getNexoId(@NotNull String modelId) {
        return modelId.split("nexo:")[1];
    }

    @Override
    public boolean isNexoEnabled() {
        return Bukkit.getServer().getPluginManager().getPlugin("Nexo") != null;
    }

}
