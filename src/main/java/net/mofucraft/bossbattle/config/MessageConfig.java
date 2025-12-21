package net.mofucraft.bossbattle.config;

import org.bukkit.configuration.file.YamlConfiguration;

public class MessageConfig {

    // Battle messages
    private String battleStart;
    private String battleVictory;
    private String battleDefeat;
    private String battleTimeout;
    private String battleLogout;
    private String battleBossRemoved;
    private String battleSurvival;

    // Chain battle messages
    private String chainBattleNextBoss;

    // Time warnings
    private String timeWarningDefault;

    // Item collection
    private String itemCollectionStart;
    private String itemCollectionWarning;
    private String itemCollectionEnd;
    private String itemCollectionLeaveHint;
    private String itemCollectionLeaveHover;

    // Ranking
    private String rankingHeader;
    private String rankingEntry;
    private String rankingNoRecords;
    private String rankingYourRank;
    private String rankingNotRanked;

    // Command messages
    private String commandNoPermission;
    private String commandPlayerOnly;
    private String commandInvalidBoss;
    private String commandAlreadyInBattle;
    private String commandBattleStarted;
    private String commandBattleStopped;
    private String commandConfigReloaded;
    private String commandNotInBattle;
    private String commandBossList;
    private String commandBossInUse;
    private String commandBossDisabled;
    private String commandLeaveNotAllowed;
    private String commandLeaveSuccess;
    private String commandBlocked;

    // Prefix
    private String prefix;

    public void load(YamlConfiguration config) {
        // Battle messages
        battleStart = config.getString("battle.start", "&a{boss_name}&rとの戦闘を開始します！");
        battleVictory = config.getString("battle.victory", "&6おめでとう！ &e{boss_name}&rを&a{time}&rで討伐しました！");
        battleDefeat = config.getString("battle.defeat", "&c{boss_name}&rに敗北しました...");
        battleTimeout = config.getString("battle.timeout", "&c時間切れ！{boss_name}&rとの戦闘に失敗しました。");
        battleLogout = config.getString("battle.logout", "&c{player}&rがログアウトしたため、戦闘が終了しました。");
        battleBossRemoved = config.getString("battle.boss-removed", "&c{boss_name}&rが消滅したため、戦闘が終了しました。");
        battleSurvival = config.getString("battle.survival", "&6おめでとう！ &e{boss_name}&rを&a{time}&r耐え抜きました！");

        // Chain battle messages
        chainBattleNextBoss = config.getString("chain-battle.next-boss", "&e次のボス: &c{boss_name}&rが出現！ ({current_boss}/{total_bosses})");

        // Time warnings
        timeWarningDefault = config.getString("time-warnings.default", "&e残り時間: &c{time}秒");

        // Item collection
        itemCollectionStart = config.getString("item-collection.start", "&aボスを討伐しました！&e{time}秒&r以内にアイテムを回収してください。");
        itemCollectionWarning = config.getString("item-collection.warning", "&e残り&c{time}秒&rでアイテム回収時間が終了します。");
        itemCollectionEnd = config.getString("item-collection.end", "&rアイテム回収時間が終了しました。");
        itemCollectionLeaveHint = config.getString("item-collection.leave-hint", "&7[&a/boss leave&7] をクリックして今すぐ離脱");
        itemCollectionLeaveHover = config.getString("item-collection.leave-hover", "&eクリックして離脱コマンドを実行");

        // Ranking (use {boss_name_plain} for color-stripped name, {boss_name} for colored)
        rankingHeader = config.getString("ranking.header", "&6=== {boss_name_plain} ランキング ===");
        rankingEntry = config.getString("ranking.entry", "&e{rank}. &f{player} &7- &a{time}");
        rankingNoRecords = config.getString("ranking.no-records", "&7まだ記録がありません。");
        rankingYourRank = config.getString("ranking.your-rank", "&eあなたの順位: &f{rank}位 &7(&a{time}&7)");
        rankingNotRanked = config.getString("ranking.not-ranked", "&7あなたはまだランキングに登録されていません。");

        // Command messages
        commandNoPermission = config.getString("command.no-permission", "&cこのコマンドを実行する権限がありません。");
        commandPlayerOnly = config.getString("command.player-only", "&cこのコマンドはプレイヤーのみ実行できます。");
        commandInvalidBoss = config.getString("command.invalid-boss", "&c指定されたボスが見つかりません: {boss}");
        commandAlreadyInBattle = config.getString("command.already-in-battle", "&cすでにボス戦に参加しています。");
        commandBattleStarted = config.getString("command.battle-started", "&a{player}が{boss_name}との戦闘を開始しました。");
        commandBattleStopped = config.getString("command.battle-stopped", "&eボス戦を強制終了しました。");
        commandConfigReloaded = config.getString("command.config-reloaded", "&a設定を再読み込みしました。");
        commandNotInBattle = config.getString("command.not-in-battle", "&cボス戦に参加していません。");
        commandBossList = config.getString("command.boss-list", "&6利用可能なボス: &f{bosses}");
        commandBossInUse = config.getString("command.boss-in-use", "&c{boss_name}&rは現在他のプレイヤーが挑戦中です。");
        commandBossDisabled = config.getString("command.boss-disabled", "&cこのボスは現在無効化されています: {boss}");
        commandLeaveNotAllowed = config.getString("command.leave-not-allowed", "&cアイテム回収時間中のみ離脱できます。");
        commandLeaveSuccess = config.getString("command.leave-success", "&a離脱しました。");
        commandBlocked = config.getString("command.blocked", "&cボス戦中はこのコマンドを使用できません。");

        // Prefix
        prefix = config.getString("prefix", "&8[&6MofuBossBattle&8] ");
    }

