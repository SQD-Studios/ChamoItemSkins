// --- plugin/src/main/java/net/chamosmp/chamoitemskins/command/SkinsCommand.java ---
package net.chamosmp.chamoitemskins.command;

import net.chamosmp.chamoitemskins.api.service.GrantService;
import net.chamosmp.chamoitemskins.api.service.SkinService;
import net.chamosmp.chamoitemskins.gui.main.MainSkinsGui;
import net.chamosmp.chamoitemskins.gui.config.GuiSlotDef;
import net.chamosmp.chamoitemskins.manager.SkinManager;
import net.chamosmp.chamoitemskins.util.ChatInputUtil;
import net.chamosmp.chamoitemskins.util.DialogUtil;
import net.strokkur.commands.Aliases;
import net.strokkur.commands.Command;
import net.strokkur.commands.Executes;
import net.strokkur.commands.paper.Description;
import net.strokkur.commands.permission.Permission;
import net.strokkur.commands.paper.Executor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

@Command("skins")
@Aliases("skin")
@Description("Open the skins menu")
public final class SkinsCommand {
    private final Plugin plugin;
    private final SkinService skinService;
    private final GrantService grantService;
    private final String guiTitle;
    private final int guiSize;
    private final List<GuiSlotDef> guiSlots;
    private final SkinManager skinManager;
    private final DialogUtil dialogUtil;
    private final ChatInputUtil chatInputUtil;

    public SkinsCommand(Plugin plugin, SkinService skinService, GrantService grantService, String guiTitle, int guiSize, List<GuiSlotDef> guiSlots, SkinManager skinManager, DialogUtil dialogUtil, ChatInputUtil chatInputUtil) {
        this.plugin = plugin;
        this.skinService = skinService;
        this.grantService = grantService;
        this.guiTitle = guiTitle;
        this.guiSize = guiSize;
        this.guiSlots = guiSlots;
        this.skinManager = skinManager;
        this.dialogUtil = dialogUtil;
        this.chatInputUtil = chatInputUtil;
    }



    @Permission("chamoitemskins.use")
    @Executes
    public void onSkins(@Executor Player player) {
        new MainSkinsGui(plugin, player, skinService, grantService, guiTitle, guiSize, guiSlots, skinManager, chatInputUtil).open();
    }

    @Permission("chamoitemskins.use.other")
    @Executes
    public void onSkinsOther(CommandSender sender, Player target) {
        new MainSkinsGui(plugin, target, skinService, grantService, guiTitle, guiSize, guiSlots, skinManager, chatInputUtil).open();
    }

}
