package pl.d4nte.bans.commands;

import pl.d4nte.bans.BansSystem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class UnjailCommand implements CommandExecutor {
    private final BansSystem plugin;
    public UnjailCommand(BansSystem p){ this.plugin = p; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("d4ntepunish.unjail")) {
            sender.sendMessage(plugin.color(plugin.getConfig().getString("messages.no-permission")));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(plugin.color(plugin.getConfig().getString("messages.usage-error")
                    .replace("{usage}", "/unjail <nick>")));
            return true;
        }
        plugin.pm().unjailAs(sender.getName(), args[0]);
        return true;
    }
}