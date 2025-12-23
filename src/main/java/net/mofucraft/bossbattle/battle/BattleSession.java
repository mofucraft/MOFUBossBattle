package net.mofucraft.bossbattle.battle;

import net.mofucraft.bossbattle.config.BossConfig;
import org.bukkit.boss.BossBar;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BattleSession {

    private final UUID playerId;
    private final String playerName;
    private final String initialBossId; // First boss ID for chain battle tracking
    private String bossId;
    private BossConfig bossConfig;

    private BattleState state;
    private long startTime;
    private long endTime;

    private BukkitTask timerTask;
    private BukkitTask itemCollectionTask;
    private BukkitTask soundLoopTask;

    // MythicMobs reference
    private UUID activeMobUuid;

    // Boss bar reference
    private BossBar bossBar;

    // Chain battle tracking
    private boolean isChainBattle;
    private List<String> remainingBosses;
    private int currentBossIndex;
    private int totalBossCount;

    public BattleSession(UUID playerId, String playerName, String bossId, BossConfig bossConfig) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.initialBossId = bossId;
        this.bossId = bossId;
        this.bossConfig = bossConfig;
        this.state = BattleState.WAITING;
        this.isChainBattle = false;
        this.remainingBosses = new ArrayList<>();
        this.currentBossIndex = 0;
        this.totalBossCount = 1;
    }

    public String getInitialBossId() {
        return initialBossId;
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
        if (soundLoopTask != null && !soundLoopTask.isCancelled()) {
            soundLoopTask.cancel();
            soundLoopTask = null;
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

    public BukkitTask getSoundLoopTask() {
        return soundLoopTask;
    }

    public void setSoundLoopTask(BukkitTask soundLoopTask) {
        this.soundLoopTask = soundLoopTask;
    }

    public UUID getActiveMobUuid() {
        return activeMobUuid;
    }

    public void setActiveMobUuid(UUID activeMobUuid) {
        this.activeMobUuid = activeMobUuid;
    }

    public BossBar getBossBar() {
        return bossBar;
    }

    public void setBossBar(BossBar bossBar) {
        this.bossBar = bossBar;
    }

    public boolean isChainBattle() {
        return isChainBattle;
    }

    public void setChainBattle(boolean chainBattle) {
        isChainBattle = chainBattle;
    }

    public List<String> getRemainingBosses() {
        return remainingBosses;
    }

    public void setRemainingBosses(List<String> remainingBosses) {
        this.remainingBosses = remainingBosses;
    }

    public int getCurrentBossIndex() {
        return currentBossIndex;
    }

    public void setCurrentBossIndex(int currentBossIndex) {
        this.currentBossIndex = currentBossIndex;
    }

    public int getTotalBossCount() {
        return totalBossCount;
    }

    public void setTotalBossCount(int totalBossCount) {
        this.totalBossCount = totalBossCount;
    }

    public void setBossId(String bossId) {
        this.bossId = bossId;
    }

    public void setBossConfig(BossConfig bossConfig) {
        this.bossConfig = bossConfig;
    }

    public void resetStartTime() {
        this.startTime = System.currentTimeMillis();
    }

    public boolean hasNextBoss() {
        return isChainBattle && currentBossIndex < remainingBosses.size();
    }

    public String getNextBossId() {
        if (hasNextBoss()) {
            return remainingBosses.get(currentBossIndex);
        }
        return null;
    }

    public void advanceToNextBoss() {
        currentBossIndex++;
    }
}
