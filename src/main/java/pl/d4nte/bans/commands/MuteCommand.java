package pl.d4nte.bans.commands;

import pl.d4nte.bans.BansSystem;
import pl.d4nte.bans.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class MuteCommand implements CommandExecutor {
    private final BansSystem plugin;
    public MuteCommand(BansSystem p){ this.plugin = p; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("d4ntepunish.mute")) {
            sender.sendMessage(plugin.color(plugin.getConfig().getString("messages.no-permission")));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(plugin.color(plugin.getConfig().getString("messages.usage-error")
                    .replace("{usage}", "/mute <nick> <czas> <powód>")));
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        long duration;
        try { duration = TimeUtil.parseDuration(args[1]); }
        catch (Exception ex) {
            sender.sendMessage(plugin.color("&cZły format czasu. Użyj np. 12h/1d/30m/1s"));
            return true;
        }
        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
        plugin.pm().muteAs(sender.getName(), target, duration, reason);
        return true;
    }
}