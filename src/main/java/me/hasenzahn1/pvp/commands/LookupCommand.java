package me.hasenzahn1.pvp.commands;

import me.hasenzahn1.pvp.PvpSystem;
import me.hasenzahn1.pvp.commands.lookup.LookupEntry;
import me.hasenzahn1.pvp.commands.lookup.LookupFilter;
import me.hasenzahn1.pvp.commands.lookup.PaginatedMenu;
import me.hasenzahn1.pvp.commands.lookup.PlayerSearchResult;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

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
            sender.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.lookup.filterError", "error", filter.getParseError())));
            return true;
        }

        List<LookupEntry> entries = PvpSystem.getInstance().getDatabase().queryLookupEntries(filter, ((Player) sender).getLocation());
        if(entries.isEmpty()){
            sender.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.lookup.noEntries")));
            return true;
        }

        PlayerSearchResult result = new PlayerSearchResult(entries);
        PvpSystem.getInstance().getPlayerSearchResults().put(((Player) sender).getUniqueId(), result);

        PaginatedMenu menu = new PaginatedMenu(((Player) sender), result);
        menu.display();
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NonNull @NotNull String[] args) {
        if (!sender.hasPermission("pvpsystem.commands.lookup") && shouldCheckLookupPermission) return List.of();

        String currentArg = args[args.length - 1];

        // Determine which filter keys are already used in previous args
        Set<String> usedKeys = new HashSet<>();
        for (int i = 0; i < args.length - 1; i++) {
            int colonIndex = args[i].indexOf(':');
            if (colonIndex != -1) {
                String key = args[i].substring(0, colonIndex);
                usedKeys.add(LookupFilter.resolveKey(key));
            }
        }

        int colonIndex = currentArg.indexOf(':');
        if (colonIndex == -1) {
            // Suggest unused filter keys
            return LookupFilter.FILTER_KEYS.stream()
                    .filter(key -> {
                        String resolved = LookupFilter.resolveKey(key.substring(0, key.length() - 1));
                        return !usedKeys.contains(resolved);
                    })
                    .filter(key -> key.toLowerCase().startsWith(currentArg.toLowerCase()))
                    .toList();
        }

        // Suggest values for the current filter key
        String key = currentArg.substring(0, colonIndex);
        String value = currentArg.substring(colonIndex + 1);
        String resolvedKey = LookupFilter.resolveKey(key);
        String prefix = currentArg.substring(0, colonIndex + 1);

        return switch (resolvedKey) {
            case "user" -> Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(value.toLowerCase()))
                    .map(name -> prefix + name)
                    .toList();
            case "attacker" -> {
                List<String> suggestions = new ArrayList<>(Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(value.toLowerCase()))
                        .map(name -> prefix + name)
                        .toList());
                ATTACKER_VALUES.stream()
                        .filter(v -> v.startsWith(value.toLowerCase()))
                        .map(v -> prefix + v)
                        .forEach(suggestions::add);
                yield suggestions;
            }
            case "cause" -> Arrays.stream(EntityDamageEvent.DamageCause.values())
                    .map(c -> c.name().toLowerCase())
                    .filter(c -> c.startsWith(value.toLowerCase()))
                    .map(c -> prefix + c)
                    .toList();
            case "world" -> Bukkit.getWorlds().stream()
                    .map(World::getName)
                    .filter(w -> w.toLowerCase().startsWith(value.toLowerCase()))
                    .map(w -> prefix + w)
                    .toList();
            case "mode" -> Stream.of("pvp", "peaceful")
                    .filter(m -> m.startsWith(value.toLowerCase()))
                    .map(m -> prefix + m)
                    .toList();
            case "type" -> Stream.of("death", "damage")
                    .filter(t -> t.startsWith(value.toLowerCase()))
                    .map(t -> prefix + t)
                    .toList();
            case "time", "after" -> Stream.of("1m", "5m", "30m", "1h", "6h", "12h", "1d", "7d", "30d")
                    .filter(d -> d.startsWith(value.toLowerCase()))
                    .map(d -> prefix + d)
                    .toList();
            default -> List.of();
        };
    }

    private static final List<String> ATTACKER_VALUES = List.of(
            "#lava", "#fire", "#tnt", "#end_crystal", "#creeper", "#explosion",
            "#fall", "#drowning", "#suffocation", "#starvation", "#poison",
            "#magic", "#void", "#falling_block", "#projectile", "#lightning",
            "#wither", "#dragon_breath", "#kinetic", "#magma_block", "#cramming",
            "#dryout", "#freeze", "#sonic_boom"
    );
}
