package net.mofucraft.bossbattle.database;

import net.mofucraft.bossbattle.MofuBossBattle;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class RankingRepository {

    private final DatabaseManager databaseManager;

    public RankingRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public CompletableFuture<Void> saveRecord(UUID playerId, String playerName, String bossId, long clearTimeMs) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO boss_rankings (player_uuid, player_name, boss_id, clear_time_ms) VALUES (?, ?, ?, ?)";

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                stmt.setString(2, playerName);
                stmt.setString(3, bossId);
                stmt.setLong(4, clearTimeMs);
                stmt.executeUpdate();
            } catch (SQLException e) {
                MofuBossBattle.getInstance().getLogger().log(Level.WARNING, "Failed to save ranking record", e);
            }
        });
    }

    public CompletableFuture<Void> saveBattleHistory(UUID playerId, String playerName, String bossId, String result, long durationMs) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO battle_history (player_uuid, player_name, boss_id, result, duration_ms) VALUES (?, ?, ?, ?, ?)";

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                stmt.setString(2, playerName);
                stmt.setString(3, bossId);
                stmt.setString(4, result);
                stmt.setLong(5, durationMs);
                stmt.executeUpdate();
            } catch (SQLException e) {
                MofuBossBattle.getInstance().getLogger().log(Level.WARNING, "Failed to save battle history", e);
            }
        });
    }

    public CompletableFuture<List<RankingEntry>> getTopRankings(String bossId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<RankingEntry> rankings = new ArrayList<>();

            String sql = """
                    SELECT player_uuid, player_name, clear_time_ms, recorded_at
                    FROM (
                        SELECT player_uuid, player_name, MIN(clear_time_ms) as clear_time_ms, MAX(recorded_at) as recorded_at
                        FROM boss_rankings
                        WHERE boss_id = ?
                        GROUP BY player_uuid, player_name
                    ) as best_times
                    ORDER BY clear_time_ms ASC
                    LIMIT ?
                    """;

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, bossId);
                stmt.setInt(2, limit);

                try (ResultSet rs = stmt.executeQuery()) {
                    int rank = 1;
                    while (rs.next()) {
                        UUID playerId = UUID.fromString(rs.getString("player_uuid"));
                        String playerName = rs.getString("player_name");
                        long clearTimeMs = rs.getLong("clear_time_ms");
                        Timestamp timestamp = rs.getTimestamp("recorded_at");
                        LocalDateTime recordedAt = timestamp != null ? timestamp.toLocalDateTime() : LocalDateTime.now();

                        rankings.add(new RankingEntry(rank++, playerId, playerName, bossId, clearTimeMs, recordedAt));
                    }
                }
            } catch (SQLException e) {
                MofuBossBattle.getInstance().getLogger().log(Level.WARNING, "Failed to get rankings", e);
            }

            return rankings;
        });
    }

    public CompletableFuture<Long> getPlayerBestTime(UUID playerId, String bossId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT MIN(clear_time_ms) as best_time FROM boss_rankings WHERE player_uuid = ? AND boss_id = ?";

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                stmt.setString(2, bossId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        long bestTime = rs.getLong("best_time");
                        if (!rs.wasNull()) {
                            return bestTime;
                        }
                    }
                }
            } catch (SQLException e) {
                MofuBossBattle.getInstance().getLogger().log(Level.WARNING, "Failed to get player best time", e);
            }

            return -1L;
        });
    }

    public CompletableFuture<Integer> getPlayerRank(UUID playerId, String bossId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                    SELECT COUNT(*) + 1 as player_rank
                    FROM (
                        SELECT player_uuid, MIN(clear_time_ms) as best_time
                        FROM boss_rankings
                        WHERE boss_id = ?
                        GROUP BY player_uuid
                    ) as bests
                    WHERE best_time < (
                        SELECT MIN(clear_time_ms)
                        FROM boss_rankings
                        WHERE player_uuid = ? AND boss_id = ?
                    )
                    """;

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, bossId);
                stmt.setString(2, playerId.toString());
                stmt.setString(3, bossId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("player_rank");
                    }
                }
            } catch (SQLException e) {
                MofuBossBattle.getInstance().getLogger().log(Level.WARNING, "Failed to get player rank", e);
            }

            return -1;
        });
    }

    public CompletableFuture<Integer> getTotalClears(String bossId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(DISTINCT player_uuid) as total FROM boss_rankings WHERE boss_id = ?";

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, bossId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("total");
                    }
                }
            } catch (SQLException e) {
                MofuBossBattle.getInstance().getLogger().log(Level.WARNING, "Failed to get total clears", e);
            }

            return 0;
        });
    }

    public CompletableFuture<Integer> resetPlayerRankings(UUID playerId, String bossId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM boss_rankings WHERE player_uuid = ? AND boss_id = ?";

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                stmt.setString(2, bossId);
                return stmt.executeUpdate();
            } catch (SQLException e) {
                MofuBossBattle.getInstance().getLogger().log(Level.WARNING, "Failed to reset player rankings", e);
            }

            return 0;
        });
    }

    public CompletableFuture<Integer> resetBossRankings(String bossId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM boss_rankings WHERE boss_id = ?";

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, bossId);
                return stmt.executeUpdate();
            } catch (SQLException e) {
                MofuBossBattle.getInstance().getLogger().log(Level.WARNING, "Failed to reset boss rankings", e);
            }

            return 0;
        });
    }

    public CompletableFuture<Integer> resetAllPlayerRankings(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM boss_rankings WHERE player_uuid = ?";

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                return stmt.executeUpdate();
            } catch (SQLException e) {
                MofuBossBattle.getInstance().getLogger().log(Level.WARNING, "Failed to reset all player rankings", e);
            }

            return 0;
        });
    }
}
