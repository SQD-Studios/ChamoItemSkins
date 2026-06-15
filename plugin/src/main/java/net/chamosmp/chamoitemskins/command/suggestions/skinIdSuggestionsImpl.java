// --- plugin/src/main/java/net/chamosmp/chamoitemskins/command/suggestions/skinIdSuggestionsImpl.java ---
package net.chamosmp.chamoitemskins.command.suggestions;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.chamosmp.chamoitemskins.api.model.Skin;
import net.chamosmp.chamoitemskins.manager.SkinManager;

import java.util.concurrent.CompletableFuture;

public final class skinIdSuggestionsImpl {
    private static SkinManager skinManager;

    private skinIdSuggestionsImpl() {}

    public static void init(SkinManager manager) {
        skinManager = manager;
    }

    @skinIdSuggestions
    public static CompletableFuture<Suggestions> provide(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        if (skinManager == null) return builder.buildFuture();
        skinManager.getSkins().stream()
                .map(Skin::id)
                .filter(id -> id.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }
}
