package me.hasenzahn1.pvp.commands;

import me.hasenzahn1.pvp.PvpSystem;
import me.hasenzahn1.pvp.database.PlayerStateEntry;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.stream.Stream;

public class PvPModifyCommand implements CommandExecutor, TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NonNull @NotNull String[] args) {
        if(!sender.hasPermission("pvpsystem.commands.pvpmodify")) {
            sender.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.noPermission")));
            return true;
        }

        if(args.length != 2){
            sender.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.invalidCommand", "command", "/" + label + " <selector> <disablepvp/forcepeaceful>")));
            return true;
        }

        OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);
        PlayerStateEntry state = PlayerStateEntry.queryForIdWithoutCreation(player.getUniqueId());
        if(state == null) {
            sender.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.notAPlayer", "name", args[0])));
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "disablepvp" -> {
                toggleDisablePvp(sender, state, player);
            }
            case "forcepeaceful" -> {
                toggleForcePeaceful(sender, state, player);
            }
            default -> {
                sender.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.invalidCommand", "command", "/" + label + " <selector> <disablepvp/forcepeaceful>")));
                return true;
            }
        }

        state.update();
        PvpSystem.getInstance().getDatabase().getPlayerStates().put(player.getUniqueId(), state);

        return true;
    }

    private void toggleDisablePvp(CommandSender sender, PlayerStateEntry state, OfflinePlayer receiver) {
        state.disablePVP = !state.disablePVP;
        state.state = true;

        if(state.disablePVP) {
            sender.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.pvpModify.disablePvp.setEnabled", "player", receiver.getName())));
        }else {
            sender.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.pvpModify.disablePvp.setDisabled", "player", receiver.getName())));
        }

        Player onlinePlayer = receiver.getPlayer();
        if(onlinePlayer == null) return;
        if(state.disablePVP) {
            onlinePlayer.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.pvpModify.disablePvp.enabled", "player", receiver.getName())));
        }else {
            onlinePlayer.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.pvpModify.disablePvp.disabled", "player", receiver.getName())));
        }
    }

    private void toggleForcePeaceful(CommandSender sender, PlayerStateEntry state, OfflinePlayer receiver) {
        state.forcePeaceful = !state.forcePeaceful;
        state.state = true;

        if(state.forcePeaceful) {
            sender.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.pvpModify.forcePeaceful.setEnabled", "player", receiver.getName())));
        }else {
            sender.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.pvpModify.forcePeaceful.setDisabled", "player", receiver.getName())));
        }

        Player onlinePlayer = receiver.getPlayer();
        if(onlinePlayer == null) return;
        if(state.forcePeaceful) {
            onlinePlayer.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.pvpModify.forcePeaceful.enabled", "player", receiver.getName())));
        }else {
            onlinePlayer.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.pvpModify.forcePeaceful.disabled", "player", receiver.getName())));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NonNull @NotNull String[] args) {
        if(args.length == 1){
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(f -> f.toLowerCase().startsWith(args[0].toLowerCase())).toList();
        }
        if(args.length == 2){
            return Stream.of("forcePeaceful", "disablepvp").filter(f -> f.toLowerCase().startsWith(args[1].toLowerCase())).toList();
        }
        return List.of();
    }
}
