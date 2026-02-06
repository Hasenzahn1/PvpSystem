package me.hasenzahn1.pvp.actions;

import me.hasenzahn1.pvp.PvpSystem;
import me.hasenzahn1.pvp.commands.lookup.LookupEntry;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class ActionTriggerManager {

    private boolean enabled = false;

    private List<String> activateCommands = new ArrayList<>();
    private List<String> deactivateCommands = new ArrayList<>();

    private int delaySinceLastEvent = 0;

    private final ArrayList<TriggerEntry> activeTriggers;

    public ActionTriggerManager(ConfigurationSection config) {
        activeTriggers = new ArrayList<>();

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
        TriggerEntry fetchedTrigger = getTrigger(entry.getUuid(), defenderUUID);
        if(fetchedTrigger != null) {
            keepAliveKey(fetchedTrigger);
            return;
        }

        TriggerEntry trigger = new TriggerEntry(entry.getUuid(), defenderUUID, System.currentTimeMillis(), System.currentTimeMillis(), entry);
        startTrigger(entry, trigger);
    }

    private void keepAliveKey(TriggerEntry trigger){
        trigger.currentTimestamp = System.currentTimeMillis();
    }

    private void startExpiredTrigger(){
        if(!enabled) return;
        new BukkitRunnable(){
            @Override
            public void run() {
                List<TriggerEntry> keyToRemove = new ArrayList<>();
                for(int i = activeTriggers.size() - 1; i >= 0; i--){
                    TriggerEntry entry = activeTriggers.get(i);
                    if(System.currentTimeMillis() - entry.currentTimestamp > delaySinceLastEvent){
                        activeTriggers.remove(i);
                        keyToRemove.add(entry);
                    }
                }
                for(TriggerEntry key : keyToRemove){
                    for(String command : deactivateCommands){
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), PvpSystem.parseReplacements(command, key.entry.getReplacementParameters()));
                    }
                }
            }
        }.runTaskTimer(PvpSystem.getInstance(), 0, 1);

    }

    private void startTrigger(LookupEntry entry, TriggerEntry trigger) {
        if(entry.isDeath()) entry.getDeathEntry().setTriggerKey(trigger.getTriggerKey());
        else entry.getDamageEntry().setTriggerKey(trigger.getTriggerKey());

        for(String command : activateCommands) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), PvpSystem.parseReplacements(command, entry.getReplacementParameters()));
        }

        activeTriggers.add(trigger);
    }

    private TriggerEntry getTrigger(UUID defender, UUID attacker) {
        for(TriggerEntry entry : activeTriggers){
            if(entry.attacker == attacker && entry.defender == defender) return entry;
        }
        return null;
    }

    private TriggerEntry getTrigger(UUID defender) {
        for(TriggerEntry entry : activeTriggers){
            if(entry.defender == defender) return entry;
        }
        return null;
    }

    public String getActionTriggerKey(Player attacker, Entity defender) {
        if(defender == null) {
            TriggerEntry trigger = getTrigger(attacker.getUniqueId());
            if(trigger == null) return "";
            return trigger.getTriggerKey();
        }

        TriggerEntry trigger = getTrigger(attacker.getUniqueId(), defender.getUniqueId());
        if(trigger == null) return "";
        return trigger.getTriggerKey();
    }

    private static class TriggerEntry {
        public UUID attacker;
        public UUID defender;
        public long startTimestamp;
        public long currentTimestamp;
        LookupEntry entry;

        public TriggerEntry(UUID defender, UUID attacker, long startTimestamp, long currentTimestamp, LookupEntry entry) {
            this.defender = defender;
            this.attacker = attacker;
            this.startTimestamp = startTimestamp;
            this.currentTimestamp = currentTimestamp;
            this.entry = entry;
        }

        public String getTriggerKey(){
            return Bukkit.getOfflinePlayer(attacker).getName() + "_" + new SimpleDateFormat("dd-MM-yyyy_hh-mm-ss").format(new Date(startTimestamp));
        }
    }

}
