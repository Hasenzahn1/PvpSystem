package me.hasenzahn1.pvp.damage;

import me.hasenzahn1.pvp.PvpSystem;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.HashMap;

public class CombatTracker {

    private HashMap<Player, Double> newMinHealthForPlayer = new HashMap<>();

    private final double damageThreshold;
    private final double damageBelowThreshold;
    private final double lastDamageDuration;

    public CombatTracker(){
        damageThreshold = PvpSystem.getInstance().getConfig().getDouble("damageThreshold", 6);
        damageBelowThreshold = PvpSystem.getInstance().getConfig().getDouble("damageBelowThreshold", 1);
        lastDamageDuration = PvpSystem.getInstance().getConfig().getDouble("lastDamageDuration", 2000);
    }

    public double getMaxDamageToApply(Player player, EntityDamageEvent event) {
        if(!newMinHealthForPlayer.containsKey(player)) newMinHealthForPlayer.put(player, damageThreshold - damageBelowThreshold);
        if(player.getHealth() >= damageThreshold) newMinHealthForPlayer.put(player, damageThreshold - damageBelowThreshold);

        // Calculate damage based on current players health and the new min Health
        double minHealth = newMinHealthForPlayer.get(player);
        double maxDamage = Math.min(player.getHealth() - minHealth, event.getFinalDamage());

        // Reduce new Minhealth
        newMinHealthForPlayer.put(player, Math.max(minHealth - damageBelowThreshold, 0));

        // Return maxDamage
        return maxDamage;
    }

}
