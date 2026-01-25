package me.hasenzahn1.pvp.commands;

import me.hasenzahn1.pvp.PvpSystem;
import me.hasenzahn1.pvp.database.PlayerStateEntry;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.stream.Stream;

public class PeacefulCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NonNull @NotNull String[] args) {
        if(!sender.hasPermission("pvpsystem.commands.peaceful")) {
            sender.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.noPermission")));
            return true;
        }

        if(!(sender instanceof Player)) {
            sender.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.noPlayer")));
            return true;
        }

        Player player = (Player) sender;

        //Toggle
        if(args.length == 0) {
            handleToggle(player);
            return true;
        }

        //On/off
        if(args.length == 1) {
            if(!isBoolean(args[0])) {
                sender.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.invalidCommand", "command", "/" + label + " <on/off>")));
                return true;
            }

            handleSet(player, player, getBoolean(args[0]));
            return true;
        }

        //<player> <on/off>
        if(args.length == 2) {
            if(!isPlayer(args[0])) {
                sender.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.invalidCommand", "command", "/" + label + " <player> <on/off>")));
                return true;
            }
            if(!isBoolean(args[1])) {
                sender.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.invalidCommand", "command", "/" + label + " <player> <on/off>")));
                return true;
            }

            if(!sender.hasPermission("pvpsystem.commands.peaceful.other")) {
                sender.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.noPermission")));
                return true;
            }

            handleSet(player, getPlayer(args[0]), getBoolean(args[1]));
        }
        return true;
    }

    private void handleToggle(Player player){
        PlayerStateEntry state = PvpSystem.getInstance().getDatabase().getPlayerStates().get(player.getUniqueId());
        state.state = !state.state;

        if (state.state) player.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.peaceful.setOn")));
        else player.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.peaceful.setOff")));
    }

    private void handleSet(Player executor, Player player, boolean newState){
        PlayerStateEntry state = PvpSystem.getInstance().getDatabase().getPlayerStates().get(player.getUniqueId());
        state.state = newState;

        if(executor == player) {
            if (state.state) executor.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.peaceful.setOn")));
            else executor.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.peaceful.setOff")));
        }else{
            if (state.state) executor.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.peaceful.setOnOther", "player", player.getName())));
            else executor.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.peaceful.setOffOther", "player", player.getName())));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NonNull @NotNull String[] args) {
        boolean hasPermission = sender.hasPermission("pvpsystem.commands.peaceful");
        boolean hasOtherPermission = sender.hasPermission("pvpsystem.commands.peaceful.other");
        Stream<String> options = Stream.of("on", "off");
        Stream<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName);

        if(!hasPermission) return List.of();
        if(args.length == 1) {
            List<String> onOffCompletions = options.filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).toList();
            if(!onOffCompletions.isEmpty()) return onOffCompletions;

            if(hasOtherPermission) return players.filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).toList();
        }

        if(args.length == 2) {
            if(!hasOtherPermission) return List.of();
            return options.filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).toList();
        }

        return List.of();
    }

    private boolean isBoolean(String string){
        return string.equalsIgnoreCase("on") || string.equalsIgnoreCase("off");
    }

    private boolean isPlayer(String string){
        return Bukkit.getPlayer(string) != null;
    }

    private boolean getBoolean(String string){
        return string.equalsIgnoreCase("on");
    }

    private Player getPlayer(String string){
        return Bukkit.getPlayer(string);
    }
}
