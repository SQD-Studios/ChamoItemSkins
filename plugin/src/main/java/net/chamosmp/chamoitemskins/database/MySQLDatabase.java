package net.chamosmp.chamoitemskins.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.chamosmp.chamoitemskins.api.objects.Skin;
import net.chamosmp.chamoitemskins.api.objects.SkinGrant;
import net.chamosmp.chamoitemskins.scheduler.SchedulerUtil;
import net.chamosmp.chamoitemskins.util.LoggerUtil;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class MySQLDatabase implements DatabaseManager {
    private final String host, port, database, username, password;
    private HikariDataSource dataSource;

    public MySQLDatabase(String host, String port, String database, String username, String password) {
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
                        expires_at  TIMESTAMP   NULL
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
            conn.createStatement().execute("""
                    CREATE TABLE IF NOT EXISTS player_favorite_skins (
                        player_uuid VARCHAR(36) NOT NULL,
                        skin_id     VARCHAR(64) NOT NULL
                    )""");
        } catch (SQLException e) {
            LoggerUtil.log(LoggerUtil.LogType.SEVERE, "Failed to initialize MySQL: " + e.getMessage());
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
                LoggerUtil.log(LoggerUtil.LogType.SEVERE, "Failed to grant skin: " + e.getMessage());
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
                LoggerUtil.log(LoggerUtil.LogType.SEVERE, "Failed to revoke skin: " + e.getMessage());
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
                LoggerUtil.log(LoggerUtil.LogType.SEVERE, "Failed to get grants: " + e.getMessage());
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
                LoggerUtil.log(LoggerUtil.LogType.SEVERE, "Failed to get active skin: " + e.getMessage());
            }
            return Optional.empty();
        }, SchedulerUtil.getVirtualThreadExecutor());
    }

    @Override
    public @NotNull CompletableFuture<Void> setActiveSkin(@NotNull UUID playerUuid, @NotNull Material material, @Nullable String skinId) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                if (skinId == null) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "DELETE FROM player_active_skins WHERE player_uuid = ? AND item_type = ?")) {
                        ps.setString(1, playerUuid.toString());
                        ps.setString(2, material.name());
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO player_active_skins (player_uuid, item_type, skin_id) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE skin_id = ?")) {
                        ps.setString(1, playerUuid.toString());
                        ps.setString(2, material.name());
                        ps.setString(3, skinId);
                        ps.setString(4, skinId);
                        ps.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                LoggerUtil.log(LoggerUtil.LogType.SEVERE, "Failed to set active skin: " + e.getMessage());
            }
        }, SchedulerUtil.getVirtualThreadExecutor());
    }

    @Override
    public @NotNull CompletableFuture<Void> logAction(@NotNull UUID playerUuid, @NotNull String action, @NotNull String target, @Nullable String metadata) {
        return CompletableFuture.runAsync(() -> {
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
                LoggerUtil.log(LoggerUtil.LogType.SEVERE, "Failed to log action: " + e.getMessage());
            }
        }, SchedulerUtil.getVirtualThreadExecutor());
    }

    @Override
    public @NotNull CompletableFuture<Void> migrateSkinId(String oldSkinId, @NotNull String newSkinId) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);
                try (PreparedStatement ps1 = conn.prepareStatement(
                        "UPDATE player_skin_grants SET skin_id = ? WHERE skin_id = ?");
                     PreparedStatement ps2 = conn.prepareStatement(
                             "UPDATE player_active_skins SET skin_id = ? WHERE skin_id = ?")) {
                    ps1.setString(1, newSkinId);
                    ps1.setString(2, oldSkinId);
                    ps1.executeUpdate();

                    ps2.setString(1, newSkinId);
                    ps2.setString(2, oldSkinId);
                    ps2.executeUpdate();

                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                    throw new RuntimeException("Failed to migrate skin IDs", e);
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to obtain connection or rollback", e);
            }
        }, SchedulerUtil.getVirtualThreadExecutor());
    }

    @Override
    public @NotNull CompletableFuture<Void> grantSkinWithExpiry(
            @NotNull UUID playerUuid,
            @NotNull String skinId,
            @NotNull String source,
            @Nullable LocalDateTime expiresAt) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("""
                             INSERT INTO player_skin_grants (grant_id, player_uuid, skin_id, source, expires_at)
                             VALUES (?, ?, ?, ?, ?)
                         """)) {
                ps.setString(1, UUID.randomUUID().toString());
                ps.setString(2, playerUuid.toString());
                ps.setString(3, skinId);
                ps.setString(4, source);
                ps.setTimestamp(5, expiresAt != null ? Timestamp.valueOf(expiresAt) : null);
                ps.executeUpdate();
            } catch (SQLException e) {
                LoggerUtil.log(LoggerUtil.LogType.SEVERE, "Failed to grant skin with expiry: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, SchedulerUtil.getVirtualThreadExecutor());
    }

    @Override
    public @NotNull CompletableFuture<Collection<ExpiredGrant>> getExpiredGrants() {
        return CompletableFuture.supplyAsync(() -> {
            Collection<ExpiredGrant> expired = new ArrayList<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("""
                             SELECT player_uuid, skin_id
                             FROM player_skin_grants
                             WHERE expires_at IS NOT NULL AND expires_at <= NOW()
                         """)) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        expired.add(new ExpiredGrant(
                                UUID.fromString(rs.getString("player_uuid")),
                                rs.getString("skin_id")
                        ));
                    }
                }
            } catch (SQLException e) {
                LoggerUtil.log(LoggerUtil.LogType.SEVERE, "Failed to get expired grants: " + e.getMessage());
            }
            return expired;
        }, SchedulerUtil.getVirtualThreadExecutor());
    }

    @Override
    public @NotNull CompletableFuture<Void> upsertActiveSkin(@NotNull UUID playerUuid, @NotNull Material material, @Nullable String skinId) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                if (skinId == null) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "DELETE FROM player_active_skins WHERE player_uuid = ? AND item_type = ?")) {
                        ps.setString(1, playerUuid.toString());
                        ps.setString(2, material.name());
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO player_active_skins (player_uuid, item_type, skin_id) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE skin_id = ?")) {
                        ps.setString(1, playerUuid.toString());
                        ps.setString(2, material.name());
                        ps.setString(3, skinId);
                        ps.setString(4, skinId);
                        ps.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                LoggerUtil.log(LoggerUtil.LogType.SEVERE, "Failed to upsert active skin: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, SchedulerUtil.getVirtualThreadExecutor());
    }

    @Override
    public @NotNull CompletableFuture<Map<Material, String>> getAllActiveSkins(@NotNull UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            Map<Material, String> activeSkins = new HashMap<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT item_type, skin_id FROM player_active_skins WHERE player_uuid = ?")) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String itemTypeStr = rs.getString("item_type");
                        Material material = Material.getMaterial(itemTypeStr);
                        if (material != null) {
                            activeSkins.put(material, rs.getString("skin_id"));
                        }
                    }
                }
            } catch (SQLException e) {
                LoggerUtil.log(LoggerUtil.LogType.SEVERE, "Failed to get all active skins: " + e.getMessage());
            }
            return activeSkins;
        }, SchedulerUtil.getVirtualThreadExecutor());
    }

    @Override
    public @NotNull CompletableFuture<Void> addFavoriteSkinToPlayer(@NotNull UUID playerUuid, @NotNull Skin skin) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO player_favorite_skins (player_uuid, skin_id) VALUES (?, ?)"
                 );
            ) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, skin.id());
                ps.executeUpdate();
            } catch (SQLException e) {
                LoggerUtil.log(LoggerUtil.LogType.SEVERE, "Failed to add favorite skin: " + e.getMessage());
            }
        }, SchedulerUtil.getVirtualThreadExecutor());
    }

    @Override
    public @NotNull CompletableFuture<Void> removeFavoriteSkinFromPlayer(@NotNull UUID playerUuid, @NotNull Skin skin) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM player_favorite_skins WHERE player_uuid = ? AND skin_id = ?")) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, skin.id());
                ps.executeUpdate();
            } catch (SQLException e) {
                LoggerUtil.log(LoggerUtil.LogType.SEVERE, "Failed to remove favorite skin: " + e.getMessage());
            }
        }, SchedulerUtil.getVirtualThreadExecutor());
    }

    @Override
    public @NotNull CompletableFuture<Collection<String>> getFavoriteSkinsFromPlayer(@NotNull UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT skin_id FROM player_favorite_skins WHERE player_uuid = ?")) {
                Collection<String> skins = new ArrayList<>();

                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        skins.add(rs.getString("skin_id"));
                    }
                }
                return skins;
            } catch (SQLException e) {
                LoggerUtil.log(LoggerUtil.LogType.SEVERE, "Failed to get favorite skins: " + e.getMessage());
                return Collections.emptyList();
            }
        }, SchedulerUtil.getVirtualThreadExecutor());
    }
}
