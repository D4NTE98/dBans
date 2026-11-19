package pl.d4nte.bans.listeners;

import pl.d4nte.bans.BansSystem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final BansSystem plugin;

    public ChatListener(BansSystem plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        if (plugin.pm().isMuted(e.getPlayer().getUniqueId(), e.getPlayer().getName())) {
            e.setCancelled(true);
            String until = plugin.pm().muteUntil(e.getPlayer().getUniqueId(), e.getPlayer().getName());
            e.getPlayer().sendMessage(plugin.color("&6Jesteś wyciszony do: &e" + until));
        }
    }
}