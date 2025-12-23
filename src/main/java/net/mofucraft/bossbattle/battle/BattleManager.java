package net.mofucraft.bossbattle.battle;

import net.mofucraft.bossbattle.MofuBossBattle;
import net.mofucraft.bossbattle.config.BossConfig;
import net.mofucraft.bossbattle.config.MessageConfig;
import net.mofucraft.bossbattle.task.BattleTimerTask;
import net.mofucraft.bossbattle.task.ItemCollectionTask;
import net.mofucraft.bossbattle.util.MessageUtil;
import net.mofucraft.bossbattle.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BattleManager {

    private final MofuBossBattle plugin;
    private final Map<UUID, BattleSession> activeBattles;
    private final Set<String> activeBosses; // Track which bosses are currently in use

    public BattleManager(MofuBossBattle plugin) {
        this.plugin = plugin;
        this.activeBattles = new HashMap<>();
        this.activeBosses = new HashSet<>();
    }

    public boolean startBattle(Player player, String bossId) {
        if (isInBattle(player.getUniqueId())) {
            return false;
        }

        // Check if another player is already fighting this boss
        if (isBossInUse(bossId)) {
            return false;
        }

        BossConfig bossConfig = plugin.getConfigManager().getBossConfig(bossId);
        if (bossConfig == null) {
            return false;
        }

        // Mark boss as in use
        activeBosses.add(bossId);

        // Create session
        BattleSession session = new BattleSession(
                player.getUniqueId(),
                player.getName(),
                bossId,
                bossConfig
        );

        // Setup chain battle if enabled
        if (bossConfig.isChainBattleEnabled()) {
            List<String> chainList = bossConfig.getChainBossList();
            if (chainList != null && !chainList.isEmpty()) {
                session.setChainBattle(true);
                session.setRemainingBosses(new ArrayList<>(chainList));
                session.setTotalBossCount(chainList.size() + 1); // Include current boss
                session.setCurrentBossIndex(0);
                // Mark all chain bosses as in use
                for (String chainBossId : chainList) {
                    activeBosses.add(chainBossId);
                }
            }
        }

        activeBattles.put(player.getUniqueId(), session);

        // Teleport player
        Location teleportLoc = bossConfig.getTeleportLocation();
        if (teleportLoc != null) {
            player.teleport(teleportLoc);

            // Apply blindness effect if configured
            int blindnessDuration = bossConfig.getTeleportBlindnessDuration();
            if (blindnessDuration > 0) {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.BLINDNESS,
                        blindnessDuration * 20, // Convert seconds to ticks
                        0,
                        false,
                        false
                ));
            }
        }

        // Start battle (timer starts now)
        session.start();

        // Create boss bar if enabled
        if (bossConfig.isShowTimeBossBar()) {
            createBossBar(player, session);
        }

        // Spawn boss via MythicMobs with delay for chunk loading
        int spawnDelay = bossConfig.getBossSpawnDelay();
        if (plugin.getMythicMobsHook() != null) {
            Location bossSpawnLoc = bossConfig.getBossSpawnLocation();
            if (bossSpawnLoc != null) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    // Check if session is still active
                    if (!session.isActive()) {
                        return;
                    }
                    UUID mobUuid = plugin.getMythicMobsHook().spawnBoss(
                            bossConfig.getMythicMobId(),
                            bossSpawnLoc,
                            bossConfig.getBossLevel()
                    );
                    session.setActiveMobUuid(mobUuid);
                }, spawnDelay);
            }
        }

        // Send start message
        MessageConfig messages = plugin.getConfigManager().getMessageConfig();
        String startMsg = bossConfig.getStartMessage();
        if (startMsg == null || startMsg.isEmpty()) {
            startMsg = messages.getBattleStart();
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("boss_name", bossConfig.getDisplayName());
        placeholders.put("player", player.getName());
        placeholders.put("time_limit", TimeUtil.formatSecondsReadable(bossConfig.getTimeLimit()));
        if (session.isChainBattle()) {
            placeholders.put("current_boss", String.valueOf(1));
            placeholders.put("total_bosses", String.valueOf(session.getTotalBossCount()));
        }

        MessageUtil.sendMessage(player, messages.withPrefix(startMsg), placeholders);

        // Play battle start sound
        playBattleStartSound(player, bossConfig);

        // Start sound loop task if configured
        if (bossConfig.getBattleLoopSound() != null && bossConfig.getBattleLoopSoundInterval() > 0) {
            BukkitTask soundTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (session.isInBattle()) {
                    playBattleLoopSound(session);
                }
            }, bossConfig.getBattleLoopSoundInterval(), bossConfig.getBattleLoopSoundInterval());
            session.setSoundLoopTask(soundTask);
        }

        // Start timer task
        BattleTimerTask timerTask = new BattleTimerTask(plugin, session);
        session.setTimerTask(timerTask.runTaskTimer(plugin, 20L, 20L));

        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("Battle started: " + player.getName() + " vs " + bossId);
        }

        return true;
    }

    private void createBossBar(Player player, BattleSession session) {
        BossConfig bossConfig = session.getBossConfig();
        String modeText = bossConfig.isSurvivalMode() ? "&a耐久" : "&c制限";
        String title = formatBossBarTitle(bossConfig, modeText, TimeUtil.formatSecondsReadable(bossConfig.getTimeLimit()));
        BossBar bossBar = Bukkit.createBossBar(title, bossConfig.getBossBarColor(), bossConfig.getBossBarStyle());
        bossBar.setProgress(1.0);
        bossBar.addPlayer(player);
        session.setBossBar(bossBar);
    }

    private String formatBossBarTitle(BossConfig bossConfig, String modeText, String timeText) {
        String format = bossConfig.getBossBarTitleFormat();
        String title = format
                .replace("{boss_name}", MessageUtil.stripColors(bossConfig.getDisplayName()))
                .replace("{mode}", modeText)
                .replace("{time}", timeText);
        return MessageUtil.colorize(title);
    }

    public void updateBossBar(BattleSession session) {
        BossBar bossBar = session.getBossBar();
        if (bossBar == null) return;

        BossConfig bossConfig = session.getBossConfig();
        int remaining = session.getRemainingSeconds();
        int total = bossConfig.getTimeLimit();

        double progress = Math.max(0, Math.min(1, (double) remaining / total));
        bossBar.setProgress(progress);

        String modeText = bossConfig.isSurvivalMode() ? "&a耐久" : "&c制限";
        String title = formatBossBarTitle(bossConfig, modeText, TimeUtil.formatSecondsReadable(remaining));
        bossBar.setTitle(title);

        // Change color based on remaining time (dynamic color change)
        if (remaining <= 30) {
            bossBar.setColor(BarColor.RED);
        } else if (remaining <= 60) {
            bossBar.setColor(BarColor.YELLOW);
        } else {
            bossBar.setColor(bossConfig.getBossBarColor());
        }
    }

    private void removeBossBar(BattleSession session) {
        BossBar bossBar = session.getBossBar();
        if (bossBar != null) {
            bossBar.removeAll();
            session.setBossBar(null);
        }
    }

    public void onBossDefeated(UUID playerId) {
        BattleSession session = activeBattles.get(playerId);
        if (session == null || !session.isInBattle()) {
            return;
        }

        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return;
        }

        BossConfig bossConfig = session.getBossConfig();
        MessageConfig messages = plugin.getConfigManager().getMessageConfig();

        // Calculate clear time
        long clearTime = session.getElapsedTime();

        // Save ranking record
        plugin.getRankingRepository().saveRecord(
                playerId,
                player.getName(),
                session.getBossId(),
                clearTime
        );

        // Save battle history
        plugin.getRankingRepository().saveBattleHistory(
                playerId,
                player.getName(),
                session.getBossId(),
                "VICTORY",
                clearTime
        );

        // Execute victory commands
        executeCommands(player, bossConfig.getVictoryCommands(), bossConfig, clearTime);

        // Send victory message
        String victoryMsg = bossConfig.getVictoryMessage();
        if (victoryMsg == null || victoryMsg.isEmpty()) {
            victoryMsg = messages.getBattleVictory();
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("boss_name", bossConfig.getDisplayName());
        placeholders.put("player", player.getName());
        placeholders.put("time", TimeUtil.formatTime(clearTime));
        placeholders.put("time_ms", String.valueOf(clearTime));

        MessageUtil.sendMessage(player, messages.withPrefix(victoryMsg), placeholders);

        // Play victory sound
        playVictorySound(player, bossConfig);

        // Send victory broadcast
        String victoryBroadcast = bossConfig.getVictoryBroadcast();
        if (victoryBroadcast != null && !victoryBroadcast.isEmpty()) {
            broadcastMessage(victoryBroadcast, placeholders);
        }

        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("Boss defeated: " + player.getName() + " cleared " + session.getBossId() + " in " + TimeUtil.formatTime(clearTime));
        }

        // Check for chain battle - spawn next boss
        if (session.hasNextBoss()) {
            startNextChainBoss(player, session);
        } else {
            // No more bosses - proceed to item collection or end
            finishBattleWithItemCollection(player, session);
        }
    }

    /**
     * Called when survival mode time expires - player wins by surviving
     */
    public void onSurvivalTimeComplete(UUID playerId) {
        BattleSession session = activeBattles.get(playerId);
        if (session == null || !session.isInBattle()) {
            return;
        }

        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return;
        }

        BossConfig bossConfig = session.getBossConfig();
        MessageConfig messages = plugin.getConfigManager().getMessageConfig();

        // Remove the boss (player survived)
        if (plugin.getMythicMobsHook() != null && session.getActiveMobUuid() != null) {
            plugin.getMythicMobsHook().removeMob(session.getActiveMobUuid());
        }

        long survivalTime = session.getElapsedTime();

        // Save ranking record for survival
        plugin.getRankingRepository().saveRecord(
                playerId,
                player.getName(),
                session.getBossId(),
                survivalTime
        );

        // Save battle history
        plugin.getRankingRepository().saveBattleHistory(
                playerId,
                player.getName(),
                session.getBossId(),
                "SURVIVAL",
                survivalTime
        );

        // Execute victory commands
        executeCommands(player, bossConfig.getVictoryCommands(), bossConfig, survivalTime);

        // Send survival victory message
        String survivalMsg = messages.getBattleSurvival();
        if (survivalMsg == null || survivalMsg.isEmpty()) {
            survivalMsg = bossConfig.getVictoryMessage();
            if (survivalMsg == null || survivalMsg.isEmpty()) {
                survivalMsg = messages.getBattleVictory();
            }
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("boss_name", bossConfig.getDisplayName());
        placeholders.put("player", player.getName());
        placeholders.put("time", TimeUtil.formatTime(survivalTime));
        placeholders.put("time_ms", String.valueOf(survivalTime));

        MessageUtil.sendMessage(player, messages.withPrefix(survivalMsg), placeholders);

        // Play victory sound
        playVictorySound(player, bossConfig);

        // Send victory broadcast
        String victoryBroadcast = bossConfig.getVictoryBroadcast();
        if (victoryBroadcast != null && !victoryBroadcast.isEmpty()) {
            broadcastMessage(victoryBroadcast, placeholders);
        }

        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("Survival complete: " + player.getName() + " survived " + session.getBossId() + " for " + TimeUtil.formatTime(survivalTime));
        }

        // Check for chain battle - spawn next boss
        if (session.hasNextBoss()) {
            startNextChainBoss(player, session);
        } else {
            // No more bosses - proceed to item collection or end
            finishBattleWithItemCollection(player, session);
        }
    }

    private void startNextChainBoss(Player player, BattleSession session) {
        String nextBossId = session.getNextBossId();
        session.advanceToNextBoss();

        BossConfig nextBossConfig = plugin.getConfigManager().getBossConfig(nextBossId);
        if (nextBossConfig == null) {
            plugin.getLogger().warning("Chain battle next boss not found: " + nextBossId);
            finishBattleWithItemCollection(player, session);
            return;
        }

        // Update session with new boss
        session.setBossId(nextBossId);
        session.setBossConfig(nextBossConfig);
        session.resetStartTime();
        session.setActiveMobUuid(null);

        // Cancel old timer
        session.cancelTasks();

        // Update boss bar
        if (nextBossConfig.isShowTimeBossBar()) {
            if (session.getBossBar() == null) {
                createBossBar(player, session);
            }
        } else {
            removeBossBar(session);
        }

        // Spawn next boss
        int spawnDelay = nextBossConfig.getBossSpawnDelay();
        if (plugin.getMythicMobsHook() != null) {
            Location bossSpawnLoc = nextBossConfig.getBossSpawnLocation();
            if (bossSpawnLoc == null) {
                // Use current boss spawn location as fallback
                bossSpawnLoc = session.getBossConfig().getBossSpawnLocation();
            }
            if (bossSpawnLoc != null) {
                final Location finalSpawnLoc = bossSpawnLoc;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!session.isActive()) {
                        return;
                    }
                    UUID mobUuid = plugin.getMythicMobsHook().spawnBoss(
                            nextBossConfig.getMythicMobId(),
                            finalSpawnLoc,
                            nextBossConfig.getBossLevel()
                    );
                    session.setActiveMobUuid(mobUuid);
                }, spawnDelay);
            }
        }

        // Send next boss start message
        MessageConfig messages = plugin.getConfigManager().getMessageConfig();
        String startMsg = nextBossConfig.getStartMessage();
        if (startMsg == null || startMsg.isEmpty()) {
            startMsg = messages.getChainBattleNextBoss();
            if (startMsg == null || startMsg.isEmpty()) {
                startMsg = messages.getBattleStart();
            }
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("boss_name", nextBossConfig.getDisplayName());
        placeholders.put("player", player.getName());
        placeholders.put("time_limit", TimeUtil.formatSecondsReadable(nextBossConfig.getTimeLimit()));
        placeholders.put("current_boss", String.valueOf(session.getCurrentBossIndex()));
        placeholders.put("total_bosses", String.valueOf(session.getTotalBossCount()));

        MessageUtil.sendMessage(player, messages.withPrefix(startMsg), placeholders);

        // Play battle start sound for next boss
        playBattleStartSound(player, nextBossConfig);

        // Start sound loop task if configured for new boss
        if (nextBossConfig.getBattleLoopSound() != null && nextBossConfig.getBattleLoopSoundInterval() > 0) {
            BukkitTask soundTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (session.isInBattle()) {
                    playBattleLoopSound(session);
                }
            }, nextBossConfig.getBattleLoopSoundInterval(), nextBossConfig.getBattleLoopSoundInterval());
            session.setSoundLoopTask(soundTask);
        }

        // Start new timer task
        BattleTimerTask timerTask = new BattleTimerTask(plugin, session);
        session.setTimerTask(timerTask.runTaskTimer(plugin, 20L, 20L));

        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("Chain battle next boss: " + player.getName() + " vs " + nextBossId);
        }
    }

    private void finishBattleWithItemCollection(Player player, BattleSession session) {
        BossConfig bossConfig = session.getBossConfig();
        MessageConfig messages = plugin.getConfigManager().getMessageConfig();

        // Stop timer
        session.cancelTasks();
        session.setState(BattleState.ITEM_COLLECTION);

        // Start item collection phase
        int collectionTime = bossConfig.getItemCollectionTime();
        if (collectionTime > 0) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("time", String.valueOf(collectionTime));
            placeholders.put("boss_name", bossConfig.getDisplayName());
            MessageUtil.sendMessage(player, messages.withPrefix(messages.getItemCollectionStart()), placeholders);

            // Send leave command hint if enabled
            if (plugin.getConfigManager().isShowLeaveCommandInChat()) {
                MessageUtil.sendClickableCommand(player,
                        messages.getItemCollectionLeaveHint(),
                        "/boss leave",
                        messages.getItemCollectionLeaveHover());
            }

            ItemCollectionTask collectionTask = new ItemCollectionTask(plugin, session);
            session.setItemCollectionTask(collectionTask.runTaskTimer(plugin, 20L, 20L));
        } else {
            // End battle immediately
            removeBossBar(session);
            endBattle(player.getUniqueId(), BattleState.COMPLETED);
        }
    }

    public void onPlayerDeath(UUID playerId) {
        BattleSession session = activeBattles.get(playerId);
        if (session == null || !session.isInBattle()) {
            return;
        }

        handleBattleFailure(playerId, BattleResult.ResultType.DEFEAT);
    }

    public void onPlayerLogout(UUID playerId) {
        BattleSession session = activeBattles.get(playerId);
        if (session == null || !session.isActive()) {
            return;
        }

        handleBattleFailure(playerId, BattleResult.ResultType.LOGOUT);
    }

    public void onTimeout(UUID playerId) {
        BattleSession session = activeBattles.get(playerId);
        if (session == null || !session.isInBattle()) {
            return;
        }

        handleBattleFailure(playerId, BattleResult.ResultType.TIMEOUT);
    }

    public void handleBattleFailure(UUID playerId, BattleResult.ResultType resultType) {
        BattleSession session = activeBattles.get(playerId);
        if (session == null) {
            return;
        }

        Player player = Bukkit.getPlayer(playerId);
        BossConfig bossConfig = session.getBossConfig();
        MessageConfig messages = plugin.getConfigManager().getMessageConfig();

        long duration = session.getElapsedTime();

        // Cancel tasks
        session.cancelTasks();

        // Remove spawned boss (skip if already removed)
        if (resultType != BattleResult.ResultType.BOSS_REMOVED) {
            if (plugin.getMythicMobsHook() != null && session.getActiveMobUuid() != null) {
                plugin.getMythicMobsHook().removeMob(session.getActiveMobUuid());
            }
        }

        // Save battle history
        plugin.getRankingRepository().saveBattleHistory(
                playerId,
                session.getPlayerName(),
                session.getBossId(),
                resultType.name(),
                duration
        );

        // Execute defeat commands
        if (player != null) {
            executeCommands(player, bossConfig.getDefeatCommands(), bossConfig, duration);
        }

        // Send appropriate message
        String message;
        switch (resultType) {
            case TIMEOUT:
                message = bossConfig.getTimeoutMessage();
                if (message == null || message.isEmpty()) {
                    message = messages.getBattleTimeout();
                }
                break;
            case LOGOUT:
                message = messages.getBattleLogout();
                break;
            case BOSS_REMOVED:
                message = messages.getBattleBossRemoved();
                break;
            default:
                message = bossConfig.getDefeatMessage();
                if (message == null || message.isEmpty()) {
                    message = messages.getBattleDefeat();
                }
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("boss_name", bossConfig.getDisplayName());
        placeholders.put("player", session.getPlayerName());

        if (player != null) {
            MessageUtil.sendMessage(player, messages.withPrefix(message), placeholders);

            // Play defeat sound
            playDefeatSound(player, bossConfig);

            // Kill player for timeout (damage from boss)
            if (resultType == BattleResult.ResultType.TIMEOUT) {
                // Try to get boss entity and deal damage from it
                Entity bossEntity = null;
                if (plugin.getMythicMobsHook() != null && session.getActiveMobUuid() != null) {
                    bossEntity = plugin.getMythicMobsHook().getEntity(session.getActiveMobUuid());
                }

                if (bossEntity instanceof LivingEntity livingBoss) {
                    // Deal fatal damage from the boss
                    player.damage(player.getHealth() + 100, livingBoss);
                } else {
                    // Fallback: just kill the player
                    player.setHealth(0);
                }
            }
        }

        // Send defeat broadcast (not for timeout - timeout has its own message)
        if (resultType != BattleResult.ResultType.TIMEOUT) {
            String defeatBroadcast = bossConfig.getDefeatBroadcast();
            if (defeatBroadcast != null && !defeatBroadcast.isEmpty()) {
                broadcastMessage(defeatBroadcast, placeholders);
            }
        }

        endBattle(playerId, BattleState.FAILED);

        // Teleport to exit location
        if (player != null) {
            Location exitLoc = bossConfig.getExitLocation();
            if (exitLoc != null) {
                player.teleport(exitLoc);
            }
        }

        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("Battle failed: " + session.getPlayerName() + " - " + resultType.name());
        }
    }

    public void endBattle(UUID playerId, BattleState endState) {
        BattleSession session = activeBattles.remove(playerId);
        if (session == null) {
            return;
        }

        // Remove boss bar
        removeBossBar(session);

        // Release boss for other players - remove initial boss and all chain bosses
        activeBosses.remove(session.getInitialBossId());
        if (session.isChainBattle()) {
            for (String chainBossId : session.getRemainingBosses()) {
                activeBosses.remove(chainBossId);
            }
        }

        session.end(endState);

        // Teleport to exit location
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && endState == BattleState.COMPLETED) {
            Location exitLoc = session.getBossConfig().getExitLocation();
            if (exitLoc != null) {
                player.teleport(exitLoc);
            }
        }
    }

    public void forceEndBattle(UUID playerId) {
        BattleSession session = activeBattles.get(playerId);
        if (session == null) {
            return;
        }

        // Remove spawned boss
        if (plugin.getMythicMobsHook() != null && session.getActiveMobUuid() != null) {
            plugin.getMythicMobsHook().removeMob(session.getActiveMobUuid());
        }

        endBattle(playerId, BattleState.FAILED);

        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            Location exitLoc = session.getBossConfig().getExitLocation();
            if (exitLoc != null) {
                player.teleport(exitLoc);
            }
        }
    }

    public void endAllBattles() {
        for (UUID playerId : new HashMap<>(activeBattles).keySet()) {
            forceEndBattle(playerId);
        }
    }

    private void executeCommands(Player player, java.util.List<String> commands, BossConfig bossConfig, long clearTime) {
        if (commands == null || commands.isEmpty()) {
            return;
        }

        for (String command : commands) {
            String processed = command
                    .replace("{player}", player.getName())
                    .replace("{boss_id}", bossConfig.getId())
                    .replace("{boss_name}", bossConfig.getDisplayName())
                    .replace("{time}", TimeUtil.formatTime(clearTime))
                    .replace("{time_ms}", String.valueOf(clearTime));

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processed);
        }
    }

    public boolean isInBattle(UUID playerId) {
        BattleSession session = activeBattles.get(playerId);
        return session != null && session.isActive();
    }

    public boolean isBossInUse(String bossId) {
        return activeBosses.contains(bossId);
    }

    public BattleSession getSession(UUID playerId) {
        return activeBattles.get(playerId);
    }

    public BattleSession getSessionByMobUuid(UUID mobUuid) {
        for (BattleSession session : activeBattles.values()) {
            if (mobUuid.equals(session.getActiveMobUuid())) {
                return session;
            }
        }
        return null;
    }

    public Map<UUID, BattleSession> getActiveBattles() {
        return new HashMap<>(activeBattles);
    }

    private void broadcastMessage(String message, Map<String, String> placeholders) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            MessageUtil.sendMessage(p, message, placeholders);
        }
    }

    // Sound methods
    private void playSound(Player player, Sound sound, float volume, float pitch) {
        if (player != null && sound != null) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    private void playBattleStartSound(Player player, BossConfig bossConfig) {
        playSound(player, bossConfig.getBattleStartSound(),
                bossConfig.getBattleStartSoundVolume(), bossConfig.getBattleStartSoundPitch());
    }

    private void playVictorySound(Player player, BossConfig bossConfig) {
        playSound(player, bossConfig.getVictorySound(),
                bossConfig.getVictorySoundVolume(), bossConfig.getVictorySoundPitch());
    }

    private void playDefeatSound(Player player, BossConfig bossConfig) {
        playSound(player, bossConfig.getDefeatSound(),
                bossConfig.getDefeatSoundVolume(), bossConfig.getDefeatSoundPitch());
    }

    public void playBattleLoopSound(BattleSession session) {
        Player player = Bukkit.getPlayer(session.getPlayerId());
        BossConfig bossConfig = session.getBossConfig();
        if (player != null && bossConfig.getBattleLoopSound() != null) {
            playSound(player, bossConfig.getBattleLoopSound(),
                    bossConfig.getBattleLoopSoundVolume(), bossConfig.getBattleLoopSoundPitch());
        }
    }

    public int getBattleLoopSoundInterval(BattleSession session) {
        return session.getBossConfig().getBattleLoopSoundInterval();
    }
}
