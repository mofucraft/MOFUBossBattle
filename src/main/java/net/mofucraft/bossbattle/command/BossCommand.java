package net.mofucraft.bossbattle.command;

import net.mofucraft.bossbattle.MofuBossBattle;
import net.mofucraft.bossbattle.battle.BattleSession;
import net.mofucraft.bossbattle.config.BossConfig;
import net.mofucraft.bossbattle.config.MessageConfig;
import net.mofucraft.bossbattle.database.RankingEntry;
import net.mofucraft.bossbattle.util.MessageUtil;
import net.mofucraft.bossbattle.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BossCommand implements CommandExecutor {

    private final MofuBossBattle plugin;

    public BossCommand(MofuBossBattle plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessageConfig messages = plugin.getConfigManager().getMessageConfig();

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "start":
                return handleStart(sender, args, messages);
            case "stop":
                return handleStop(sender, args, messages);
            case "list":
                return handleList(sender, messages);
            case "ranking":
                return handleRanking(sender, args, messages);
            case "myrank":
                return handleMyRank(sender, args, messages);
            case "reload":
                return handleReload(sender, messages);
            case "help":
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleStart(CommandSender sender, String[] args, MessageConfig messages) {
        if (!sender.hasPermission("mofubossbattle.start")) {
            MessageUtil.sendMessage((Player) sender, messages.withPrefix(messages.getCommandNoPermission()));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("Usage: /boss start <boss_id> [player]");
            return true;
        }

        String bossId = args[1];
        Player target;

        if (args.length >= 3 && sender.hasPermission("mofubossbattle.start.others")) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage("Player not found: " + args[2]);
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(messages.getCommandPlayerOnly());
            return true;
        }

        if (!plugin.getConfigManager().hasBoss(bossId)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("boss", bossId);
            MessageUtil.sendMessage(target, messages.withPrefix(messages.getCommandInvalidBoss()), placeholders);
            return true;
        }

        if (plugin.getBattleManager().isInBattle(target.getUniqueId())) {
            MessageUtil.sendMessage(target, messages.withPrefix(messages.getCommandAlreadyInBattle()));
            return true;
        }

        boolean started = plugin.getBattleManager().startBattle(target, bossId);
        if (started) {
            BossConfig bossConfig = plugin.getConfigManager().getBossConfig(bossId);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", target.getName());
            placeholders.put("boss_name", bossConfig.getDisplayName());

            if (sender != target) {
                MessageUtil.sendMessage((Player) sender, messages.withPrefix(messages.getCommandBattleStarted()), placeholders);
            }
        }

        return true;
    }

    private boolean handleStop(CommandSender sender, String[] args, MessageConfig messages) {
        if (!sender.hasPermission("mofubossbattle.stop")) {
            if (sender instanceof Player) {
                MessageUtil.sendMessage((Player) sender, messages.withPrefix(messages.getCommandNoPermission()));
            }
            return true;
        }

        Player target;

        if (args.length >= 2 && sender.hasPermission("mofubossbattle.stop.others")) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("Player not found: " + args[1]);
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(messages.getCommandPlayerOnly());
            return true;
        }

        if (!plugin.getBattleManager().isInBattle(target.getUniqueId())) {
            MessageUtil.sendMessage(target, messages.withPrefix(messages.getCommandNotInBattle()));
            return true;
        }

        plugin.getBattleManager().forceEndBattle(target.getUniqueId());
        MessageUtil.sendMessage(target, messages.withPrefix(messages.getCommandBattleStopped()));

        return true;
    }

    private boolean handleList(CommandSender sender, MessageConfig messages) {
        if (!sender.hasPermission("mofubossbattle.list")) {
            if (sender instanceof Player) {
                MessageUtil.sendMessage((Player) sender, messages.withPrefix(messages.getCommandNoPermission()));
            }
            return true;
        }

        Collection<BossConfig> bosses = plugin.getConfigManager().getAllBossConfigs();
        if (bosses.isEmpty()) {
            sender.sendMessage("No bosses configured.");
            return true;
        }

        String bossListStr = bosses.stream()
                .map(BossConfig::getId)
                .collect(Collectors.joining(", "));

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("bosses", bossListStr);

        if (sender instanceof Player) {
            MessageUtil.sendMessage((Player) sender, messages.withPrefix(messages.getCommandBossList()), placeholders);
        } else {
            sender.sendMessage("Available bosses: " + bossListStr);
        }

        return true;
    }

    private boolean handleRanking(CommandSender sender, String[] args, MessageConfig messages) {
        if (!sender.hasPermission("mofubossbattle.ranking")) {
            if (sender instanceof Player) {
                MessageUtil.sendMessage((Player) sender, messages.withPrefix(messages.getCommandNoPermission()));
            }
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("Usage: /boss ranking <boss_id> [page]");
            return true;
        }

        String bossId = args[1];
        if (!plugin.getConfigManager().hasBoss(bossId)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("boss", bossId);
            if (sender instanceof Player) {
                MessageUtil.sendMessage((Player) sender, messages.withPrefix(messages.getCommandInvalidBoss()), placeholders);
            }
            return true;
        }

        BossConfig bossConfig = plugin.getConfigManager().getBossConfig(bossId);
        int page = 1;
        if (args.length >= 3) {
            try {
                page = Integer.parseInt(args[2]);
            } catch (NumberFormatException ignored) {
            }
        }

        final int finalPage = page;

        plugin.getRankingRepository().getTopRankings(bossId, 10).thenAccept(rankings -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (rankings.isEmpty()) {
                    if (sender instanceof Player) {
                        MessageUtil.sendMessage((Player) sender, messages.withPrefix(messages.getRankingNoRecords()));
                    } else {
                        sender.sendMessage("No records found.");
                    }
                    return;
                }

                Map<String, String> headerPlaceholders = new HashMap<>();
                headerPlaceholders.put("boss_name", bossConfig.getDisplayName());
                headerPlaceholders.put("page", String.valueOf(finalPage));

                if (sender instanceof Player) {
                    MessageUtil.sendMessage((Player) sender, messages.getRankingHeader(), headerPlaceholders);
                } else {
                    sender.sendMessage("=== " + bossConfig.getDisplayName() + " Ranking ===");
                }

                for (RankingEntry entry : rankings) {
                    Map<String, String> entryPlaceholders = new HashMap<>();
                    entryPlaceholders.put("rank", String.valueOf(entry.getRank()));
                    entryPlaceholders.put("player", entry.getPlayerName());
                    entryPlaceholders.put("time", TimeUtil.formatTime(entry.getClearTimeMillis()));

                    if (sender instanceof Player) {
                        MessageUtil.sendMessage((Player) sender, messages.getRankingEntry(), entryPlaceholders);
                    } else {
                        sender.sendMessage(entry.getRank() + ". " + entry.getPlayerName() + " - " + TimeUtil.formatTime(entry.getClearTimeMillis()));
                    }
                }
            });
        });

        return true;
    }

    private boolean handleMyRank(CommandSender sender, String[] args, MessageConfig messages) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.getCommandPlayerOnly());
            return true;
        }

        if (!player.hasPermission("mofubossbattle.myrank")) {
            MessageUtil.sendMessage(player, messages.withPrefix(messages.getCommandNoPermission()));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("Usage: /boss myrank <boss_id>");
            return true;
        }

        String bossId = args[1];
        if (!plugin.getConfigManager().hasBoss(bossId)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("boss", bossId);
            MessageUtil.sendMessage(player, messages.withPrefix(messages.getCommandInvalidBoss()), placeholders);
            return true;
        }

        plugin.getRankingRepository().getPlayerRank(player.getUniqueId(), bossId).thenAccept(rank -> {
            plugin.getRankingRepository().getPlayerBestTime(player.getUniqueId(), bossId).thenAccept(bestTime -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (rank <= 0 || bestTime < 0) {
                        MessageUtil.sendMessage(player, messages.withPrefix(messages.getRankingNotRanked()));
                    } else {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("rank", String.valueOf(rank));
                        placeholders.put("time", TimeUtil.formatTime(bestTime));
                        MessageUtil.sendMessage(player, messages.withPrefix(messages.getRankingYourRank()), placeholders);
                    }
                });
            });
        });

        return true;
    }

    private boolean handleReload(CommandSender sender, MessageConfig messages) {
        if (!sender.hasPermission("mofubossbattle.reload")) {
            if (sender instanceof Player) {
                MessageUtil.sendMessage((Player) sender, messages.withPrefix(messages.getCommandNoPermission()));
            }
            return true;
        }

        plugin.reload();

        if (sender instanceof Player) {
            MessageUtil.sendMessage((Player) sender, messages.withPrefix(messages.getCommandConfigReloaded()));
        } else {
            sender.sendMessage("Configuration reloaded.");
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== MofuBossBattle Commands ===");
        sender.sendMessage("§e/boss start <boss_id> [player] §7- Start a boss battle");
        sender.sendMessage("§e/boss stop [player] §7- Stop a boss battle");
        sender.sendMessage("§e/boss list §7- List available bosses");
        sender.sendMessage("§e/boss ranking <boss_id> §7- View rankings");
        sender.sendMessage("§e/boss myrank <boss_id> §7- View your rank");
        sender.sendMessage("§e/boss reload §7- Reload configuration");
        sender.sendMessage("§e/boss help §7- Show this help");
    }
}
