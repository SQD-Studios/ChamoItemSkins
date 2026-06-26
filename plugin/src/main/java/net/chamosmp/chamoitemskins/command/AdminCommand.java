// --- plugin/src/main/java/net/chamosmp/chamoitemskins/command/AdminCommand.java ---
package net.chamosmp.chamoitemskins.command;

import net.chamosmp.chamoitemskins.ChamoItemSkinsPlugin;
import net.chamosmp.chamoitemskins.api.model.Skin;
import net.chamosmp.chamoitemskins.api.service.GrantService;
import net.chamosmp.chamoitemskins.api.service.SkinService;
import net.chamosmp.chamoitemskins.command.suggestions.skinIdSuggestions;
import net.chamosmp.chamoitemskins.gui.admin.AdminGui;
import net.chamosmp.chamoitemskins.gui.config.GuiSlotDef;
import net.chamosmp.chamoitemskins.util.DialogUtil;
import net.chamosmp.chamoitemskins.util.MessageUtil;
import net.chamosmp.chamoitemskins.util.NoteUtil;
import net.strokkur.commands.Aliases;
import net.strokkur.commands.Command;
import net.strokkur.commands.Executes;
import net.strokkur.commands.paper.Description;
import net.strokkur.commands.paper.Executor;
import net.strokkur.commands.permission.Permission;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.util.List;

@Command("skinsadmin")
@Aliases({"sa", "skinadmin"})
@Description("Admin command for ChamoItemSkins")
public final class AdminCommand {
    private final Plugin plugin;
    private final SkinService skinService;
    private final GrantService grantService;
    private final FileConfiguration config;
    private final String adminGuiTitle;
    private final int adminGuiSize;
    private final List<GuiSlotDef> adminGuiSlots;
    private final DialogUtil dialogUtil;

    public AdminCommand(Plugin plugin, SkinService skinService, GrantService grantService, FileConfiguration config,
                        String adminGuiTitle, int adminGuiSize, List<GuiSlotDef> adminGuiSlots, DialogUtil dialogUtil) {
        this.plugin = plugin;
        this.skinService = skinService;
        this.grantService = grantService;
        this.config = config;
        this.adminGuiTitle = adminGuiTitle;
        this.adminGuiSize = adminGuiSize;
        this.adminGuiSlots = adminGuiSlots;
        this.dialogUtil = dialogUtil;
    }

    @Permission("chamoitemskins.admin.editor")
    @Executes
    public void onBase(@Executor Player player) {
        onGui(player);
    }

    @Permission("chamoitemskins.admin.editor")
    @Executes("gui")
    public void onGui(@Executor Player player) {
        new AdminGui(plugin, player, adminGuiTitle, adminGuiSize, adminGuiSlots, dialogUtil).open();
    }

    @Permission("chamoitemskins.admin.editor")
    @Executes("editor")
    public void onEditor(@Executor Player player) {
        new AdminGui(plugin, player, adminGuiTitle, adminGuiSize, adminGuiSlots, dialogUtil).open();
    }

    @Permission("chamoitemskins.admin.reload")
    @Executes("reload")
    public void onReload(CommandSender sender) {
        if (plugin instanceof ChamoItemSkinsPlugin chamoPlugin) {
            chamoPlugin.reloadPlugin();
            net.chamosmp.chamoitemskins.util.ConfigUtil.loadOrAdapt(plugin, "config.yml");
            net.chamosmp.chamoitemskins.util.ConfigUtil.loadOrAdapt(plugin, "guis/gui.yml");
            net.chamosmp.chamoitemskins.util.ConfigUtil.loadOrAdapt(plugin, "guis/admin-gui.yml");
            net.chamosmp.chamoitemskins.util.ConfigUtil.loadOrAdapt(plugin, "skins.yml");
            
            MessageUtil.sendMessage(sender, config.getString("messages.reload-success", "<green>ChamoItemSkins reloaded."));
        } else {
            MessageUtil.sendMessage(sender, "<red>Failed to reload plugin: Unexpected plugin instance.");
        }
    }

    @Permission("chamoitemskins.admin.give")
    @Executes("give")
    public void onGive(CommandSender sender, Player target, @skinIdSuggestions String skinId) {
        skinService.getSkin(skinId).ifPresentOrElse(skin -> {
            Material defMat = Material.matchMaterial(config.getString("note.default-material", "PAPER"));
            String nameTmpl = config.getString("note.display-name", "<gold><bold>Skin Note");
            List<String> loreTmpl = config.getStringList("note.lore");
            
            target.getInventory().addItem(NoteUtil.createNote(plugin, skin, defMat, nameTmpl, loreTmpl));
            MessageUtil.sendMessage(sender, "<green>Gave " + skin.id() + " note to " + target.getName());
        }, () -> MessageUtil.sendMessage(sender, "<red>Skin not found: " + skinId));
    }

    @Permission("chamoitemskins.admin.access.give")
    @Executes("access give")
    public void onAccessGive(CommandSender sender, Player target, @skinIdSuggestions String skinId) {
        skinService.getSkin(skinId).ifPresentOrElse(skin -> {
            grantService.hasSkin(target.getUniqueId(), skinId).thenAccept(has -> {
                if (has) {
                    MessageUtil.sendMessage(sender, "<red>" + target.getName() + " already has access to " + skinId);
                    return;
                }
                grantService.grantSkin(target.getUniqueId(), skinId, "COMMAND").thenRun(() -> {
                    MessageUtil.sendMessage(sender, "<green>Granted access to " + skinId + " for " + target.getName());
                });
            });
        }, () -> MessageUtil.sendMessage(sender, "<red>Skin not found: " + skinId));
    }

    @Permission("chamoitemskins.admin.access.revoke")
    @Executes("access revoke")
    public void onAccessRevoke(CommandSender sender, Player target, @skinIdSuggestions String skinId) {
        skinService.getSkin(skinId).ifPresentOrElse(skin -> {
            grantService.hasSkin(target.getUniqueId(), skinId).thenAccept(has -> {
                if (!has) {
                    MessageUtil.sendMessage(sender, "<red>" + target.getName() + " does not have access to " + skinId);
                    return;
                }
                grantService.revokeSkin(target.getUniqueId(), skinId).thenRun(() -> {
                    MessageUtil.sendMessage(sender, "<green>Revoked access to " + skinId + " from " + target.getName());
                });
            });
        }, () -> MessageUtil.sendMessage(sender, "<red>Skin not found: " + skinId));
    }
}
