package net.mofucraft.bossbattle.task;

import net.mofucraft.bossbattle.MofuBossBattle;
import net.mofucraft.bossbattle.battle.BattleSession;
import net.mofucraft.bossbattle.config.BossConfig;
import net.mofucraft.bossbattle.config.MessageConfig;
import net.mofucraft.bossbattle.util.MessageUtil;
import net.mofucraft.bossbattle.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class BattleTimerTask extends BukkitRunnable {

    private final MofuBossBattle plugin;
    private final BattleSession session;
    private int lastWarningSecond = -1;

    public BattleTimerTask(MofuBossBattle plugin, BattleSession session) {
        this.plugin = plugin;
        this.session = session;
    }

    @Override
    public void run() {
        if (!session.isInBattle()) {
            cancel();
            return;
        }

        Player player = Bukkit.getPlayer(session.getPlayerId());
        if (player == null || !player.isOnline()) {
            plugin.getBattleManager().onPlayerLogout(session.getPlayerId());
            cancel();
            return;
        }

        int remainingSeconds = session.getRemainingSeconds();

        // Check timeout
        if (remainingSeconds <= 0) {
            plugin.getBattleManager().onTimeout(session.getPlayerId());
            cancel();
            return;
        }

        // Check for time warnings
        BossConfig bossConfig = session.getBossConfig();
        Map<Integer, String> warnings = bossConfig.getTimeWarnings();

        if (warnings != null && warnings.containsKey(remainingSeconds) && remainingSeconds != lastWarningSecond) {
            lastWarningSecond = remainingSeconds;
            String warningMessage = warnings.get(remainingSeconds);

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("time", String.valueOf(remainingSeconds));
            placeholders.put("time_formatted", TimeUtil.formatSecondsReadable(remainingSeconds));
            placeholders.put("boss_name", bossConfig.getDisplayName());

            MessageUtil.sendMessage(player, warningMessage, placeholders);
        }

        // Show action bar with remaining time
        MessageConfig messages = plugin.getConfigManager().getMessageConfig();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("time", String.valueOf(remainingSeconds));
        placeholders.put("time_formatted", TimeUtil.formatSecondsReadable(remainingSeconds));
        placeholders.put("boss_name", bossConfig.getDisplayName());

        MessageUtil.sendActionBar(player, messages.getTimeWarningDefault(), placeholders);
    }
}
