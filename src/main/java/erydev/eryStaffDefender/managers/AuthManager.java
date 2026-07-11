package erydev.eryStaffDefender.managers;

import erydev.eryStaffDefender.EryStaffDefender;
import erydev.eryStaffDefender.config.EffectSettings;
import erydev.eryStaffDefender.config.SessionSettings;
import erydev.eryStaffDefender.config.TitleSettings;
import erydev.eryStaffDefender.effect.LockEffect;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AuthManager {

    private static final String STAFF_NODE = "erystaffdefender.staff";

    private final EryStaffDefender plugin;
    private final Map<UUID, Integer> attempts = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> locked = new ConcurrentHashMap<>();

    public AuthManager(@NotNull EryStaffDefender plugin) {
        this.plugin = plugin;
    }

    public boolean isStaff(@NotNull Player player) {
        return player.hasPermission(STAFF_NODE) || (plugin.settings().staff().includeOps() && player.isOp());
    }

    public boolean isLocked(@NotNull UUID uuid) {
        return locked.getOrDefault(uuid, false);
    }

    public void lock(@NotNull Player player, boolean hasKey) {
        locked.put(player.getUniqueId(), true);
        attempts.put(player.getUniqueId(), 0);
        applyEffects(player);
        if (plugin.settings().locked().invulnerable()) {
            player.setInvulnerable(true);
        }
        promptTitle(player, hasKey);
        plugin.messages().send(player, hasKey ? "need-key" : "need-setkey");
    }

    public void unlock(@NotNull Player player) {
        locked.remove(player.getUniqueId());
        attempts.remove(player.getUniqueId());
        clearEffects(player);
        if (plugin.settings().locked().invulnerable()) {
            player.setInvulnerable(false);
        }
        clearTitle(player);
    }

    public void forget(@NotNull UUID uuid) {
        locked.remove(uuid);
        attempts.remove(uuid);
    }

    public @NotNull String getIp(@NotNull Player player) {
        if (player.getAddress() == null || player.getAddress().getAddress() == null) {
            return "unknown";
        }
        return player.getAddress().getAddress().getHostAddress();
    }

    public boolean hasValidSession(@NotNull Player player) {
        SessionSettings sessions = plugin.settings().sessions();
        if (!sessions.enabled()) {
            return false;
        }
        long last = plugin.getDatabase().getSessionTime(player.getUniqueId(), getIp(player));
        return last > 0 && (System.currentTimeMillis() - last) <= sessions.durationMillis();
    }

    public void markAuthenticated(@NotNull Player player) {
        if (plugin.settings().sessions().enabled()) {
            plugin.getDatabase().saveSession(player.getUniqueId(), getIp(player), System.currentTimeMillis());
        }
    }

    public int registerFailedAttempt(@NotNull UUID uuid) {
        int value = attempts.getOrDefault(uuid, 0) + 1;
        attempts.put(uuid, value);
        return value;
    }

    public int attemptsLeft(@NotNull UUID uuid) {
        int max = plugin.settings().security().maxAttempts();
        return Math.max(0, max - attempts.getOrDefault(uuid, 0));
    }

    public void promptTitle(@NotNull Player player, boolean hasKey) {
        TitleSettings titles = plugin.settings().titles();
        if (!titles.enabled()) {
            return;
        }
        String base = hasKey ? "need-key" : "need-setkey";
        String title = plugin.messages().title(base, "title");
        String subtitle = plugin.messages().title(base, "subtitle");
        try {
            player.sendTitle(title, subtitle, titles.fadeIn(), titles.stay(), titles.fadeOut());
        } catch (NoSuchMethodError legacy) {
            player.sendTitle(title, subtitle);
        }
    }

    private void applyEffects(@NotNull Player player) {
        EffectSettings effects = plugin.settings().effects();
        for (LockEffect effect : LockEffect.values()) {
            effect.applyIfEnabled(player, effects);
        }
    }

    private void clearEffects(@NotNull Player player) {
        for (LockEffect effect : LockEffect.values()) {
            effect.remove(player);
        }
    }

    private void clearTitle(@NotNull Player player) {
        try {
            player.resetTitle();
        } catch (Throwable ignored) {
        }
    }
}
