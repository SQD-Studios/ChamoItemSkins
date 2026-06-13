// --- plugin/src/main/java/net/chamosmp/chamoitemskins/ChamoItemSkinsPlugin.java ---
package net.chamosmp.chamoitemskins;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.chamosmp.chamoitemskins.api.ChamoItemSkinsApi;
import net.chamosmp.chamoitemskins.api.service.GrantService;
import net.chamosmp.chamoitemskins.api.service.LogService;
import net.chamosmp.chamoitemskins.api.service.SkinService;
import net.chamosmp.chamoitemskins.command.AdminCommandBrigadier;
import net.chamosmp.chamoitemskins.command.SkinsCommandBrigadier;
import net.chamosmp.chamoitemskins.database.DatabaseManager;
import net.chamosmp.chamoitemskins.database.MySQLDatabase;
import net.chamosmp.chamoitemskins.database.SQLiteDatabase;
import net.chamosmp.chamoitemskins.gui.config.GuiSlotDef;
import net.chamosmp.chamoitemskins.gui.config.SlotType;
import net.chamosmp.chamoitemskins.gui.main.MainSkinsGui;
import net.chamosmp.chamoitemskins.gui.main.SkinSelectionGui;
import net.chamosmp.chamoitemskins.listener.GuiListener;
import net.chamosmp.chamoitemskins.listener.NoteListener;
import net.chamosmp.chamoitemskins.manager.CacheManager;
import net.chamosmp.chamoitemskins.manager.GrantManager;
import net.chamosmp.chamoitemskins.manager.LogManager;
import net.chamosmp.chamoitemskins.manager.SkinManager;
import net.chamosmp.chamoitemskins.bettermodel.BetterModelService;
import net.chamosmp.chamoitemskins.placeholder.ChamoItemSkinsExpansion;
import net.chamosmp.chamoitemskins.util.ChatInputUtil;
import net.chamosmp.chamoitemskins.util.ConfigUtil;
import net.chamosmp.chamoitemskins.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class ChamoItemSkinsPlugin extends JavaPlugin implements ChamoItemSkinsApi {
    private static ChamoItemSkinsPlugin instance;
    private DatabaseManager databaseManager;
    private SkinManager skinManager;
    private GrantManager grantManager;
    private CacheManager cacheManager;
    private LogManager logManager;
    private BetterModelService betterModelService;
    private ChatInputUtil chatInputUtil;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        // 4. Register API
        Bukkit.getServicesManager().register(ChamoItemSkinsApi.class, this, this, ServicePriority.Normal);
        Bukkit.getServicesManager().register(SkinService.class, getSkinService(), this, ServicePriority.Normal);
        Bukkit.getServicesManager().register(GrantService.class, getGrantService(), this, ServicePriority.Normal);
        Bukkit.getServicesManager().register(LogService.class, getLogService(), this, ServicePriority.Normal);

        reloadPlugin();

        // 5. Register Listeners
        Bukkit.getPluginManager().registerEvents(new NoteListener(this, skinManager, grantManager, getConfig()), this);
        Bukkit.getPluginManager().registerEvents(new GuiListener(), this);

        // 6. Register Placeholders
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new ChamoItemSkinsExpansion(skinManager, grantManager).register();
        }

        // 7. Register Commands
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS.newHandler(event -> {
            var guiConfig = ConfigUtil.loadOrAdapt(this, "gui.yml");
            var adminGuiConfig = ConfigUtil.loadOrAdapt(this, "admin-gui.yml");

            List<GuiSlotDef> mainSlots = parseSlots(guiConfig.getConfigurationSection("slots"));
            String skinsTitle = guiConfig.getString("title", "Skins");
            int skinsSize = guiConfig.getInt("size", 54);

            List<GuiSlotDef> adminSlots = parseSlots(adminGuiConfig.getConfigurationSection("slots"));
            String adminTitle = adminGuiConfig.getString("title", "Admin");
            int adminSize = adminGuiConfig.getInt("size", 54);

            SkinsCommandBrigadier.register(event.registrar(), this, skinManager, grantManager, skinsTitle, skinsSize, mainSlots);
            AdminCommandBrigadier.register(event.registrar(), this, skinManager, grantManager, getConfig(), adminTitle, adminSize, adminSlots);
        }));

        getLogger().info("ChamoItemSkins enabled successfully.");
    }

    public void reloadPlugin() {
        // 1. Load Configurations
        saveDefaultConfig();
        reloadConfig();
        var config = getConfig();
        MessageUtil.loadLanguages(this);

        // 2. Initialize Database
        DatabaseManager oldDb = this.databaseManager;
        setupDatabase(config);
        if (oldDb != null) {
            oldDb.close();
        }

        // 3. Initialize Managers
        if (this.skinManager == null) {
            this.skinManager = new SkinManager(this);
        }
        this.skinManager.reloadSkins();

        if (this.cacheManager == null) {
            this.cacheManager = new CacheManager(config.getLong("cache.ttl-seconds", 300));
        }

        if (this.logManager == null) {
            this.logManager = new LogManager(this, databaseManager);
        }

        if (this.betterModelService == null) {
            this.betterModelService = new BetterModelService();
        }
        
        if (this.grantManager == null) {
            this.grantManager = new GrantManager(this, databaseManager, cacheManager, skinManager, logManager, betterModelService);
        } else {
            this.grantManager = new GrantManager(this, databaseManager, cacheManager, skinManager, logManager, betterModelService);
        }

        if (this.chatInputUtil == null) {
            this.chatInputUtil = new ChatInputUtil(this);
        }

        // Re-register services to ensure they point to current manager instances
        Bukkit.getServicesManager().unregisterAll(this);
        Bukkit.getServicesManager().register(ChamoItemSkinsApi.class, this, this, ServicePriority.Normal);
        Bukkit.getServicesManager().register(SkinService.class, getSkinService(), this, ServicePriority.Normal);
        Bukkit.getServicesManager().register(GrantService.class, getGrantService(), this, ServicePriority.Normal);
        Bukkit.getServicesManager().register(LogService.class, getLogService(), this, ServicePriority.Normal);
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) databaseManager.close();
        // PaperCommandManager might not have unregisterAll or it might be different
        getLogger().info("ChamoItemSkins disabled.");
    }

    private void setupDatabase(FileConfiguration config) {
        String type = config.getString("database.type", "sqlite");
        if (type.equalsIgnoreCase("mysql")) {
            this.databaseManager = new MySQLDatabase(this,
                    config.getString("database.host"),
                    config.getString("database.port"),
                    config.getString("database.database"),
                    config.getString("database.username"),
                    config.getString("database.password"));
        } else {
            this.databaseManager = new SQLiteDatabase(this, new File(getDataFolder(), "data.db"));
        }
        this.databaseManager.init();
    }

    private List<GuiSlotDef> parseSlots(ConfigurationSection section) {
        List<GuiSlotDef> slots = new ArrayList<>();
        if (section == null) return slots;
        for (String key : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s == null) continue;
            
            SlotType type = parseSlotType(s.getString("type", "Decorative"), s);
            slots.add(new GuiSlotDef(
                    type,
                    s.getInt("slot"),
                    Material.matchMaterial(s.getString("material", "STONE")),
                    s.getString("name", ""),
                    s.getStringList("lore"),
                    s.getBoolean("glow", false)
            ));
        }
        return slots;
    }

    private SlotType parseSlotType(String typeStr, ConfigurationSection section) {
        return switch (typeStr.toUpperCase()) {
            case "SKINSLOT" -> new SlotType.SkinSlot(section.getInt("index", 0));
            case "FILTERSLOT" -> new SlotType.FilterSlot();
            case "BACKSLOT" -> new SlotType.BackSlot();
            case "ACTIONSLOT" -> new SlotType.ActionSlot(section.getString("action", ""));
            default -> new SlotType.Decorative();
        };
    }

    @Override
    public @NotNull SkinService getSkinService() {
        return skinManager;
    }

    @Override
    public @NotNull GrantService getGrantService() {
        return grantManager;
    }

    @Override
    public @NotNull LogService getLogService() {
        return logManager;
    }

    public ChatInputUtil getChatInputUtil() {
        return chatInputUtil;
    }
}
