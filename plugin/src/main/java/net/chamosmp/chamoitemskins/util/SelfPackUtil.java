package net.chamosmp.chamoitemskins.util;

import com.sun.net.httpserver.SimpleFileServer;
import net.chamosmp.chamoitemskins.ChamoItemSkinsPlugin;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SelfPackUtil implements Listener {

    private static final Logger log = LoggerFactory.getLogger(SelfPackUtil.class);
    private final ChamoItemSkinsPlugin plugin;
    private final int port;
    private final InetSocketAddress address;
    private final String ip;
    private final Path pluginFolder;
    private final File pluginFile;

    public SelfPackUtil(ChamoItemSkinsPlugin plugin) throws IOException {
        this.plugin = plugin;
        this.port = plugin.getConfig().getInt("selfpack.port");
        this.isEnabled = plugin.getConfig().getBoolean("selfpack.enable");
        this.isPreMakeEnabled = plugin.getConfig().getBoolean("selfpack.pre-make");
        this.address = new InetSocketAddress(this.port);
        this.ip = plugin.getConfig().getString("selfpack.ip", plugin.getServer().getIp());
        this.pluginFolder = plugin.getDataFolder().toPath();
        this.pluginFile = plugin.getDataFolder();

        String pluginPath = plugin.getDataFolder().toString();

        File selfpack = new File(pluginPath, "selfpack");
        File models = new File(selfpack, "models");

        File textures = new File(models, "textures");
        File items = new File(models, "items");

        if (!selfpack.exists()) {
            textures.mkdirs();
            items.mkdirs();
        } else if (!models.exists()) {
            textures.mkdirs();
            items.mkdirs();
        } else if (!textures.exists()) {
            textures.mkdirs();
        } else if (!items.exists()) {
            items.mkdirs();
        }

        if (isPreMakeEnabled) {
            preMakePack();
        }
        if (isEnabled) {
            hostFiles(plugin.getDataFolder().toPath().resolve("selfpack"));
        }
    }

    private final boolean isEnabled;
    private final boolean isPreMakeEnabled;


    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (isEnabled) {
            String url = "http://" + ip + ":" + port + "/resourcepack.zip";
            sendResourcePack(player, url);
        }
    }

    public void hostFiles(Path path) {
        try {
            Path absolutePath = path.toAbsolutePath();
            var server = SimpleFileServer.createFileServer(
                    address,
                    absolutePath,
                    SimpleFileServer.OutputLevel.NONE
            );
            server.start();
            log.info("Resource Pack Hosting Started on: {}:{}/resourcepack.zip", ip, port);
        } catch (Exception e) {
            log.error("Self Host Pack Exception: ", e);
        }
    }

    private void preMakePack() throws IOException {
        File selfpack = new File(pluginFile, "selfpack");
        File resourcePack = new File(selfpack, "resourcepack.zip");
        if (resourcePack.exists()) {
            resourcePack.delete();
        }
        preMadePack();
    }

    private void preMadePack() throws IOException {
        Path modelsSource = pluginFolder.resolve("selfpack").resolve("models");
        Path itemsSource = pluginFolder.resolve("selfpack").resolve("models").resolve("items");
        Path texturesSource = pluginFolder.resolve("selfpack").resolve("models").resolve("textures");
        File outputZip = pluginFolder.resolve("selfpack").resolve("resourcepack.zip").toFile();

        // At least models and textures must exist
        if (!Files.exists(modelsSource) || !Files.exists(texturesSource) || !Files.exists(itemsSource)) {
            throw new FileNotFoundException("Source folders 'models', 'textures' or 'items' not found.");
        }

        try (FileOutputStream fos = new FileOutputStream(outputZip);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            addPackMcMeta(zos);

            // 1. 3D models
            addDirectoryToZip(zos, modelsSource, "assets/chamoitemskins/models/item/");

            // 2. Item models
            if (Files.exists(itemsSource)) {
                addDirectoryToZip(zos, itemsSource, "assets/chamoitemskins/items/");
            }

            // 3. Textures
            addDirectoryToZip(zos, texturesSource, "assets/chamoitemskins/textures/item/");
        }
    }


    private void sendResourcePack(Player player, String packUrl) {
        try {
            String hash = getZipHash();
            UUID packUUID = UUID.randomUUID();

            ResourcePackInfo packInfo = ResourcePackInfo.resourcePackInfo()
                    .id(packUUID)
                    .uri(URI.create(packUrl))
                    .hash(hash)
                    .build();

            ResourcePackRequest request = ResourcePackRequest.resourcePackRequest()
                    .packs(packInfo)
                    .required(true)
                    .replace(false)
                    .build();

            player.sendResourcePacks(request);
        } catch (Exception e) {
            log.error("Failed to send resource pack: ", e);
        }
    }

    private void addDirectoryToZip(ZipOutputStream zos, Path sourceDir, String targetPath) throws IOException {
        try (Stream<Path> walk = Files.walk(sourceDir)) {
            walk.filter(Files::isRegularFile)
                    .forEach(path -> {
                        String relativePath = sourceDir.relativize(path).toString();
                        // Replace OS-specific separators with '/'
                        String entryName = targetPath + relativePath.replace(File.separatorChar, '/');
                        try {
                            ZipEntry entry = new ZipEntry(entryName);
                            zos.putNextEntry(entry);
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }
    }

    private void addPackMcMeta(ZipOutputStream zos) throws IOException {
        String escapedDescription = plugin.getConfig().getString("selfpack.description", "Default pack")
                .replace("\\", "\\\\")   // Escape backslashes first
                .replace("\"", "\\\"")   // Escape quotes
                .replace("\n", "\\n")    // Escape newlines
                .replace("\r", "\\r");   // Escape carriage returns
        String json = String.format("""
                {
                  "pack": {
                    "pack_format": 42,
                    "supported_formats": [
                      42,
                      88
                    ],
                    "min_format": [
                      42,
                      0
                    ],
                    "max_format": [
                      88,
                      0
                    ],
                    "description": "%s"
                  }
                }
        """, escapedDescription);
        ZipEntry entry = new ZipEntry("pack.mcmeta");
        zos.putNextEntry(entry);
        zos.write(json.getBytes());
        zos.closeEntry();
    }

    private String getZipHash() throws Exception {
        Path zipPath = plugin.getDataFolder().toPath().resolve("selfpack").resolve("resourcepack.zip");
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] hashBytes = digest.digest(Files.readAllBytes(zipPath));
        return bytesToHex(hashBytes);  // returns hex string
    }

    private String bytesToHex(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }
}
