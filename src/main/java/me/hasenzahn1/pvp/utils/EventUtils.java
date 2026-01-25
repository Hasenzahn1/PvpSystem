package me.hasenzahn1.pvp.utils;

import org.bukkit.damage.DamageSource;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class EventUtils {

    public static Player getCausingPlayerFromEvent(EntityDamageEvent event) {
        DamageSource source = event.getDamageSource();
        if(source.getCausingEntity() == null) return null;
        if(source.getCausingEntity() instanceof Player) return (Player)source.getCausingEntity();
        return null;
    }

    public static String getStringCauseFromEvent(EntityDamageEvent damageEvent) {
        if(damageEvent == null) return "#unknown";
        if(!(damageEvent.getEntity() instanceof Player)) return "#unknown";
        Player player = ((Player) damageEvent.getEntity());

        // Check player
        DamageSource source = damageEvent.getDamageSource();
        if(source.getCausingEntity() instanceof Player) return source.getCausingEntity().getUniqueId().toString();
        if(source.getCausingEntity() != null) return "#" + source.getCausingEntity().getType().name().toLowerCase();

        // no player involved return damage cause
        EntityDamageEvent.DamageCause cause = damageEvent.getCause();

        return switch (cause) {
            case LAVA -> "#lava";
            case FIRE, FIRE_TICK -> "#fire";
            case BLOCK_EXPLOSION, ENTITY_EXPLOSION -> {
                if (damageEvent instanceof EntityDamageByEntityEvent) {
                    Entity damagingEntity = ((EntityDamageByEntityEvent) damageEvent).getDamager();
                    if (damagingEntity instanceof TNTPrimed) {
                        yield "#tnt";
                    } else if (damagingEntity instanceof EnderCrystal) {
                        yield "#end_crystal";
                    } else if (damagingEntity instanceof Creeper) {
                        yield "#creeper";
                    }
                }
                yield "#explosion";
            }
            case FALL -> "#fall";
            case DROWNING -> "#drowning";
            case SUFFOCATION -> "#suffocation";
            case STARVATION -> "#starvation";
            case POISON -> "#poison";
            case MAGIC -> "#magic";
            case VOID -> "#void";
            case FALLING_BLOCK -> "#falling_block";
            case PROJECTILE -> "#projectile";
            case ENTITY_ATTACK, ENTITY_SWEEP_ATTACK -> {
                if (damageEvent instanceof EntityDamageByEntityEvent) {
                    Entity damagingEntity = ((EntityDamageByEntityEvent) damageEvent).getDamager();
                    yield "#" + damagingEntity.getType().name().toLowerCase();
                }
                yield "#entity";
            }
            case LIGHTNING -> "#lightning";
            case WITHER -> "#wither";
            case DRAGON_BREATH -> "#dragon_breath";
            case FLY_INTO_WALL -> "#kinetic";
            case HOT_FLOOR -> "#magma_block";
            case CRAMMING -> "#cramming";
            case DRYOUT -> "#dryout";
            case FREEZE -> "#freeze";
            case SONIC_BOOM -> "#sonic_boom";
            default -> "#" + cause.name().toLowerCase();
        };
    }

}
