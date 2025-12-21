package net.mofucraft.bossbattle.config;

import net.mofucraft.bossbattle.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BossConfig {

    private final String id;
    private boolean enabled;
    private String displayName;
    private String mythicMobId;
    private int timeLimit;
    private int itemCollectionTime;
    private int invincibilityTime;
    private int bossLevel;
    private int bossSpawnDelay;
    private int teleportBlindnessDuration;

    // Survival mode (endurance mode) - player wins when time expires instead of losing
    private boolean survivalMode;

    // Boss bar settings
    private boolean showTimeBossBar;

    // Chain battle settings
    private boolean chainBattleEnabled;
    private String nextBossId;
    private List<String> chainBossList;

    // Locations
    private Location teleportLocation;
    private Location waitingLocation;
    private Location bossSpawnLocation;
    private Location exitLocation;

    // Messages
    private String startMessage;
    private String victoryMessage;
    private String defeatMessage;
    private String timeoutMessage;
    private String victoryBroadcast;
    private String defeatBroadcast;
    private Map<Integer, String> timeWarnings;

    // Commands
    private List<String> victoryCommands;
    private List<String> defeatCommands;

    public BossConfig(String id) {
        this.id = id;
        this.timeWarnings = new HashMap<>();
    }

    public static BossConfig fromConfig(YamlConfiguration config) {
        String id = config.getString("id");
        if (id == null || id.isEmpty()) {
            return null;
        }

        BossConfig boss = new BossConfig(id);
        boss.enabled = config.getBoolean("enabled", true);
        boss.displayName = config.getString("display-name", id);
        boss.mythicMobId = config.getString("mythic-mob-id", id);
        boss.timeLimit = config.getInt("time-limit", 300);
        boss.itemCollectionTime = config.getInt("item-collection-time", 30);
        boss.invincibilityTime = config.getInt("invincibility-time", 0);
        boss.bossLevel = config.getInt("boss-level", 1);
        boss.bossSpawnDelay = config.getInt("boss-spawn-delay", 20);
        boss.teleportBlindnessDuration = config.getInt("teleport-blindness-duration", 0);

        // Survival mode (endurance mode)
        boss.survivalMode = config.getBoolean("survival-mode", false);

        // Boss bar settings
        boss.showTimeBossBar = config.getBoolean("show-time-bossbar", true);

        // Chain battle settings
        ConfigurationSection chainSection = config.getConfigurationSection("chain-battle");
        if (chainSection != null) {
            boss.chainBattleEnabled = chainSection.getBoolean("enabled", false);
            boss.nextBossId = chainSection.getString("next-boss", null);
            boss.chainBossList = chainSection.getStringList("boss-list");
        } else {
            boss.chainBattleEnabled = false;
            boss.nextBossId = null;
            boss.chainBossList = new ArrayList<>();
        }

        // Load locations
        ConfigurationSection locationsSection = config.getConfigurationSection("locations");
        if (locationsSection != null) {
            boss.teleportLocation = LocationUtil.fromConfig(locationsSection.getConfigurationSection("teleport"));
            boss.waitingLocation = LocationUtil.fromConfig(locationsSection.getConfigurationSection("waiting"));
            boss.bossSpawnLocation = LocationUtil.fromConfig(locationsSection.getConfigurationSection("boss-spawn"));
            boss.exitLocation = LocationUtil.fromConfig(locationsSection.getConfigurationSection("exit"));
        }

        // Load messages
        ConfigurationSection messagesSection = config.getConfigurationSection("messages");
        if (messagesSection != null) {
            boss.startMessage = messagesSection.getString("start");
            boss.victoryMessage = messagesSection.getString("victory");
            boss.defeatMessage = messagesSection.getString("defeat");
            boss.timeoutMessage = messagesSection.getString("timeout");
            boss.victoryBroadcast = messagesSection.getString("victory-broadcast");
            boss.defeatBroadcast = messagesSection.getString("defeat-broadcast");
        }

        // Load time warnings
        ConfigurationSection warningsSection = config.getConfigurationSection("time-warnings");
        if (warningsSection != null) {
            for (String key : warningsSection.getKeys(false)) {
                try {
                    int seconds = Integer.parseInt(key);
                    String message = warningsSection.getString(key);
                    if (message != null) {
                        boss.timeWarnings.put(seconds, message);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // Load commands
        boss.victoryCommands = config.getStringList("victory-commands");
        boss.defeatCommands = config.getStringList("defeat-commands");

        return boss;
    }

    // Getters
    public String getId() {
        return id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMythicMobId() {
        return mythicMobId;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public int getItemCollectionTime() {
        return itemCollectionTime;
    }

    public int getInvincibilityTime() {
        return invincibilityTime;
    }

    public int getBossLevel() {
        return bossLevel;
    }

    public int getBossSpawnDelay() {
        return bossSpawnDelay;
    }

    public int getTeleportBlindnessDuration() {
        return teleportBlindnessDuration;
    }

    public Location getTeleportLocation() {
        return teleportLocation;
    }

    public Location getWaitingLocation() {
        return waitingLocation;
    }

    public Location getBossSpawnLocation() {
        return bossSpawnLocation;
    }

    public Location getExitLocation() {
        return exitLocation;
    }

    public String getStartMessage() {
        return startMessage;
    }

    public String getVictoryMessage() {
        return victoryMessage;
    }

    public String getDefeatMessage() {
        return defeatMessage;
    }

    public String getTimeoutMessage() {
        return timeoutMessage;
    }

    public String getVictoryBroadcast() {
        return victoryBroadcast;
    }

    public String getDefeatBroadcast() {
        return defeatBroadcast;
    }

    public Map<Integer, String> getTimeWarnings() {
        return timeWarnings;
    }

    public List<String> getVictoryCommands() {
        return victoryCommands;
    }

    public List<String> getDefeatCommands() {
        return defeatCommands;
    }

    public boolean isSurvivalMode() {
        return survivalMode;
    }

    public boolean isShowTimeBossBar() {
        return showTimeBossBar;
    }

    public boolean isChainBattleEnabled() {
        return chainBattleEnabled;
    }

    public String getNextBossId() {
        return nextBossId;
    }

    public List<String> getChainBossList() {
        return chainBossList;
    }
}
