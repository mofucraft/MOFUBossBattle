package net.mofucraft.bossbattle.task;

import net.mofucraft.bossbattle.MofuBossBattle;
import net.mofucraft.bossbattle.battle.BattleSession;
import net.mofucraft.bossbattle.battle.BattleState;
import net.mofucraft.bossbattle.config.BossConfig;
import net.mofucraft.bossbattle.config.MessageConfig;
import net.mofucraft.bossbattle.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class ItemCollectionTask extends BukkitRunnable {

    private final MofuBossBattle plugin;
    private final BattleSession session;
    private final long startTime;
    private int lastWarningSecond = -1;

    public ItemCollectionTask(MofuBossBattle plugin, BattleSession session) {
        this.plugin = plugin;
        this.session = session;
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public void run() {
        if (session.getState() != BattleState.ITEM_COLLECTION) {
            cancel();
            return;
        }

        Player player = Bukkit.getPlayer(session.getPlayerId());
        if (player == null || !player.isOnline()) {
            plugin.getBattleManager().endBattle(session.getPlayerId(), BattleState.COMPLETED);
            cancel();
            return;
        }

        BossConfig bossConfig = session.getBossConfig();
        int collectionTime = bossConfig.getItemCollectionTime();
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        int remainingSeconds = collectionTime - (int) elapsed;

        MessageConfig messages = plugin.getConfigManager().getMessageConfig();

        // Check if time is up
        if (remainingSeconds <= 0) {
            // Send end message
            MessageUtil.sendMessage(player, messages.withPrefix(messages.getItemCollectionEnd()));

            // Teleport to exit
            Location exitLoc = bossConfig.getExitLocation();
            if (exitLoc != null) {
                player.teleport(exitLoc);
            }

            plugin.getBattleManager().endBattle(session.getPlayerId(), BattleState.COMPLETED);
            cancel();
            return;
        }

        // Warning at certain intervals
        if (remainingSeconds <= 10 && remainingSeconds != lastWarningSecond) {
            lastWarningSecond = remainingSeconds;

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("time", String.valueOf(remainingSeconds));

            MessageUtil.sendMessage(player, messages.withPrefix(messages.getItemCollectionWarning()), placeholders);
        }

        // Show action bar
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("time", String.valueOf(remainingSeconds));

        MessageUtil.sendActionBar(player, "&eアイテム回収中... 残り&c" + remainingSeconds + "秒", placeholders);
    }
}
