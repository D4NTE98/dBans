package pl.d4nte.bans.listeners;

import pl.d4nte.bans.BansSystem;
import pl.d4nte.bans.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class JailListener implements Listener {

    private final BansSystem plugin;

    public JailListener(BansSystem plugin) {
        this.plugin = plugin;
        // periodic task to free expired jailings
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try (PreparedStatement ps = plugin.db().getConnection().prepareStatement(
                    "UPDATE d4nte_jailings SET active=0 WHERE active=1 AND expires_at < ?")) {
                ps.setLong(1, TimeUtil.now());
                ps.executeUpdate();
            } catch (SQLException ignored) {}
        }, 20L, 20L * 60L);
    }

    public static void teleportToJail(Player p, String jailId) {
        try {
            BansSystem pl = BansSystem.get();
            try (PreparedStatement ps = pl.db().getConnection().prepareStatement(
                    "SELECT world,x,y,z,yaw,pitch FROM d4nte_jails WHERE id=?")) {
                ps.setString(1, jailId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        World w = Bukkit.getWorld(rs.getString("world"));
                        if (w == null) return;
                        Location loc = new Location(w, rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                                rs.getFloat("yaw"), rs.getFloat("pitch"));
                        Bukkit.getScheduler().runTask(pl, () -> p.teleport(loc));
                    }
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (plugin.pm().isJailed(e.getPlayer().getUniqueId(), e.getPlayer().getName())) {
            Optional<String> jailId = plugin.pm().currentJailId(e.getPlayer().getUniqueId(), e.getPlayer().getName());
            jailId.ifPresent(id -> teleportToJail(e.getPlayer(), id));
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (e.getFrom().getBlockX() == e.getTo().getBlockX() &&
            e.getFrom().getBlockY() == e.getTo().getBlockY() &&
            e.getFrom().getBlockZ() == e.getTo().getBlockZ()) return;

        if (plugin.pm().isJailed(e.getPlayer().getUniqueId(), e.getPlayer().getName())) {
            Optional<String> jailId = plugin.pm().currentJailId(e.getPlayer().getUniqueId(), e.getPlayer().getName());
            jailId.ifPresent(id -> {
                e.setCancelled(true);
                teleportToJail(e.getPlayer(), id);
                e.getPlayer().sendActionBar(plugin.color(plugin.getConfig().getString("messages.still-jailed")
                        .replace("{until}", "?").replace("{id}", id)));
            });
        }
    }
}