package net.chamosmp.chamoitemskins.command.suggestions.bundles;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.chamosmp.chamoitemskins.api.model.SkinBundle;
import net.chamosmp.chamoitemskins.manager.SkinManager;

import java.util.concurrent.CompletableFuture;

public final class bundleSuggestionsImpl {
    private static SkinManager skinManager;

    private bundleSuggestionsImpl() {
    }

    public static void init(SkinManager manager) {
        skinManager = manager;
    }

    @bundleSuggestions
    public static CompletableFuture<Suggestions> provide(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        if (skinManager == null) return builder.buildFuture();
        skinManager.getBundles().stream()
                .map(SkinBundle::id)
                .filter(id -> id.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }
}
