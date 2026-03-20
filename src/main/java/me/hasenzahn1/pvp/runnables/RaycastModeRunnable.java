package me.hasenzahn1.pvp.runnables;

import me.hasenzahn1.pvp.PvpSystem;
import me.hasenzahn1.pvp.database.PlayerStateEntry;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;

public class RaycastModeRunnable extends BukkitRunnable {

    @Override
    public void run() {
        for(Player p : Bukkit.getOnlinePlayers()) {
            RayTraceResult result = p.rayTraceEntities(10);
            if(result == null) continue;
            if(result.getHitEntity() == null) continue;
            if(!(result.getHitEntity() instanceof Player)) continue;

            Player player = ((Player) result.getHitEntity());
            PlayerStateEntry entry = PvpSystem.getInstance().getDatabase().getPlayerStates().get(player.getUniqueId());
            if(entry == null) continue;

            if(entry.state){
                p.sendActionBar(Component.text(PvpSystem.getLang("runnables.raycastPeacefulEnabled", "player", player.getName())));
            }else{
                p.sendActionBar(Component.text(PvpSystem.getLang("runnables.raycastPvPEnabled", "player", player.getName())));
            }
        }
    }

}
