package erydev.eryStaffDefender.config;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public final class StaffSettings {

    private final boolean includeOps;

    public StaffSettings(boolean includeOps) {
        this.includeOps = includeOps;
    }

    public boolean includeOps() {
        return includeOps;
    }

    public static @NotNull StaffSettings from(@NotNull ConfigurationSection section) {
        return new StaffSettings(section.getBoolean("include-ops", true));
    }
}
