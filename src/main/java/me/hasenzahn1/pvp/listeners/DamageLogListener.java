package me.hasenzahn1.pvp.listeners;

import me.hasenzahn1.pvp.PvpSystem;
import me.hasenzahn1.pvp.commands.lookup.LookupEntry;
import me.hasenzahn1.pvp.database.PlayerDamageEntry;
import me.hasenzahn1.pvp.database.PlayerDeathEntry;
import me.hasenzahn1.pvp.utils.EventUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class DamageLogListener implements Listener {

    private final double damageThreshold;

    public DamageLogListener(){
        damageThreshold = PvpSystem.getInstance().getConfig().getDouble("damageThreshold", 6);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDamageEvent(EntityDamageEvent event) {
        if(!(event.getEntity() instanceof Player)) return;
        Player victim = (Player) event.getEntity();

        //Create Log Entry
        PlayerDamageEntry entry = new PlayerDamageEntry(event, DamageListener.lastDamageForPlayer.get(victim));
        entry.create();

        //Get Attacking Player
        Entity attackingEntity = EventUtils.getAttackingEntity(event);
        if(!(attackingEntity instanceof Player)) return; //Only message if PVP
        Player attacker = (Player) attackingEntity;

        //Get Modes
        boolean victimMode = PvpSystem.getInstance().getDatabase().getPlayerStates().get(victim.getUniqueId()).state;
        boolean attackerMode = PvpSystem.getInstance().getDatabase().getPlayerStates().get(attackingEntity.getUniqueId()).state;
        if(!victimMode && !attackerMode) return; // Ignore PVP vs PVP
        PvpSystem.getInstance().getActionTriggerManager().registerDamage(new LookupEntry(entry));

        //Ignore damage above damage Threshold
        if(victim.getHealth() - event.getFinalDamage() >= damageThreshold) return;

        //Send for attacker
        if(attackerMode) attacker.sendMessage(Component.text(PvpSystem.getPrefixedLang("damage.message.attacker.isPeaceful", "attacker", attacker.getName(), "defender", victim.getName())));
        else attacker.sendMessage(Component.text(PvpSystem.getPrefixedLang("damage.message.attacker.isNotPeaceful", "attacker", attacker.getName(), "defender", victim.getName())));

        //Send for defender
        if(victimMode) victim.sendMessage(Component.text(PvpSystem.getPrefixedLang("damage.message.defender.isPeaceful", "attacker", attacker.getName(), "defender", victim.getName())));
        else victim.sendMessage(Component.text(PvpSystem.getPrefixedLang("damage.message.defender.isNotPeaceful", "attacker", attacker.getName(), "defender", victim.getName())));
    }

    @EventHandler(ignoreCancelled = true)
    public void onDeathEvent(PlayerDeathEvent event) {
        Player victim = event.getEntity();

        PlayerDeathEntry entry = new PlayerDeathEntry(event);
        entry.create();

        //Get Attacking Player
        Entity attackingEntity = EventUtils.getAttackingEntity(event);
        if(!(attackingEntity instanceof Player)) return; //Only message if PVP
        Player attacker = (Player) attackingEntity;

        //Get Modes
        boolean victimMode = PvpSystem.getInstance().getDatabase().getPlayerStates().get(victim.getUniqueId()).state;
        boolean attackerMode = PvpSystem.getInstance().getDatabase().getPlayerStates().get(attackingEntity.getUniqueId()).state;
        if(!victimMode && !attackerMode) return; // Ignore PVP vs PVP

        //Send for attacker
        if(attackerMode) attacker.sendMessage(Component.text(PvpSystem.getPrefixedLang("death.message.attacker.isPeaceful", "attacker", attacker.getName(), "defender", victim.getName())));
        else attacker.sendMessage(Component.text(PvpSystem.getPrefixedLang("death.message.attacker.isNotPeaceful", "attacker", attacker.getName(), "defender", victim.getName())));

        //Send for defender
        if(victimMode) victim.sendMessage(Component.text(PvpSystem.getPrefixedLang("death.message.defender.isPeaceful", "attacker", attacker.getName(), "defender", victim.getName())));
        else victim.sendMessage(Component.text(PvpSystem.getPrefixedLang("death.message.defender.isNotPeaceful", "attacker", attacker.getName(), "defender", victim.getName())));
    }

}
