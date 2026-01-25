package me.hasenzahn1.pvp;

import me.hasenzahn1.pvp.commands.DeathHistoryCommand;
import me.hasenzahn1.pvp.commands.PeacefulCommand;
import me.hasenzahn1.pvp.database.DatabaseManager;
import me.hasenzahn1.pvp.listeners.ConnectionListener;
import me.hasenzahn1.pvp.listeners.DamageListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public final class PvpSystem extends JavaPlugin {

    private static PvpSystem instance;
    private static String PREFIX;
    private static final boolean DEV_MODE = true;

    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        instance = this;
        databaseManager = new DatabaseManager();

        if(DEV_MODE) {
            saveResource("config.yml", true);
            reloadConfig();
        }

        PREFIX = ChatColor.translateAlternateColorCodes('&', getConfig().getString("prefix"));

        try{
            databaseManager.connect(getDataFolder() + "/data.db");
        }catch (SQLException e){
            getLogger().severe("Could not connect to database!");
        }

        getCommand("peaceful").setExecutor(new PeacefulCommand());
        getCommand("peaceful").setTabCompleter(new PeacefulCommand());

        DeathHistoryCommand deathHistoryCommand = new DeathHistoryCommand();
        getCommand("deathhistory").setExecutor(deathHistoryCommand);
        getCommand("deathhistory").setTabCompleter(deathHistoryCommand);

        Bukkit.getPluginManager().registerEvents(new ConnectionListener(), this);
        Bukkit.getPluginManager().registerEvents(new DamageListener(), this);
    }

    @Override
    public void onDisable() {
        databaseManager.disconnect();
    }

    public DatabaseManager getDatabase() {
        return databaseManager;
    }

    public static PvpSystem getInstance() {
        return instance;
    }

    public static String getLang(String key, String... args) {
        String lang = PvpSystem.getInstance().getConfig().getString("lang." + key, "&cUnknown language key &6" + key);
        for (int i = 0; i + 1 < args.length; i += 2) {
            lang = lang.replace("%" + args[i] + "%", args[i + 1]);
        }
        return ChatColor.translateAlternateColorCodes('&', lang);
    }

    public static String getPrefixedLang(String key, String... args) {
        return PREFIX + getLang(key, args);
    }
}
