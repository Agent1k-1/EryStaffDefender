package erydev.eryStaffDefender.managers;

import erydev.eryStaffDefender.EryStaffDefender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AuthManager {

    private final EryStaffDefender plugin;
    private final Map<UUID, Integer> attempts = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> locked = new ConcurrentHashMap<>();

    public AuthManager(EryStaffDefender plugin) {
        this.plugin = plugin;
    }

    public boolean isLocked(UUID uuid) {
        return locked.getOrDefault(uuid, false);
    }

    public void lock(Player player) {
        locked.put(player.getUniqueId(), true);
        attempts.put(player.getUniqueId(), 0);
        applyEffects(player);
        if (plugin.getConfig().getBoolean("locked.invulnerable")) {
            player.setInvulnerable(true);
        }
        promptTitle(player);
        FileConfiguration cfg = plugin.getConfig();
        if (plugin.getDatabase().hasKey(player.getUniqueId())) {
            plugin.messages().send(player, "need-key");
        } else {
            plugin.messages().send(player, "need-setkey");
        }
    }

    public void unlock(Player player) {
        locked.remove(player.getUniqueId());
        attempts.remove(player.getUniqueId());
        clearEffects(player);
        if (plugin.getConfig().getBoolean("locked.invulnerable")) {
            player.setInvulnerable(false);
        }
        clearTitle(player);
    }

    public void forget(UUID uuid) {
        locked.remove(uuid);
        attempts.remove(uuid);
    }

    public String getIp(Player player) {
        if (player.getAddress() == null || player.getAddress().getAddress() == null) {
            return "unknown";
        }
        return player.getAddress().getAddress().getHostAddress();
    }

    public boolean hasValidSession(Player player) {
        if (!plugin.getConfig().getBoolean("sessions.enabled")) {
            return false;
        }
        long duration = plugin.getConfig().getLong("sessions.duration-seconds", 3600L) * 1000L;
        long last = plugin.getDatabase().getSessionTime(player.getUniqueId(), getIp(player));
        return last > 0 && (System.currentTimeMillis() - last) <= duration;
    }

    public void markAuthenticated(Player player) {
        if (plugin.getConfig().getBoolean("sessions.enabled", true)) {
            plugin.getDatabase().saveSession(player.getUniqueId(), getIp(player), System.currentTimeMillis());
        }
    }

    public int registerFailedAttempt(UUID uuid) {
        int value = attempts.getOrDefault(uuid, 0) + 1;
        attempts.put(uuid, value);
        return value;
    }

    public int attemptsLeft(UUID uuid) {
        int max = plugin.getConfig().getInt("security.max-attempts");
        return Math.max(0, max - attempts.getOrDefault(uuid, 0));
    }

    private void applyEffects(Player player) {
        FileConfiguration cfg = plugin.getConfig();
        int amp = cfg.getInt("effects.effect-amplifier");
        if (cfg.getBoolean("effects.blindness")) {
            addEffect(player, "BLINDNESS", amp);
        }
        if (cfg.getBoolean("effects.slowness")) {
            addEffect(player, "SLOW", amp);
        }
        if (cfg.getBoolean("effects.darkness")) {
            addEffect(player, "DARKNESS", amp);
        }
        if (cfg.getBoolean("effects.invisibility")) {
            addEffect(player, "INVISIBILITY", amp);
        }
    }

    private void addEffect(Player player, String name, int amp) {
        PotionEffectType type = PotionEffectType.getByName(name);
        if (type == null) {
            return;
        }
        player.addPotionEffect(new PotionEffect(type, Integer.MAX_VALUE, amp, false, false, false));
    }

    private void clearEffects(Player player) {
        removeEffect(player, "BLINDNESS");
        removeEffect(player, "SLOW");
        removeEffect(player, "DARKNESS");
        removeEffect(player, "INVISIBILITY");
    }

    private void removeEffect(Player player, String name) {
        PotionEffectType type = PotionEffectType.getByName(name);
        if (type != null) {
            player.removePotionEffect(type);
        }
    }

    public void promptTitle(Player player) {
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.getBoolean("titles.enabled")) {
            return;
        }
        boolean needKey = plugin.getDatabase().hasKey(player.getUniqueId());
        String base = needKey ? "need-key" : "need-setkey";
        String title = plugin.messages().title(base, "title");
        String subtitle = plugin.messages().title(base, "subtitle");
        int fadeIn = cfg.getInt("titles.fade-in");
        int stay = cfg.getInt("titles.stay");
        int fadeOut = cfg.getInt("titles.fade-out");
        try {
            player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
        } catch (NoSuchMethodError legacy) {
            player.sendTitle(title, subtitle);
        }
    }

    private void clearTitle(Player player) {
        try {
            player.resetTitle();
        } catch (Throwable ignored) {
        }
    }
}
