package erydev.eryStaffDefender.commands;

import erydev.eryStaffDefender.EryStaffDefender;
import erydev.eryStaffDefender.config.SecuritySettings;
import erydev.eryStaffDefender.utils.SchedulerUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class KeyCommands implements CommandExecutor, TabCompleter {

    private final EryStaffDefender plugin;

    public KeyCommands(@NotNull EryStaffDefender plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }
        Player player = (Player) sender;
        switch (command.getName().toLowerCase(Locale.ROOT)) {
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

    private boolean handleSetKey(@NotNull Player player, String[] args) {
        if (!plugin.auth().isStaff(player)) {
            plugin.messages().send(player, "not-staff");
            return true;
        }
        if (!plugin.settings().security().allowSelfSetKey()) {
            plugin.messages().send(player, "self-setkey-disabled");
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
        int min = plugin.settings().security().minKeyLength();
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

    private boolean handleKey(@NotNull Player player, String[] args) {
        if (!plugin.auth().isStaff(player)) {
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
        SecuritySettings security = plugin.settings().security();
        int used = plugin.auth().registerFailedAttempt(player.getUniqueId());
        int left = plugin.auth().attemptsLeft(player.getUniqueId());
        plugin.messages().send(player, "key-wrong", "%attempts%", String.valueOf(left));
        if (used >= security.maxAttempts() && security.kickOnMaxAttempts()) {
            String reason = plugin.messages().plain("kick-max-attempts");
            SchedulerUtil.runOnEntity(plugin, player, () -> player.kickPlayer(reason));
        }
        return true;
    }

    private boolean handleChangeKey(@NotNull Player player, String[] args) {
        if (!plugin.auth().isStaff(player)) {
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
        int min = plugin.settings().security().minKeyLength();
        if (args[1].length() < min) {
            plugin.messages().send(player, "key-too-short", "%min%", String.valueOf(min));
            return true;
        }
        plugin.getDatabase().setKey(player.getUniqueId(), player.getName(), args[1]);
        if (plugin.settings().sessions().resetOnKeyChange()) {
            plugin.getDatabase().clearSessions(player.getUniqueId());
        }
        plugin.auth().markAuthenticated(player);
        plugin.messages().send(player, "key-changed");
        return true;
    }
}
