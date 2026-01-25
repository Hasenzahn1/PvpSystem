package me.hasenzahn1.pvp.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import me.hasenzahn1.pvp.PvpSystem;
import me.hasenzahn1.pvp.utils.EventUtils;
import org.bukkit.Bukkit;
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

    public PlayerDamageEntry() {}

    public PlayerDamageEntry(EntityDamageEvent event, double originalDamage) {
        this.uuid = event.getEntity().getUniqueId();
        this.world = event.getEntity().getWorld().getName();
        this.x = event.getEntity().getLocation().getX();
        this.y = event.getEntity().getLocation().getY();
        this.z = event.getEntity().getLocation().getZ();
        this.originalDamage = originalDamage;
        this.damage = event.getDamage();
        this.attacker = EventUtils.getStringCauseFromEvent(event);
        this.cause = event.getCause();
        this.timestamp = System.currentTimeMillis();

        //Load Defendermode
        PlayerStateEntry defenderState = PvpSystem.getInstance().getDatabase().getPlayerStates().getOrDefault(event.getEntity().getUniqueId(), null);
        if(defenderState == null) this.defenderMode = -1;
        else this.defenderMode = defenderState.state ? 1 : 0;

        //Load Attackemode
        Player attackingPlayer = getAttacker();
        if(attackingPlayer == null) this.attackerMode = -1;
        else {
            PlayerStateEntry attackerState = PvpSystem.getInstance().getDatabase().getPlayerStates().getOrDefault(attackingPlayer.getUniqueId(), null);
            if(attackerState == null) this.attackerMode = -1;
            else this.attackerMode = attackerState.state ? 1 : 0;
        }
    }

    private Player getAttacker(){
        if(this.attacker.startsWith("#")) return null;
        return Bukkit.getPlayer(UUID.fromString(this.attacker));
    }



    public void create(){
        PvpSystem.getInstance().getDatabase().addDamageEntry(this);
    }
}
