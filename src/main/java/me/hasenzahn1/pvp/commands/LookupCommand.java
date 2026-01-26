package me.hasenzahn1.pvp.commands;

import me.hasenzahn1.pvp.PvpSystem;
import me.hasenzahn1.pvp.commands.lookup.LookupEntry;
import me.hasenzahn1.pvp.commands.lookup.LookupFilter;
import me.hasenzahn1.pvp.commands.lookup.PaginatedMenu;
import me.hasenzahn1.pvp.commands.lookup.PlayerSearchResult;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class LookupCommand implements CommandExecutor, TabCompleter {

    protected boolean shouldCheckLookupPermission;

    public LookupCommand() {
        this.shouldCheckLookupPermission = true;
    }

    protected LookupCommand(boolean shouldCheckLookupPermission) {
        this.shouldCheckLookupPermission = shouldCheckLookupPermission;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NonNull @NotNull String[] args) {
        if(!sender.hasPermission("pvpsystem.commands.lookup") && shouldCheckLookupPermission){
            sender.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.noPermission")));
            return true;
        }
        if(!(sender instanceof Player)){
            sender.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.noPlayer")));
            return true;
        }

        LookupFilter filter = new LookupFilter(((Player) sender));
        if(!filter.parse(args)){
            sender.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.lookup.filterError", "filter", filter.getFilterSummary())));
            return true;
        }

        List<LookupEntry> entries = PvpSystem.getInstance().getDatabase().queryLookupEntries(filter, ((Player) sender).getLocation());
        PlayerSearchResult result = new PlayerSearchResult(entries);
        PvpSystem.getInstance().getPlayerSearchResults().put(((Player) sender).getUniqueId(), result);

        PaginatedMenu menu = new PaginatedMenu(((Player) sender), result);
        menu.display();
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NonNull @NotNull String[] args) {
        return List.of();
    }
}
