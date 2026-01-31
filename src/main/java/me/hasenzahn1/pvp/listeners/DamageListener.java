package me.hasenzahn1.pvp.listeners;

import me.hasenzahn1.pvp.PvpSystem;
import me.hasenzahn1.pvp.commands.PeacefulCommand;
import me.hasenzahn1.pvp.commands.lookup.LookupEntry;
import me.hasenzahn1.pvp.database.PlayerDamageEntry;
import me.hasenzahn1.pvp.database.PlayerDeathEntry;
import me.hasenzahn1.pvp.database.PlayerStateEntry;
import me.hasenzahn1.pvp.utils.EventUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.HashMap;
import java.util.UUID;

public class DamageListener implements Listener {

    private final int damageThreshold;
    private final int damageBelowThreshold;
    private final int lastDamageDuration;

    private final HashMap<UUID, Long> timeAtLastDeath;

    public DamageListener() {
        damageThreshold = PvpSystem.getInstance().getConfig().getInt("damageThreshold", 6);
        damageBelowThreshold = PvpSystem.getInstance().getConfig().getInt("damageBelowThreshold", 1);
        lastDamageDuration = PvpSystem.getInstance().getConfig().getInt("lastDamageDuration", 2000);
        timeAtLastDeath = new HashMap<>();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        if(!(event.getEntity() instanceof Player)) return;
        double damage = event.getFinalDamage();
        if(event.getCause() == EntityDamageEvent.DamageCause.FALL){
            if(System.currentTimeMillis() - timeAtLastDeath.getOrDefault(event.getEntity().getUniqueId(), 0L) >= lastDamageDuration) return;
            EntityDamageEvent evt = event.getEntity().getLastDamageCause();
            if(evt == null) return;
            handleDamageFromSource(event, evt);
        }
        handleDamageFromSource(event, event);
        timeAtLastDeath.put(event.getEntity().getUniqueId(), System.currentTimeMillis());

        PlayerDamageEntry entry = new PlayerDamageEntry(event, damage);
        if(entry.getAttackerMode() == 1 || entry.getDefenderMode() == 1) PvpSystem.getInstance().getActionTriggerManager().registerDamage(new LookupEntry(entry));
        entry.create();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        PlayerDeathEntry entry = new PlayerDeathEntry(event);
        if(entry.getAttackerMode() == 1 || entry.getDefenderMode() == 1) PvpSystem.getInstance().getActionTriggerManager().registerDamage(new LookupEntry(entry));
        entry.create();

        Player player = EventUtils.getCausingPlayerFromEvent(event.getEntity().getLastDamageCause());
        if(player == null) return;
        handleDeath(player, event.getPlayer());
    }

    private void handleDamageFromSource(EntityDamageEvent event, EntityDamageEvent toCheck) {
        Player attacker = EventUtils.getCausingPlayerFromEvent(toCheck);
        if(attacker == null) return;

        PeacefulCommand.PVP_ACTION_TIMESTAMPS.put(attacker.getUniqueId(), System.currentTimeMillis());

        handleDamage(event, attacker, ((Player) event.getEntity()));
    }


    private void handleDamage(EntityDamageEvent event, Player attacker, Player defender) {
        PlayerStateEntry attackerState = PvpSystem.getInstance().getDatabase().getPlayerStates().get(attacker.getUniqueId());
        PlayerStateEntry defenderState = PvpSystem.getInstance().getDatabase().getPlayerStates().get(defender.getUniqueId());

        if(attackerState == null) return;
        if(defenderState == null) return;

        //Both deactivated Peaceful mode
        if(!attackerState.state && !defenderState.state) return;

        //Above Damage threshold
        double finalDamage = event.getFinalDamage();
        event.setDamage(EntityDamageEvent.DamageModifier.ARMOR, 0);
        if(defender.getHealth() > damageThreshold) {
            event.setDamage(EntityDamageEvent.DamageModifier.BASE, Math.min(finalDamage, defender.getHealth() - damageThreshold));
            return;
        }

        //Below Damage Threshold
        event.setDamage(EntityDamageEvent.DamageModifier.BASE, Math.min(damageBelowThreshold, finalDamage));


        //Send Message
        //Send for attacker
        if(attackerState.state) attacker.sendMessage(Component.text(PvpSystem.getPrefixedLang("damage.message.attacker.isPeaceful", "attacker", attacker.getName(), "defender", defender.getName())));
        else attacker.sendMessage(Component.text(PvpSystem.getPrefixedLang("damage.message.attacker.isNotPeaceful", "attacker", attacker.getName(), "defender", defender.getName())));

        //Send for defender
        if(defenderState.state) defender.sendMessage(Component.text(PvpSystem.getPrefixedLang("damage.message.defender.isPeaceful", "attacker", attacker.getName(), "defender", defender.getName())));
        else defender.sendMessage(Component.text(PvpSystem.getPrefixedLang("damage.message.defender.isNotPeaceful", "attacker", attacker.getName(), "defender", defender.getName())));
    }

    private void handleDeath(Player attacker, Player defender) {
        PeacefulCommand.PVP_ACTION_TIMESTAMPS.remove(defender.getUniqueId());

        PlayerStateEntry attackerState = PvpSystem.getInstance().getDatabase().getPlayerStates().get(attacker.getUniqueId());
        PlayerStateEntry defenderState = PvpSystem.getInstance().getDatabase().getPlayerStates().get(defender.getUniqueId());

        if(attackerState == null) return;
        if(defenderState == null) return;

        //Both deactivated Peaceful mode
        if(!attackerState.state && !defenderState.state) return;

        if(attackerState.state) attacker.sendMessage(Component.text(PvpSystem.getPrefixedLang("death.message.attacker.isPeaceful", "attacker", attacker.getName(), "defender", defender.getName())));
        else attacker.sendMessage(Component.text(PvpSystem.getPrefixedLang("death.message.attacker.isNotPeaceful", "attacker", attacker.getName(), "defender", defender.getName())));

        //Send for defender
        if(defenderState.state) defender.sendMessage(Component.text(PvpSystem.getPrefixedLang("death.message.defender.isPeaceful", "attacker", attacker.getName(), "defender", defender.getName())));
        else defender.sendMessage(Component.text(PvpSystem.getPrefixedLang("death.message.defender.isNotPeaceful", "attacker", attacker.getName(), "defender", defender.getName())));
    }
}
