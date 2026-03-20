package me.hasenzahn1.pvp.runnables;

import me.hasenzahn1.pvp.PvpSystem;
import me.hasenzahn1.pvp.commands.PeacefulCommand;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CountdownFinishedRunnable extends BukkitRunnable {

    private final int peacefulCommandCooldownAfterPvp;

    public CountdownFinishedRunnable() {
        peacefulCommandCooldownAfterPvp = PvpSystem.getInstance().getConfig().getInt("peacefulCommandCooldownAfterPvp");
    }

    @Override
    public void run() {
        for(Map.Entry<UUID, Long> entry : new HashMap<>(PeacefulCommand.PVP_ACTION_TIMESTAMPS).entrySet()) {
            if((System.currentTimeMillis() - entry.getValue()) > peacefulCommandCooldownAfterPvp) {
                PeacefulCommand.PVP_ACTION_TIMESTAMPS.remove(entry.getKey());
                Player player = Bukkit.getPlayer(entry.getKey());
                if(player == null) continue;
                player.sendActionBar(Component.text(PvpSystem.getPrefixedLang("runnables.cooldownFinished")));
            }
        }
    }

}
