package pl.d4nte.bans.listeners;

import pl.d4nte.bans.BansSystem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.Optional;

public class LoginListener implements Listener {

    private final BansSystem plugin;

    public LoginListener(BansSystem plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent e) {
        String name = e.getName();
        String ip = e.getAddress().getHostAddress();
        if (plugin.pm().isBanned(e.getUniqueId(), name, ip)) {
            Optional<String> screen = plugin.pm().banScreen(e.getUniqueId(), name, ip);
            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, screen.orElse(plugin.color("&cZostałeś zbanowany.")));
        }
    }
}