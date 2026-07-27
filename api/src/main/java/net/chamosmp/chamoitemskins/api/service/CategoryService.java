package net.chamosmp.chamoitemskins.api.service;

import net.chamosmp.chamoitemskins.api.objects.Category;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public interface CategoryService {

    /**
     * Gets all loaded categories.
     *
     * @return A list of categories.
     */
    @NotNull List<Category> getCategories();

    /**
     * Gets an unmodifiable map of all categories.
     *
     * @return The rarity map.
     */
    @NotNull Map<String, Category> getCategoryMap();

    /**
     * Get a category by a string name
     *
     * @param name The {@link String} name of the category.
     * @return Returns the Category, based of the name
     */
    Category getCategoryByName(String name);
}
