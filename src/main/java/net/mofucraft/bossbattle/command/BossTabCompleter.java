package net.mofucraft.bossbattle.command;

import net.mofucraft.bossbattle.MofuBossBattle;
import net.mofucraft.bossbattle.config.BossConfig;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BossTabCompleter implements TabCompleter {

    private final MofuBossBattle plugin;
    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "start", "stop", "list", "ranking", "myrank", "reload", "help"
    );

    public BossTabCompleter(MofuBossBattle plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            completions = SUBCOMMANDS.stream()
                    .filter(cmd -> cmd.startsWith(input))
                    .filter(cmd -> hasPermissionForSubcommand(sender, cmd))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            String input = args[1].toLowerCase();

            switch (subCommand) {
                case "start":
                case "ranking":
                case "myrank":
                    completions = plugin.getConfigManager().getAllBossConfigs().stream()
                            .map(BossConfig::getId)
                            .filter(id -> id.toLowerCase().startsWith(input))
                            .collect(Collectors.toList());
                    break;
                case "stop":
                    if (sender.hasPermission("mofubossbattle.stop.others")) {
                        completions = Bukkit.getOnlinePlayers().stream()
                                .filter(p -> plugin.getBattleManager().isInBattle(p.getUniqueId()))
                                .map(Player::getName)
                                .filter(name -> name.toLowerCase().startsWith(input))
                                .collect(Collectors.toList());
                    }
                    break;
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            String input = args[2].toLowerCase();

            if (subCommand.equals("start") && sender.hasPermission("mofubossbattle.start.others")) {
                completions = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(input))
                        .collect(Collectors.toList());
            }
        }

        return completions;
    }

    private boolean hasPermissionForSubcommand(CommandSender sender, String subCommand) {
        return switch (subCommand) {
            case "start" -> sender.hasPermission("mofubossbattle.start");
            case "stop" -> sender.hasPermission("mofubossbattle.stop");
            case "list" -> sender.hasPermission("mofubossbattle.list");
            case "ranking" -> sender.hasPermission("mofubossbattle.ranking");
            case "myrank" -> sender.hasPermission("mofubossbattle.myrank");
            case "reload" -> sender.hasPermission("mofubossbattle.reload");
            default -> true;
        };
    }
}
