package net.chamosmp.chamoitemskins.manager;

import net.chamosmp.chamoitemskins.api.service.LanguageService;
import net.chamosmp.chamoitemskins.util.ConfigUtil;
import net.chamosmp.chamoitemskins.util.LoggerUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class LanguageManager implements LanguageService {

    private final Plugin plugin;
    private final String defaultLang = "en";
    private String currentLang;
    private final Map<String, String> messages = new HashMap<>();

    public LanguageManager(Plugin plugin) {
        this.plugin = plugin;
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }
        String langCode = plugin.getConfig().getString("language", defaultLang);
        loadLanguage(langCode);
    }

    /**
     * Loads a language file from lang/<code >.yml</code><br>
     * Falls back to default language if the file is missing or invalid.
     */
    private void loadLanguage(String langCode) {
        try {
            YamlConfiguration yaml = ConfigUtil.loadOrAdapt(plugin, "lang/" + langCode + ".yml");
            messages.clear();
            flatten("", yaml.getValues(true));
            currentLang = langCode;
            LoggerUtil.log(LoggerUtil.LogType.INFO, "Loaded current language: " + currentLang + " (" + messages.size() + " messages)");
        } catch (Exception e) {
            LoggerUtil.log(LoggerUtil.LogType.SEVERE, "Failed to load language file: " + langCode + ". Exception: " + e.getMessage());
        }
    }

    /**
     * Recursively flattens nested YAML keys into dot‑notation.
     * e.g. messages.already-owned > "messages.already-owned"
     */
    private void flatten(String prefix, Map<String, Object> source) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            if (entry.getValue() instanceof Map) {
                flatten(key, (Map<String, Object>) entry.getValue());
            } else {
                messages.put(key, entry.getValue().toString());
            }
        }
    }

    @Override
    public String getMessage(String key, Map<?, ?> placeholders) {
        String template = messages.getOrDefault(key, key);
        if (placeholders != null) {
            for (Map.Entry<?, ?> entry : placeholders.entrySet()) {
                template = template.replace("{" + entry.getKey().toString() + "}", entry.getValue().toString());
            }
        }
        return template;
    }

    @Override
    public String getMessage(String key) {
        return getMessage(key, null);
    }

    /// It's just a glorified {@link ConfigUtil#loadDataFile(Plugin, String)} but it does it in a try/catch and defaults to the lang folder. I'm welcome.
    private void createLanguageFile(String langName) {
        try {
            ConfigUtil.loadOrAdapt(plugin, "lang/" + langName);
        } catch (Exception e) {
            LoggerUtil.log(LoggerUtil.LogType.SEVERE, "Could not create default language file: " + e.getMessage());
        }
    }
}