package net.mofucraft.bossbattle.config;

import net.mofucraft.bossbattle.MofuBossBattle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ConfigManager {

    private final MofuBossBattle plugin;
    private final Map<String, BossConfig> bossConfigs;
    private final MessageConfig messageConfig;

    // Database settings
    private String dbHost;
    private int dbPort;
    private String dbDatabase;
    private String dbUsername;
    private String dbPassword;
    private int dbPoolSize;

    // Global settings
    private String prefix;
    private int defaultTimeLimit;
    private int defaultItemCollectionTime;
    private boolean enabled;
    private boolean debug;

    // Battle command restriction settings
    private boolean commandRestrictionEnabled;
    private java.util.List<String> allowedCommands;
    private String commandRestrictionBypassPermission;

    // Item collection leave settings
    private boolean showLeaveCommandInChat;

    public ConfigManager(MofuBossBattle plugin) {
        this.plugin = plugin;
        this.bossConfigs = new HashMap<>();
        this.messageConfig = new MessageConfig();
    }

    public void loadAll() {
        // Save default configs if they don't exist
        plugin.saveDefaultConfig();
        saveDefaultMessages();
        saveDefaultBossConfig();

        // Load main config
        plugin.reloadConfig();
        loadMainConfig();

        // Load messages
        loadMessages();

        // Load boss configs
        loadBossConfigs();

        plugin.getLogger().info("Loaded " + bossConfigs.size() + " boss configuration(s).");
    }

    private void loadMainConfig() {
        FileConfiguration config = plugin.getConfig();

        // Database settings
        dbHost = config.getString("database.host", "localhost");
        dbPort = config.getInt("database.port", 3306);
        dbDatabase = config.getString("database.database", "mofubossbattle");
        dbUsername = config.getString("database.username", "root");
        dbPassword = config.getString("database.password", "password");
        dbPoolSize = config.getInt("database.pool-size", 10);

        // Global settings
        prefix = config.getString("settings.prefix", "&8[&6MofuBossBattle&8] ");
        defaultTimeLimit = config.getInt("settings.default-time-limit", 300);
        defaultItemCollectionTime = config.getInt("settings.default-item-collection-time", 30);
        enabled = config.getBoolean("settings.enabled", true);
        debug = config.getBoolean("debug", false);

        // Command restriction settings
        commandRestrictionEnabled = config.getBoolean("battle.command-restriction.enabled", true);
        allowedCommands = config.getStringList("battle.command-restriction.allowed-commands");
        commandRestrictionBypassPermission = config.getString("battle.command-restriction.bypass-permission", "mofubossbattle.bypass.commands");

        // Item collection settings
        showLeaveCommandInChat = config.getBoolean("battle.item-collection.show-leave-command", true);
    }

    private void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveDefaultMessages();
        }

        YamlConfiguration messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        // Load defaults from jar
        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            messagesConfig.setDefaults(defaultConfig);
        }

        messageConfig.load(messagesConfig);

        // Override prefix from config.yml if set
        if (prefix != null && !prefix.isEmpty()) {
            messageConfig.setPrefix(prefix);
        }
    }

    private void loadBossConfigs() {
        bossConfigs.clear();

        File bossesFolder = new File(plugin.getDataFolder(), "bosses");
        if (!bossesFolder.exists()) {
            bossesFolder.mkdirs();
        }

        File[] files = bossesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                BossConfig bossConfig = BossConfig.fromConfig(config);
                if (bossConfig != null) {
                    bossConfigs.put(bossConfig.getId(), bossConfig);
                    if (debug) {
                        plugin.getLogger().info("Loaded boss config: " + bossConfig.getId());
                    }
                } else {
                    plugin.getLogger().warning("Failed to load boss config from: " + file.getName());
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error loading boss config: " + file.getName(), e);
            }
        }
    }

    private void saveDefaultMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
    }

    private void saveDefaultBossConfig() {
        File bossesFolder = new File(plugin.getDataFolder(), "bosses");
        if (!bossesFolder.exists()) {
            bossesFolder.mkdirs();
        }

        File exampleFile = new File(bossesFolder, "example_boss.yml");
        if (!exampleFile.exists()) {
            plugin.saveResource("bosses/example_boss.yml", false);
        }
    }

    // Getters
    public BossConfig getBossConfig(String bossId) {
        return bossConfigs.get(bossId);
    }

    public Collection<BossConfig> getAllBossConfigs() {
        return bossConfigs.values();
    }

    public boolean hasBoss(String bossId) {
        return bossConfigs.containsKey(bossId);
    }

    public MessageConfig getMessageConfig() {
        return messageConfig;
    }

    public String getDbHost() {
        return dbHost;
    }

    public int getDbPort() {
        return dbPort;
    }

    public String getDbDatabase() {
        return dbDatabase;
    }

    public String getDbUsername() {
        return dbUsername;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public int getDbPoolSize() {
        return dbPoolSize;
    }

    public int getDefaultTimeLimit() {
        return defaultTimeLimit;
    }

    public int getDefaultItemCollectionTime() {
        return defaultItemCollectionTime;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isCommandRestrictionEnabled() {
        return commandRestrictionEnabled;
    }

    public java.util.List<String> getAllowedCommands() {
        return allowedCommands;
    }

    public String getCommandRestrictionBypassPermission() {
        return commandRestrictionBypassPermission;
    }

    public boolean isShowLeaveCommandInChat() {
        return showLeaveCommandInChat;
    }
}
