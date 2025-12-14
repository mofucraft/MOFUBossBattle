package net.mofucraft.bossbattle.battle;

import java.time.LocalDateTime;
import java.util.UUID;

public class BattleResult {

    public enum ResultType {
        VICTORY,
        DEFEAT,
        TIMEOUT,
        LOGOUT,
        BOSS_REMOVED
    }

    private final UUID playerId;
    private final String playerName;
    private final String bossId;
    private final ResultType resultType;
    private final long durationMillis;
    private final LocalDateTime timestamp;

    public BattleResult(UUID playerId, String playerName, String bossId, ResultType resultType, long durationMillis) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.bossId = bossId;
        this.resultType = resultType;
        this.durationMillis = durationMillis;
        this.timestamp = LocalDateTime.now();
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

    public ResultType getResultType() {
        return resultType;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public boolean isVictory() {
        return resultType == ResultType.VICTORY;
    }
}
