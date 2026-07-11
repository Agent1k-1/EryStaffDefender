package erydev.eryStaffDefender.config;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public final class TitleSettings {

    private final boolean enabled;
    private final int fadeIn;
    private final int stay;
    private final int fadeOut;

    public TitleSettings(boolean enabled, int fadeIn, int stay, int fadeOut) {
        this.enabled = enabled;
        this.fadeIn = fadeIn;
        this.stay = stay;
        this.fadeOut = fadeOut;
    }

    public boolean enabled() {
        return enabled;
    }

    public int fadeIn() {
        return fadeIn;
    }

    public int stay() {
        return stay;
    }

    public int fadeOut() {
        return fadeOut;
    }

    public static @NotNull TitleSettings from(@NotNull ConfigurationSection section) {
        return new TitleSettings(
                section.getBoolean("enabled", true),
                section.getInt("fade-in", 10),
                section.getInt("stay", 70),
                section.getInt("fade-out", 20));
    }
}
