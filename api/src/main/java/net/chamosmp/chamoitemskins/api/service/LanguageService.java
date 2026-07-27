package net.chamosmp.chamoitemskins.api.service;

import org.jetbrains.annotations.ApiStatus;

import java.util.Map;

@ApiStatus.Internal
public interface LanguageService {

    /// Gets the message from they key of the current lang file
    ///
    /// @param key          The config key to get the message of
    /// @param placeholders The placeholders, you make a {@code Map<The User Placeholder, What to replace it with>}. If it is like {@code Map.of("skin_name", skin.name()}, if the user puts in the message {skin_name}, it will replace it with the contents of skin.name()
    /// @return The parsed string from the lang file key
    String getMessage(String key, Map<?, ?> placeholders);

    /// Gets the message from they key of the current lang file
    ///
    /// @param key The config key to get the message of
    /// @return The string from the key in the lang file
    String getMessage(String key);
}
