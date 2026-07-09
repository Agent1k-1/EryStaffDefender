package erydev.eryStaffDefender.commands;

import erydev.eryStaffDefender.EryStaffDefender;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class AdminCommand implements CommandExecutor, TabCompleter {

    private final EryStaffDefender plugin;

    public AdminCommand(EryStaffDefender plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            plugin.messages().send(sender, "usage-admin");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload":
                return reload(sender);
            case "setkey":
                return setKey(sender, args);
            case "delkey":
                return delKey(sender, args);
            case "help":
                return help(sender);
            case "skip":
                return skip(sender, args);
            case "end":
                return end(sender, args);
            default:
                plugin.messages().send(sender, "usage-admin");
                return true;
        }
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission("erystaffdefender.reload")) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        plugin.reloadConfig();
        plugin.messages().load();
        plugin.messages().send(sender, "reloaded");
        return true;
    }

    private boolean setKey(CommandSender sender, String[] args) {
        if (!sender.hasPermission("erystaffdefender.setkey")) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        if (args.length != 3) {
            plugin.messages().send(sender, "usage-admin");
            return true;
        }
        OfflinePlayer target = resolve(args[1]);
        if (target == null) {
            plugin.messages().send(sender, "player-never-joined");
            return true;
        }
        int min = plugin.getConfig().getInt("security.min-key-length");
        if (args[2].length() < min) {
            plugin.messages().send(sender, "key-too-short", "%min%", String.valueOf(min));
            return true;
        }
        plugin.getDatabase().setKey(target.getUniqueId(), args[1], args[2]);
        if (plugin.getConfig().getBoolean("sessions.reset-on-key-change")) {
            plugin.getDatabase().clearSessions(target.getUniqueId());
        }
        plugin.messages().send(sender, "admin-key-set", "%player%", args[1]);
        return true;
    }

    private boolean delKey(CommandSender sender, String[] args) {
        if (!sender.hasPermission("erystaffdefender.delkey")) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        if (args.length != 2) {
            plugin.messages().send(sender, "usage-admin");
            return true;
        }
        OfflinePlayer target = resolve(args[1]);
        if (target == null) {
            plugin.messages().send(sender, "player-never-joined");
            return true;
        }
        if (!plugin.getDatabase().deleteKey(target.getUniqueId())) {
            plugin.messages().send(sender, "admin-no-key", "%player%", args[1]);
            return true;
        }
        // no key anymore, so drop any active sessions too, otherwise he stays logged in
        plugin.getDatabase().clearSessions(target.getUniqueId());
        plugin.messages().send(sender, "admin-key-deleted", "%player%", args[1]);
        return true;
    }

    private boolean help(CommandSender sender) {
        if (!sender.hasPermission("erystaffdefender.help")) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        for (String line : plugin.messages().rawList("help")) {
            sender.sendMessage(plugin.messages().color(line));
        }
        return true;
    }

    private boolean skip(CommandSender sender, String[] args) {
        if (!sender.hasPermission("erystaffdefender.skip")) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        if (args.length != 2) {
            plugin.messages().send(sender, "usage-admin");
            return true;
        }
        OfflinePlayer target = resolve(args[1]);
        if (target == null) {
            plugin.messages().send(sender, "player-never-joined");
            return true;
        }
        plugin.getDatabase().setSkip(target.getUniqueId(), args[1], true);
        plugin.messages().send(sender, "admin-skip-set", "%player%", args[1]);
        return true;
    }

    private boolean end(CommandSender sender, String[] args) {
        if (!sender.hasPermission("erystaffdefender.end")) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        if (args.length != 2) {
            plugin.messages().send(sender, "usage-admin");
            return true;
        }
        OfflinePlayer target = resolve(args[1]);
        if (target == null) {
            plugin.messages().send(sender, "player-never-joined");
            return true;
        }
        plugin.getDatabase().clearSessions(target.getUniqueId());
        plugin.messages().send(sender, "admin-sessions-ended", "%player%", args[1]);
        return true;
    }

    private OfflinePlayer resolve(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        if (offline.hasPlayedBefore() || offline.isOnline()) {
            return offline;
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(Arrays.asList("reload", "setkey", "delkey", "skip", "end", "help"));
            options.removeIf(o -> !o.startsWith(args[0].toLowerCase(Locale.ROOT)));
            return options;
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("setkey") || args[0].equalsIgnoreCase("delkey")
                || args[0].equalsIgnoreCase("skip") || args[0].equalsIgnoreCase("end"))) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    names.add(p.getName());
                }
            }
            return names;
        }
        return new ArrayList<>();
    }
}
