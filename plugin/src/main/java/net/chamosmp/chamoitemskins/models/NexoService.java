package net.chamosmp.chamoitemskins.models;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import net.chamosmp.chamoitemskins.api.models.Nexo;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class NexoService implements Nexo {

    public boolean isNexo(@NotNull String modelId) {
        return modelId.contains("nexo:");
    }

    @Override
    public void applyNexoItem(@NotNull ItemStack itemStack, @NotNull String nexoId) {
        if (!isNexoEnabled()) {
            return;
        }
        ItemBuilder builder = NexoItems.itemFromId(getNexoId(nexoId));
        if (builder == null) return;
        ItemStack newItem = builder.build();
        int amount = itemStack.getAmount();
        itemStack.setType(newItem.getType());
        itemStack.setItemMeta(newItem.getItemMeta());
        itemStack.setAmount(amount);
    }

    @Override
    public ItemStack getNexoItem(@NotNull ItemStack itemStack, @NotNull String nexoId) {
        if (!isNexoEnabled()) return itemStack;

        ItemBuilder builder = NexoItems.itemFromId(getNexoId(nexoId));
        if (builder == null) return itemStack;

        ItemStack newItem = builder.build();

        return itemStack.withType(newItem.getType());
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