    // Getters
    public String getBattleStart() {
        return battleStart;
    }

    public String getBattleVictory() {
        return battleVictory;
    }

    public String getBattleDefeat() {
        return battleDefeat;
    }

    public String getBattleTimeout() {
        return battleTimeout;
    }

    public String getBattleLogout() {
        return battleLogout;
    }

    public String getBattleBossRemoved() {
        return battleBossRemoved;
    }

    public String getBattleSurvival() {
        return battleSurvival;
    }

    public String getChainBattleNextBoss() {
        return chainBattleNextBoss;
    }

    public String getTimeWarningDefault() {
        return timeWarningDefault;
    }

    public String getItemCollectionStart() {
        return itemCollectionStart;
    }

    public String getItemCollectionWarning() {
        return itemCollectionWarning;
    }

    public String getItemCollectionEnd() {
        return itemCollectionEnd;
    }

    public String getItemCollectionLeaveHint() {
        return itemCollectionLeaveHint;
    }

    public String getItemCollectionLeaveHover() {
        return itemCollectionLeaveHover;
    }

    public String getRankingHeader() {
        return rankingHeader;
    }

    public String getRankingEntry() {
        return rankingEntry;
    }

    public String getRankingNoRecords() {
        return rankingNoRecords;
    }

    public String getRankingYourRank() {
        return rankingYourRank;
    }

    public String getRankingNotRanked() {
        return rankingNotRanked;
    }

    public String getCommandNoPermission() {
        return commandNoPermission;
    }

    public String getCommandPlayerOnly() {
        return commandPlayerOnly;
    }

    public String getCommandInvalidBoss() {
        return commandInvalidBoss;
    }

    public String getCommandAlreadyInBattle() {
        return commandAlreadyInBattle;
    }

    public String getCommandBattleStarted() {
        return commandBattleStarted;
    }

    public String getCommandBattleStopped() {
        return commandBattleStopped;
    }

    public String getCommandConfigReloaded() {
        return commandConfigReloaded;
    }

    public String getCommandNotInBattle() {
        return commandNotInBattle;
    }

    public String getCommandBossList() {
        return commandBossList;
    }

    public String getCommandBossInUse() {
        return commandBossInUse;
    }

    public String getCommandBossDisabled() {
        return commandBossDisabled;
    }

    public String getCommandLeaveNotAllowed() {
        return commandLeaveNotAllowed;
    }

    public String getCommandLeaveSuccess() {
        return commandLeaveSuccess;
    }

    public String getCommandBlocked() {
        return commandBlocked;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String withPrefix(String message) {
        return prefix + message;
    }
}
