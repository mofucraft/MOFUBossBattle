package net.mofucraft.bossbattle.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Map;

public final class MessageUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private MessageUtil() {
    }

    public static Component parse(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        // Support both legacy color codes (&) and MiniMessage format
        return LEGACY_SERIALIZER.deserialize(message);
    }

    public static Component parse(String message, Map<String, String> placeholders) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        String processed = message;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            // Add reset code after placeholder value to prevent color bleeding
            String value = entry.getValue();
            if (value != null && value.contains("&")) {
                value = value + "&r";
            }
            processed = processed.replace("{" + entry.getKey() + "}", value != null ? value : "");
        }
        return parse(processed);
    }

    /**
     * Strip color codes from a string
     */
    public static String stripColors(String message) {
        if (message == null) {
            return null;
        }
        // Remove legacy color codes (&x, §x)
        return message.replaceAll("[&§][0-9a-fk-or]", "");
    }

    /**
     * Convert legacy color codes (&x) to section codes (§x) for use in BossBar, etc.
     */
    public static String colorize(String message) {
        if (message == null) {
            return null;
        }
        return message.replace('&', '§');
    }

    public static void sendMessage(Player player, String message) {
        if (player != null && message != null && !message.isEmpty()) {
            player.sendMessage(parse(message));
        }
    }

    public static void sendMessage(Player player, String message, Map<String, String> placeholders) {
        if (player != null && message != null && !message.isEmpty()) {
            player.sendMessage(parse(message, placeholders));
        }
    }

    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player == null) return;

        Component titleComponent = title != null ? parse(title) : Component.empty();
        Component subtitleComponent = subtitle != null ? parse(subtitle) : Component.empty();

        Title.Times times = Title.Times.times(
                Duration.ofMillis(fadeIn * 50L),
                Duration.ofMillis(stay * 50L),
                Duration.ofMillis(fadeOut * 50L)
        );

        player.showTitle(Title.title(titleComponent, subtitleComponent, times));
    }

    public static void sendActionBar(Player player, String message) {
        if (player != null && message != null && !message.isEmpty()) {
            player.sendActionBar(parse(message));
        }
    }

    public static void sendActionBar(Player player, String message, Map<String, String> placeholders) {
        if (player != null && message != null && !message.isEmpty()) {
            player.sendActionBar(parse(message, placeholders));
        }
    }

    /**
     * Send a clickable command message to a player
     */
    public static void sendClickableCommand(Player player, String message, String command, String hoverText) {
        if (player == null || message == null || message.isEmpty()) {
            return;
        }

        Component component = parse(message)
                .clickEvent(ClickEvent.runCommand(command))
                .hoverEvent(HoverEvent.showText(parse(hoverText != null ? hoverText : command)));

        player.sendMessage(component);
    }
}
