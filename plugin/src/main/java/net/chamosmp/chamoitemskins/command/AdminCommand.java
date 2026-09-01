package net.chamosmp.chamoitemskins.command;

import net.chamosmp.chamoitemskins.api.service.GrantService;
import net.chamosmp.chamoitemskins.api.service.SkinService;
import net.chamosmp.chamoitemskins.command.suggestions.bundles.BundleSuggestions;
import net.chamosmp.chamoitemskins.command.suggestions.skinId.SkinIdSuggestions;
import net.chamosmp.chamoitemskins.gui.admin.AdminGui;
import net.chamosmp.chamoitemskins.gui.config.GuiSlotDef;
import net.chamosmp.chamoitemskins.gui.editor.EditorGui;
import net.chamosmp.chamoitemskins.manager.CategoryManager;
import net.chamosmp.chamoitemskins.manager.MigrateManager;
import net.chamosmp.chamoitemskins.manager.RarityManager;
import net.chamosmp.chamoitemskins.models.ModelService;
import net.chamosmp.chamoitemskins.util.ChatInputUtil;
import net.chamosmp.chamoitemskins.util.DialogUtil;
import net.chamosmp.chamoitemskins.util.MessageUtil;
import net.chamosmp.chamoitemskins.util.NoteUtil;
import net.strokkur.commands.Aliases;
import net.strokkur.commands.Command;
import net.strokkur.commands.Executes;
import net.strokkur.commands.Subcommand;
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
import java.util.Optional;

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
    private final MigrateManager migrateManager;
    private final MessageUtil messageUtil;
    private final ModelService modelService;
    private final CategoryManager categoryManager;
    private final RarityManager rarityManager;
    private final ChatInputUtil chatInputUtil;

    public AdminCommand(Plugin plugin, SkinService skinService, GrantService grantService, FileConfiguration config,
                        String adminGuiTitle, int adminGuiSize, List<GuiSlotDef> adminGuiSlots, DialogUtil dialogUtil, MigrateManager migrateManager, MessageUtil messageUtil, ModelService modelService, CategoryManager categoryManager, RarityManager rarityManager, ChatInputUtil chatInputUtil) {
        this.plugin = plugin;
        this.skinService = skinService;
        this.grantService = grantService;
        this.config = config;
        this.adminGuiTitle = adminGuiTitle;
        this.adminGuiSize = adminGuiSize;
        this.adminGuiSlots = adminGuiSlots;
        this.dialogUtil = dialogUtil;
        this.migrateManager = migrateManager;
        this.messageUtil = messageUtil;
        this.modelService = modelService;
        this.categoryManager = categoryManager;
        this.rarityManager = rarityManager;
        this.chatInputUtil = chatInputUtil;
    }

    @Permission("chamoitemskins.admin.editor")
    @Executes
    public void onBase(@Executor Player player) {
        new AdminGui(plugin, player, adminGuiTitle, adminGuiSize, adminGuiSlots, messageUtil, categoryManager, modelService, rarityManager, chatInputUtil, skinService).open();
    }

    @Permission("chamoitemskins.admin.editor")
    @Executes("editor")
    public void onEditor(@Executor Player player) {
        new EditorGui(plugin, player, messageUtil, categoryManager, modelService, rarityManager, chatInputUtil).open();
    }

    @Subcommand("access")
    public class onAccess {
        @Permission("chamoitemskins.admin.access.give")
        @Executes("give")
        public void onAccessGive(CommandSender sender, Player target, @SkinIdSuggestions String skinId, Optional<Integer> days) {
            if (days.isPresent()) {
                giveAccess(sender, target, skinId, days.get());

            } else {
                giveAccess(sender, target, skinId, -1);
            }
        }

        @Permission("chamoitemskins.admin.access.revoke")
        @Executes("revoke")
        public void onAccessRevoke(CommandSender sender, Player target, @SkinIdSuggestions String skinId) {
            skinService.getSkin(skinId).ifPresentOrElse(skin -> {
                grantService.hasSkin(target.getUniqueId(), skinId).thenAccept(has -> {
                    if (!has) {
                        messageUtil.sendLangMessage(sender, "revoke-not-owned", Map.of("id", skinId, "target", target.getName()));
                        return;
                    }
                    grantService.revokeSkin(target.getUniqueId(), skinId).thenRun(() -> {
                        messageUtil.sendLangMessage(sender, "admin-revoke-success", Map.of("id", skinId, "target", target.getName()));
                    });
                });
            }, () -> {
                Map<String, String> placeholders = Map.of("skin_id", skinId);
                messageUtil.sendLangMessage(sender, "skin-not-found", placeholders);
            });
        }
    }


    private void giveAccess(CommandSender sender, Player target, String skinId, int days) {
        skinService.getSkin(skinId).ifPresentOrElse(skin -> {
            grantService.hasSkin(target.getUniqueId(), skinId).thenAccept(has -> {
                if (has) {
                    messageUtil.sendLangMessage(sender, "already-has-access", Map.of("id", skinId, "target", target.getName()));
                    return;
                }
                grantService.grantSkin(target.getUniqueId(), skinId, "COMMAND", days).thenRun(() -> {
                    messageUtil.sendLangMessage(sender, "admin-grant-success", Map.of("id", skinId, "target", target.getName()));
                });
            });
        }, () -> {
            Map<String, String> placeholders = Map.of("skin_id", skinId);
            messageUtil.sendLangMessage(sender, "skin-not-found", placeholders);
        });
    }


    @Permission("chamoitemskins.admin.give")
    @Executes("give")
    public void onGive(CommandSender sender, Player target, @SkinIdSuggestions String skinId, Optional<Integer> amount, Optional<Integer> time) {
        int amountN = 1;
        int timeN = -1;

        if (amount.isPresent()) {
            amountN = amount.get();
        }
        if (time.isPresent()) {
            timeN = time.get();
        }


        giveSkinNotes(sender, target, skinId, amountN, timeN);
    }

    private void giveSkinNotes(CommandSender sender, Player target, String skinId, int amount, int time) {
        skinService.getSkin(skinId).ifPresentOrElse(skin -> {
            Material defMat = Material.matchMaterial(config.getString("note.default-material", "PAPER"));
            List<String> loreTmpl = config.getStringList("note.lore");
            for (int i = 0; i < amount; i++) {
                if (defMat == null) return;
                target.getInventory().addItem(NoteUtil.createNote(plugin, skin, defMat, loreTmpl, time));
            }
            messageUtil.sendLangMessage(sender, "admin-give-note", Map.of("skin_id", skin.id(), "target", target.getName(), "amount", String.valueOf(amount)));
        }, () -> {
            Map<String, String> placeholders = Map.of("skin_id", skinId);
            messageUtil.sendLangMessage(sender, "skin-not-found", placeholders);
        });
    }

    @Subcommand("migrate")
    public class migrate {

        @Permission("chamoitemskins.admin.migrate")
        @Executes("hmcwarps")
        public void onMigrate(CommandSender sender) {
            migrateManager.migrateHMC(sender);
        }

    }


    @Permission("chamoitemskins.admin.reload")
    @Executes("reload")
    public void onReload(CommandSender sender) {
        try {
            plugin.reloadConfig();
            skinService.reloadSkins();
            sender.sendRichMessage("<aqua>Reloaded config.");
        } catch (Exception e) {
            sender.sendRichMessage("<aqua>Failed to reload config." + e);
        }
    }

    @Permission("chamoitemskins.admin.help")
    @Executes("help")
    public void onHelp(CommandSender sender) {
        sender.sendRichMessage("""
                ------- <light_purple>ChamoItemSkins</light_purple> | List Of Commands -------
                <red>Admin</red>
                <yellow>/sa</yellow> - <green>Opens the admin gui</green>
                <yellow>/sa access give </yellow><aqua><player> <skinid> [time]</aqua> - <green>Gives direct access to a skin</green>
                <yellow>/sa access revoke </yellow><aqua><player> <skinid></aqua> - <green>Revokes access to a skin</green>
                <yellow>/sa give </yellow><aqua><player> <skinid> [amount] [time]</aqua> - <green>Gives a physical note, to get access to a skin</green>
                <yellow>/sa editor - <green>Opens the skin editor
                <yellow>/sa help</yellow> - <green>This message</green>
                <yellow>/sa migrate</yellow> <aqua><hmcwarps></aqua> - <green>Starts the migration of HMCWarps</green>
                <yellow>/sa reload</yellow> - <green>Reloads the plugin</green>
                
                <light_purple>User</light_purple>
                <yellow>/skin</yellow> - <green>Opens the skin gui
                """);
    }

    @Subcommand("bundle")
    public class bundle {
        @Subcommand("access")
        public class access {

            @Permission("chamoitemskins.admin.access.give")
            @Executes("give")
            public void onBundleAccessGive(CommandSender sender, Player target, @BundleSuggestions String bundleId) {
                giveBundleAccess(sender, target, bundleId);
            }

            @Permission("chamoitemskins.admin.access.revoke")
            @Executes("revoke")
            public void onBundleAccessRevoke(CommandSender sender, Player target, @BundleSuggestions String bundleId) {
                skinService.getBundle(bundleId).ifPresentOrElse(_ -> {
                    grantService.hasBundle(target.getUniqueId(), bundleId).thenAccept(has -> {
                        if (!has) {
                            messageUtil.sendLangMessage(sender, "revoke-not-owned", Map.of("id", bundleId, "target", target.getName()));
                            return;
                        }
                        grantService.revokeBundle(target.getUniqueId(), bundleId).thenRun(() -> {
                            messageUtil.sendLangMessage(sender, "admin-revoke-success", Map.of("id", bundleId, "target", target.getName()));
                        });
                    });
                }, () -> {
                    Map<String, String> placeholders = Map.of("bundle_id", bundleId);
                    messageUtil.sendLangMessage(sender, "bundle-not-found", placeholders);
                });
            }

            private void giveBundleAccess(CommandSender sender, Player target, String bundleId) {
                skinService.getBundle(bundleId).ifPresentOrElse(bundle -> {
                    grantService.hasBundle(target.getUniqueId(), bundle.id()).thenAccept(has -> {
                        if (has) {
                            messageUtil.sendLangMessage(sender, "already-has-access", Map.of("id", bundle.id(), "target", target.getName()));
                            return;
                        }
                        grantService.grantBundle(target.getUniqueId(), bundle.id(), "COMMAND").thenRun(() -> {
                            messageUtil.sendLangMessage(sender, "admin-grant-success", Map.of("id", bundle.id(), "target", target.getName()));
                        });
                    });
                }, () -> {
                    Map<String, String> placeholders = Map.of("bundle_id", bundleId);
                    messageUtil.sendLangMessage(sender, "bundle-not-found", placeholders);
                });
            }
        }
    }
}
