package com.yiorno.mofubossbattle;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class MOFUBossBattle extends JavaPlugin implements Listener {

    public static String prefix = "&8✞&4ボスバトル&8✞ &f";

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        Config config = new Config(this);
        config.load();

        //getServer().getPluginManager().registerEvent(, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
        if(cmd.getName().equalsIgnoreCase("boss")){

            Player player = (Player)sender;
            String arena;

            if(args.length!=0) {

                if(args[1]==null){
                    player.sendMessage(prefix + "アリーナを入力してください");
                    return true;
                } else {
                    arena = args[1];
                }

                Manage manage = new Manage();

                if (args[0] == "join") {
                    //空きチェック
                    if(manage.isFull(arena)==true){
                        //順番に並ばせて観戦
                        player.sendMessage(prefix + "現在戦っているひとがいます！");
                        //player.sendMessage(prefix + "待っている人数: ");
                        //manage.wait(player, arena);
                    } else {
                        //スタート
                        manage.start(player, arena);
                    }
                }


                if (args[0] == "watch") {
                    //戦闘中チェック
                    if(manage.isFull(arena)==true){
                        //観戦
                        manage.watch(player, arena);
                    } else {
                        //観戦不可
                        player.sendMessage(prefix + "戦っている人がいないため観戦できません");
                    }
                }


                if (args[0] == "check") {
                }

            }
        }

        if(cmd.getName().equalsIgnoreCase("forgive")){

            Player player = (Player)sender;
            Manage manage = new Manage();

            if(manage.isFighting(player)==true){

            }else{
                player.sendMessage(prefix + "観戦していません");
            }

        }

        if(cmd.getName().equalsIgnoreCase("exit")){

            Player player = (Player)sender;
            Manage manage = new Manage();

            if(manage.isWatching(player)==true){

            }else{
                player.sendMessage(prefix + "観戦していません");
            }
        }

        sender.sendMessage("&8===="+prefix+"&8====");
        sender.sendMessage("/boss watch <1 or 2> : 観戦する");
        return false;
    }

    @EventHandler
    public void onLose(PlayerDeathEvent e){
        Manage manage = new Manage();
        if(manage.isFighting(e.getEntity().getPlayer())){

        }else{
            return;
        }
    }
}
