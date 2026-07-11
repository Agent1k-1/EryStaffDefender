package erydev.eryStaffDefender.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

public final class PluginSettings {

    private final String language;
    private final StaffSettings staff;
    private final SecuritySettings security;
    private final SessionSettings sessions;
    private final LockedSettings locked;
    private final EffectSettings effects;
    private final TitleSettings titles;

    public PluginSettings(@NotNull String language, @NotNull StaffSettings staff, @NotNull SecuritySettings security,
                          @NotNull SessionSettings sessions, @NotNull LockedSettings locked, @NotNull EffectSettings effects,
                          @NotNull TitleSettings titles) {
        this.language = language;
        this.staff = staff;
        this.security = security;
        this.sessions = sessions;
        this.locked = locked;
        this.effects = effects;
        this.titles = titles;
    }

    public @NotNull String language() {
        return language;
    }

    public @NotNull StaffSettings staff() {
        return staff;
    }

    public @NotNull SecuritySettings security() {
        return security;
    }

    public @NotNull SessionSettings sessions() {
        return sessions;
    }

    public @NotNull LockedSettings locked() {
        return locked;
    }

    public @NotNull EffectSettings effects() {
        return effects;
    }

    public @NotNull TitleSettings titles() {
        return titles;
    }

    public static @NotNull PluginSettings from(@NotNull FileConfiguration config) {
        return new PluginSettings(
                config.getString("language", "en"),
                StaffSettings.from(section(config, "staff")),
                SecuritySettings.from(section(config, "security")),
                SessionSettings.from(section(config, "sessions")),
                LockedSettings.from(section(config, "locked")),
                EffectSettings.from(section(config, "effects")),
                TitleSettings.from(section(config, "titles")));
    }

    private static @NotNull ConfigurationSection section(@NotNull FileConfiguration config, @NotNull String name) {
        ConfigurationSection section = config.getConfigurationSection(name);
        return section != null ? section : config.createSection(name);
    }
}
