// --- plugin/src/main/java/net/chamosmp/chamoitemskins/database/MySQLDatabase.java ---
package net.chamosmp.chamoitemskins.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.chamosmp.chamoitemskins.api.model.SkinGrant;
import net.chamosmp.chamoitemskins.scheduler.SchedulerUtil;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class MySQLDatabase implements DatabaseManager {
    private final Plugin plugin;
    private final String host, port, database, username, password;
    private HikariDataSource dataSource;

    public MySQLDatabase(Plugin plugin, String host, String port, String database, String username, String password) {
        this.plugin = plugin;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    @Override
    public void init() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
        config.setUsername(username);
        config.setPassword(password);
        config.addDataSourceProperty("useUnicode", "true");
        config.addDataSourceProperty("characterEncoding", "utf8");
        config.setMaximumPoolSize(10);
        config.setPoolName("ChamoItemSkins-MySQL");

        this.dataSource = new HikariDataSource(config);

        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS player_skin_grants (
                    grant_id    VARCHAR(36) PRIMARY KEY,
                    player_uuid VARCHAR(36) NOT NULL,
                    skin_id     VARCHAR(64) NOT NULL,
                    granted_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    source      VARCHAR(32) NOT NULL,
                    INDEX (player_uuid)
                )""");
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS player_active_skins (
                    player_uuid VARCHAR(36) NOT NULL,
                    item_type   VARCHAR(64) NOT NULL,
                    skin_id     VARCHAR(64) NOT NULL,
                    PRIMARY KEY (player_uuid, item_type)
                )""");
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS player_skin_logs (
                    log_id      VARCHAR(36) PRIMARY KEY,
                    player_uuid VARCHAR(36) NOT NULL,
                    action      VARCHAR(32) NOT NULL,
                    target      VARCHAR(64) NOT NULL,
                    metadata    TEXT,
                    timestamp   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    INDEX (player_uuid)
                )""");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize MySQL: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        if (dataSource != null) dataSource.close();
    }

    @Override
    public @NotNull CompletableFuture<Void> grantSkin(@NotNull UUID playerUuid, @NotNull String skinId, @NotNull String source) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("""
                     INSERT INTO player_skin_grants (grant_id, player_uuid, skin_id, source)
                     VALUES (?, ?, ?, ?)
                 """)) {
                ps.setString(1, UUID.randomUUID().toString());
                ps.setString(2, playerUuid.toString());
                ps.setString(3, skinId);
                ps.setString(4, source);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to grant skin: " + e.getMessage());
            }
        }, SchedulerUtil.getVirtualThreadExecutor());
    }

    @Override
    public @NotNull CompletableFuture<Void> revokeSkin(@NotNull UUID playerUuid, @NotNull String skinId) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("""
                     DELETE FROM player_skin_grants WHERE player_uuid = ? AND skin_id = ?
                 """)) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, skinId);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to revoke skin: " + e.getMessage());
            }
        }, SchedulerUtil.getVirtualThreadExecutor());
    }

    @Override
    public @NotNull CompletableFuture<Collection<SkinGrant>> getGrants(@NotNull UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            Collection<SkinGrant> grants = new ArrayList<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM player_skin_grants WHERE player_uuid = ?")) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        grants.add(new SkinGrant(
                            UUID.fromString(rs.getString("grant_id")),
                            UUID.fromString(rs.getString("player_uuid")),
                            rs.getString("skin_id"),
                            rs.getTimestamp("granted_at").toInstant(),
                            rs.getString("source")
                        ));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get grants: " + e.getMessage());
            }
            return grants;
        }, SchedulerUtil.getVirtualThreadExecutor());
    }

    @Override
    public @NotNull CompletableFuture<Optional<String>> getActiveSkin(@NotNull UUID playerUuid, @NotNull Material material) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT skin_id FROM player_active_skins WHERE player_uuid = ? AND item_type = ?")) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, material.name());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return Optional.of(rs.getString("skin_id"));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get active skin: " + e.getMessage());
            }
            return Optional.empty();
        }, SchedulerUtil.getVirtualThreadExecutor());
    }

    @Override
    public @NotNull CompletableFuture<Void> setActiveSkin(@NotNull UUID playerUuid, @NotNull Material material, @Nullable String skinId) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                if (skinId == null) {
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM player_active_skins WHERE player_uuid = ? AND item_type = ?")) {
                        ps.setString(1, playerUuid.toString());
                        ps.setString(2, material.name());
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = conn.prepareStatement("""
                        INSERT INTO player_active_skins (player_uuid, item_type, skin_id)
                        VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE skin_id = ?
                    """)) {
                        ps.setString(1, playerUuid.toString());
                        ps.setString(2, material.name());
                        ps.setString(3, skinId);
                        ps.setString(4, skinId);
                        ps.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to set active skin: " + e.getMessage());
            }
        }, SchedulerUtil.getVirtualThreadExecutor());
    }

    @Override
    public void logAction(@NotNull UUID playerUuid, @NotNull String action, @NotNull String target, @Nullable String metadata) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                 INSERT INTO player_skin_logs (log_id, player_uuid, action, target, metadata)
                 VALUES (?, ?, ?, ?, ?)
             """)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, playerUuid.toString());
            ps.setString(3, action);
            ps.setString(4, target);
            ps.setString(5, metadata);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to log action: " + e.getMessage());
        }
    }
}
