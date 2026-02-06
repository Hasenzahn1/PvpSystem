package me.hasenzahn1.pvp;

import me.hasenzahn1.pvp.actions.ActionTriggerManager;
import me.hasenzahn1.pvp.commands.DeathHistoryCommand;
import me.hasenzahn1.pvp.commands.LookupCommand;
import me.hasenzahn1.pvp.commands.PeacefulCommand;
import me.hasenzahn1.pvp.commands.lookup.PlayerSearchResult;
import me.hasenzahn1.pvp.database.DatabaseManager;
import me.hasenzahn1.pvp.listeners.ConnectionListener;
import me.hasenzahn1.pvp.listeners.DamageListener;
import me.hasenzahn1.pvp.listeners.DamageLogListener;
import me.hasenzahn1.pvp.listeners.InventoryListener;
import me.hasenzahn1.pvp.menu.MenuButton;
import me.hasenzahn1.pvp.papi.PvpPlaceholderExtension;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public final class PvpSystem extends JavaPlugin {

    private static PvpSystem instance;
    private static String PREFIX;
    private static final boolean DEV_MODE = false;

    private DatabaseManager databaseManager;

    private final HashMap<UUID, PlayerSearchResult> playerSearchResults = new HashMap<>();

    private ArrayList<MenuButton> damageMenuButtons;
    private ArrayList<MenuButton> deathMenuButtons;

    private ActionTriggerManager actionTriggerManager;

    @Override
    public void onEnable() {
        instance = this;
        databaseManager = new DatabaseManager();

        if(DEV_MODE || !new File(getDataFolder(), "config.yml").exists()) {
            saveResource("config.yml", true);
            reloadConfig();
        }

        PREFIX = ChatColor.translateAlternateColorCodes('&', getConfig().getString("prefix"));

        parseMenuButton();

        actionTriggerManager = new ActionTriggerManager(getConfig().getConfigurationSection("actionTrigger"));

        try{
            databaseManager.connect(getDataFolder() + "/data.db");
        }catch (SQLException e){
            getLogger().severe("Could not connect to database!");
        }

        getCommand("peaceful").setExecutor(new PeacefulCommand());
        getCommand("peaceful").setTabCompleter(new PeacefulCommand());

        getCommand("deathhistory").setExecutor(new DeathHistoryCommand());
        getCommand("deathhistory").setTabCompleter(new DeathHistoryCommand());

        getCommand("pvplookup").setExecutor(new LookupCommand());
        getCommand("pvplookup").setTabCompleter(new LookupCommand());

        Bukkit.getPluginManager().registerEvents(new ConnectionListener(), this);
        Bukkit.getPluginManager().registerEvents(new DamageListener(), this);
        Bukkit.getPluginManager().registerEvents(new InventoryListener(), this);
        Bukkit.getPluginManager().registerEvents(new DamageLogListener(), this);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PvpPlaceholderExtension().register();
        }
    }

    public void parseMenuButton() {
        damageMenuButtons = new ArrayList<>();
        deathMenuButtons = new ArrayList<>();

        if(getConfig().getConfigurationSection("deathEntryButtons") != null) {
            for (String key : getConfig().getConfigurationSection("deathEntryButtons").getKeys(false)) {
                String name = key.replace("deathEntryButtons.", "");
                MenuButton button = new MenuButton(name,
                        getConfig().getString("deathEntryButtons." + key + ".display"),
                        getConfig().getString("deathEntryButtons." + key + ".hover"),
                        getConfig().getString("deathEntryButtons." + key + ".command"),
                        MenuButton.DisplayCondition.valueOf(getConfig().getString("deathEntryButtons." + key + ".displayCondition", "ALL")));
                deathMenuButtons.add(button);
            }
        }

        if(getConfig().getConfigurationSection("damageEntryButtons") != null) {
            for (String key : getConfig().getConfigurationSection("damageEntryButtons").getKeys(false)) {
                String name = key.replace("damageEntryButtons.", "");
                MenuButton button = new MenuButton(name,
                        getConfig().getString("damageEntryButtons." + key + ".display"),
                        getConfig().getString("damageEntryButtons." + key + ".hover"),
                        getConfig().getString("damageEntryButtons." + key + ".command"),
                        MenuButton.DisplayCondition.valueOf(getConfig().getString("damageEntryButtons." + key + ".displayCondition", "ALL")));
                damageMenuButtons.add(button);
            }
        }

        System.out.println(damageMenuButtons);
        System.out.println(deathMenuButtons);
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

    public static String getLang(String key, Object... args) {
        String lang = PvpSystem.getInstance().getConfig().getString("lang." + key, "&cUnknown language key &6" + key);
        return parseReplacements(lang, args);
    }

    public static String parseReplacements(String string, Object... args) {
        for (int i = 0; i + 1 < args.length; i += 2) {
            string = string.replace("%" + args[i].toString() + "%", args[i + 1] + "");
        }
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    public static String getPrefixedLang(String key, Object... args) {
        return PREFIX + getLang(key, args);
    }

    public HashMap<UUID, PlayerSearchResult> getPlayerSearchResults() {
        return playerSearchResults;
    }

    public ArrayList<MenuButton> getDamageMenuButtons() {
        return damageMenuButtons;
    }

    public ArrayList<MenuButton> getDeathMenuButtons() {
        return deathMenuButtons;
    }

    public ActionTriggerManager getActionTriggerManager() {
        return actionTriggerManager;
    }
}
