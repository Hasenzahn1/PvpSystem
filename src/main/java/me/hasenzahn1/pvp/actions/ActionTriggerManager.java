package me.hasenzahn1.pvp.actions;

import me.hasenzahn1.pvp.PvpSystem;
import me.hasenzahn1.pvp.commands.lookup.LookupEntry;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.SimpleDateFormat;
import java.util.*;

public class ActionTriggerManager {

    private boolean enabled = false;

    private List<String> activateCommands = new ArrayList<>();
    private List<String> deactivateCommands = new ArrayList<>();

    private int delaySinceLastEvent = 0;

    private final HashMap<String, String> actionTriggerKeys;
    private final HashMap<String, Long> lastTimestamp;
    private final HashMap<String, LookupEntry> actionTriggerEntries;

    public ActionTriggerManager(ConfigurationSection config) {
        actionTriggerKeys = new HashMap<>();
        lastTimestamp = new HashMap<>();
        actionTriggerEntries = new HashMap<>();

        if(config == null) return;
        enabled = config.getBoolean("enabled", false);
        delaySinceLastEvent = config.getInt("delaySinceLastEvent", 0);
        activateCommands = config.getStringList("activateCommands");
        deactivateCommands = config.getStringList("deactivateCommands");
        startExpiredTrigger();
    }

    public void registerDamage(LookupEntry entry) {
        if(!enabled) return;
        if(Bukkit.getPlayer(entry.getAttackerName()) == null) return;
        UUID defenderUUID = Bukkit.getPlayer(entry.getAttackerName()).getUniqueId();
        String key = createUniqueKey(entry.getUuid(), defenderUUID);
        if(actionTriggerKeys.containsKey(key)) {
            keepAliveKey(key);
            return;
        }

        startTrigger(entry, key);
    }

    private void keepAliveKey(String key){
        lastTimestamp.put(key, System.currentTimeMillis());
    }

    private void startExpiredTrigger(){
        if(!enabled) return;
        new BukkitRunnable(){
            @Override
            public void run() {
                List<String> keyToRemove = new ArrayList<>();
                for(Map.Entry<String, Long> entry : lastTimestamp.entrySet()){
                    if(System.currentTimeMillis() - entry.getValue() > delaySinceLastEvent){
                        keyToRemove.add(entry.getKey());
                    }
                }
                for(String key : keyToRemove){
                    for(String command : deactivateCommands){
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), PvpSystem.parseReplacements(command, actionTriggerEntries.get(key).getReplacementParameters()));
                    }
                    lastTimestamp.remove(key);
                    actionTriggerKeys.remove(key);
                    actionTriggerEntries.remove(key);
                }
            }
        }.runTaskTimer(PvpSystem.getInstance(), 0, 1);

    }

    private void startTrigger(LookupEntry entry, String key) {
        actionTriggerKeys.put(key, entry.getAttackerName() + "_" + new SimpleDateFormat("dd-MM-yyyy_hh-mm-ss").format(new Date()));
        if(entry.isDeath()) entry.getDeathEntry().setTriggerKey(actionTriggerKeys.get(key));
        else entry.getDamageEntry().setTriggerKey(actionTriggerKeys.get(key));

        for(String command : activateCommands) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), PvpSystem.parseReplacements(command, entry.getReplacementParameters()));
        }
        lastTimestamp.put(key, System.currentTimeMillis());
        actionTriggerEntries.put(key, entry);
    }

    public String getActionTriggerKey(Player attacker, Player defender) {
        String key = createUniqueKey(attacker.getUniqueId(), defender.getUniqueId());
        return actionTriggerKeys.get(key);
    }

    public static String createUniqueKey(UUID a, UUID b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("UUIDs must not be null");
        }

        UUID first = a.compareTo(b) <= 0 ? a : b;
        UUID second = a.compareTo(b) <= 0 ? b : a;

        return first.toString() + "_" + second.toString();
    }

}
