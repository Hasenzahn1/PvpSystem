package me.hasenzahn1.pvp.commands.lookup;

import me.hasenzahn1.pvp.database.PlayerDamageEntry;
import me.hasenzahn1.pvp.database.PlayerDeathEntry;
import me.hasenzahn1.pvp.database.Serializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class LookupEntry {

    private final boolean isDeath;
    private final PlayerDeathEntry playerDeathEntry;
    private final PlayerDamageEntry playerDamageEntry;

    public LookupEntry(PlayerDeathEntry playerDeathEntry) {
        this.playerDeathEntry = playerDeathEntry;
        this.playerDamageEntry = null;
        this.isDeath = true;
    }

    public LookupEntry(PlayerDamageEntry playerDamageEntry) {
        this.playerDamageEntry = playerDamageEntry;
        this.playerDeathEntry = null;
        this.isDeath = false;
    }

    public boolean isDeath() {
        return isDeath;
    }

    public boolean isDamage() {
        return !isDeath;
    }

    public PlayerDeathEntry getDeathEntry() {
        return playerDeathEntry;
    }

    public PlayerDamageEntry getDamageEntry() {
        return playerDamageEntry;
    }

    // Common accessors
    public long getTimestamp() {
        return isDeath ? playerDeathEntry.getTimestamp() : playerDamageEntry.getTimestamp();
    }

    public UUID getUuid() {
        return isDeath ? playerDeathEntry.getUuid() : playerDamageEntry.getUuid();
    }

    public String getWorld() {
        return isDeath ? playerDeathEntry.getWorld() : playerDamageEntry.getWorld();
    }

    public double getX() {
        return (int) (isDeath ? playerDeathEntry.getX() : playerDamageEntry.getX());
    }

    public double getY() {
        return (int) (isDeath ? playerDeathEntry.getY() : playerDamageEntry.getY());
    }

    public double getZ() {
        return (int) (isDeath ? playerDeathEntry.getZ() : playerDamageEntry.getZ());
    }

    public String getAttackerString() {
        return isDeath ? playerDeathEntry.getAttackerString() : playerDamageEntry.getAttackerString();
    }

    public EntityDamageEvent.DamageCause getCause() {
        return isDeath ? playerDeathEntry.getCause() : playerDamageEntry.getCause();
    }

    public int getDefenderMode() {
        return isDeath ? playerDeathEntry.getDefenderMode() : playerDamageEntry.getDefenderMode();
    }

    public int getAttackerMode() {
        return isDeath ? playerDeathEntry.getAttackerMode() : playerDamageEntry.getAttackerMode();
    }

    public double getDefenderHealth(){
        return isDeath ? 0 : playerDamageEntry.getDefenderHealth();
    }

    public double getAttackerHealth(){
        return isDeath ? playerDeathEntry.getAttackerHealth() : playerDamageEntry.getAttackerHealth();
    }

    public int getItemsInInv(){
        if(isDamage()) return 0;

        ItemStack[] content = Serializer.base64ToItemStackArray(playerDeathEntry.getInventoryContentsBase64());
        ItemStack[] armor = Serializer.base64ToItemStackArray(playerDeathEntry.getArmorContentsBase64());
        ItemStack[] offhand = Serializer.base64ToItemStackArray(playerDeathEntry.getOffhandBase64());

        return countFromItemStackArray(content) + countFromItemStackArray(armor) + countFromItemStackArray(offhand);
    }

    private int countFromItemStackArray(ItemStack[] items){
        int count = 0;
        for(ItemStack item : items){
            if(item != null && item.getType() != Material.AIR) count++;
        }
        return count;
    }

    public Location getLocation() {
        World world = Bukkit.getWorld(getWorld());
        if(world == null) return null;

        return new Location(world, getX(), getY(), getZ());
    }

    // Death-specific
    public int getLevels() {
        return isDeath ? playerDeathEntry.getLevels() : 0;
    }

    // Damage-specific
    public double getDamage() {
        return isDamage() ? playerDamageEntry.getDamage() : 0;
    }

    public double getOriginalDamage() {
        return isDamage() ? playerDamageEntry.getOriginalDamage() : 0;
    }

    // Helper methods
    public String getDefenderName() {
        return Bukkit.getOfflinePlayer(getUuid()).getName();
    }

    public String getAttackerName() {
        String attacker = getAttackerString();
        if (attacker == null) return "Unknown";
        if (attacker.startsWith("#")) {
            String causeName = attacker.substring(1);
            if (causeName.isEmpty()) return "Unknown";
            return "#" + causeName.substring(0, 1).toUpperCase() + causeName.substring(1).toLowerCase().replace("_", " ");
        }
        try {
            UUID attackerUuid = UUID.fromString(attacker);
            return Bukkit.getOfflinePlayer(attackerUuid).getName();
        } catch (IllegalArgumentException e) {
            return attacker;
        }
    }
}
