package net.mofucraft.bossbattle.listener;

import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import net.mofucraft.bossbattle.MofuBossBattle;
import net.mofucraft.bossbattle.battle.BattleSession;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

public class MythicMobListener implements Listener {

    private final MofuBossBattle plugin;

    public MythicMobListener(MofuBossBattle plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMythicMobDeath(MythicMobDeathEvent event) {
        Entity entity = event.getEntity();
        UUID mobUuid = entity.getUniqueId();

        // Find the battle session for this mob
        BattleSession session = plugin.getBattleManager().getSessionByMobUuid(mobUuid);

        if (session != null && session.isInBattle()) {
            // Check if the mob type matches the boss config
            String expectedMobId = session.getBossConfig().getMythicMobId();
            String actualMobId = event.getMobType().getInternalName();

            if (expectedMobId.equalsIgnoreCase(actualMobId)) {
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getLogger().info("Boss defeated: " + actualMobId + " by " + session.getPlayerName());
                }

                // Trigger victory
                plugin.getBattleManager().onBossDefeated(session.getPlayerId());
            }
        }
    }
}
