package net.mofucraft.bossbattle.database;

import java.time.LocalDateTime;
import java.util.UUID;

public class RankingEntry {

    private final int rank;
    private final UUID playerId;
    private final String playerName;
    private final String bossId;
    private final long clearTimeMillis;
    private final LocalDateTime recordedAt;

    public RankingEntry(int rank, UUID playerId, String playerName, String bossId, long clearTimeMillis, LocalDateTime recordedAt) {
        this.rank = rank;
        this.playerId = playerId;
        this.playerName = playerName;
        this.bossId = bossId;
        this.clearTimeMillis = clearTimeMillis;
        this.recordedAt = recordedAt;
    }

    public int getRank() {
        return rank;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getBossId() {
        return bossId;
    }

    public long getClearTimeMillis() {
        return clearTimeMillis;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }
}
