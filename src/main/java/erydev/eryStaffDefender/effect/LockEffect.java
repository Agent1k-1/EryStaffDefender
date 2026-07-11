package erydev.eryStaffDefender.effect;

import erydev.eryStaffDefender.config.EffectSettings;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public enum LockEffect {

    BLINDNESS("BLINDNESS", EffectSettings::blindness),
    SLOWNESS("SLOW", EffectSettings::slowness),
    DARKNESS("DARKNESS", EffectSettings::darkness),
    INVISIBILITY("INVISIBILITY", EffectSettings::invisibility);

    private final String typeName;
    private final Predicate<EffectSettings> selector;

    LockEffect(@NotNull String typeName, @NotNull Predicate<EffectSettings> selector) {
        this.typeName = typeName;
        this.selector = selector;
    }

    public void applyIfEnabled(@NotNull Player player, @NotNull EffectSettings settings) {
        PotionEffectType type = PotionEffectType.getByName(typeName);
        if (type != null && selector.test(settings)) {
            player.addPotionEffect(new PotionEffect(type, Integer.MAX_VALUE, settings.amplifier(), false, false, false));
        }
    }

    public void remove(@NotNull Player player) {
        PotionEffectType type = PotionEffectType.getByName(typeName);
        if (type != null) {
            player.removePotionEffect(type);
        }
    }
}
