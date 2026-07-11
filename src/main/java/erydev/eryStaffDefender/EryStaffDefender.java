package erydev.eryStaffDefender;

import erydev.eryStaffDefender.commands.AdminCommand;
import erydev.eryStaffDefender.commands.KeyCommands;
import erydev.eryStaffDefender.config.PluginSettings;
import erydev.eryStaffDefender.lang.Messages;
import erydev.eryStaffDefender.listeners.LockListener;
import erydev.eryStaffDefender.managers.AuthManager;
import erydev.eryStaffDefender.storage.KeyDatabase;
import erydev.eryStaffDefender.utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.logging.Logger;

public final class EryStaffDefender extends JavaPlugin {

    private static final int METRICS_ID = 32482;

    private volatile PluginSettings settings;
    private KeyDatabase database;
    private AuthManager auth;
    private Messages messages;

    @Override
    public void onEnable() {
        printBanner();
        saveDefaultConfig();
        settings = PluginSettings.from(getConfig());
        new Metrics(this, METRICS_ID);

        database = new KeyDatabase(this);
        try {
            database.connect();
        } catch (SQLException e) {
            getLogger().severe("Could not open the key database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        messages = new Messages(this);
        auth = new AuthManager(this);

        getServer().getPluginManager().registerEvents(new LockListener(this), this);
        registerCommands();
        lockOnlineStaff();

        getLogger().info("EryStaffDefender enabled" + (SchedulerUtil.isFolia() ? " (Folia mode)." : "."));
    }

    @Override
    public void onDisable() {
        if (auth != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (auth.isLocked(player.getUniqueId())) {
                    auth.unlock(player);
                }
            }
        }
        if (database != null) {
            database.close();
        }
    }

    public void reloadAll() {
        reloadConfig();
        settings = PluginSettings.from(getConfig());
        messages.load();
    }

    public @NotNull PluginSettings settings() {
        return settings;
    }

    public @NotNull KeyDatabase getDatabase() {
        return database;
    }

    public @NotNull AuthManager auth() {
        return auth;
    }

    public @NotNull Messages messages() {
        return messages;
    }

    private void registerCommands() {
        KeyCommands keyCommands = new KeyCommands(this);
        for (String name : new String[]{"setkey", "key", "changekey"}) {
            getCommand(name).setExecutor(keyCommands);
            getCommand(name).setTabCompleter(keyCommands);
        }
        AdminCommand adminCommand = new AdminCommand(this);
        getCommand("erystaffdefender").setExecutor(adminCommand);
        getCommand("erystaffdefender").setTabCompleter(adminCommand);
    }

    private void lockOnlineStaff() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (auth.isStaff(player) && !database.isSkip(player.getUniqueId())) {
                auth.lock(player, database.hasKey(player.getUniqueId()));
            }
        }
    }

    private void printBanner() {
        Logger log = getLogger();
        log.info("");
        log.info("  ######  ######  ######");
        log.info("  ##      ##      ##   ##");
        log.info("  #####   ######  ##   ##");
        log.info("  ##          ##  ##   ##");
        log.info("  ######  ######  ######");
        log.info("");
        log.info("  EryDev");
        log.info("  кодер: Danchik");
        log.info("");
    }
}
