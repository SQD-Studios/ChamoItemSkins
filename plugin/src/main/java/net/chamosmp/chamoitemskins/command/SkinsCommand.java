package net.chamosmp.chamoitemskins.command;

import com.sqdstudios.sqdlib.paper.chamogui.config.GuiSlot;
import com.sqdstudios.sqdlib.paper.dialog.SimpleDialog;
import net.chamosmp.chamoitemskins.ChamoItemSkinsPlugin;
import net.chamosmp.chamoitemskins.api.service.GrantService;
import net.chamosmp.chamoitemskins.api.service.SkinService;
import net.chamosmp.chamoitemskins.gui.main.MainSkinsGui;
import net.chamosmp.chamoitemskins.manager.FavoriteManager;
import net.chamosmp.chamoitemskins.manager.RarityManager;
import net.chamosmp.chamoitemskins.models.ModelService;
import net.chamosmp.chamoitemskins.util.MessageUtil;
import net.strokkur.commands.Aliases;
import net.strokkur.commands.Command;
import net.strokkur.commands.Executes;
import net.strokkur.commands.paper.Description;
import net.strokkur.commands.paper.Executor;
import net.strokkur.commands.permission.Permission;
import org.bukkit.entity.Player;

import java.util.List;

@Command("skins")
@Aliases("skin")
@Description("Open the skins menu")
@Permission("chamoitemskins.use")
public final class SkinsCommand {
    private final ChamoItemSkinsPlugin plugin;
    private final SkinService skinService;
    private final GrantService grantService;
    private final String guiTitle;
    private final int guiSize;
    private final List<GuiSlot> guiSlots;
    private final SimpleDialog chatInputUtil;
    private final ModelService modelService;
    private final RarityManager rarityManager;
    private final MessageUtil messageUtil;
    private final FavoriteManager favoriteManager;

    public SkinsCommand(ChamoItemSkinsPlugin plugin, SkinService skinService, GrantService grantService, String guiTitle, int guiSize, List<GuiSlot> guiSlots, SimpleDialog chatInputUtil, ModelService modelService, RarityManager rarityManager, MessageUtil messageUtil, FavoriteManager favoriteManager) {
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

    @Executes
    public void onSkins(@Executor Player player) {
        new MainSkinsGui(plugin, player, skinService, grantService, guiTitle, guiSize, guiSlots, chatInputUtil, modelService, rarityManager, messageUtil, favoriteManager).open();
    }
}
