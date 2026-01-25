package me.hasenzahn1.pvp.listeners;

import me.hasenzahn1.pvp.PvpSystem;
import me.hasenzahn1.pvp.database.PlayerStateEntry;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class ConnectionListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        PlayerStateEntry playerStateEntry = PlayerStateEntry.queryForId(event.getPlayer().getUniqueId());
        if(playerStateEntry == null) return;
        PvpSystem.getInstance().getDatabase().getPlayerStates().put(event.getPlayer().getUniqueId(), playerStateEntry);

        int messageDuration = PvpSystem.getInstance().getConfig().getInt("joinMessageDuration", 20);
        String message = "";
        if(playerStateEntry.state) message = PvpSystem.getLang("join.message.peaceful");
        else message = PvpSystem.getLang("join.message.notPeaceful");

        String finalMessage = message;
        final int[] counter = {messageDuration / 10};
        new BukkitRunnable() {

            @Override
            public void run() {
                if(counter[0] <= 0) cancel();
                counter[0]--;

                event.getPlayer().sendActionBar(Component.text(finalMessage));
            }
        }.runTaskTimer(PvpSystem.getInstance(), 0, 10);
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event){
        PlayerStateEntry state = PvpSystem.getInstance().getDatabase().getPlayerStates().get(event.getPlayer().getUniqueId());
        if(state == null) return;

        PvpSystem.getInstance().getDatabase().getPlayerStates().remove(event.getPlayer().getUniqueId());
        state.update();
    }

}
