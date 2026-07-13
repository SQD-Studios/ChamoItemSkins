// --- plugin/src/main/java/net/chamosmp/chamoitemskins/database/SQLiteDatabase.java ---
package net.chamosmp.chamoitemskins.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.chamosmp.chamoitemskins.api.model.SkinGrant;
import net.chamosmp.chamoitemskins.scheduler.SchedulerUtil;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class SQLiteDatabase implements DatabaseManager {
    private final Plugin plugin;
    private final File dbFile;
    private HikariDataSource dataSource;

    public SQLiteDatabase(Plugin plugin, File dbFile) {
        this.plugin = plugin;
        this.dbFile = dbFile;
    }

    @Override
    public void init() {
        if (!dbFile.getParentFile().exists()) dbFile.getParentFile().mkdirs();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setPoolName("ChamoItemSkins-SQLite");
        config.setMaximumPoolSize(1); // SQLite only supports one writer at a time

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
                CREATE INDEX IF NOT EXISTS idx_player_uuid ON player_skin_grants(player_uuid)
            """);
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
                    timestamp   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
                )""");
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE player_skin_grants ADD COLUMN expires_at TIMESTAMP");
            } catch (SQLException ignored) {
                // Column already exists
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize SQLite: " + e.getMessage());
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
                    try (PreparedStatement ps = conn.prepareStatement(
                            "DELETE FROM player_active_skins WHERE player_uuid = ? AND item_type = ?")) {
                        ps.setString(1, playerUuid.toString());
                        ps.setString(2, material.name());
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT OR REPLACE INTO player_active_skins (player_uuid, item_type, skin_id) VALUES (?, ?, ?)")) {
                        ps.setString(1, playerUuid.toString());
                        ps.setString(2, material.name());
                        ps.setString(3, skinId);
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
                plugin.getLogger().severe("Failed to grant skin with expiry: " + e.getMessage());
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
                     WHERE expires_at IS NOT NULL AND expires_at <= CURRENT_TIMESTAMP
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
                plugin.getLogger().severe("Failed to get expired grants: " + e.getMessage());
            }
            return expired;
        }, SchedulerUtil.getVirtualThreadExecutor());
    }
}

