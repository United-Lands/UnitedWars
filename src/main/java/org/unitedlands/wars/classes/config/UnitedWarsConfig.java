package org.unitedlands.wars.classes.config;

import java.util.List;

import org.unitedlands.annotations.UnitedConfig;
import org.unitedlands.annotations.UnitedSection;
import org.unitedlands.annotations.UnitedSetting;
import org.unitedlands.registrars.config.UnitedConfigHandler;
import org.unitedlands.registrars.config.UnitedConfigs;
import org.unitedlands.registrars.config.UnitedDynamicSection;
import org.unitedlands.unitedlands.classes.configs.GeneralConfig.GeopolAttributeDefaults;

@UnitedConfig(file = "config.yml") 
public interface UnitedWarsConfig extends UnitedConfigHandler  {

    static UnitedWarsConfig get() { return UnitedConfigs.get(UnitedWarsConfig.class); } // Pflicht

    // Einfacher "developer-mode: true/false" in yaml
    @UnitedSetting(key = "developer-mode", def = "false") 
    boolean developerMode();

    @UnitedSection(key = "mysql")
    MysqlSettings mysql();

    record MysqlSettings(
            @UnitedSetting(key = "host",     def = "localhost")     String host,
            @UnitedSetting(key = "port",     def = "3306")          int    port,
            @UnitedSetting(key = "username", def = "unitedlands")   String username,
            @UnitedSetting(key = "password", def = "unitedlands")   String password,
            @UnitedSetting(key = "database", def = "unitedlands")   String database
    ) {}

    @UnitedSetting(key = "warscheduler-check-interval", def = "15")
    long warSchedulerCheckInterval();

    @UnitedSection(key = "geopol-attribute-defaults")
    UnitedDynamicSection<GeopolAttributeDefaults> geopolAttributeDefaults();
    
    @UnitedSetting(key = "world-blacklist")
    List<String> worldBlacklist();

    @UnitedSection(key = "war-goal-settings")
    UnitedDynamicSection<WarGoalSetting> warGoalSettings();

    record WarGoalSetting(
        @UnitedSetting(key = "required-rank", def = "")       String requiredRank,
        @UnitedSetting(key = "warmup-time",   def = "86400")  long   warmupTime,
        @UnitedSetting(key = "duration",      def = "259200") long   duration,
        @UnitedSetting(key = "cost",          def = "30")     int    cost,
        @UnitedSetting(key = "war-lives",     def = "5")      int    warLives,
        @UnitedSection(key = "scorecaps")                     UnitedDynamicSection<GeneralIntValueSetting> scoreCaps
    ) { }

    @UnitedSection (key = "military-ranks")
    UnitedDynamicSection<MilitaryRankSetting> militaryRanks();

    record MilitaryRankSetting(
        @UnitedSetting(key = "score-multiplier", def = "1.0")           double  scoreMultiplier,
        @UnitedSetting(key = "level",            def = "settlement")    String  level,
        @UnitedSetting(key = "unique",           def = "false")         boolean unique
    ) { }

    @UnitedSection (key = "siege-settings")
    SiegeSettings siegeSettings();

    record SiegeSettings(
        @UnitedSetting(key = "capture-markers",                 def = "")       CaptureMarkerSettings   captureMarkers,
        @UnitedSetting(key = "warcamp-templates",               def = "5")      int                     warcampTemplates,
        @UnitedSetting(key = "override-activity-requirement",   def = "false")  boolean                 overrideActivityRequirement,
        @UnitedSetting(key = "require-siege-from-border",       def = "true")   boolean                 requireSiegeFromBorder,
        @UnitedSetting(key = "use-superiority-multiplier",      def = "true")   boolean                 useSuperiorityMultiplier,
        @UnitedSetting(key = "health-decay-rate",               def = "1")      int                     healthDecayRate,
        @UnitedSetting(key = "health-restore-rate",             def = "1")      int                     healthRestoreRate,
        @UnitedSection(key = "chunk-max-health")                                UnitedDynamicSection<GeneralIntValueSetting> chunkMaxHealth,
        @UnitedSetting(key = "disabled-commands",               def = "")       List<String> disabledCommands
    ) { }

    record CaptureMarkerSettings(
        @UnitedSetting(key = "use",         def = "true")       boolean use,
        @UnitedSetting(key = "layer-key",   def = "unitedwars") String  layerKey,
        @UnitedSetting(key = "layer-name",  def = "Wars")       String  layerName,
        @UnitedSetting(key = "marker",      def = "siege")      String  marker
    ) { }

    record GeneralIntValueSetting(
        @UnitedSetting(key = "value", def = "1") int val
    ) { }


    
    @UnitedSection (key = "score-settings")
    ScoreSettings scoreSettings();

    record ScoreSettings(
        @UnitedSetting(key = "activity",        def = "1")  int                                             activity,
        @UnitedSetting(key = "pvp-kill",        def = "")   PvpKillScoreSettings                             pvpKills,
        @UnitedSection(key = "chunk-capture")               UnitedDynamicSection<GeneralIntValueSetting>    chunkCapture

    ) { }

    record PvpKillScoreSettings(
        @UnitedSetting(key = "leader-kill-bonus-multiplier", def = "1") double leaderKillMultiplier,
        @UnitedSection(key = "rank-scores")                             UnitedDynamicSection<GeneralIntValueSetting> rankScores
    ) { }

    @UnitedSection(key = "notification-settings")
    UnitedDynamicSection<ScoreNotificationSetting> notificationSettings();

    record ScoreNotificationSetting(
        @UnitedSetting(key = "silent",  def = "false")          boolean silent,
        @UnitedSetting(key = "message", def = "score-default")  String message
    ) { }

    @UnitedSection (key = "war-book")
    WarBookSettings warBook();

    record WarBookSettings(
        @UnitedSetting(key = "name",    def = "Unnamed War")        String          name,
        @UnitedSetting(key = "content", def = "(Missing content)")  String          content,
        @UnitedSetting(key = "lore",    def = "")                   List<String>    lore
    ) { }



}
