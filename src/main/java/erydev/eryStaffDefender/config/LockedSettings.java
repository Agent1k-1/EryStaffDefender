package erydev.eryStaffDefender.config;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class LockedSettings {

    private final boolean freezeMovement;
    private final boolean allowHeadRotation;
    private final boolean blockChat;
    private final boolean blockCommands;
    private final Set<String> allowedCommands;
    private final boolean blockInteract;
    private final boolean blockBlockBreak;
    private final boolean blockBlockPlace;
    private final boolean blockItemDrop;
    private final boolean blockItemPickup;
    private final boolean blockInventory;
    private final boolean blockDamage;
    private final boolean invulnerable;

    public LockedSettings(boolean freezeMovement, boolean allowHeadRotation, boolean blockChat, boolean blockCommands,
                          @NotNull Set<String> allowedCommands, boolean blockInteract, boolean blockBlockBreak,
                          boolean blockBlockPlace, boolean blockItemDrop, boolean blockItemPickup, boolean blockInventory,
                          boolean blockDamage, boolean invulnerable) {
        this.freezeMovement = freezeMovement;
        this.allowHeadRotation = allowHeadRotation;
        this.blockChat = blockChat;
        this.blockCommands = blockCommands;
        this.allowedCommands = allowedCommands;
        this.blockInteract = blockInteract;
        this.blockBlockBreak = blockBlockBreak;
        this.blockBlockPlace = blockBlockPlace;
        this.blockItemDrop = blockItemDrop;
        this.blockItemPickup = blockItemPickup;
        this.blockInventory = blockInventory;
        this.blockDamage = blockDamage;
        this.invulnerable = invulnerable;
    }

    public boolean freezeMovement() {
        return freezeMovement;
    }

    public boolean allowHeadRotation() {
        return allowHeadRotation;
    }

    public boolean blockChat() {
        return blockChat;
    }

    public boolean blockCommands() {
        return blockCommands;
    }

    public boolean commandAllowed(@NotNull String label) {
        return allowedCommands.contains(label);
    }

    public boolean blockInteract() {
        return blockInteract;
    }

    public boolean blockBlockBreak() {
        return blockBlockBreak;
    }

    public boolean blockBlockPlace() {
        return blockBlockPlace;
    }

    public boolean blockItemDrop() {
        return blockItemDrop;
    }

    public boolean blockItemPickup() {
        return blockItemPickup;
    }

    public boolean blockInventory() {
        return blockInventory;
    }

    public boolean blockDamage() {
        return blockDamage;
    }

    public boolean invulnerable() {
        return invulnerable;
    }

    public static @NotNull LockedSettings from(@NotNull ConfigurationSection section) {
        Set<String> allowed = new HashSet<>();
        for (String command : section.getStringList("allow-commands")) {
            allowed.add(command.toLowerCase(Locale.ROOT));
        }
        return new LockedSettings(
                section.getBoolean("freeze-movement", true),
                section.getBoolean("allow-head-rotation", true),
                section.getBoolean("block-chat", true),
                section.getBoolean("block-commands", true),
                Collections.unmodifiableSet(allowed),
                section.getBoolean("block-interact", true),
                section.getBoolean("block-block-break", true),
                section.getBoolean("block-block-place", true),
                section.getBoolean("block-item-drop", true),
                section.getBoolean("block-item-pickup", true),
                section.getBoolean("block-inventory", true),
                section.getBoolean("block-damage", true),
                section.getBoolean("invulnerable", true));
    }
}
