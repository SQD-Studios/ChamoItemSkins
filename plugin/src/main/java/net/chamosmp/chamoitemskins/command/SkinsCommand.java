package net.chamosmp.chamoitemskins.command;

import net.chamosmp.chamoitemskins.ChamoItemSkinsPlugin;
import net.chamosmp.chamoitemskins.api.service.GrantService;
import net.chamosmp.chamoitemskins.api.service.SkinService;
import net.chamosmp.chamoitemskins.gui.main.MainSkinsGui;
import net.chamosmp.chamoitemskins.gui.config.GuiSlotDef;
import net.chamosmp.chamoitemskins.manager.FavoriteManager;
import net.chamosmp.chamoitemskins.manager.RarityManager;
import net.chamosmp.chamoitemskins.models.ModelService;
import net.chamosmp.chamoitemskins.util.ChatInputUtil;
import net.chamosmp.chamoitemskins.util.MessageUtil;
import net.strokkur.commands.Aliases;
import net.strokkur.commands.Command;
import net.strokkur.commands.Executes;
import net.strokkur.commands.paper.Description;
import net.strokkur.commands.permission.Permission;
import net.strokkur.commands.paper.Executor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

@Command("skins")
@Aliases("skin")
@Description("Open the skins menu")
public final class SkinsCommand {
    private final ChamoItemSkinsPlugin plugin;
    private final SkinService skinService;
    private final GrantService grantService;
    private final String guiTitle;
    private final int guiSize;
    private final List<GuiSlotDef> guiSlots;
    private final ChatInputUtil chatInputUtil;
    private final ModelService modelService;
    private final RarityManager rarityManager;
    private final MessageUtil messageUtil;
    private final FavoriteManager favoriteManager;

    public SkinsCommand(ChamoItemSkinsPlugin plugin, SkinService skinService, GrantService grantService, String guiTitle, int guiSize, List<GuiSlotDef> guiSlots, ChatInputUtil chatInputUtil, ModelService modelService, RarityManager rarityManager, MessageUtil messageUtil, FavoriteManager favoriteManager) {
        this.plugin = plugin;
        this.skinService = skinService;
        this.grantService = grantService;
        this.guiTitle = guiTitle;
        this.guiSize = guiSize;
        this.guiSlots = guiSlots;
        this.chatInputUtil = chatInputUtil;
        this.modelService = modelService;
        this.rarityManager = rarityManager;
        this.messageUtil = messageUtil;
        this.favoriteManager = favoriteManager;
    }


    @Permission("chamoitemskins.use")
    @Executes
    public void onSkins(@Executor Player player) {
        new MainSkinsGui(plugin, player, skinService, grantService, guiTitle, guiSize, guiSlots, chatInputUtil, modelService, rarityManager, messageUtil, favoriteManager).open();
    }

    @Permission("chamoitemskins.use.other")
    @Executes
    public void onSkinsOther(Player target) {
        new MainSkinsGui(plugin, target, skinService, grantService, guiTitle, guiSize, guiSlots, chatInputUtil, modelService, rarityManager, messageUtil, favoriteManager).open();
    }

}
