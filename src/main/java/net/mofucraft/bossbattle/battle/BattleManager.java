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
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.HashSet;
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

        MessageUtil.sendMessage(player, messages.withPrefix(startMsg), placeholders);

        // Start timer task
        BattleTimerTask timerTask = new BattleTimerTask(plugin, session);
        session.setTimerTask(timerTask.runTaskTimer(plugin, 20L, 20L));

        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("Battle started: " + player.getName() + " vs " + bossId);
        }

        return true;
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

        // Stop timer
        session.cancelTasks();
        session.setState(BattleState.ITEM_COLLECTION);

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

        // Send victory broadcast
        String victoryBroadcast = bossConfig.getVictoryBroadcast();
        if (victoryBroadcast != null && !victoryBroadcast.isEmpty()) {
            broadcastMessage(victoryBroadcast, placeholders);
        }

        // Start item collection phase
        int collectionTime = bossConfig.getItemCollectionTime();
        if (collectionTime > 0) {
            placeholders.put("time", String.valueOf(collectionTime));
            MessageUtil.sendMessage(player, messages.withPrefix(messages.getItemCollectionStart()), placeholders);

            ItemCollectionTask collectionTask = new ItemCollectionTask(plugin, session);
            session.setItemCollectionTask(collectionTask.runTaskTimer(plugin, 20L, 20L));
        } else {
            // End battle immediately
            endBattle(playerId, BattleState.COMPLETED);
        }

        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("Boss defeated: " + player.getName() + " cleared " + session.getBossId() + " in " + TimeUtil.formatTime(clearTime));
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

        // Release boss for other players
        activeBosses.remove(session.getBossId());

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
}
