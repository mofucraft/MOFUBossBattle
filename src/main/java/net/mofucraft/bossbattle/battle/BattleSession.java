package net.mofucraft.bossbattle.battle;

import net.mofucraft.bossbattle.config.BossConfig;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public class BattleSession {

    private final UUID playerId;
    private final String playerName;
    private final String bossId;
    private final BossConfig bossConfig;

    private BattleState state;
    private long startTime;
    private long endTime;

    private BukkitTask timerTask;
    private BukkitTask itemCollectionTask;

    // MythicMobs reference
    private UUID activeMobUuid;

    public BattleSession(UUID playerId, String playerName, String bossId, BossConfig bossConfig) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.bossId = bossId;
        this.bossConfig = bossConfig;
        this.state = BattleState.WAITING;
    }

    public void start() {
        this.startTime = System.currentTimeMillis();
        this.state = BattleState.IN_PROGRESS;
    }

    public void end(BattleState endState) {
        this.endTime = System.currentTimeMillis();
        this.state = endState;
        cancelTasks();
    }

    public void cancelTasks() {
        if (timerTask != null && !timerTask.isCancelled()) {
            timerTask.cancel();
            timerTask = null;
        }
        if (itemCollectionTask != null && !itemCollectionTask.isCancelled()) {
            itemCollectionTask.cancel();
            itemCollectionTask = null;
        }
    }

    public long getElapsedTime() {
        if (startTime == 0) {
            return 0;
        }
        if (endTime > 0) {
            return endTime - startTime;
        }
        return System.currentTimeMillis() - startTime;
    }

    public int getRemainingSeconds() {
        if (startTime == 0) {
            return bossConfig.getTimeLimit();
        }
        long elapsed = getElapsedTime() / 1000;
        int remaining = bossConfig.getTimeLimit() - (int) elapsed;
        return Math.max(0, remaining);
    }

    public boolean isActive() {
        return state == BattleState.IN_PROGRESS || state == BattleState.ITEM_COLLECTION;
    }

    public boolean isInBattle() {
        return state == BattleState.IN_PROGRESS;
    }

    // Getters and Setters
    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getBossId() {
        return bossId;
    }

    public BossConfig getBossConfig() {
        return bossConfig;
    }

    public BattleState getState() {
        return state;
    }

    public void setState(BattleState state) {
        this.state = state;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public BukkitTask getTimerTask() {
        return timerTask;
    }

    public void setTimerTask(BukkitTask timerTask) {
        this.timerTask = timerTask;
    }

    public BukkitTask getItemCollectionTask() {
        return itemCollectionTask;
    }

    public void setItemCollectionTask(BukkitTask itemCollectionTask) {
        this.itemCollectionTask = itemCollectionTask;
    }

    public UUID getActiveMobUuid() {
        return activeMobUuid;
    }

    public void setActiveMobUuid(UUID activeMobUuid) {
        this.activeMobUuid = activeMobUuid;
    }
}
