package erydev.eryStaffDefender.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public final class SchedulerUtil {

    private static final boolean FOLIA = detectFolia();

    private SchedulerUtil() {
    }

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    public static void runOnEntity(Plugin plugin, Player player, Runnable task) {
        if (!FOLIA) {
            if (Bukkit.isPrimaryThread()) {
                task.run();
            } else {
                Bukkit.getScheduler().runTask(plugin, task);
            }
            return;
        }
        try {
            Object entityScheduler = player.getClass().getMethod("getScheduler").invoke(player);
            Method execute = entityScheduler.getClass().getMethod("execute", Plugin.class, Runnable.class, Runnable.class, long.class);
            execute.invoke(entityScheduler, plugin, task, null, 1L);
        } catch (Exception e) {
            task.run();
        }
    }

    public static void runAsync(Plugin plugin, Runnable task) {
        if (!FOLIA) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
            return;
        }
        try {
            Object asyncScheduler = Bukkit.getServer().getClass().getMethod("getAsyncScheduler").invoke(Bukkit.getServer());
            Method run = asyncScheduler.getClass().getMethod("runNow", Plugin.class, java.util.function.Consumer.class);
            run.invoke(asyncScheduler, plugin, (java.util.function.Consumer<Object>) t -> task.run());
        } catch (Exception e) {
            task.run();
        }
    }

    public static void runGlobal(Plugin plugin, Runnable task) {
        if (!FOLIA) {
            Bukkit.getScheduler().runTask(plugin, task);
            return;
        }
        try {
            Object globalScheduler = Bukkit.getServer().getClass().getMethod("getGlobalRegionScheduler").invoke(Bukkit.getServer());
            Method run = globalScheduler.getClass().getMethod("run", Plugin.class, java.util.function.Consumer.class);
            run.invoke(globalScheduler, plugin, (java.util.function.Consumer<Object>) t -> task.run());
        } catch (Exception e) {
            task.run();
        }
    }
}
