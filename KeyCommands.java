package erydev.eryStaffDefender.commands;

import erydev.eryStaffDefender.EryStaffDefender;
import erydev.eryStaffDefender.utils.SchedulerUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public final class KeyCommands implements CommandExecutor, TabCompleter {

    private final EryStaffDefender plugin;

    public KeyCommands(EryStaffDefender plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }
        Player player = (Player) sender;
        String name = command.getName().toLowerCase();
        switch (name) {
            case "setkey":
                return handleSetKey(player, args);
            case "key":
                return handleKey(player, args);
            case "changekey":
                return handleChangeKey(player, args);
            default:
                return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }

    private boolean handleSetKey(Player player, String[] args) {
        if (!player.hasPermission("erystaffdefender.staff")) {
            plugin.messages().send(player, "not-staff");
            return true;
        }
        if (args.length != 1) {
            plugin.messages().send(player, "usage-setkey");
            return true;
        }
        if (plugin.getDatabase().hasKey(player.getUniqueId())) {
            plugin.messages().send(player, "key-already-exists");
            return true;
        }
        int min = plugin.getConfig().getInt("security.min-key-length");
        if (args[0].length() < min) {
            plugin.messages().send(player, "key-too-short", "%min%", String.valueOf(min));
            return true;
        }
        plugin.getDatabase().setKey(player.getUniqueId(), player.getName(), args[0]);
        plugin.auth().unlock(player);
        plugin.auth().markAuthenticated(player);
        plugin.messages().send(player, "key-created");
        return true;
    }

    private boolean handleKey(Player player, String[] args) {
        if (!player.hasPermission("erystaffdefender.staff")) {
            plugin.messages().send(player, "not-staff");
            return true;
        }
        if (args.length != 1) {
            plugin.messages().send(player, "usage-key");
            return true;
        }
        if (!plugin.getDatabase().hasKey(player.getUniqueId())) {
            plugin.messages().send(player, "key-not-set");
            return true;
        }
        if (!plugin.auth().isLocked(player.getUniqueId())) {
            plugin.messages().send(player, "key-accepted");
            return true;
        }
        if (plugin.getDatabase().checkKey(player.getUniqueId(), args[0])) {
            plugin.auth().unlock(player);
            plugin.auth().markAuthenticated(player);
            plugin.messages().send(player, "key-accepted");
            return true;
        }
        int used = plugin.auth().registerFailedAttempt(player.getUniqueId());
        int left = plugin.auth().attemptsLeft(player.getUniqueId());
        plugin.messages().send(player, "key-wrong", "%attempts%", String.valueOf(left));
        int max = plugin.getConfig().getInt("security.max-attempts");
        if (used >= max && plugin.getConfig().getBoolean("security.kick-on-max-attempts")) {
            String reason = plugin.messages().plain("kick-max-attempts");
            SchedulerUtil.runOnEntity(plugin, player, () -> player.kickPlayer(reason));
        }
        return true;
    }

    private boolean handleChangeKey(Player player, String[] args) {
        if (!player.hasPermission("erystaffdefender.staff")) {
            plugin.messages().send(player, "not-staff");
            return true;
        }
        if (args.length != 2) {
            plugin.messages().send(player, "usage-changekey");
            return true;
        }
        if (!plugin.getDatabase().hasKey(player.getUniqueId())) {
            plugin.messages().send(player, "key-not-set");
            return true;
        }
        if (!plugin.getDatabase().checkKey(player.getUniqueId(), args[0])) {
            plugin.messages().send(player, "key-wrong", "%attempts%", "-");
            return true;
        }
        int min = plugin.getConfig().getInt("security.min-key-length");
        if (args[1].length() < min) {
            plugin.messages().send(player, "key-too-short", "%min%", String.valueOf(min));
            return true;
        }
        plugin.getDatabase().setKey(player.getUniqueId(), player.getName(), args[1]);
        if (plugin.getConfig().getBoolean("sessions.reset-on-key-change")) {
            plugin.getDatabase().clearSessions(player.getUniqueId());
        }
        plugin.auth().markAuthenticated(player);
        plugin.messages().send(player, "key-changed");
        return true;
    }
}
