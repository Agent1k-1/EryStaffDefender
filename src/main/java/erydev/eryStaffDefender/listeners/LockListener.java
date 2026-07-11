package erydev.eryStaffDefender.listeners;

import erydev.eryStaffDefender.EryStaffDefender;
import erydev.eryStaffDefender.config.LockedSettings;
import erydev.eryStaffDefender.managers.AuthManager;
import erydev.eryStaffDefender.utils.SchedulerUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
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
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class LockListener implements Listener {

    private final EryStaffDefender plugin;

    public LockListener(@NotNull EryStaffDefender plugin) {
        this.plugin = plugin;
    }

    private AuthManager auth() {
        return plugin.auth();
    }

    private LockedSettings locked() {
        return plugin.settings().locked();
    }

    private boolean isLocked(@NotNull Player player) {
        return auth().isLocked(player.getUniqueId());
    }

    private void deny(@NotNull Player player) {
        plugin.messages().send(player, "locked-action");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!auth().isStaff(player)) {
            return;
        }
        SchedulerUtil.runAsync(plugin, () -> resolveJoin(player));
    }

    private void resolveJoin(@NotNull Player player) {
        if (plugin.getDatabase().isSkip(player.getUniqueId())) {
            plugin.getDatabase().setSkip(player.getUniqueId(), player.getName(), false);
            return;
        }
        boolean hasKey = plugin.getDatabase().hasKey(player.getUniqueId());
        boolean sessionValid = hasKey && auth().hasValidSession(player);
        SchedulerUtil.runOnEntity(plugin, player, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (sessionValid) {
                plugin.messages().send(player, "session-restored");
            } else {
                auth().lock(player, hasKey);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        auth().forget(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMove(PlayerMoveEvent event) {
        if (!isLocked(event.getPlayer()) || !locked().freezeMovement() || event.getTo() == null) {
            return;
        }
        boolean sameBlock = event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ();
        if (sameBlock) {
            return;
        }
        if (locked().allowHeadRotation()) {
            Location to = event.getFrom().clone();
            to.setYaw(event.getTo().getYaw());
            to.setPitch(event.getTo().getPitch());
            event.setTo(to);
        } else {
            event.setTo(event.getFrom());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (isLocked(event.getPlayer()) && locked().blockChat()) {
            event.setCancelled(true);
            deny(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!isLocked(player) || !locked().blockCommands()) {
            return;
        }
        String raw = event.getMessage().substring(1).trim().toLowerCase(Locale.ROOT);
        String label = raw.split(" ")[0];
        if (locked().commandAllowed(label)) {
            return;
        }
        event.setCancelled(true);
        deny(player);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if (isLocked(event.getPlayer()) && locked().blockInteract()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (isLocked(event.getPlayer()) && locked().blockInteract()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreak(BlockBreakEvent event) {
        if (isLocked(event.getPlayer()) && locked().blockBlockBreak()) {
            event.setCancelled(true);
            deny(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlace(BlockPlaceEvent event) {
        if (isLocked(event.getPlayer()) && locked().blockBlockPlace()) {
            event.setCancelled(true);
            deny(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrop(PlayerDropItemEvent event) {
        if (isLocked(event.getPlayer()) && locked().blockItemDrop()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (isLocked(player) && locked().blockItemPickup()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getPlayer();
        if (isLocked(player) && locked().blockInventory()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if (isLocked(player) && locked().blockInventory()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (isLocked(player) && locked().blockDamage()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamageOthers(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getDamager();
        if (isLocked(player) && locked().blockDamage()) {
            event.setCancelled(true);
            deny(player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getTarget();
        if (isLocked(player) && locked().invulnerable()) {
            event.setCancelled(true);
        }
    }
}
