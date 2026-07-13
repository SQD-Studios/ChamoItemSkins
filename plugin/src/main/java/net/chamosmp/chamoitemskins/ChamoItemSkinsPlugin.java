package net.chamosmp.chamoitemskins;

import de.skyslycer.hmcwraps.HMCWraps;
import de.skyslycer.hmcwraps.messages.Messages;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.chamosmp.chamoitemskins.api.ChamoItemSkinsApi;
import net.chamosmp.chamoitemskins.api.service.GrantService;
import net.chamosmp.chamoitemskins.api.service.LogService;
import net.chamosmp.chamoitemskins.api.service.SkinService;
import net.chamosmp.chamoitemskins.lang.LanguageManager;
import net.chamosmp.chamoitemskins.manager.*;
import net.chamosmp.chamoitemskins.models.ModelService;
import net.chamosmp.chamoitemskins.command.AdminCommandBrigadier;
import net.chamosmp.chamoitemskins.command.SkinsCommandBrigadier;
import net.chamosmp.chamoitemskins.command.suggestions.skinIdSuggestionsImpl;
import net.chamosmp.chamoitemskins.database.DatabaseManager;
import net.chamosmp.chamoitemskins.database.MySQLDatabase;
import net.chamosmp.chamoitemskins.database.SQLiteDatabase;
import net.chamosmp.chamoitemskins.gui.GuiFillerUtil;
import net.chamosmp.chamoitemskins.gui.config.GuiSlotDef;
import net.chamosmp.chamoitemskins.gui.config.SlotType;
import net.chamosmp.chamoitemskins.listener.GuiListener;
import net.chamosmp.chamoitemskins.listener.NoteListener;
import net.chamosmp.chamoitemskins.listener.SkinApplyListener;
import net.chamosmp.chamoitemskins.placeholder.ChamoItemSkinsExpansion;
import net.chamosmp.chamoitemskins.scheduler.SchedulerUtil;
import net.chamosmp.chamoitemskins.util.*;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class ChamoItemSkinsPlugin extends JavaPlugin implements ChamoItemSkinsApi {
    private DatabaseManager databaseManager;
    private SkinManager skinManager;
    private GrantManager grantManager;
    private CacheManager cacheManager;
    private LogManager logManager;
    private RarityManager rarityManager;
    private GuiFillerUtil guiFillerUtil;
    private ModelService modelService;
    private ChatInputUtil chatInputUtil;
    private DialogUtil dialogUtil;
    private MigrateManager migrateManager;
    private LanguageManager langManager;


    /**
     * When the plugins load, at the very start of your server
     */
    @Override
    public void onLoad() {
        new MessageUtil(langManager);

        NoteUtil.init(this);
    }

    /**
     * Do I need to say a lot? The event when the plugin gets enabled, after the dependencies
     * declared in the {@code plugin.yml}.
     */
    @Override
    public void onEnable() {

        migrateManager = new MigrateManager(this, skinManager);
        Bukkit.getServicesManager().register(ChamoItemSkinsApi.class, this, this, ServicePriority.Normal);
        Bukkit.getServicesManager().register(SkinService.class, getSkinService(), this, ServicePriority.Normal);
        Bukkit.getServicesManager().register(GrantService.class, getGrantService(), this, ServicePriority.Normal);
        Bukkit.getServicesManager().register(LogService.class, getLogService(), this, ServicePriority.Normal);

        reloadPlugin();

        Bukkit.getPluginManager().registerEvents(new NoteListener(this, skinManager, grantManager, getConfig(), new MessageUtil(langManager)), this);
        Bukkit.getPluginManager().registerEvents(new GuiListener(), this);
        Bukkit.getPluginManager().registerEvents(new SkinApplyListener(grantManager), this);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new ChamoItemSkinsExpansion(skinManager, grantManager, rarityManager, this).register();
        }

        skinIdSuggestionsImpl.init(skinManager);
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS.newHandler(event -> {
            try {
                if (skinManager == null || grantManager == null) {
                    initManagers();
                }

                var guiConfig = ConfigUtil.loadDataFile(this, "guis/gui.yml");
                var adminGuiConfig = ConfigUtil.loadDataFile(this, "guis/admin-gui.yml");

                List<GuiSlotDef> mainSlots = parseSlots(guiConfig.getConfigurationSection("slots"));
                String skinsTitle = guiConfig.getString("title", "Skins");
                int skinsSize = guiConfig.getInt("size", 54);

                List<GuiSlotDef> adminSlots = parseSlots(adminGuiConfig.getConfigurationSection("slots"));
                String adminTitle = adminGuiConfig.getString("title", "Admin");
                int adminSize = adminGuiConfig.getInt("size", 54);

                SkinsCommandBrigadier.register(event.registrar(), this, skinManager, grantManager, skinsTitle, skinsSize, mainSlots, skinManager, dialogUtil, chatInputUtil);
                AdminCommandBrigadier.register(event.registrar(), this, skinManager, grantManager, getConfig(), adminTitle, adminSize, adminSlots, dialogUtil, migrateManager, new MessageUtil(langManager));
                getLogger().info("Successfully registered commands.");
            } catch (Exception e) {
                getLogger().severe("Failed to register commands: " + e.getMessage());
                e.printStackTrace();
            }
        }));
        Metrics metrics = new Metrics(this, 31970);
        metrics.addCustomChart(
                new SingleLineChart("totalskins", () -> getSkinService().getSkins().size())
        );

        getLogger().info("Successfully loaded metrics.");
        getLogger().info("ChamoItemSkins enabled successfully.");
    }

    private void initManagers() {
        var config = getConfig();
        if (this.databaseManager == null) setupDatabase(config);
        if (this.rarityManager == null) {
            this.rarityManager = new RarityManager(this);
            this.rarityManager.load(config);
        }
        if (this.skinManager == null) {
            this.skinManager = new SkinManager(this, rarityManager, databaseManager);
            this.skinManager.reloadSkins();
        }
        if (this.modelService == null) this.modelService = new ModelService();
        if (this.cacheManager == null) this.cacheManager = new CacheManager(config.getLong("cache.ttl-seconds", 300));
        if (this.logManager == null) this.logManager = new LogManager(this, databaseManager);
        if (this.grantManager == null) {
            this.grantManager = new GrantManager(this, databaseManager, cacheManager, skinManager, logManager, modelService);
        }
        if (this.langManager == null) this.langManager = new LanguageManager(this);
    }

    /**
     * The method to reload the plugin
     */
    public void reloadPlugin() {

        ConfigUtil.loadOrAdapt(this, "config.yml");
        ConfigUtil.loadDataFile(this, "guis/gui.yml");
        ConfigUtil.loadDataFile(this, "guis/admin-gui.yml");
        saveDefaultConfig();
        reloadConfig();
        
        initManagers();
        
        this.guiFillerUtil = GuiFillerUtil.load(getConfig());
        this.dialogUtil = new DialogUtil(this);
        this.chatInputUtil = new ChatInputUtil(this, dialogUtil, new MessageUtil(langManager));


        Bukkit.getServicesManager().unregisterAll(this);
        Bukkit.getServicesManager().register(ChamoItemSkinsApi.class, this, this, ServicePriority.Normal);
        Bukkit.getServicesManager().register(SkinService.class, getSkinService(), this, ServicePriority.Normal);
        Bukkit.getServicesManager().register(GrantService.class, getGrantService(), this, ServicePriority.Normal);
        Bukkit.getServicesManager().register(LogService.class, getLogService(), this, ServicePriority.Normal);

        SchedulerUtil.runAsync(this, () -> grantManager.checkAndRevokeExpiredGrants());

        SchedulerUtil.runDelayed(this, () -> grantManager.checkAndRevokeExpiredGrants(),  6000L);

        // Periodically Reload the plugin (And maybe break some things :D)
        SchedulerUtil.runDelayed(this, this::reloadPlugin, 7000L);
    }

    /**
     * When the plugin gets disabled
     */
    @Override
    public void onDisable() {
        if (databaseManager != null) databaseManager.close();
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

    public @NotNull RarityManager getRarityManager() {
        return rarityManager;
    }

    public @NotNull GuiFillerUtil getGuiFillerUtil() {
        return guiFillerUtil != null ? guiFillerUtil : GuiFillerUtil.load(getConfig());
    }

    public ChatInputUtil getChatInputUtil() {
        return chatInputUtil;
    }

    public @NotNull ModelService getModelService() {
        return modelService;
    }
}
