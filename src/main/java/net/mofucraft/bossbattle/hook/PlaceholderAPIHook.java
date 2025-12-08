package net.mofucraft.bossbattle.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.mofucraft.bossbattle.MofuBossBattle;
import net.mofucraft.bossbattle.battle.BattleSession;
import net.mofucraft.bossbattle.database.RankingEntry;
import net.mofucraft.bossbattle.util.TimeUtil;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class PlaceholderAPIHook extends PlaceholderExpansion {

    private final MofuBossBattle plugin;
    private final ConcurrentHashMap<String, CachedValue<?>> cache;

    public PlaceholderAPIHook(MofuBossBattle plugin) {
        this.plugin = plugin;
        this.cache = new ConcurrentHashMap<>();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "mofubossbattle";
    }

    @Override
    public @NotNull String getAuthor() {
        return "MofuCraft";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        String[] parts = params.split("_");

        if (parts.length < 1) {
            return null;
        }

        switch (parts[0]) {
            case "ranking":
                return handleRankingPlaceholder(parts);
            case "myrank":
                return handleMyRankPlaceholder(player, parts);
            case "mybest":
                return handleMyBestPlaceholder(player, parts);
            case "in":
                if (parts.length > 1 && parts[1].equals("battle")) {
                    return handleInBattlePlaceholder(player);
                }
                break;
            case "current":
                if (parts.length > 1 && parts[1].equals("boss")) {
                    return handleCurrentBossPlaceholder(player);
                }
                break;
            case "time":
                return handleTimePlaceholder(player, parts);
            case "total":
                return handleTotalPlaceholder(parts);
        }

        return null;
    }

    // %mofubossbattle_ranking_<boss>_<rank>_name%
    // %mofubossbattle_ranking_<boss>_<rank>_time%
    // %mofubossbattle_ranking_<boss>_top_name%
    // %mofubossbattle_ranking_<boss>_top_time%
    private String handleRankingPlaceholder(String[] parts) {
        if (parts.length < 4) {
            return null;
        }

        String bossId = parts[1];
        String rankStr = parts[2];
        String field = parts[3];

        int rank;
        if (rankStr.equalsIgnoreCase("top")) {
            rank = 1;
        } else {
            try {
                rank = Integer.parseInt(rankStr);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        String cacheKey = "ranking_" + bossId + "_" + rank;
        CachedValue<List<RankingEntry>> cached = getCachedRankings(bossId, rank);

        if (cached == null || cached.value == null || cached.value.isEmpty()) {
            return "-";
        }

        List<RankingEntry> rankings = cached.value;
        if (rank > rankings.size()) {
            return "-";
        }

        RankingEntry entry = rankings.get(rank - 1);

        return switch (field) {
            case "name" -> entry.getPlayerName();
            case "time" -> TimeUtil.formatTime(entry.getClearTimeMillis());
            case "raw" -> String.valueOf(entry.getClearTimeMillis());
            default -> null;
        };
    }

    // %mofubossbattle_myrank_<boss>%
    private String handleMyRankPlaceholder(OfflinePlayer player, String[] parts) {
        if (player == null || parts.length < 2) {
            return null;
        }

        String bossId = parts[1];
        String cacheKey = "myrank_" + player.getUniqueId() + "_" + bossId;

        CachedValue<Integer> cached = getCached(cacheKey, Integer.class);
        if (cached == null || cached.isExpired()) {
            plugin.getRankingRepository().getPlayerRank(player.getUniqueId(), bossId)
                    .thenAccept(rank -> cache.put(cacheKey, new CachedValue<>(rank)));
            return cached != null ? String.valueOf(cached.value) : "-";
        }

        return cached.value > 0 ? String.valueOf(cached.value) : "-";
    }

    // %mofubossbattle_mybest_<boss>%
    // %mofubossbattle_mybest_<boss>_raw%
    private String handleMyBestPlaceholder(OfflinePlayer player, String[] parts) {
        if (player == null || parts.length < 2) {
            return null;
        }

        String bossId = parts[1];
        boolean raw = parts.length > 2 && parts[2].equals("raw");
        String cacheKey = "mybest_" + player.getUniqueId() + "_" + bossId;

        CachedValue<Long> cached = getCached(cacheKey, Long.class);
        if (cached == null || cached.isExpired()) {
            plugin.getRankingRepository().getPlayerBestTime(player.getUniqueId(), bossId)
                    .thenAccept(time -> cache.put(cacheKey, new CachedValue<>(time)));
            if (cached != null && cached.value >= 0) {
                return raw ? String.valueOf(cached.value) : TimeUtil.formatTime(cached.value);
            }
            return "-";
        }

        if (cached.value < 0) {
            return "-";
        }

        return raw ? String.valueOf(cached.value) : TimeUtil.formatTime(cached.value);
    }

    // %mofubossbattle_in_battle%
    private String handleInBattlePlaceholder(OfflinePlayer player) {
        if (player == null || !player.isOnline()) {
            return "false";
        }

        return String.valueOf(plugin.getBattleManager().isInBattle(player.getUniqueId()));
    }

    // %mofubossbattle_current_boss%
    private String handleCurrentBossPlaceholder(OfflinePlayer player) {
        if (player == null || !player.isOnline()) {
            return "";
        }

        BattleSession session = plugin.getBattleManager().getSession(player.getUniqueId());
        if (session == null) {
            return "";
        }

        return session.getBossConfig().getDisplayName();
    }

    // %mofubossbattle_time_remaining%
    // %mofubossbattle_time_elapsed%
    private String handleTimePlaceholder(OfflinePlayer player, String[] parts) {
        if (player == null || !player.isOnline() || parts.length < 2) {
            return "-";
        }

        BattleSession session = plugin.getBattleManager().getSession(player.getUniqueId());
        if (session == null || !session.isInBattle()) {
            return "-";
        }

        return switch (parts[1]) {
            case "remaining" -> TimeUtil.formatSecondsReadable(session.getRemainingSeconds());
            case "elapsed" -> TimeUtil.formatTimeShort(session.getElapsedTime());
            default -> null;
        };
    }

    // %mofubossbattle_total_clears_<boss>%
    private String handleTotalPlaceholder(String[] parts) {
        if (parts.length < 3 || !parts[1].equals("clears")) {
            return null;
        }

        String bossId = parts[2];
        String cacheKey = "total_clears_" + bossId;

        CachedValue<Integer> cached = getCached(cacheKey, Integer.class);
        if (cached == null || cached.isExpired()) {
            plugin.getRankingRepository().getTotalClears(bossId)
                    .thenAccept(total -> cache.put(cacheKey, new CachedValue<>(total)));
            return cached != null ? String.valueOf(cached.value) : "0";
        }

        return String.valueOf(cached.value);
    }

    @SuppressWarnings("unchecked")
    private <T> CachedValue<T> getCached(String key, Class<T> type) {
        CachedValue<?> cached = cache.get(key);
        if (cached != null && type.isInstance(cached.value)) {
            return (CachedValue<T>) cached;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private CachedValue<List<RankingEntry>> getCachedRankings(String bossId, int maxRank) {
        String cacheKey = "rankings_" + bossId;
        CachedValue<?> cached = cache.get(cacheKey);

        if (cached == null || cached.isExpired()) {
            plugin.getRankingRepository().getTopRankings(bossId, Math.max(maxRank, 10))
                    .thenAccept(rankings -> cache.put(cacheKey, new CachedValue<>(rankings)));

            if (cached != null && cached.value instanceof List) {
                return (CachedValue<List<RankingEntry>>) cached;
            }
            return null;
        }

        if (cached.value instanceof List) {
            return (CachedValue<List<RankingEntry>>) cached;
        }
        return null;
    }

    private static class CachedValue<T> {
        final T value;
        final long timestamp;
        static final long CACHE_DURATION = TimeUnit.SECONDS.toMillis(30);

        CachedValue(T value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_DURATION;
        }
    }
}
