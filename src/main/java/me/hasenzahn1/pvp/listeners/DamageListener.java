package me.hasenzahn1.pvp.listeners;

import me.hasenzahn1.pvp.PvpSystem;
import me.hasenzahn1.pvp.commands.PeacefulCommand;
import me.hasenzahn1.pvp.commands.lookup.LookupEntry;
import me.hasenzahn1.pvp.damage.CombatTracker;
import me.hasenzahn1.pvp.database.PlayerDamageEntry;
import me.hasenzahn1.pvp.database.PlayerDeathEntry;
import me.hasenzahn1.pvp.database.PlayerStateEntry;
import me.hasenzahn1.pvp.debug.ObjectPrinter;
import me.hasenzahn1.pvp.utils.EventUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.HashMap;
import java.util.UUID;

public class DamageListener implements Listener {

    private final CombatTracker combatTracker;

    public static final HashMap<Player, Double> lastDamageForPlayer = new HashMap<>();

    public DamageListener() {
        combatTracker = new CombatTracker();
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if(!isPlayer(event.getEntity())) return;

        Player victim = (Player) event.getEntity();
        lastDamageForPlayer.put(victim, event.getFinalDamage());

        Entity attackingEntity = EventUtils.getAttackingEntity(event);
        if(!isPlayer(attackingEntity)) return; // Only PvP should be restricted
        PeacefulCommand.PVP_ACTION_TIMESTAMPS.put(attackingEntity.getUniqueId(), System.currentTimeMillis());

        boolean victimMode = PvpSystem.getInstance().getDatabase().getPlayerStates().get(victim.getUniqueId()).state;
        boolean attackerMode = PvpSystem.getInstance().getDatabase().getPlayerStates().get(attackingEntity.getUniqueId()).state;
        if(!victimMode && !attackerMode) return; // Ignore PVP vs PVP

        double damage = combatTracker.getMaxDamageToApply(victim, event);
        event.setDamage(damage);
        event.setDamage(EntityDamageEvent.DamageModifier.ARMOR, 0);
        event.setDamage(EntityDamageEvent.DamageModifier.MAGIC, 0);
    }

    private boolean isPlayer(Entity e) {
        return (e instanceof Player);
    }
}
