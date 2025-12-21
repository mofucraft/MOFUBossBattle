package net.mofucraft.bossbattle.listener;

import net.mofucraft.bossbattle.MofuBossBattle;
import net.mofucraft.bossbattle.battle.BattleSession;
import net.mofucraft.bossbattle.config.MessageConfig;
import net.mofucraft.bossbattle.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;

public class CommandRestrictionListener implements Listener {

    private final MofuBossBattle plugin;

    public CommandRestrictionListener(MofuBossBattle plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled()) {
            return;
        }

        // Check if command restriction is enabled
        if (!plugin.getConfigManager().isCommandRestrictionEnabled()) {
            return;
        }

        Player player = event.getPlayer();

        // Check if player is in battle
        BattleSession session = plugin.getBattleManager().getSession(player.getUniqueId());
        if (session == null || !session.isInBattle()) {
            return;
        }

        // Check if player has bypass permission
        String bypassPermission = plugin.getConfigManager().getCommandRestrictionBypassPermission();
        if (bypassPermission != null && !bypassPermission.isEmpty() && player.hasPermission(bypassPermission)) {
            return;
        }

        String command = event.getMessage().toLowerCase();
        // Remove the leading slash
        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        // Get the base command (first word)
        String baseCommand = command.split(" ")[0];

        // Always allow boss commands
        if (baseCommand.equals("boss")) {
            return;
        }

        // Check against allowed commands list
        List<String> allowedCommands = plugin.getConfigManager().getAllowedCommands();
        if (allowedCommands != null) {
            for (String allowed : allowedCommands) {
                String allowedLower = allowed.toLowerCase();
                // Check if command matches (with or without leading slash)
                if (allowedLower.startsWith("/")) {
                    allowedLower = allowedLower.substring(1);
                }

                // Check for exact match or prefix match (for commands with arguments)
                if (baseCommand.equals(allowedLower) || command.startsWith(allowedLower + " ")) {
                    return;
                }
            }
        }

        // Block the command
        event.setCancelled(true);

        MessageConfig messages = plugin.getConfigManager().getMessageConfig();
        MessageUtil.sendMessage(player, messages.withPrefix(messages.getCommandBlocked()));
    }
}
