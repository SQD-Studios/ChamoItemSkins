package net.chamosmp.chamoitemskins.models;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class NexoService {

    public boolean isNexo(@NotNull String modelId) {
        return modelId.contains("nexo:");
    }

    @SuppressWarnings("deprecation")
    public void applyNexoItem(@NotNull ItemStack itemStack, @NotNull String nexoId) {
        ItemBuilder builder = NexoItems.itemFromId(getNexoId(nexoId));
        if (builder == null) return;
        ItemStack newItem = builder.build();
        int amount = itemStack.getAmount();
        itemStack.setType(newItem.getType());
        itemStack.setItemMeta(newItem.getItemMeta());
        itemStack.setAmount(amount);
    }

    public String getNexoId(@NotNull String modelId) {
        return modelId.split("nexo:")[1];
    }

}
