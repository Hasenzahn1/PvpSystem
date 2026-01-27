package me.hasenzahn1.pvp.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.hasenzahn1.pvp.PvpSystem;
import me.hasenzahn1.pvp.database.PlayerStateEntry;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PvpPlaceholderExtension extends PlaceholderExpansion {
    @Override
    public @NotNull String getIdentifier() {
        return "pvpsystem";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Hasenzahn1";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if(params.equalsIgnoreCase("isPeaceful")) {
            PlayerStateEntry entry = PvpSystem.getInstance().getDatabase().getPlayerStates().get(player.getUniqueId());
            if(entry != null && entry.state) return "true";
            return "false";
        }
        return null;
    }
}
