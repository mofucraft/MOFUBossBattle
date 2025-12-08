package net.mofucraft.bossbattle.listener;

import net.mofucraft.bossbattle.MofuBossBattle;
import net.mofucraft.bossbattle.battle.BattleSession;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerEventListener implements Listener {

    private final MofuBossBattle plugin;
    private final Map<UUID, Location> respawnLocations;

    public PlayerEventListener(MofuBossBattle plugin) {
        this.plugin = plugin;
        this.respawnLocations = new HashMap<>();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (plugin.getBattleManager().isInBattle(playerId)) {
            plugin.getBattleManager().onPlayerLogout(playerId);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerId = player.getUniqueId();

        BattleSession session = plugin.getBattleManager().getSession(playerId);
        if (session != null && session.isInBattle()) {
            // Store respawn location
            Location exitLoc = session.getBossConfig().getExitLocation();
            if (exitLoc != null) {
                respawnLocations.put(playerId, exitLoc);
            }

            plugin.getBattleManager().onPlayerDeath(playerId);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        Location respawnLoc = respawnLocations.remove(playerId);
        if (respawnLoc != null) {
            event.setRespawnLocation(respawnLoc);
        }
    }
}
