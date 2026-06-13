// --- plugin/src/main/java/net/chamosmp/chamoitemskins/ChamoItemSkinsPlugin.java ---
package net.chamosmp.chamoitemskins;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.chamosmp.chamoitemskins.api.ChamoItemSkinsApi;
import net.chamosmp.chamoitemskins.api.service.GrantService;
import net.chamosmp.chamoitemskins.api.service.SkinService;
import net.chamosmp.chamoitemskins.command.AdminCommand;
import net.chamosmp.chamoitemskins.command.AdminCommandBrigadier;
import net.chamosmp.chamoitemskins.command.SkinsCommand;
import net.chamosmp.chamoitemskins.command.SkinsCommandBrigadier;
import net.chamosmp.chamoitemskins.database.DatabaseManager;
import net.chamosmp.chamoitemskins.database.MySQLDatabase;
import net.chamosmp.chamoitemskins.database.SQLiteDatabase;
import net.chamosmp.chamoitemskins.gui.config.GuiSlotDef;
import net.chamosmp.chamoitemskins.gui.config.SlotType;
import net.chamosmp.chamoitemskins.listener.GuiListener;
import net.chamosmp.chamoitemskins.listener.NoteListener;
import net.chamosmp.chamoitemskins.manager.CacheManager;
import net.chamosmp.chamoitemskins.manager.GrantManager;
import net.chamosmp.chamoitemskins.manager.SkinManager;
import net.chamosmp.chamoitemskins.placeholder.ChamoItemSkinsExpansion;
import net.chamosmp.chamoitemskins.util.ConfigUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
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
    private SkinsCommand skinsCommand;
    private AdminCommand adminCommand;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        // 1. Load Configurations
        var config = ConfigUtil.loadOrAdapt(this, "config.yml");
        var guiConfig = ConfigUtil.loadOrAdapt(this, "gui.yml");
        var adminGuiConfig = ConfigUtil.loadOrAdapt(this, "admin-gui.yml");

        // 2. Initialize Database
        setupDatabase(config);

        // 3. Initialize Managers
        this.skinManager = new SkinManager(this);
        this.skinManager.reloadSkins();
        
        this.cacheManager = new CacheManager(config.getLong("cache.ttl-seconds", 300));
        this.grantManager = new GrantManager(this, databaseManager, cacheManager, skinManager);

        // 4. Register API
        Bukkit.getServicesManager().register(ChamoItemSkinsApi.class, this, this, ServicePriority.Normal);

        // 5. Register Listeners
        Bukkit.getPluginManager().registerEvents(new NoteListener(this, skinManager, grantManager, config), this);
        Bukkit.getPluginManager().registerEvents(new GuiListener(), this);

        // 6. Register Placeholders
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new ChamoItemSkinsExpansion(skinManager, grantManager).register();
        }

        // 7. Register Commands
        List<GuiSlotDef> mainSlots = parseSlots(guiConfig.getConfigurationSection("slots"));
        String skinsTitle = guiConfig.getString("title", "Skins");
        int skinsSize = guiConfig.getInt("size", 54);
        this.skinsCommand = new SkinsCommand(this, skinManager, grantManager, skinsTitle, skinsSize, mainSlots);

        List<GuiSlotDef> adminSlots = parseSlots(adminGuiConfig.getConfigurationSection("slots"));
        String adminTitle = adminGuiConfig.getString("title", "Admin");
        int adminSize = adminGuiConfig.getInt("size", 54);
        this.adminCommand = new AdminCommand(this, skinManager, grantManager, config, adminTitle, adminSize, adminSlots);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS.newHandler(event -> {
            SkinsCommandBrigadier.register(event.registrar(), this, skinManager, grantManager, skinsTitle, skinsSize, mainSlots);
            AdminCommandBrigadier.register(event.registrar(), this, skinManager, grantManager, config, adminTitle, adminSize, adminSlots);
        }));

        getLogger().info("ChamoItemSkins enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) databaseManager.close();
        // PaperCommandManager might not have unregisterAll or it might be different
        getLogger().info("ChamoItemSkins disabled.");
    }

    private void setupDatabase(YamlConfiguration config) {
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
}
