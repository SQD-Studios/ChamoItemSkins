package net.chamosmp.chamoitemskins.api.model;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record Category(
        @NotNull String name,
        List<String> allowedItems
) {
    public List<Material> getAllowedMaterials() {
        List<Material> materials = new ArrayList<>();
        if (allowedItems == null) {
            materials.addAll(Arrays.asList(Material.values()));
            return materials;
        }

        for (String item : allowedItems) {
            Material exact = Material.matchMaterial(item);
            if (exact != null) {
                materials.add(exact);
                continue;
            }
            String upperItem = item.toUpperCase();
            for (Material m : Material.values()) {
                if (m.name().contains(upperItem)) {
                    materials.add(m);
                }
            }
        }

        return materials;
    }

    public boolean isAllowed(@NotNull Material material) {
        return getAllowedMaterials().contains(material);
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull List<String> getAllowedItems() {
        return allowedItems;
    }
}
