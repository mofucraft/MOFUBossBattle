package net.mofucraft.bossbattle.config;

import net.mofucraft.bossbattle.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
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
    private BarColor bossBarColor;
    private BarStyle bossBarStyle;
    private String bossBarTitleFormat;

    // Sound settings
    private Sound battleStartSound;
    private float battleStartSoundVolume;
    private float battleStartSoundPitch;
    private Sound battleLoopSound;
    private float battleLoopSoundVolume;
    private float battleLoopSoundPitch;
    private int battleLoopSoundInterval; // in ticks
    private Sound victorySound;
    private float victorySoundVolume;
    private float victorySoundPitch;
    private Sound defeatSound;
    private float defeatSoundVolume;
    private float defeatSoundPitch;

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
        ConfigurationSection bossBarSection = config.getConfigurationSection("bossbar");
        if (bossBarSection != null) {
            boss.bossBarColor = parseBarColor(bossBarSection.getString("color", "RED"));
            boss.bossBarStyle = parseBarStyle(bossBarSection.getString("style", "SOLID"));
            boss.bossBarTitleFormat = bossBarSection.getString("title-format", "&c{boss_name} &7[{mode}] &e{time}");
        } else {
            boss.bossBarColor = BarColor.RED;
            boss.bossBarStyle = BarStyle.SOLID;
            boss.bossBarTitleFormat = "&c{boss_name} &7[{mode}] &e{time}";
        }

        // Sound settings
        ConfigurationSection soundSection = config.getConfigurationSection("sounds");
        if (soundSection != null) {
            boss.battleStartSound = parseSound(soundSection.getString("battle-start.sound"));
            boss.battleStartSoundVolume = (float) soundSection.getDouble("battle-start.volume", 1.0);
            boss.battleStartSoundPitch = (float) soundSection.getDouble("battle-start.pitch", 1.0);

            boss.battleLoopSound = parseSound(soundSection.getString("battle-loop.sound"));
            boss.battleLoopSoundVolume = (float) soundSection.getDouble("battle-loop.volume", 1.0);
            boss.battleLoopSoundPitch = (float) soundSection.getDouble("battle-loop.pitch", 1.0);
            boss.battleLoopSoundInterval = soundSection.getInt("battle-loop.interval", 100); // 5 seconds default

            boss.victorySound = parseSound(soundSection.getString("victory.sound"));
            boss.victorySoundVolume = (float) soundSection.getDouble("victory.volume", 1.0);
            boss.victorySoundPitch = (float) soundSection.getDouble("victory.pitch", 1.0);

            boss.defeatSound = parseSound(soundSection.getString("defeat.sound"));
            boss.defeatSoundVolume = (float) soundSection.getDouble("defeat.volume", 1.0);
            boss.defeatSoundPitch = (float) soundSection.getDouble("defeat.pitch", 1.0);
        } else {
            // Defaults
            boss.battleStartSoundVolume = 1.0f;
            boss.battleStartSoundPitch = 1.0f;
            boss.battleLoopSoundVolume = 1.0f;
            boss.battleLoopSoundPitch = 1.0f;
            boss.battleLoopSoundInterval = 100;
            boss.victorySoundVolume = 1.0f;
            boss.victorySoundPitch = 1.0f;
            boss.defeatSoundVolume = 1.0f;
            boss.defeatSoundPitch = 1.0f;
        }

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

    public BarColor getBossBarColor() {
        return bossBarColor;
    }

    public BarStyle getBossBarStyle() {
        return bossBarStyle;
    }

    public String getBossBarTitleFormat() {
        return bossBarTitleFormat;
    }

    public Sound getBattleStartSound() {
        return battleStartSound;
    }

    public float getBattleStartSoundVolume() {
        return battleStartSoundVolume;
    }

    public float getBattleStartSoundPitch() {
        return battleStartSoundPitch;
    }

    public Sound getBattleLoopSound() {
        return battleLoopSound;
    }

    public float getBattleLoopSoundVolume() {
        return battleLoopSoundVolume;
    }

    public float getBattleLoopSoundPitch() {
        return battleLoopSoundPitch;
    }

    public int getBattleLoopSoundInterval() {
        return battleLoopSoundInterval;
    }

    public Sound getVictorySound() {
        return victorySound;
    }

    public float getVictorySoundVolume() {
        return victorySoundVolume;
    }

    public float getVictorySoundPitch() {
        return victorySoundPitch;
    }

    public Sound getDefeatSound() {
        return defeatSound;
    }

    public float getDefeatSoundVolume() {
        return defeatSoundVolume;
    }

    public float getDefeatSoundPitch() {
        return defeatSoundPitch;
    }

    // Helper methods for parsing
    private static BarColor parseBarColor(String colorStr) {
        if (colorStr == null) return BarColor.RED;
        try {
            return BarColor.valueOf(colorStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BarColor.RED;
        }
    }

    private static BarStyle parseBarStyle(String styleStr) {
        if (styleStr == null) return BarStyle.SOLID;
        try {
            return BarStyle.valueOf(styleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BarStyle.SOLID;
        }
    }

    private static Sound parseSound(String soundStr) {
        if (soundStr == null || soundStr.isEmpty()) return null;
        try {
            // Convert ENTITY_ENDER_DRAGON_GROWL format to entity.ender_dragon.growl
            String key = soundStr.toLowerCase().replace("_", ".");
            // Handle minecraft: prefix
            if (!key.contains(":")) {
                key = "minecraft:" + key;
            }
            NamespacedKey namespacedKey = NamespacedKey.fromString(key);
            if (namespacedKey != null) {
                Sound sound = Registry.SOUNDS.get(namespacedKey);
                if (sound != null) {
                    return sound;
                }
            }
            // Fallback: try direct namespaced key without conversion
            NamespacedKey directKey = NamespacedKey.fromString(soundStr.toLowerCase());
            if (directKey != null) {
                return Registry.SOUNDS.get(directKey);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
