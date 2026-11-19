package pl.d4nte.bans;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import pl.d4nte.bans.commands.*;
import pl.d4nte.bans.data.Database;
import pl.d4nte.bans.listeners.ChatListener;
import pl.d4nte.bans.listeners.JailListener;
import pl.d4nte.bans.listeners.LoginListener;
import pl.d4nte.bans.logic.PunishManager;

public class BansSystem extends JavaPlugin {

    private static BansSystem instance;
    private Database database;
    private PunishManager punishManager;

    public static BansSystem get() { return instance; }
    public Database db() { return database; }
    public PunishManager pm() { return punishManager; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        FileConfiguration cfg = getConfig();
        try {
            this.database = new Database(
                cfg.getString("mysql.host"),
                cfg.getInt("mysql.port"),
                cfg.getString("mysql.database"),
                cfg.getString("mysql.user"),
                cfg.getString("mysql.password"),
                cfg.getBoolean("mysql.useSSL")
            );
            this.database.init();
        } catch (Exception e) {
            getLogger().severe("Failed to connect to MySQL: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.punishManager = new PunishManager(this);

        // Register commands
        getCommand("ban").setExecutor(new BanCommand(this));
        getCommand("tempban").setExecutor(new TempbanCommand(this));
        getCommand("unban").setExecutor(new UnbanCommand(this));
        getCommand("mute").setExecutor(new MuteCommand(this));
        getCommand("unmute").setExecutor(new UnmuteCommand(this));
        getCommand("kick").setExecutor(new KickCommand(this));
        getCommand("jail").setExecutor(new JailCommand(this));
        getCommand("unjail").setExecutor(new UnjailCommand(this));
        getCommand("setjail").setExecutor(new SetJailCommand(this));

        // Listeners
        Bukkit.getPluginManager().registerEvents(new LoginListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new JailListener(this), this);

        getLogger().info(color("&aD4ntePunish enabled."));
    }

    @Override
    public void onDisable() {
        if (database != null) database.close();
        getLogger().info(color("&cD4ntePunish disabled."));
    }

    public String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}