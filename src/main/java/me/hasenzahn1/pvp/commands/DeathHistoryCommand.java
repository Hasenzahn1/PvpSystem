package me.hasenzahn1.pvp.commands;

import me.hasenzahn1.pvp.PvpSystem;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class DeathHistoryCommand extends LookupCommand {

    public DeathHistoryCommand() {
        super(false);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NonNull @NotNull String[] args) {
        if(!sender.hasPermission("pvpsystem.commands.deathhistory")){
            sender.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.noPermission")));
            return true;
        }
        if(!(sender instanceof Player)){
            sender.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.noPlayer")));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.invalidCommand", "command", "/" + label + " <user> <lookupArgs>")));
            return true;
        }

        args[0] = "u:" + args[0];
        String[] newArgs = new  String[args.length + 1];
        System.arraycopy(args, 0, newArgs, 1, args.length);
        newArgs[0] = "ty:death";
        super.onCommand(sender, command, label, newArgs);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NonNull @NotNull String[] args) {
        if(!sender.hasPermission("pvpsystem.commands.deathhistory")) return List.of();
        if(args.length == 1) return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(f -> f.toLowerCase().startsWith(args[0].toLowerCase())).toList();

        args[0] = "u:" + args[0];
        String[] newArgs = new  String[args.length + 1];
        System.arraycopy(args, 0, newArgs, 1, args.length);
        newArgs[0] = "ty:death";
        return super.onTabComplete(sender, command, label, newArgs);
    }
}
