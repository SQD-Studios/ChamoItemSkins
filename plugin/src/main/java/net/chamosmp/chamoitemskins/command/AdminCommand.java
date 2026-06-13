// --- plugin/src/main/java/net/chamosmp/chamoitemskins/command/AdminCommand.java ---
package net.chamosmp.chamoitemskins.command;

import net.chamosmp.chamoitemskins.api.model.Skin;
import net.chamosmp.chamoitemskins.api.service.GrantService;
import net.chamosmp.chamoitemskins.api.service.SkinService;
import net.chamosmp.chamoitemskins.gui.AdminGui;
import net.chamosmp.chamoitemskins.gui.config.GuiSlotDef;
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
import java.util.Map;

@Command("skinsadmin")
public final class AdminCommand {
    private final Plugin plugin;
    private final SkinService skinService;
    private final GrantService grantService;
    private final FileConfiguration config;
    private final String adminGuiTitle;
    private final int adminGuiSize;
    private final List<GuiSlotDef> adminGuiSlots;

    public AdminCommand(Plugin plugin, SkinService skinService, GrantService grantService, FileConfiguration config,
                        String adminGuiTitle, int adminGuiSize, List<GuiSlotDef> adminGuiSlots) {
        this.plugin = plugin;
        this.skinService = skinService;
        this.grantService = grantService;
        this.config = config;
        this.adminGuiTitle = adminGuiTitle;
        this.adminGuiSize = adminGuiSize;
        this.adminGuiSlots = adminGuiSlots;
    }

    @Permission("chamoitemskins.admin")
    @Executes("gui")
    public void onGui(Player player) {
        new AdminGui(plugin, player, adminGuiTitle, adminGuiSize, adminGuiSlots).open();
    }

    @Permission("chamoitemskins.admin")
    @Executes("reload")
    public void onReload(CommandSender sender) {
        plugin.onDisable();
        plugin.onEnable();
        MessageUtil.sendMessage(sender, config.getString("messages.reload-success", "<green>ChamoItemSkins reloaded."));
    }

    @Permission("chamoitemskins.admin")
    @Executes("give")
    public void onGive(CommandSender sender, Player target, String skinId) {
        skinService.getSkin(skinId).ifPresentOrElse(skin -> {
            Material defMat = Material.matchMaterial(config.getString("note.default-material", "PAPER"));
            String nameTmpl = config.getString("note.display-name", "<gold><bold>Skin Note");
            List<String> loreTmpl = config.getStringList("note.lore");
            
            target.getInventory().addItem(NoteUtil.createNote(plugin, skin, defMat, nameTmpl, loreTmpl));
            MessageUtil.sendMessage(sender, "<green>Gave " + skin.id() + " note to " + target.getName());
        }, () -> MessageUtil.sendMessage(sender, "<red>Skin not found: " + skinId));
    }
}
