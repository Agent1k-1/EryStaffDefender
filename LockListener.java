package erydev.eryStaffDefender.listeners;

import erydev.eryStaffDefender.EryStaffDefender;
import erydev.eryStaffDefender.managers.AuthManager;
import erydev.eryStaffDefender.utils.SchedulerUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;

import java.util.List;
import java.util.Locale;

public final class LockListener implements Listener {

    private final EryStaffDefender plugin;

    public LockListener(EryStaffDefender plugin) {
        this.plugin = plugin;
    }

    private AuthManager auth() {
        return plugin.auth();
    }

    private boolean locked(Player player) {
        return auth().isLocked(player.getUniqueId());
    }

    private void deny(Player player) {
        plugin.messages().send(player, "locked-action");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("erystaffdefender.staff")) {
            return;
        }
        if (plugin.getDatabase().isSkip(player.getUniqueId())) {
            plugin.getDatabase().setSkip(player.getUniqueId(), player.getName(), false);
            return;
        }
        if (plugin.getDatabase().hasKey(player.getUniqueId()) && auth().hasValidSession(player)) {
            SchedulerUtil.runOnEntity(plugin, player, () -> {
                if (player.isOnline()) {
                    plugin.messages().send(player, "session-restored");
                }
            });
            return;
        }
        SchedulerUtil.runOnEntity(plugin, player, () -> {
            if (player.isOnline()) {
                auth().lock(player);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        auth().forget(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMove(PlayerMoveEvent event) {
        if (!locked(event.getPlayer())) {
            return;
        }
        if (!plugin.getConfig().getBoolean("locked.freeze-movement")) {
            return;
        }
        if (event.getTo() == null) {
            return;
        }
        boolean sameBlock = event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ();
        if (sameBlock) {
            return;
        }
        if (plugin.getConfig().getBoolean("locked.allow-head-rotation")) {
            org.bukkit.Location to = event.getFrom().clone();
            to.setYaw(event.getTo().getYaw());
            to.setPitch(event.getTo().getPitch());
            event.setTo(to);
        } else {
            event.setTo(event.getFrom());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (locked(event.getPlayer()) && plugin.getConfig().getBoolean("locked.block-chat")) {
            event.setCancelled(true);
            deny(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!locked(player) || !plugin.getConfig().getBoolean("locked.block-commands")) {
            return;
        }
        String raw = event.getMessage().substring(1).trim().toLowerCase(Locale.ROOT);
        String label = raw.split(" ")[0];
        List<String> allowed = plugin.getConfig().getStringList("locked.allow-commands");
        for (String entry : allowed) {
            if (label.equals(entry.toLowerCase(Locale.ROOT))) {
                return;
            }
        }
        event.setCancelled(true);
        deny(player);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if (locked(event.getPlayer()) && plugin.getConfig().getBoolean("locked.block-interact")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (locked(event.getPlayer()) && plugin.getConfig().getBoolean("locked.block-interact")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreak(BlockBreakEvent event) {
        if (locked(event.getPlayer()) && plugin.getConfig().getBoolean("locked.block-block-break")) {
            event.setCancelled(true);
            deny(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlace(BlockPlaceEvent event) {
        if (locked(event.getPlayer()) && plugin.getConfig().getBoolean("locked.block-block-place")) {
            event.setCancelled(true);
            deny(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrop(PlayerDropItemEvent event) {
        if (locked(event.getPlayer()) && plugin.getConfig().getBoolean("locked.block-item-drop")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (locked(player) && plugin.getConfig().getBoolean("locked.block-item-pickup")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getPlayer();
        if (locked(player) && plugin.getConfig().getBoolean("locked.block-inventory")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if (locked(player) && plugin.getConfig().getBoolean("locked.block-inventory")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (locked(player) && plugin.getConfig().getBoolean("locked.block-damage")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamageOthers(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            if (locked(player) && plugin.getConfig().getBoolean("locked.block-damage")) {
                event.setCancelled(true);
                deny(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (event.getTarget() instanceof Player) {
            Player player = (Player) event.getTarget();
            if (locked(player) && plugin.getConfig().getBoolean("locked.invulnerable")) {
                event.setCancelled(true);
            }
        }
    }
}
