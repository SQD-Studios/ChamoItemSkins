# BetterModel Integration Preferences

## Model ID Suggestions
- Implementation: Use `@betterModelIdSuggestions` in command parameters to provide tab-completion from `BetterModel.modelKeys()`.
- Implementation class: `net.chamosmp.chamoitemskins.command.suggestions.betterModelIdSuggestionsImpl`.

## Initialization Safety
- `BetterModelService` must handle cases where `BetterModel` is not yet initialized (e.g., during early plugin load or if the dependency is missing).
- Always use `try-catch` around `BetterModel.model()`, `BetterModel.platform()`, and `BetterModel.config()` to prevent `NullPointerException` with the message "BetterModel hasn't been initialized yet!".
- Use `BetterModelService#isAvailable()` to check for readiness.
- **Rendering Protection:** Avoid applying item models (via `setItemModel`) if `BetterModel` is required but not ready, as this results in the "black and purple" missing texture pattern on the client.
- **Namespacing:** Default to `bettermodel` namespace for item models when `BetterModel.config().itemNamespace()` is unavailable.

## Skin Editor
- When editing model IDs in the GUI, `ChatInputUtil` is used.
- `ChatInputUtil` has been enhanced to support tab-completion suggestions via `TabCompleteEvent`.
- Suggestions are provided by passing a `Supplier<Collection<String>>` to `getInput`.
- For Model IDs, use `() -> kr.toxicity.model.api.BetterModel.modelKeys()`.
- Note: Chat tab-completion may vary depending on the client and server configuration (usually works when not starting with `/` if the server handles it).
