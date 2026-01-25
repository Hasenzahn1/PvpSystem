package me.hasenzahn1.pvp.commands;

import com.google.common.collect.ImmutableMap;
import me.hasenzahn1.pvp.PvpSystem;
import me.hasenzahn1.pvp.database.PlayerDeathEntry;
import me.hasenzahn1.pvp.database.Serializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class DeathHistoryCommand implements CommandExecutor, TabExecutor {

    public static final int ENTRIES = 10;

    private static final ClickCallback.Options CALLBACK_OPTIONS = ClickCallback.Options.builder()
            .lifetime(Duration.ofMinutes(10))
            .uses(ClickCallback.UNLIMITED_USES)
            .build();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("pvpsystem.commands.deathhistory")) {
            sender.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.noPermission")));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.noPlayer")));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.invalidCommand", "command", "/deathhistory <player> [page]")));
            return true;
        }

        String playerName = args[0];
        int page = 1;
        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
                if (page < 1) page = 1;
            } catch (NumberFormatException e) {
                page = 1;
            }
        }

        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(playerName);
        if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()) {
            sender.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.deathhistory.playerNotFound", "player", playerName)));
            return true;
        }

        List<PlayerDeathEntry> deaths = PvpSystem.getInstance().getDatabase().getDeathsForPlayer(targetPlayer.getUniqueId());
        if (deaths.isEmpty()) {
            sender.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.deathhistory.noDeaths", "player", targetPlayer.getName())));
            return true;
        }

        displayDeathHistory(player, targetPlayer, deaths, page);
        return true;
    }

    private void displayDeathHistory(Player viewer, OfflinePlayer target, List<PlayerDeathEntry> deaths, int page) {
        int maxPage = (int) Math.ceil((double) deaths.size() / ENTRIES);
        if (page > maxPage) page = maxPage;

        int startIndex = (page - 1) * ENTRIES;
        int endIndex = Math.min(startIndex + ENTRIES, deaths.size());

        TextComponent.Builder message = Component.text();

        // Header
        message.append(Component.text(PvpSystem.getLang("commands.deathhistory.ui.header", "player", target.getName())));
        message.append(Component.newline());

        // Death entries
        for (int i = startIndex; i < endIndex; i++) {
            PlayerDeathEntry death = deaths.get(i);
            message.append(buildDeathEntry(viewer, death));
            message.append(Component.newline());
        }

        // Pagination
        message.append(buildPagination(viewer, target, page, maxPage));
        message.append(Component.newline());

        viewer.sendMessage(message.build());
    }

    private Component buildDeathEntry(Player viewer, PlayerDeathEntry death) {
        // Format placeholders
        String timestamp = DATE_FORMAT.format(new Date(death.getTimestamp()));
        String timeInDays = calculateTime(death.getTimestamp());
        String cause = death.getCause() != null ? death.getCause().name() : "Unknown";
        String attacker = death.getAttackerName();
        String defenderMode = getModeDisplay(death.getDefenderMode());
        String defenderModeLong = getModeDisplayLong(death.getDefenderMode());
        String attackerMode = getModeDisplay(death.getAttackerMode());
        String attackerModeLong = getModeDisplayLong(death.getAttackerMode());
        String levelsS = String.valueOf(death.getLevels());
        String xp = String.valueOf(death.getXp());
        String world = death.getWorld();
        String x = String.format("%.0f", death.getX());
        String y = String.format("%.0f", death.getY());
        String z = String.format("%.0f", death.getZ());

        //Days
        String daysText = PvpSystem.getLang("commands.deathhistory.ui.entry.days", "time", timeInDays);
        String daysHover = PvpSystem.getLang("commands.deathhistory.ui.entry.daysHover", "timestamp", timestamp);
        Component days = Component.text(daysText).hoverEvent(Component.text(daysHover));

        //Mode
        String modeText = PvpSystem.getLang("commands.deathhistory.ui.entry.mode", "defenderMode", defenderMode, "attackerMode", attackerMode);
        String modeHover = PvpSystem.getLang("commands.deathhistory.ui.entry.modeHover", "defenderModeLong", defenderModeLong, "attackerModeLong", attackerModeLong);
        Component mode = Component.text(modeText).hoverEvent(Component.text(modeHover));

        //Source
        String sourceText = PvpSystem.getLang("commands.deathhistory.ui.entry.source", "attacker", attacker);
        String sourceHover = PvpSystem.getLang("commands.deathhistory.ui.entry.sourceHover", "attacker", attacker, "cause", cause);
        Component source = Component.text(sourceText).hoverEvent(Component.text(sourceHover));

        //Levels
        String levelsText = PvpSystem.getLang("commands.deathhistory.ui.entry.levels", "levels", levelsS);
        String levelsHover = PvpSystem.getLang("commands.deathhistory.ui.entry.levelsHover", "levels", attacker, "xp", xp);
        Component levels = Component.text(levelsText).hoverEvent(Component.text(levelsHover));

        //Tp Button
        Component teleportButton = Component.text("");
        if (viewer.hasPermission("pvpsystem.commands.deathhistory.teleport")) {
            teleportButton = buildTeleportButton(death, world, x, y, z);
        }

        //View Button
        Component viewButton = Component.text("");
        if (viewer.hasPermission("pvpsystem.commands.deathhistory.view")) {
            viewButton = buildViewButton(viewer, death);
        }

        //Reset Button
        Component resetButton = Component.text("");
        if (viewer.hasPermission("pvpsystem.commands.deathhistory.reset")) {
            resetButton = buildResetButton(death);
        }

        //combine line
        String lineTemplate = PvpSystem.getLang("commands.deathhistory.ui.entry.line");
        return replaceComponents(lineTemplate, new ImmutableMap.Builder<String, Component>()
                    .put("days", days)
                    .put("mode", mode)
                    .put("source", source)
                    .put("levels", levels)
                    .put("tp", teleportButton)
                    .put("view", viewButton)
                    .put("reset", resetButton)
            .build()
        );
    }

    private Component replaceComponents(String template, Map<String, Component> placeholders) {
        TextComponent.Builder builder = Component.text();

        int index = 0;
        while (index < template.length()) {
            int start = template.indexOf('%', index);
            if (start == -1) {
                builder.append(Component.text(template.substring(index)));
                break;
            }

            int end = template.indexOf('%', start + 1);
            if (end == -1) {
                builder.append(Component.text(template.substring(index)));
                break;
            }

            // Text before placeholder
            if (start > index) {
                builder.append(Component.text(template.substring(index, start)));
            }

            String key = template.substring(start + 1, end);
            Component replacement = placeholders.getOrDefault(key, Component.text("%" + key + "%"));

            builder.append(replacement);
            index = end + 1;
        }

        return builder.build();
    }

    private String calculateTime(long date) {
        long now = System.currentTimeMillis();
        long diffMillis = now - date;
        double days = diffMillis / (1000.0 * 60 * 60 * 24);
        return String.format(Locale.ENGLISH, "%.1f", Math.round(days * 10.0) / 10.0);
    }

    private Component buildTeleportButton(PlayerDeathEntry death, String world, String x, String y, String z) {
        String buttonText = PvpSystem.getLang("commands.deathhistory.ui.buttons.teleport");
        String hoverText = PvpSystem.getLang("commands.deathhistory.ui.buttons.teleportHover",
                "world", world,
                "x", x,
                "y", y,
                "z", z
        );

        return Component.text(buttonText)
                .hoverEvent(HoverEvent.showText(Component.text(hoverText)))
                .clickEvent(ClickEvent.callback(audience -> {
                    if (audience instanceof Player player) {
                        teleportToDeathLocation(player, death);
                    }
                }, CALLBACK_OPTIONS));
    }

    private Component buildViewButton(Player viewer, PlayerDeathEntry death) {
        String buttonText = PvpSystem.getLang("commands.deathhistory.ui.buttons.view");
        String hoverText = PvpSystem.getLang("commands.deathhistory.ui.buttons.viewHover");

        return Component.text(buttonText)
                .hoverEvent(HoverEvent.showText(Component.text(hoverText)))
                .clickEvent(ClickEvent.callback(audience -> {
                    if (audience instanceof Player player) {
                        openInventoryView(player, death);
                    }
                }, CALLBACK_OPTIONS));
    }

    private Component buildResetButton(PlayerDeathEntry death) {
        String buttonText = PvpSystem.getLang("commands.deathhistory.ui.buttons.reset");
        String hoverText = PvpSystem.getLang("commands.deathhistory.ui.buttons.resetHover");

        return Component.text(buttonText)
                .hoverEvent(HoverEvent.showText(Component.text(hoverText)))
                .clickEvent(ClickEvent.callback(audience -> {
                    if (audience instanceof Player player) {
                        resetInventoryAndXp(player, death);
                    }
                }, CALLBACK_OPTIONS));
    }

    private Component buildPagination(Player viewer, OfflinePlayer target, int currentPage, int maxPage) {
        TextComponent.Builder pagination = Component.text();

        // Build page numbers string
        StringBuilder pagesBuilder = new StringBuilder();
        int startPage = Math.max(1, currentPage - 3);
        int endPage = Math.min(maxPage, currentPage + 3);

        TextComponent.Builder pagesComponent = Component.text();
        for (int i = startPage; i <= endPage; i++) {
            if (i > startPage) {
                pagesComponent.append(Component.text(
                        PvpSystem.getLang("commands.deathhistory.ui.pagination.pageSeparator")
                ));
            }

            String pageText;
            if (i == currentPage) {
                pageText = PvpSystem.getLang("commands.deathhistory.ui.pagination.pageNumberCurrent", "page", String.valueOf(i));
            } else {
                pageText = PvpSystem.getLang("commands.deathhistory.ui.pagination.pageNumber", "page", String.valueOf(i));
            }

            String pageHover = PvpSystem.getLang("commands.deathhistory.ui.pagination.pageHover", "page", String.valueOf(i));
            final int pageNum = i;

            Component pageComponent = Component.text(pageText)
                    .hoverEvent(HoverEvent.showText(Component.text(pageHover)))
                    .clickEvent(ClickEvent.callback(audience -> {
                        if (audience instanceof Player player) {
                            List<PlayerDeathEntry> deaths = PvpSystem.getInstance().getDatabase().getDeathsForPlayer(target.getUniqueId());
                            displayDeathHistory(player, target, deaths, pageNum);
                        }
                    }, CALLBACK_OPTIONS));

            pagesComponent.append(pageComponent);
        }

        // Full pagination format
        String format = PvpSystem.getLang("commands.deathhistory.ui.pagination.format",
                "page", String.valueOf(currentPage),
                "maxPage", String.valueOf(maxPage)
        );

        // Split format at %pages% and insert component
        String[] parts = format.split("%pages%", 2);
        if (parts.length == 2) {
            pagination.append(Component.text(parts[0]));
            pagination.append(pagesComponent.build());
            pagination.append(Component.text(parts[1]));
        } else {
            pagination.append(Component.text(format));
            pagination.append(pagesComponent.build());
        }

        return pagination.build();
    }

    // Action methods

    private void teleportToDeathLocation(Player player, PlayerDeathEntry death) {
        World world = Bukkit.getWorld(death.getWorld());
        if (world == null) {
            player.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.deathhistory.ui.actions.teleportWorldNotFound", "world", death.getWorld())));
            return;
        }

        Location location = new Location(world, death.getX(), death.getY(), death.getZ());
        player.teleport(location);
        player.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.deathhistory.ui.actions.teleported")));
    }

    private void openInventoryView(Player player, PlayerDeathEntry death) {
        OfflinePlayer deadPlayer = Bukkit.getOfflinePlayer(death.getUuid());
        String title = PvpSystem.getLang("commands.deathhistory.ui.actions.viewTitle", "player", deadPlayer.getName());

        // Create inventory
        Inventory inventory = Bukkit.createInventory(null, 9*5, Component.text(title));

        // Deserialize and set inventory contents
        try {
            ItemStack[] contents = Serializer.base64ToItemStackArray(death.getInventoryContentsBase64());
            for(int i = 0; i < 9; i++) {
                inventory.setItem(i + 27, contents[i]);
            }

            for (int i = 9; i < Math.min(contents.length, 36); i++) {
                inventory.setItem(i - 9, contents[i]);
            }

            // Armor slots (slots 36-39 in our display)
            ItemStack[] armor = Serializer.base64ToItemStackArray(death.getArmorContentsBase64());
            // Display armor in row 5 (slots 36-39): boots, leggings, chestplate, helmet
            for (int i = 0; i < Math.min(armor.length, 4); i++) {
                inventory.setItem(36 + i, armor[3-i]);
            }

            // Offhand (slot 40)
            ItemStack[] offhand = Serializer.base64ToItemStackArray(death.getOffhandBase64());
            if (offhand.length > 0) {
                inventory.setItem(40, offhand[0]);
            }
        } catch (Exception e) {
            player.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.noPermission"))); // Reuse for error
            e.printStackTrace();
            return;
        }

        player.openInventory(inventory);
    }

    private void resetInventoryAndXp(Player executor, PlayerDeathEntry death) {
        Player targetPlayer = Bukkit.getPlayer(death.getUuid());
        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(death.getUuid());

        if (targetPlayer == null || !targetPlayer.isOnline()) {
            executor.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.deathhistory.ui.actions.resetPlayerOffline", "player", offlineTarget.getName())));
            return;
        }

        try {
            // Restore inventory
            ItemStack[] contents = Serializer.base64ToItemStackArray(death.getInventoryContentsBase64());
            targetPlayer.getInventory().setContents(contents);

            // Restore armor
            ItemStack[] armor = Serializer.base64ToItemStackArray(death.getArmorContentsBase64());
            targetPlayer.getInventory().setArmorContents(armor);

            // Restore offhand
            ItemStack[] offhand = Serializer.base64ToItemStackArray(death.getOffhandBase64());
            if (offhand.length > 0) {
                targetPlayer.getInventory().setItemInOffHand(offhand[0]);
            }

            // Restore XP
            targetPlayer.setLevel(death.getLevels());
            targetPlayer.setExp(death.getXp());

            executor.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.deathhistory.ui.actions.resetSuccess", "player", targetPlayer.getName())));
        } catch (Exception e) {
            executor.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.noPermission"))); // Reuse for error
            e.printStackTrace();
        }
    }

    private String getModeDisplay(int mode) {
        return switch (mode) {
            case 0 -> PvpSystem.getLang("commands.deathhistory.ui.modes.pvp");
            case 1 -> PvpSystem.getLang("commands.deathhistory.ui.modes.peaceful");
            default -> PvpSystem.getLang("commands.deathhistory.ui.modes.unknown");
        };
    }

    private String getModeDisplayLong(int mode) {
        return switch (mode) {
            case 0 -> PvpSystem.getLang("commands.deathhistory.ui.modes.pvpLong");
            case 1 -> PvpSystem.getLang("commands.deathhistory.ui.modes.peacefulLong");
            default -> PvpSystem.getLang("commands.deathhistory.ui.modes.unknownLong");
        };
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("pvpsystem.commands.deathhistory")) {
            return List.of();
        }

        if (args.length == 1) {
            // Player name suggestions
            String partial = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            // Page number suggestions
            return List.of("1", "2", "3");
        }

        return List.of();
    }
}
