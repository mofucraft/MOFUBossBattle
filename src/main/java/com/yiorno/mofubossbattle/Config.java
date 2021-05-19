package com.yiorno.mofubossbattle;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.List;

//Quorted from HimaJyn
public class Config {

    private final Plugin plugin;
    private FileConfiguration config = null;

    public static int allArenaCount;

    public Config(Plugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        plugin.saveDefaultConfig();
        if (config != null) {
            plugin.reloadConfig();
        }
        config = plugin.getConfig();

        String[] allArena = config.getStringList("arena").toArray(new String[0]);
        allArenaCount = allArena.length;

        for(String arena : allArena){
            Variable.map.put(arena, null);
        }

        System.out.println("allArena:" + allArena);
        System.out.println("allArenaCount:" + allArenaCount);
    }

}
