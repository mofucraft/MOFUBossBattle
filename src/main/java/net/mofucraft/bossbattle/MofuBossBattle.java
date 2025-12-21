package net.mofucraft.bossbattle;

import net.mofucraft.bossbattle.battle.BattleManager;
import net.mofucraft.bossbattle.command.BossCommand;
import net.mofucraft.bossbattle.command.BossTabCompleter;
import net.mofucraft.bossbattle.config.ConfigManager;
import net.mofucraft.bossbattle.database.DatabaseManager;
import net.mofucraft.bossbattle.database.RankingRepository;
import net.mofucraft.bossbattle.hook.MythicMobsHook;
import net.mofucraft.bossbattle.hook.PlaceholderAPIHook;
import net.mofucraft.bossbattle.listener.CommandRestrictionListener;
import net.mofucraft.bossbattle.listener.MythicMobListener;
import net.mofucraft.bossbattle.listener.PlayerEventListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class MofuBossBattle extends JavaPlugin {

    private static MofuBossBattle instance;

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private RankingRepository rankingRepository;
    private BattleManager battleManager;
    private MythicMobsHook mythicMobsHook;
    private PlaceholderAPIHook placeholderAPIHook;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize configuration
        configManager = new ConfigManager(this);
        configManager.loadAll();

        // Initialize database
        databaseManager = new DatabaseManager(this, configManager);
        if (!databaseManager.connect()) {
            getLogger().severe("Failed to connect to database! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        rankingRepository = new RankingRepository(databaseManager);

        // Initialize battle manager
        battleManager = new BattleManager(this);

        // Check for MythicMobs
        if (getServer().getPluginManager().getPlugin("MythicMobs") != null) {
            mythicMobsHook = new MythicMobsHook(this);
            getServer().getPluginManager().registerEvents(new MythicMobListener(this), this);
            getLogger().info("MythicMobs integration enabled!");
        } else {
            getLogger().warning("MythicMobs not found! Boss spawning will not work.");
        }

        // Check for PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderAPIHook = new PlaceholderAPIHook(this);
            placeholderAPIHook.register();
            getLogger().info("PlaceholderAPI integration enabled!");
        } else {
            getLogger().info("PlaceholderAPI not found. Placeholders will not be available.");
        }

        // Register event listeners
        getServer().getPluginManager().registerEvents(new PlayerEventListener(this), this);
        getServer().getPluginManager().registerEvents(new CommandRestrictionListener(this), this);

        // Register commands
        PluginCommand bossCommand = getCommand("boss");
        if (bossCommand != null) {
            BossCommand executor = new BossCommand(this);
            bossCommand.setExecutor(executor);
            bossCommand.setTabCompleter(new BossTabCompleter(this));
        }

        getLogger().info("MofuBossBattle has been enabled!");
    }

    @Override
    public void onDisable() {
        // End all active battles
        if (battleManager != null) {
            battleManager.endAllBattles();
        }

        // Close database connection
        if (databaseManager != null) {
            databaseManager.disconnect();
        }

        getLogger().info("MofuBossBattle has been disabled!");
    }

    public void reload() {
        configManager.loadAll();
        getLogger().info("Configuration reloaded!");
    }

    public static MofuBossBattle getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public RankingRepository getRankingRepository() {
        return rankingRepository;
    }

    public BattleManager getBattleManager() {
        return battleManager;
    }

    public MythicMobsHook getMythicMobsHook() {
        return mythicMobsHook;
    }

    public PlaceholderAPIHook getPlaceholderAPIHook() {
        return placeholderAPIHook;
    }
}
