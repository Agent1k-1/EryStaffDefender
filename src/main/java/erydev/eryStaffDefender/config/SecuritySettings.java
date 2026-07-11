package erydev.eryStaffDefender.config;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public final class SecuritySettings {

    private final int maxAttempts;
    private final boolean kickOnMaxAttempts;
    private final int minKeyLength;
    private final boolean allowSelfSetKey;

    public SecuritySettings(int maxAttempts, boolean kickOnMaxAttempts, int minKeyLength, boolean allowSelfSetKey) {
        this.maxAttempts = maxAttempts;
        this.kickOnMaxAttempts = kickOnMaxAttempts;
        this.minKeyLength = minKeyLength;
        this.allowSelfSetKey = allowSelfSetKey;
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    public boolean kickOnMaxAttempts() {
        return kickOnMaxAttempts;
    }

    public int minKeyLength() {
        return minKeyLength;
    }

    public boolean allowSelfSetKey() {
        return allowSelfSetKey;
    }

    public static @NotNull SecuritySettings from(@NotNull ConfigurationSection section) {
        return new SecuritySettings(
                section.getInt("max-attempts", 3),
                section.getBoolean("kick-on-max-attempts", true),
                section.getInt("min-key-length", 4),
                section.getBoolean("allow-self-setkey", true));
    }
}
