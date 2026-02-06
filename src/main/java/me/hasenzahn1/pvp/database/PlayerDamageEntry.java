package me.hasenzahn1.pvp.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import me.hasenzahn1.pvp.PvpSystem;
import me.hasenzahn1.pvp.utils.EventUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.UUID;

@DatabaseTable(tableName = "damageEntries")
public class PlayerDamageEntry {

    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField
    private UUID uuid;

    @DatabaseField
    private String world;

    @DatabaseField
    private double x;

    @DatabaseField
    private double y;

    @DatabaseField
    private double z;

    @DatabaseField
    private String attacker;

    @DatabaseField
    private double originalDamage;

    @DatabaseField
    private double damage;

    @DatabaseField
    private EntityDamageEvent.DamageCause cause;

    @DatabaseField
    private long timestamp;

    @DatabaseField
    private int defenderMode;

    @DatabaseField
    private int attackerMode;

    @DatabaseField
    private double attackerHealth;

    @DatabaseField
    private double defenderHealth;

    @DatabaseField
    private String triggerKey;

    public PlayerDamageEntry() {}

    public PlayerDamageEntry(EntityDamageEvent event, double originalDamage) {
        this.uuid = event.getEntity().getUniqueId();
        this.world = event.getEntity().getWorld().getName();
        this.x = event.getEntity().getLocation().getX();
        this.y = event.getEntity().getLocation().getY();
        this.z = event.getEntity().getLocation().getZ();
        this.originalDamage = originalDamage;
        this.damage = event.getFinalDamage();
        this.attacker = EventUtils.getStringCauseFromEvent(event);
        this.cause = event.getCause();
        this.timestamp = System.currentTimeMillis();
        this.defenderHealth = (event.getEntity() instanceof LivingEntity) ? ((LivingEntity) event.getEntity()).getHealth() : -1;

        //Load Defendermode
        PlayerStateEntry defenderState = PvpSystem.getInstance().getDatabase().getPlayerStates().getOrDefault(event.getEntity().getUniqueId(), null);
        if(defenderState == null) this.defenderMode = -1;
        else this.defenderMode = defenderState.state ? 1 : 0;

        //Load Attackemode
        Entity attackingEntity = EventUtils.getAttackingEntity(event);
        this.attackerHealth = (attackingEntity instanceof LivingEntity) ? ((LivingEntity) attackingEntity).getHealth() : -1;
        if(!(attackingEntity instanceof Player)) this.attackerMode = -1;
        else {
            PlayerStateEntry attackerState = PvpSystem.getInstance().getDatabase().getPlayerStates().getOrDefault(attackingEntity.getUniqueId(), null);
            if(attackerState == null) this.attackerMode = -1;
            else this.attackerMode = attackerState.state ? 1 : 0;
        }

        //Gather trigger key
        triggerKey = PvpSystem.getInstance().getActionTriggerManager().getActionTriggerKey(Bukkit.getPlayer(uuid), attackingEntity);
    }

    public void create(){
        PvpSystem.getInstance().getDatabase().addDamageEntry(this);
    }

    // Getters
    public int getId() {
        return id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getWorld() {
        return world;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public String getAttackerString() {
        return attacker;
    }

    public double getOriginalDamage() {
        return originalDamage;
    }

    public double getDamage() {
        return damage;
    }

    public EntityDamageEvent.DamageCause getCause() {
        return cause;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getDefenderMode() {
        return defenderMode;
    }

    public int getAttackerMode() {
        return attackerMode;
    }

    public double getAttackerHealth() {
        return attackerHealth;
    }

    public double getDefenderHealth() {
        return defenderHealth;
    }

    public String getTriggerKey() {
        return triggerKey;
    }

    public void setTriggerKey(String triggerKey) {
        this.triggerKey = triggerKey;
    }
}
