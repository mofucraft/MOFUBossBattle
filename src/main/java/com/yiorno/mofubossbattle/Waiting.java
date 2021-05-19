package com.yiorno.mofubossbattle;

import org.bukkit.entity.Player;

public class Waiting {

    Player player;
    String arena;
    int waitingNumber;

    public void waiting(Player p, String a, int n){

        player = p;
        arena = a;
        waitingNumber = n;

    }

}
