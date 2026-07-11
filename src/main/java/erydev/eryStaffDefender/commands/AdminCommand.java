package erydev.eryStaffDefender.commands;

import erydev.eryStaffDefender.EryStaffDefender;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class AdminCommand implements CommandExecutor, TabCompleter {

    private final EryStaffDefender plugin;

    public AdminCommand(@NotNull EryStaffDefender plugin) {
        this.plugin = plugin;
    }

    @FunctionalInterface
    private interface Handler {
        boolean run(@NotNull AdminCommand ctx, @NotNull CommandSender sender, @NotNull String[] args);
    }

    private enum Sub {
        RELOAD("reload", "erystaffdefender.reload", false, AdminCommand::reload),
        SETKEY("setkey", "erystaffdefender.setkey", true, AdminCommand::setKey),
        DELKEY("delkey", "erystaffdefender.delkey", true, AdminCommand::delKey),
        SKIP("skip", "erystaffdefender.skip", true, AdminCommand::skip),
        END("end", "erystaffdefender.end", true, AdminCommand::end),
        HELP("help", "erystaffdefender.help", false, AdminCommand::help);

        private final String label;
        private final String permission;
        private final boolean targetsPlayer;
        private final Handler handler;

        Sub(String label, String permission, boolean targetsPlayer, Handler handler) {
            this.label = label;
            this.permission = permission;
            this.targetsPlayer = targetsPlayer;
            this.handler = handler;
        }

        static @NotNull Optional<Sub> byLabel(@NotNull String label) {
            return Arrays.stream(values()).filter(sub -> sub.label.equals(label)).findFirst();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            plugin.messages().send(sender, "usage-admin");
            return true;
        }
        Optional<Sub> match = Sub.byLabel(args[0].toLowerCase(Locale.ROOT));
        if (!match.isPresent()) {
            plugin.messages().send(sender, "usage-admin");
            return true;
        }
        Sub sub = match.get();
        if (!sender.hasPermission(sub.permission)) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        return sub.handler.run(this, sender, args);
    }

    private boolean reload(@NotNull CommandSender sender, @NotNull String[] args) {
        plugin.reloadAll();
        plugin.messages().send(sender, "reloaded");
        return true;
    }

    private boolean setKey(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length != 3) {
            plugin.messages().send(sender, "usage-admin");
            return true;
        }
        OfflinePlayer target = resolve(args[1]);
        if (target == null) {
            plugin.messages().send(sender, "player-never-joined");
            return true;
        }
        int min = plugin.settings().security().minKeyLength();
        if (args[2].length() < min) {
            plugin.messages().send(sender, "key-too-short", "%min%", String.valueOf(min));
            return true;
        }
        plugin.getDatabase().setKey(target.getUniqueId(), args[1], args[2]);
        if (plugin.settings().sessions().resetOnKeyChange()) {
            plugin.getDatabase().clearSessions(target.getUniqueId());
        }
        plugin.messages().send(sender, "admin-key-set", "%player%", args[1]);
        return true;
    }

    private boolean delKey(@NotNull CommandSender sender, @NotNull String[] args) {
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
        plugin.getDatabase().clearSessions(target.getUniqueId());
        plugin.messages().send(sender, "admin-key-deleted", "%player%", args[1]);
        return true;
    }

    private boolean skip(@NotNull CommandSender sender, @NotNull String[] args) {
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

    private boolean end(@NotNull CommandSender sender, @NotNull String[] args) {
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

    private boolean help(@NotNull CommandSender sender, @NotNull String[] args) {
        for (String line : plugin.messages().rawList("help")) {
            sender.sendMessage(plugin.messages().color(line));
        }
        return true;
    }

    private @Nullable OfflinePlayer resolve(@NotNull String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        return offline.hasPlayedBefore() || offline.isOnline() ? offline : null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> options = new ArrayList<>();
            for (Sub sub : Sub.values()) {
                if (sender.hasPermission(sub.permission) && sub.label.startsWith(prefix)) {
                    options.add(sub.label);
                }
            }
            return options;
        }
        if (args.length == 2) {
            Optional<Sub> match = Sub.byLabel(args[0].toLowerCase(Locale.ROOT));
            if (match.isPresent() && match.get().targetsPlayer && sender.hasPermission(match.get().permission)) {
                return onlineNames(args[1]);
            }
        }
        return new ArrayList<>();
    }

    private @NotNull List<String> onlineNames(@NotNull String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().toLowerCase(Locale.ROOT).startsWith(lower)) {
                names.add(player.getName());
            }
        }
        return names;
    }
}
