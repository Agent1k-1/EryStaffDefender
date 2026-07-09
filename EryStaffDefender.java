package erydev.eryStaffDefender;

import erydev.eryStaffDefender.commands.AdminCommand;
import erydev.eryStaffDefender.commands.KeyCommands;
import erydev.eryStaffDefender.lang.Messages;
import erydev.eryStaffDefender.listeners.LockListener;
import erydev.eryStaffDefender.managers.AuthManager;
import erydev.eryStaffDefender.storage.KeyDatabase;
import erydev.eryStaffDefender.utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.logging.Logger;

import static javax.sql.rowset.spi.SyncFactory.getLogger;

public final class EryStaffDefender extends JavaPlugin {

    private KeyDatabase database;
    private AuthManager auth;
    private Messages messages;

    @Override
    public void onEnable() {
           int pluginId = 32482;
        Metrics metrics = new Metrics(this, pluginId);

        // Optional: Add custom charts
        metrics.addCustomChart(
            new Metrics.SimplePie("chart_id", () -> "My value")
        );
                printBanner();
        saveDefaultConfig();
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

        KeyCommands keyCommands = new KeyCommands(this);
        getCommand("setkey").setExecutor(keyCommands);
        getCommand("key").setExecutor(keyCommands);
        getCommand("changekey").setExecutor(keyCommands);
        getCommand("setkey").setTabCompleter(keyCommands);
        getCommand("key").setTabCompleter(keyCommands);
        getCommand("changekey").setTabCompleter(keyCommands);

        AdminCommand adminCommand = new AdminCommand(this);
        getCommand("erystaffdefender").setExecutor(adminCommand);
        getCommand("erystaffdefender").setTabCompleter(adminCommand);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("erystaffdefender.staff") && !database.isSkip(player.getUniqueId())) {
                auth.lock(player);
            }
        }

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

    public KeyDatabase getDatabase() {
        return database;
    }

    public AuthManager auth() {
        return auth;
    }

    public Messages messages() {
        return messages;
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
