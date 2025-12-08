package net.mofucraft.bossbattle.util;

import net.kyori.adventure.text.Component;
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
            processed = processed.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return parse(processed);
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
}
