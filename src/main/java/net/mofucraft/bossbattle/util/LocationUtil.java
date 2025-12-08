package net.mofucraft.bossbattle.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public final class LocationUtil {

    private LocationUtil() {
    }

    public static Location fromConfig(ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        String worldName = section.getString("world");
        if (worldName == null) {
            return null;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }

        double x = section.getDouble("x", 0);
        double y = section.getDouble("y", 0);
        double z = section.getDouble("z", 0);
        float yaw = (float) section.getDouble("yaw", 0);
        float pitch = (float) section.getDouble("pitch", 0);

        return new Location(world, x, y, z, yaw, pitch);
    }

    public static void toConfig(ConfigurationSection section, Location location) {
        if (section == null || location == null) {
            return;
        }

        if (location.getWorld() != null) {
            section.set("world", location.getWorld().getName());
        }
        section.set("x", location.getX());
        section.set("y", location.getY());
        section.set("z", location.getZ());
        section.set("yaw", location.getYaw());
        section.set("pitch", location.getPitch());
    }

    public static String formatLocation(Location location) {
        if (location == null) {
            return "null";
        }

        String worldName = location.getWorld() != null ? location.getWorld().getName() : "unknown";
        return String.format("%s (%.1f, %.1f, %.1f)",
                worldName,
                location.getX(),
                location.getY(),
                location.getZ());
    }
}
