package me.hasenzahn1.pvp.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import me.hasenzahn1.pvp.PvpSystem;
import me.hasenzahn1.pvp.utils.EventUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.UUID;

@DatabaseTable(tableName = "deathEntries")
public class PlayerDeathEntry {

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
    private int levels;

    @DatabaseField
    private float xp;

    @DatabaseField
    private String inventoryContentsBase64;

    @DatabaseField
    private String armorContentsBase64;

    @DatabaseField
    private String offhandBase64;

    @DatabaseField
    private EntityDamageEvent.DamageCause cause;

    @DatabaseField
    private long timestamp;

    @DatabaseField
    private int defenderMode;

    @DatabaseField
    private int attackerMode;

    public PlayerDeathEntry() {}

    public PlayerDeathEntry(PlayerDeathEvent event){
        this.uuid = event.getPlayer().getUniqueId();
        this.world = event.getPlayer().getWorld().getName();
        this.x = event.getPlayer().getLocation().getX();
        this.y = event.getPlayer().getLocation().getY();
        this.z = event.getPlayer().getLocation().getZ();
        this.attacker = EventUtils.getStringCauseFromEvent(event.getPlayer().getLastDamageCause());
        this.levels = event.getPlayer().getLevel();
        this.xp = event.getPlayer().getExp();
        this.inventoryContentsBase64 = Serializer.itemStackArrayToBase64(event.getPlayer().getInventory().getContents());
        this.armorContentsBase64 = Serializer.itemStackArrayToBase64(event.getPlayer().getInventory().getArmorContents());
        this.offhandBase64 = Serializer.itemStackArrayToBase64(new ItemStack[] {event.getPlayer().getInventory().getItemInOffHand()});
        this.cause = event.getEntity().getLastDamageCause() != null ? event.getEntity().getLastDamageCause().getCause() : EntityDamageEvent.DamageCause.CUSTOM;
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

    public Player getAttacker(){
        if(this.attacker.startsWith("#")) return null;
        return Bukkit.getPlayer(UUID.fromString(this.attacker));
    }



    public void update(){
        try {
            PvpSystem.getInstance().getDatabase().getPlayerDeathDao().update(this);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void create(){
        try {
            PvpSystem.getInstance().getDatabase().getPlayerDeathDao().create(this);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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

    public int getLevels() {
        return levels;
    }

    public float getXp() {
        return xp;
    }

    public String getInventoryContentsBase64() {
        return inventoryContentsBase64;
    }

    public String getArmorContentsBase64() {
        return armorContentsBase64;
    }

    public String getOffhandBase64() {
        return offhandBase64;
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

    /**
     * Gets the attacker's name. Returns the player name if attacker was a player,
     * otherwise returns a formatted cause string (e.g., "Zombie", "Fall", "Lava")
     */
    public String getAttackerName() {
        if (attacker == null) return "Unknown";
        if (attacker.startsWith("#")) {
            String causeName = attacker.substring(1);
            if (causeName.isEmpty()) return "Unknown";
            return causeName.substring(0, 1).toUpperCase() + causeName.substring(1).toLowerCase().replace("_", " ");
        }
        // It's a UUID, try to get player name
        try {
            UUID attackerUuid = UUID.fromString(attacker);
            return Bukkit.getOfflinePlayer(attackerUuid).getName();
        } catch (IllegalArgumentException e) {
            return attacker;
        }
    }

    /**
     * Gets the mode as a readable string
     * @param mode -1 = unknown, 0 = PvP, 1 = Peaceful
     */
    public static String getModeString(int mode) {
        return switch (mode) {
            case 0 -> "PvP";
            case 1 -> "Peaceful";
            default -> "Unknown";
        };
    }
}
