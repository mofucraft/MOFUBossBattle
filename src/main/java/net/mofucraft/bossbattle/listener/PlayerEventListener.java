package net.mofucraft.bossbattle.listener;

import net.mofucraft.bossbattle.MofuBossBattle;
import net.mofucraft.bossbattle.battle.BattleSession;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerEventListener implements Listener {

    private final MofuBossBattle plugin;
    private final Map<UUID, Location> respawnLocations;
    private final Map<UUID, Location> logoutTeleportLocations;

    public PlayerEventListener(MofuBossBattle plugin) {
        this.plugin = plugin;
        this.respawnLocations = new HashMap<>();
        this.logoutTeleportLocations = new HashMap<>();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (plugin.getBattleManager().isInBattle(playerId)) {
            // Store exit location for next login
            BattleSession session = plugin.getBattleManager().getSession(playerId);
            if (session != null) {
                Location exitLoc = session.getBossConfig().getExitLocation();
                if (exitLoc != null) {
                    logoutTeleportLocations.put(playerId, exitLoc);
                }
            }

            plugin.getBattleManager().onPlayerLogout(playerId);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Teleport to exit location if logged out during battle
        Location exitLoc = logoutTeleportLocations.remove(playerId);
        if (exitLoc != null) {
            // Delay teleport slightly to ensure player is fully loaded
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.teleport(exitLoc);
                }
            }, 5L);
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
