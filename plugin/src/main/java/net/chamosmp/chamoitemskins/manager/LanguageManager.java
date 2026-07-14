package net.chamosmp.chamoitemskins.manager;

import net.chamosmp.chamoitemskins.util.ConfigUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {

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
    public void loadLanguage(String langCode) {
        File langFile = new File(plugin.getDataFolder(), "lang/" + langCode + ".yml");
        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file 'lang/" + langCode + ".yml' not found. Using default: " + defaultLang);
            langFile = new File(plugin.getDataFolder(), "lang/" + defaultLang + ".yml");
            if (!langFile.exists()) {
                plugin.getLogger().severe("Default language file 'lang/" + defaultLang + ".yml' missing! Creating one...");
                createLanguageFile(defaultLang);
                langFile = new File(plugin.getDataFolder(), "lang/" + defaultLang + ".yml");
            }
        }
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(langFile);
            messages.clear();
            flatten("", yaml.getValues(true));
            currentLang = langCode;
            plugin.getLogger().info("Loaded language: " + currentLang + " (" + messages.size() + " messages)");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load language file: " + langFile.getName());
            e.printStackTrace();
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

    /**
     * Gets a message by key. Supports placeholders like {player}.
     */
    public String getMessage(String key, Map<String, String> placeholders) {
        String template = messages.getOrDefault(key, /*"<red>Missing: " +*/ key); // If it ain't broke don't fix it (I won't go and create messages for everything lal
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                template = template.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return template;
    }

    public String getMessage(String key) {
        return getMessage(key, null);
    }

    /// It's just a glorified {@link ConfigUtil#loadDataFile(Plugin, String)} but it does it in a try/catch and defaults to the lang folder. I'm welcome.
    private void createLanguageFile(String langName) {
        try {
            ConfigUtil.loadOrAdapt(plugin, "lang/" + langName);
        } catch (Exception e) {
            plugin.getLogger().severe("Could not create default language file: " + e.getMessage());
        }
    }
}