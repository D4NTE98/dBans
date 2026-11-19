package pl.d4nte.bans.logic;

import pl.d4nte.bans.BansSystem;
import pl.d4nte.bans.util.TimeUtil;
import pl.d4nte.bans.util.DiscordWebhook;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import pl.d4nte.bans.listeners.JailListener;

import java.net.InetSocketAddress;
import java.sql.*;
import java.util.Optional;
import java.util.UUID;

public class PunishManager {

    private final BansSystem plugin;
    private final DiscordWebhook webhook;

    public PunishManager(BansSystem plugin) {
        this.plugin = plugin;
        this.webhook = new DiscordWebhook(plugin);
    // PATCHED methods to allow console staff name
    public void banAs(String staffName, OfflinePlayer target, String reason) {
        String ip = "unknown";
        if (target.isOnline()) {
            java.net.InetSocketAddress addr = ((org.bukkit.entity.Player)target).getAddress();
            if (addr != null) ip = addr.getAddress().getHostAddress();
        }
        try (PreparedStatement ps = plugin.db().getConnection().prepareStatement(
                "INSERT INTO d4nte_bans (uuid,name,ip,reason,staff,created_at,expires_at,active) VALUES (?,?,?,?,?,?,NULL,1)")) {
            ps.setString(1, target.getUniqueId().toString());
            ps.setString(2, target.getName() == null ? "unknown" : target.getName());
            ps.setString(3, ip);
            ps.setString(4, reason);
            ps.setString(5, staffName);
            ps.setLong(6, TimeUtil.now());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
        webhook.sendEmbed("Permanent Ban",
                target.getName() + " został zbanowany.", 0xFF0000,
                DiscordWebhook.map("Admin", staffName, "Powód", reason));
        if (target.isOnline()) {
            ((org.bukkit.entity.Player)target).kickPlayer(plugin.color(plugin.getConfig().getString("messages.ban-screen-perm")
                    .replace("{reason}", reason).replace("{staff}", staffName)));
        }
        org.bukkit.Bukkit.broadcastMessage(plugin.color(plugin.getConfig().getString("messages.ban-broadcast-perm")
                .replace("{target}", target.getName()).replace("{staff}", staffName).replace("{reason}", reason)));
    }

    public void tempbanAs(String staffName, OfflinePlayer target, long durationMs, String reason) {
        String ip = "unknown";
        if (target.isOnline()) {
            java.net.InetSocketAddress addr = ((org.bukkit.entity.Player)target).getAddress();
            if (addr != null) ip = addr.getAddress().getHostAddress();
        }
        long now = TimeUtil.now();
        long expires = now + durationMs;
        try (PreparedStatement ps = plugin.db().getConnection().prepareStatement(
                "INSERT INTO d4nte_bans (uuid,name,ip,reason,staff,created_at,expires_at,active) VALUES (?,?,?,?,?,?,?,1)")) {
            ps.setString(1, target.getUniqueId().toString());
            ps.setString(2, target.getName() == null ? "unknown" : target.getName());
            ps.setString(3, ip);
            ps.setString(4, reason);
            ps.setString(5, staffName);
            ps.setLong(6, now);
            ps.setLong(7, expires);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
        webhook.sendEmbed("TempBan",
                target.getName() + " został tymczasowo zbanowany.", 0xE53935,
                DiscordWebhook.map("Admin", staffName, "Powód", reason, "Do", TimeUtil.formatUntil(expires)));
        if (target.isOnline()) {
            ((org.bukkit.entity.Player)target).kickPlayer(plugin.color(plugin.getConfig().getString("messages.ban-screen-temp")
                    .replace("{reason}", reason).replace("{staff}", staffName)
                    .replace("{until}", TimeUtil.formatUntil(expires))));
        }
        String timeStr = humanDuration(durationMs);
        org.bukkit.Bukkit.broadcastMessage(plugin.color(plugin.getConfig().getString("messages.ban-broadcast-temp")
                .replace("{target}", target.getName()).replace("{staff}", staffName)
                .replace("{reason}", reason).replace("{time}", timeStr)));
    }

    public void unbanAs(String staffName, String nameOrUuid) {
        try (PreparedStatement ps = plugin.db().getConnection().prepareStatement(
                "UPDATE d4nte_bans SET active=0 WHERE active=1 AND (uuid=? OR name=?)")) {
            ps.setString(1, nameOrUuid);
            ps.setString(2, nameOrUuid);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
        org.bukkit.Bukkit.broadcastMessage(plugin.color(plugin.getConfig().getString("messages.unban-broadcast")
                .replace("{target}", nameOrUuid).replace("{staff}", staffName)));
    }

    public void muteAs(String staffName, OfflinePlayer target, long durationMs, String reason) {
        long now = TimeUtil.now();
        long expires = now + durationMs;
        try (PreparedStatement ps = plugin.db().getConnection().prepareStatement(
                "INSERT INTO d4nte_mutes (uuid,name,reason,staff,created_at,expires_at,active) VALUES (?,?,?,?,?,?,1)")) {
            ps.setString(1, target.getUniqueId().toString());
            ps.setString(2, target.getName() == null ? "unknown" : target.getName());
            ps.setString(3, reason);
            ps.setString(4, staffName);
            ps.setLong(5, now);
            ps.setLong(6, expires);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
        org.bukkit.Bukkit.broadcastMessage(plugin.color(plugin.getConfig().getString("messages.mute-broadcast")
                .replace("{target}", target.getName()).replace("{staff}", staffName)
                .replace("{reason}", reason).replace("{time}", humanDuration(durationMs))));
        webhook.sendEmbed("Mute",
                target.getName() + " został wyciszony.", 0xFFA000,
                DiscordWebhook.map("Admin", staffName, "Powód", reason, "Do", TimeUtil.formatUntil(expires)));
    }

    public void kickAs(String staffName, org.bukkit.entity.Player target, String reason) {
        try (PreparedStatement ps = plugin.db().getConnection().prepareStatement(
                "INSERT INTO d4nte_kicks (uuid,name,reason,staff,created_at,ip) VALUES (?,?,?,?,?,?)")) {
            ps.setString(1, target.getUniqueId().toString());
            ps.setString(2, target.getName());
            ps.setString(3, reason);
            ps.setString(4, staffName);
            ps.setLong(5, TimeUtil.now());
            ps.setString(6, target.getAddress() == null ? "unknown" : target.getAddress().getAddress().getHostAddress());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
        target.kickPlayer(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&cZostałeś wyrzucony: &4" + reason));
        org.bukkit.Bukkit.broadcastMessage(plugin.color(plugin.getConfig().getString("messages.kick-broadcast")
                .replace("{target}", target.getName()).replace("{staff}", staffName).replace("{reason}", reason)));
        webhook.sendEmbed("Kick", target.getName() + " został wyrzucony.", 0xC62828,
                DiscordWebhook.map("Admin", staffName, "Powód", reason));
    }

    public void jailAs(String staffName, OfflinePlayer target, String jailId, long durationMs, String reason) throws SQLException {
        try (PreparedStatement ps = plugin.db().getConnection().prepareStatement(
                "SELECT id FROM d4nte_jails WHERE id=?")) {
            ps.setString(1, jailId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Jail not found");
            }
        }
        long now = TimeUtil.now();
        long expires = now + durationMs;
        try (PreparedStatement ps = plugin.db().getConnection().prepareStatement(
                "INSERT INTO d4nte_jailings (uuid,name,jail_id,reason,staff,created_at,expires_at,active) VALUES (?,?,?,?,?,?,?,1)")) {
            ps.setString(1, target.getUniqueId().toString());
            ps.setString(2, target.getName() == null ? "unknown" : target.getName());
            ps.setString(3, jailId);
            ps.setString(4, reason);
            ps.setString(5, staffName);
            ps.setLong(6, now);
            ps.setLong(7, expires);
            ps.executeUpdate();
        }
        if (target.isOnline()) {
            JailListener.teleportToJail((org.bukkit.entity.Player) target, jailId);
        }
        org.bukkit.Bukkit.broadcastMessage(plugin.color(plugin.getConfig().getString("messages.jail-broadcast")
                .replace("{target}", target.getName()).replace("{staff}", staffName)
                .replace("{reason}", reason).replace("{time}", humanDuration(durationMs)).replace("{id}", jailId)));
        webhook.sendEmbed("Jail", target.getName() + " trafił do więzienia.", 0x607D8B,
                DiscordWebhook.map("Admin", staffName, "Powód", reason, "Cela", jailId, "Do", TimeUtil.formatUntil(expires)));
    }

    public void unjailAs(String staffName, String nameOrUuid) {
        try (PreparedStatement ps = plugin.db().getConnection().prepareStatement(
                "UPDATE d4nte_jailings SET active=0 WHERE active=1 AND (uuid=? OR name=?)")) {
            ps.setString(1, nameOrUuid);
            ps.setString(2, nameOrUuid);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
        org.bukkit.Bukkit.broadcastMessage(plugin.color(plugin.getConfig().getString("messages.unjail-broadcast")
                .replace("{target}", nameOrUuid).replace("{staff}", staffName)));
    }
}

    public boolean isBanned(UUID uuid, String name, String ip) {
        try (PreparedStatement ps = plugin.db().getConnection().prepareStatement(
                "SELECT reason, staff, expires_at, active FROM d4nte_bans WHERE active=1 AND (uuid=? OR name=? OR ip=?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setString(3, ip);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long expires = rs.getLong("expires_at");
                    if (rs.wasNull() || expires > TimeUtil.now()) return true;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public Optional<String> banScreen(UUID uuid, String name, String ip) {
        try (PreparedStatement ps = plugin.db().getConnection().prepareStatement(
                "SELECT reason, staff, expires_at FROM d4nte_bans WHERE active=1 AND (uuid=? OR name=? OR ip=?) ORDER BY id DESC LIMIT 1")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setString(3, ip);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String reason = rs.getString("reason");
                    String staff = rs.getString("staff");
                    long expires = rs.getLong("expires_at");
                    String msg;
                    if (rs.wasNull()) {
                        msg = plugin.color(plugin.getConfig().getString("messages.ban-screen-perm")
                                .replace("{reason}", reason).replace("{staff}", staff));
                    } else {
                        msg = plugin.color(plugin.getConfig().getString("messages.ban-screen-temp")
                                .replace("{reason}", reason).replace("{staff}", staff)
                                .replace("{until}", TimeUtil.formatUntil(expires)));
                    }
                    return Optional.of(msg);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty();
    }

    public void ban(Player staff, OfflinePlayer target, String reason) {
        String ip = "unknown";
        if (target.isOnline()) {
            InetSocketAddress addr = ((Player)target).getAddress();
            if (addr != null) ip = addr.getAddress().getHostAddress();
        }
        try (PreparedStatement ps = plugin.db().getConnection().prepareStatement(
                "INSERT INTO d4nte_bans (uuid,name,ip,reason,staff,created_at,expires_at,active) VALUES (?,?,?,?,?,?,NULL,1)")) {
            ps.setString(1, target.getUniqueId().toString());
            ps.setString(2, target.getName() == null ? "unknown" : target.getName());
            ps.setString(3, ip);
            ps.setString(4, reason);
            ps.setString(5, staff.getName());
            ps.setLong(6, TimeUtil.now());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
        // Discord
        webhook.sendEmbed("Permanent Ban",
                target.getName() + " został zbanowany.", 0xFF0000,
                DiscordWebhook.map("Admin", staff.getName(), "Powód", reason));
        // Kick if online
        if (target.isOnline()) {
            ((Player)target).kickPlayer(plugin.color(plugin.getConfig().getString("messages.ban-screen-perm")
                    .replace("{reason}", reason).replace("{staff}", staff.getName())));
        }
        Bukkit.broadcastMessage(plugin.color(plugin.getConfig().getString("messages.ban-broadcast-perm")
                .replace("{target}", target.getName()).replace("{staff}", staff.getName()).replace("{reason}", reason)));
    }

    public void tempban(Player staff, OfflinePlayer target, long durationMs, String reason) {
        String ip = "unknown";
        if (target.isOnline()) {
            InetSocketAddress addr = ((Player)target).getAddress();
            if (addr != null) ip = addr.getAddress().getHostAddress();
        }
        long now = TimeUtil.now();
        long expires = now + durationMs;
        try (PreparedStatement ps = plugin.db().getConnection().prepareStatement(
                "INSERT INTO d4nte_bans (uuid,name,ip,reason,staff,created_at,expires_at,active) VALUES (?,?,?,?,?,?,?,1)")) {
            ps.setString(1, target.getUniqueId().toString());
            ps.setString(2, target.getName() == null ? "unknown" : target.getName());
            ps.setString(3, ip);
            ps.setString(4, reason);
            ps.setString(5, staff.getName());
            ps.setLong(6, now);
            ps.setLong(7, expires);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
        webhook.sendEmbed("TempBan",
                target.getName() + " został tymczasowo zbanowany.", 0xE53935,
                DiscordWebhook.map("Admin", staff.getName(), "Powód", reason, "Do", TimeUtil.formatUntil(expires)));
        if (target.isOnline()) {
            ((Player)target).kickPlayer(plugin.color(plugin.getConfig().getString("messages.ban-screen-temp")
                    .replace("{reason}", reason).replace("{staff}", staff.getName())
                    .replace("{until}", TimeUtil.formatUntil(expires))));
        }
        String timeStr = humanDuration(durationMs);
        Bukkit.broadcastMessage(plugin.color(plugin.getConfig().getString("messages.ban-broadcast-temp")
                .replace("{target}", target.getName()).replace("{staff}", staff.getName())
                .replace("{reason}", reason).replace("{time}", timeStr)));
    }

    public void unban(Player staff, String nameOrUuid) {
        try (PreparedStatement ps = plugin.db().getConnection().prepareStatement(
                "UPDATE d4nte_bans SET active=0 WHERE active=1 AND (uuid=? OR name=?)")) {
            ps.setString(1, nameOrUuid);
            ps.setString(2, nameOrUuid);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
        Bukkit.broadcastMessage(plugin.color(plugin.getConfig().getString("messages.unban-broadcast")
                .replace("{target}", nameOrUuid).replace("{staff}", staff.getName())));
    }

    public void mute(Player staff, OfflinePlayer target, long durationMs, String reason) {
        long now = TimeUtil.now();
        long expires = now + durationMs;
        try (PreparedStatement ps = plugin.db().getConnection().prepareStatement(
                "INSERT INTO d4nte_mutes (uuid,name,reason,staff,created_at,expires_at,active) VALUES (?,?,?,?,?,?,1)")) {
            ps.setString(1, target.getUniqueId().toString());
            ps.setString(2, target.getName() == null ? "unknown" : target.getName());
            ps.setString(3, reason);
            ps.setString(4, staff.getName());
            ps.setLong(5, now);
            ps.setLong(6, expires);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
        Bukkit.broadcastMessage(plugin.color(plugin.getConfig().getString("messages.mute-broadcast")
                .replace("{target}", target.getName()).replace("{staff}", staff.getName())
                .replace("{reason}", reason).replace("{time}", humanDuration(durationMs))));
        webhook.sendEmbed("Mute",
                target.getName() + " został wyciszony.", 0xFFA000,
                DiscordWebhook.map("Admin", staff.getName(), "Powód", reason, "Do", TimeUtil.formatUntil(expires)));
    }

    public void unmute(Player staff, String nameOrUuid) {
        try (PreparedStatement ps = plugin.db().getConnection().prepareStatement(
                "UPDATE d4nte_mutes SET active=0 WHERE active=1 AND (uuid=? OR name=?)")) {
            ps.setString(1, nameOrUuid);
            ps.setString(2, nameOrUuid);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
        Bukkit.broadcastMessage(plugin.color(plugin.getConfig().getString("messages.unmute-broadcast")
                .replace("{target}", nameOrUuid).replace("{staff}", staff.getName())));
    }

    public boolean isMuted(UUID uuid, String name) {
        try (PreparedStatement ps = plugin.db().getConnection().prepareStatement(
                "SELECT expires_at FROM d4nte_mutes WHERE active=1 AND (uuid=? OR name=?) ORDER BY id DESC LIMIT 1")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long expires = rs.getLong("expires_at");
                    if (expires > TimeUtil.now()) return true;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public String muteUntil(UUID uuid, String name) {
        try (PreparedStatement ps = plugin.db().getConnection().prepareStatement(
                "SELECT expires_at FROM d4nte_mutes WHERE active=1 AND (uuid=? OR name=?) ORDER BY id DESC LIMIT 1")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return TimeUtil.formatUntil(rs.getLong("expires_at"));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return "?";
    }

    public void kick(Player staff, Player target, String reason) {
        try (PreparedStatement ps = plugin.db().getConnection().prepareStatement(
                "INSERT INTO d4nte_kicks (uuid,name,reason,staff,created_at,ip) VALUES (?,?,?,?,?,?)")) {
            ps.setString(1, target.getUniqueId().toString());
            ps.setString(2, target.getName());
            ps.setString(3, reason);
            ps.setString(4, staff.getName());
            ps.setLong(5, TimeUtil.now());
            ps.setString(6, target.getAddress() == null ? "unknown" : target.getAddress().getAddress().getHostAddress());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
        target.kickPlayer(ChatColor.translateAlternateColorCodes('&', "&cZostałeś wyrzucony: &4" + reason));
        Bukkit.broadcastMessage(plugin.color(plugin.getConfig().getString("messages.kick-broadcast")
                .replace("{target}", target.getName()).replace("{staff}", staff.getName()).replace("{reason}", reason)));
        webhook.sendEmbed("Kick", target.getName() + " został wyrzucony.", 0xC62828,
                DiscordWebhook.map("Admin", staff.getName(), "Powód", reason));
    }

    public void jail(Player staff, OfflinePlayer target, String jailId, long durationMs, String reason) throws SQLException {
        // verify jail exists
        try (PreparedStatement ps = plugin.db().getConnection().prepareStatement(
                "SELECT id FROM d4nte_jails WHERE id=?")) {
            ps.setString(1, jailId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Jail not found");
            }
        }
        long now = TimeUtil.now();
        long expires = now + durationMs;
        try (PreparedStatement ps = plugin.db().getConnection().prepareStatement(
                "INSERT INTO d4nte_jailings (uuid,name,jail_id,reason,staff,created_at,expires_at,active) VALUES (?,?,?,?,?,?,?,1)")) {
            ps.setString(1, target.getUniqueId().toString());
            ps.setString(2, target.getName() == null ? "unknown" : target.getName());
            ps.setString(3, jailId);
            ps.setString(4, reason);
            ps.setString(5, staff.getName());
            ps.setLong(6, now);
            ps.setLong(7, expires);
            ps.executeUpdate();
        }
        if (target.isOnline()) {
            JailListener.teleportToJail((Player) target, jailId);
        }
        Bukkit.broadcastMessage(plugin.color(plugin.getConfig().getString("messages.jail-broadcast")
                .replace("{target}", target.getName()).replace("{staff}", staff.getName())
                .replace("{reason}", reason).replace("{time}", humanDuration(durationMs)).replace("{id}", jailId)));
        webhook.sendEmbed("Jail", target.getName() + " trafił do więzienia.", 0x607D8B,
                DiscordWebhook.map("Admin", staff.getName(), "Powód", reason, "Cela", jailId, "Do", TimeUtil.formatUntil(expires)));
    }

    public void unjail(Player staff, String nameOrUuid) {
        try (PreparedStatement ps = plugin.db().getConnection().prepareStatement(
                "UPDATE d4nte_jailings SET active=0 WHERE active=1 AND (uuid=? OR name=?)")) {
            ps.setString(1, nameOrUuid);
            ps.setString(2, nameOrUuid);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
        Bukkit.broadcastMessage(plugin.color(plugin.getConfig().getString("messages.unjail-broadcast")
                .replace("{target}", nameOrUuid).replace("{staff}", staff.getName())));
    }

    public boolean isJailed(UUID uuid, String name) {
        try (PreparedStatement ps = plugin.db().getConnection().prepareStatement(
                "SELECT jail_id, expires_at FROM d4nte_jailings WHERE active=1 AND (uuid=? OR name=?) ORDER BY id DESC LIMIT 1")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long expires = rs.getLong("expires_at");
                    return expires > TimeUtil.now();
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public Optional<String> currentJailId(UUID uuid, String name) {
        try (PreparedStatement ps = plugin.db().getConnection().prepareStatement(
                "SELECT jail_id FROM d4nte_jailings WHERE active=1 AND (uuid=? OR name=?) ORDER BY id DESC LIMIT 1")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(rs.getString("jail_id"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty();
    }

    private String humanDuration(long ms) {
        long s = ms / 1000;
        long w = s / (7*24*3600); s %= 7*24*3600;
        long d = s / (24*3600); s %= 24*3600;
        long h = s / 3600; s %= 3600;
        long m = s / 60; s %= 60;
        StringBuilder b = new StringBuilder();
        if (w>0) b.append(w).append("w ");
        if (d>0) b.append(d).append("d ");
        if (h>0) b.append(h).append("h ");
        if (m>0) b.append(m).append("m ");
        if (s>0 || b.length()==0) b.append(s).append("s");
        return b.toString().trim();
    }
}