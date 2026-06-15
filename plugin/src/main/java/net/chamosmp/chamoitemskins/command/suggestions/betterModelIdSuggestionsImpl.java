package net.chamosmp.chamoitemskins.command.suggestions;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import kr.toxicity.model.api.BetterModel;

import java.util.concurrent.CompletableFuture;

public final class betterModelIdSuggestionsImpl {
    private betterModelIdSuggestionsImpl() {}

    @betterModelIdSuggestions
    public static CompletableFuture<Suggestions> provide(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        try {
            BetterModel.modelKeys().stream()
                    .filter(key -> key.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                    .forEach(builder::suggest);
        } catch (NullPointerException ignored) {
            // BetterModel not initialized yet
        }
        return builder.buildFuture();
    }
}
