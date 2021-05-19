package com.yiorno.mofubossbattle;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class Manage {

    public boolean isFull(String arena){
        if(Variable.map.get(arena)!=null){
            return true;
        } else {
            return false;
        }
    }

    public boolean isFighting(Player player){
        if(Variable.map.containsValue(player)==true){
            return true;
        } else {
            return false;
        }
    }

    public boolean isWatching(Player player){
        if(Variable.watching.contains(player)==true){
            return true;
        } else {
            return false;
        }
    }

    public void start(Player player, String arena){

    }

    public void watch(Player player, String arena){
        sendArena(player, arena);
        player.setGameMode(GameMode.SPECTATOR);
        Variable.watching.add(player);
    }

    public void end(Player player, String arena){

        //REMOVE MOBS
        //TP PLAYER
        //ADJUST WAITING

    }

    public void victory(Player player, String arena){

    }

    public void endWatching(Player player){
        Variable.watching.remove(player);
        sendSpawn(player);
        player.setGameMode(GameMode.SURVIVAL);
    }

    public void wait(Player player, String arena){
    }

    public void sendSpawn(Player player){
        player.teleport(player.getLocation().getWorld().getSpawnLocation());
    }

    public void sendArena(Player player, String arena){
        //player.teleport(arenaSpawn(arena));
    }
}
