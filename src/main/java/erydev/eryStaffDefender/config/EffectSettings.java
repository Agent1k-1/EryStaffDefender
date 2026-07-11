package erydev.eryStaffDefender.config;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public final class EffectSettings {

    private final boolean blindness;
    private final boolean slowness;
    private final boolean darkness;
    private final boolean invisibility;
    private final int amplifier;

    public EffectSettings(boolean blindness, boolean slowness, boolean darkness, boolean invisibility, int amplifier) {
        this.blindness = blindness;
        this.slowness = slowness;
        this.darkness = darkness;
        this.invisibility = invisibility;
        this.amplifier = amplifier;
    }

    public boolean blindness() {
        return blindness;
    }

    public boolean slowness() {
        return slowness;
    }

    public boolean darkness() {
        return darkness;
    }

    public boolean invisibility() {
        return invisibility;
    }

    public int amplifier() {
        return amplifier;
    }

    public static @NotNull EffectSettings from(@NotNull ConfigurationSection section) {
        return new EffectSettings(
                section.getBoolean("blindness", false),
                section.getBoolean("slowness", false),
                section.getBoolean("darkness", false),
                section.getBoolean("invisibility", false),
                section.getInt("effect-amplifier", 0));
    }
}
