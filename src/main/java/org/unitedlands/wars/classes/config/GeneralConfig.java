package org.unitedlands.wars.classes.config;

import org.unitedlands.annotations.UnitedConfig;
import org.unitedlands.annotations.UnitedSection;
import org.unitedlands.annotations.UnitedSetting;
import org.unitedlands.registrars.config.UnitedConfigHandler;
import org.unitedlands.registrars.config.UnitedConfigs;
import org.unitedlands.registrars.config.UnitedDynamicSection;
import org.unitedlands.unitedlands.classes.configs.GeneralConfig.GeopolAttributeDefaults;

@UnitedConfig(file = "config.yml") // file property möglich, wenn config z.B. "settings.yml" heißen soll
public interface GeneralConfig extends UnitedConfigHandler  {

    static GeneralConfig get() { return UnitedConfigs.get(GeneralConfig.class); } // Pflicht

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


    @UnitedSection(key = "geopol-attribute-defaults")
    UnitedDynamicSection<GeopolAttributeDefaults> geopolAttributeDefaults();
    
}
