package net.mofucraft.bossbattle.util;

public final class TimeUtil {

    private TimeUtil() {
    }

    /**
     * Format milliseconds to MM:SS.mmm format
     */
    public static String formatTime(long millis) {
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long ms = millis % 1000;

        return String.format("%02d:%02d.%03d", minutes, seconds, ms);
    }

    /**
     * Format milliseconds to MM:SS format (no milliseconds)
     */
    public static String formatTimeShort(long millis) {
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * Format seconds to readable string (e.g., "5分", "30秒")
     */
    public static String formatSecondsReadable(int seconds) {
        if (seconds >= 60) {
            int minutes = seconds / 60;
            int remainingSeconds = seconds % 60;
            if (remainingSeconds == 0) {
                return minutes + "分";
            }
            return minutes + "分" + remainingSeconds + "秒";
        }
        return seconds + "秒";
    }

    /**
     * Parse time string to seconds (e.g., "5m30s" -> 330)
     */
    public static int parseTimeString(String time) {
        if (time == null || time.isEmpty()) {
            return 0;
        }

        time = time.toLowerCase().trim();
        int totalSeconds = 0;

        // Try to parse as plain number first
        try {
            return Integer.parseInt(time);
        } catch (NumberFormatException ignored) {
        }

        // Parse format like "5m30s"
        StringBuilder number = new StringBuilder();
        for (char c : time.toCharArray()) {
            if (Character.isDigit(c)) {
                number.append(c);
            } else if (c == 'm' && number.length() > 0) {
                totalSeconds += Integer.parseInt(number.toString()) * 60;
                number = new StringBuilder();
            } else if (c == 's' && number.length() > 0) {
                totalSeconds += Integer.parseInt(number.toString());
                number = new StringBuilder();
            }
        }

        return totalSeconds;
    }
}
