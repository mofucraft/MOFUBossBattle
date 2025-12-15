package net.mofucraft.bossbattle.hook;

import io.lumine.mythic.api.MythicProvider;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import net.mofucraft.bossbattle.MofuBossBattle;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class MythicMobsHook {

    private final MofuBossBattle plugin;

    public MythicMobsHook(MofuBossBattle plugin) {
        this.plugin = plugin;
    }

    public UUID spawnBoss(String mobId, Location location, int level) {
        try {
            Optional<MythicMob> mythicMobOpt = MythicProvider.get().getMobManager().getMythicMob(mobId);

            if (mythicMobOpt.isEmpty()) {
                plugin.getLogger().warning("MythicMob not found: " + mobId);
                return null;
            }

            MythicMob mythicMob = mythicMobOpt.get();
            ActiveMob activeMob = mythicMob.spawn(BukkitAdapter.adapt(location), level);

            if (activeMob != null && activeMob.getEntity() != null) {
                Entity entity = activeMob.getEntity().getBukkitEntity();
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getLogger().info("Spawned MythicMob: " + mobId + " at " + location);
                }
                return entity.getUniqueId();
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to spawn MythicMob: " + mobId, e);
        }

        return null;
    }

    public void removeMob(UUID mobUuid) {
        try {
            ActiveMob activeMob = MythicBukkit.inst().getMobManager().getActiveMob(mobUuid).orElse(null);

            if (activeMob != null) {
                activeMob.remove();

                if (plugin.getConfigManager().isDebug()) {
                    plugin.getLogger().info("Removed MythicMob: " + mobUuid);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to remove MythicMob: " + mobUuid, e);
        }
    }

    public boolean isMythicMob(Entity entity) {
        try {
            return MythicBukkit.inst().getMobManager().getActiveMob(entity.getUniqueId()).isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    public String getMythicMobType(Entity entity) {
        try {
            ActiveMob activeMob = MythicBukkit.inst().getMobManager().getActiveMob(entity.getUniqueId()).orElse(null);
            if (activeMob != null) {
                return activeMob.getMobType();
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get MythicMob type", e);
        }
        return null;
    }

    public Entity getEntity(UUID mobUuid) {
        try {
            ActiveMob activeMob = MythicBukkit.inst().getMobManager().getActiveMob(mobUuid).orElse(null);
            if (activeMob != null && activeMob.getEntity() != null) {
                return activeMob.getEntity().getBukkitEntity();
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get MythicMob entity: " + mobUuid, e);
        }
        return null;
    }
}
