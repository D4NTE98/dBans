package pl.d4nte.bans.commands;

import pl.d4nte.bans.BansSystem;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SetJailCommand implements CommandExecutor {
    private final BansSystem plugin;
    public SetJailCommand(BansSystem p){ this.plugin = p; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("d4ntepunish.setjail")) {
            sender.sendMessage(plugin.color(plugin.getConfig().getString("messages.no-permission")));
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can run this command.");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(plugin.color(plugin.getConfig().getString("messages.usage-error")
                    .replace("{usage}", "/setjail <id>")));
            return true;
        }
        Player p = (Player) sender;
        Location l = p.getLocation();
        try (PreparedStatement ps = plugin.db().getConnection().prepareStatement(
                "REPLACE INTO d4nte_jails (id,world,x,y,z,yaw,pitch) VALUES (?,?,?,?,?,?,?)")) {
            ps.setString(1, args[0]);
            ps.setString(2, l.getWorld().getName());
            ps.setDouble(3, l.getX());
            ps.setDouble(4, l.getY());
            ps.setDouble(5, l.getZ());
            ps.setFloat(6, l.getYaw());
            ps.setFloat(7, l.getPitch());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(plugin.color("&cBłąd zapisu jaila."));
            return true;
        }
        sender.sendMessage(plugin.color("&aUstawiono jail &2" + args[0] + " &apod Twoją pozycją."));
        return true;
    }
}