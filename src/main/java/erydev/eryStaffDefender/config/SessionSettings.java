package erydev.eryStaffDefender.config;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public final class SessionSettings {

    private final boolean enabled;
    private final long durationSeconds;
    private final boolean resetOnKeyChange;

    public SessionSettings(boolean enabled, long durationSeconds, boolean resetOnKeyChange) {
        this.enabled = enabled;
        this.durationSeconds = durationSeconds;
        this.resetOnKeyChange = resetOnKeyChange;
    }

    public boolean enabled() {
        return enabled;
    }

    public long durationSeconds() {
        return durationSeconds;
    }

    public long durationMillis() {
        return durationSeconds * 1000L;
    }

    public boolean resetOnKeyChange() {
        return resetOnKeyChange;
    }

    public static @NotNull SessionSettings from(@NotNull ConfigurationSection section) {
        return new SessionSettings(
                section.getBoolean("enabled", false),
                section.getLong("duration-seconds", 3600L),
                section.getBoolean("reset-on-key-change", true));
    }
}
