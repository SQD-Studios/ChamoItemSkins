package net.chamosmp.chamoitemskins.util;

import net.chamosmp.chamoitemskins.scheduler.SchedulerUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateUtil implements Listener {

    private static final Logger log = LoggerFactory.getLogger(UpdateUtil.class);
    private final Plugin plugin;

    /**
     * Class constructor
     *
     * @param plugin The plugin instance
     */
    public UpdateUtil(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onConnect(PlayerJoinEvent event) throws IOException, InterruptedException {
        Player player = event.getPlayer();
        String version = remoteVer();

        if (version == null || version.equals("failed")) {
            return;
        }

        if (isNewerVersion(plugin.getPluginMeta().getVersion(), version) && player.hasPermission("chamoitemskins.update")) {
            SchedulerUtil.runForEntity(plugin, player, () -> {
                player.sendRichMessage("<white>Download the plugin update <u><click:open_url:https://github.com/SQD-Studios/ChamoItemSkins/releases>here<r>");
            }, () -> {
            });
        }
    }

    public void versionCheck() throws Exception {
        String pluginVer = plugin.getPluginMeta().getVersion();
        String version = remoteVer();

        if (!version.equals("failed")) {
            if (isNewerVersion(pluginVer, version)) {
                LoggerUtil.log(LoggerUtil.LogType.INFO, """
                        New update available. Your version: " + pluginVer + ", latest version: " + version
                        Download plugin here: https://github.com/SQD-Studios/ChamoItemSkins/releases
                        """);
            }
        } else {
            LoggerUtil.log(LoggerUtil.LogType.WARNING, "Failed to check for updates.");
        }
    }

    public String remoteVer() throws IOException, InterruptedException {
        String baseUrl = "https://api.modrinth.com/v2";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/project/LTLCgsgz/version"))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();

        // Parse version_number from JSON using regex — no ObjectMapper needed
        Pattern pattern = Pattern.compile("\"version_number\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(responseBody);

        if (!matcher.find()) {
            return "failed";
        }

        return matcher.group(1);

    }

    public static boolean isNewerVersion(String current, String latest) {
        String[] currentParts = current.split("\\.");
        String[] latestParts = latest.split("\\.");

        int maxLength = Math.max(currentParts.length, latestParts.length);

        for (int i = 0; i < maxLength; i++) {
            int currentValue = 0;
            int latestValue = 0;
            try {
                currentValue =
                        i < currentParts.length
                                ? Integer.parseInt(currentParts[i])
                                : 0;

                latestValue =
                        i < latestParts.length
                                ? Integer.parseInt(latestParts[i])
                                : 0;
            } catch (NumberFormatException e) {
                log.error("Had error parsing versions: ", e);
            }
            if (latestValue > currentValue) {
                return true;
            }

            if (latestValue < currentValue) {
                return false;
            }
        }

        return false;
    }

}

