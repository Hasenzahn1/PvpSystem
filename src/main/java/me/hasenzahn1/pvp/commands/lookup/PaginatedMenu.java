package me.hasenzahn1.pvp.commands.lookup;

import me.hasenzahn1.pvp.PvpSystem;
import me.hasenzahn1.pvp.database.PlayerDamageEntry;
import me.hasenzahn1.pvp.database.Serializer;
import me.hasenzahn1.pvp.utils.PlaceholderUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;

public class PaginatedMenu {

    private static final ClickCallback.Options OPTIONS = ClickCallback.Options.builder()
            .lifetime(Duration.ofMinutes(60))
            .uses(100)
            .build();

    public static final ArrayList<UUID> playersWithInvOpen = new ArrayList<>();

    private Player player;
    private PlayerSearchResult result;

    public PaginatedMenu(Player player, PlayerSearchResult result) {
        this.player = player;
        this.result = result;
    }

    public void display(){
        List<LookupEntry> entries = result.getEntriesForPage();

        TextComponent.Builder builder = Component.text();

        //Header
        String header = PvpSystem.getLang("commands.lookup.ui.header", "entries", result.getEntryCount());
        Component headerComponent = Component.text(header);
        builder.append(headerComponent).appendNewline();

        //Entries
        for(LookupEntry entry : entries){
            builder.append(buildEntryComponent(entry)).appendNewline();
        }

        //Pagination
        builder.append(buildPaginationComponent());

        player.sendMessage(builder.build());
    }

    private Component buildEntryComponent(LookupEntry entry){
        long timestamp = entry.getTimestamp();
        String defender = entry.getDefenderName();
        int defenderMode = entry.getDefenderMode();
        String attacker = entry.getAttackerName();
        int attackerMode = entry.getAttackerMode();
        Location location = entry.getLocation();
        double defenderHealth = entry.getDefenderHealth();
        double attackerHealth = entry.getAttackerHealth();

        SimpleDateFormat formatter = new SimpleDateFormat(PvpSystem.getLang("commands.lookup.ui.entry.timestamp"));
        Component timestampComponent = Component.text(formatter.format(timestamp));

        Component defenderComponent = Component.text(getModeColor(defenderMode) + defender);
        Component attackerComponent = Component.text(getModeColor(attackerMode) + attacker);

        Component defenderHealthComponent = Component.text(Math.round(defenderHealth) + "");
        Component attackerHealthComponent = Component.text(Math.round(attackerHealth) + "");

        Component posComponent = buildPosComponent(location);
        if(!player.hasPermission("pvpsystem.commands.lookup.teleport")) posComponent = Component.text("");

        Component infoComponent = buildInfoComponent(entry);
        if(!player.hasPermission("pvpsystem.commands.lookup.info")) infoComponent = Component.text("");

        String lineTemplate = entry.isDeath() ? PvpSystem.getLang("commands.lookup.ui.entry.deathLine") : PvpSystem.getLang("commands.lookup.ui.entry.damageLine");

        return PlaceholderUtil.parseTemplate(lineTemplate, Map.of(
                "timestamp", timestampComponent,
                "defender", defenderComponent,
                "attacker", attackerComponent,
                "defenderHealth", defenderHealthComponent,
                "attackerHealth", attackerHealthComponent,
                "pos", posComponent,
                "info", infoComponent
        ));
    }

    private Component buildPosComponent(Location location){
        if(location == null) return null;
        return Component.text(PvpSystem.getLang("commands.lookup.ui.entry.pos"))
                .hoverEvent(Component.text(PvpSystem.getLang("commands.lookup.ui.entry.posHover",
                        "world", location.getWorld().getName(),
                        "x", location.getBlockX(),
                        "y", location.getBlockY(),
                        "z", location.getBlockZ()
                )))
                .clickEvent(ClickEvent.callback((audience) -> {
                    player.setGameMode(GameMode.SPECTATOR);
                    player.teleport(new Location(location.getWorld(), location.getX(), Math.max(location.getY(), -64), location.getZ()));

                    player.sendMessage(buildSuccessTeleportComponent());
                }, OPTIONS));
    }

    private Component buildSuccessTeleportComponent(){
        return Component.text(PvpSystem.getPrefixedLang("commands.lookup.ui.entry.teleportSuccess"))
                .hoverEvent(Component.text(PvpSystem.getLang("commands.lookup.ui.entry.teleportHover")))
                .clickEvent(ClickEvent.callback((audience) -> {
                    player.setGameMode(GameMode.SURVIVAL);
                }, OPTIONS));
    }

    private Component buildInfoComponent(LookupEntry entry){
        String template = entry.isDeath() ? "commands.lookup.ui.entry.deathInfoHover" : "commands.lookup.ui.entry.damageInfoHover";
        Component hover = Component.text(PvpSystem.getLang(template,
                "defender", getModeColor(entry.getDefenderMode()) + entry.getDefenderName(),
                "attacker", getModeColor(entry.getAttackerMode()) + entry.getAttackerName(),
                "cause", entry.getCause(),
                "attackerHealth", String.format("%.1f", entry.getAttackerHealth()),
                "defenderHealth", String.format("%.1f", entry.getDefenderHealth()),
                "timestamp", new SimpleDateFormat(PvpSystem.getLang("commands.lookup.ui.entry.timestamp")).format(entry.getTimestamp()),
                "world", entry.getWorld(),
                "x", entry.getX(),
                "y", entry.getY(),
                "z", entry.getZ(),
                "levels", entry.getLevels(),
                "damage", String.format("%.1f", entry.getDamage()),
                "originalDamage", String.format("%.1f", entry.getOriginalDamage()),
                "itemsInInv", entry.getItemsInInv()
                ));
        return Component.text(PvpSystem.getLang("commands.lookup.ui.entry.info")).hoverEvent(hover).clickEvent(ClickEvent.callback((audience) -> {
            displayInfoInterface(entry);
        }, OPTIONS));
    }

    private void displayInfoInterface(LookupEntry entry){
        String template = entry.isDeath() ? "commands.lookup.ui.info.deathInfo" : "commands.lookup.ui.info.damageInfo";
        Component text = Component.text(PvpSystem.getLang(template,
                "defender", getModeColor(entry.getDefenderMode()) + entry.getDefenderName(),
                "attacker", getModeColor(entry.getAttackerMode()) + entry.getAttackerName(),
                "cause", entry.getCause(),
                "attackerHealth", String.format("%.1f", entry.getAttackerHealth()),
                "defenderHealth", String.format("%.1f", entry.getDefenderHealth()),
                "timestamp", new SimpleDateFormat(PvpSystem.getLang("commands.lookup.ui.entry.timestamp")).format(entry.getTimestamp()),
                "world", entry.getWorld(),
                "x", entry.getX(),
                "y", entry.getY(),
                "z", entry.getZ(),
                "levels", entry.getLevels(),
                "damage", String.format("%.1f", entry.getDamage()),
                "originalDamage", String.format("%.1f", entry.getOriginalDamage()),
                "itemsInInv", entry.getItemsInInv()
        ));
        player.sendMessage(text);
        if(!entry.isDeath()) return;

        TextComponent.Builder builder = Component.text();
        if(player.hasPermission("pvpsystem.commands.lookup.view")) builder.append(buildViewComponent(entry));
        if(player.hasPermission("pvpsystem.commands.lookup.openinv")) builder.append(buildOpenInvComponent(entry));
        if(player.hasPermission("pvpsystem.commands.lookup.reset")) builder.append(buildResetComponent(entry));

        player.sendMessage(builder.build());
    }

    private Component buildViewComponent(LookupEntry entry){
        if(entry.isDamage()) return Component.text("");
        return Component.text(PvpSystem.getLang("commands.lookup.ui.info.viewButton"))
                .hoverEvent(Component.text(PvpSystem.getLang("commands.lookup.ui.info.viewHover")))
                .clickEvent(ClickEvent.callback((audience) -> {
                    Inventory inventory = Bukkit.createInventory(null, 9*5, Component.text(PvpSystem.getLang("commands.lookup.ui.inventoryTitle", "player", entry.getDefenderName())));
                    ItemStack[] contents = Serializer.base64ToItemStackArray(entry.getDeathEntry().getInventoryContentsBase64());
                    ItemStack[] armor = Serializer.base64ToItemStackArray(entry.getDeathEntry().getArmorContentsBase64());
                    ItemStack[] offhand = Serializer.base64ToItemStackArray(entry.getDeathEntry().getOffhandBase64());

                    for(int i = 9; i < contents.length; i++){
                        inventory.setItem(i - 9, contents[i]);
                    }

                    for(int i = 0; i < 9; i++){
                        inventory.setItem(9*3 + i, contents[i]);
                    }

                    for(int i = 0; i < armor.length; i++){
                        inventory.setItem(9*4+3-i, armor[i]);
                    }

                    inventory.setItem(9*4+4, offhand[0]);

                    playersWithInvOpen.add(player.getUniqueId());
                    player.openInventory(inventory);
                },  OPTIONS));
    }

    private Component buildResetComponent(LookupEntry entry){
        if(entry.isDamage()) return Component.text("");
        return Component.text(PvpSystem.getLang("commands.lookup.ui.info.resetButton"))
                .hoverEvent(Component.text(PvpSystem.getLang("commands.lookup.ui.info.resetHover")))
                .clickEvent(ClickEvent.callback((audience) -> {
                    Player player = Bukkit.getPlayer(entry.getUuid());
                    if(player == null) {
                        this.player.sendMessage(Component.text(PvpSystem.getPrefixedLang("commands.lookup.ui.playerNotOnline")));
                        return;
                    }

                    ItemStack[] contents = Serializer.base64ToItemStackArray(entry.getDeathEntry().getInventoryContentsBase64());
                    ItemStack[] armor = Serializer.base64ToItemStackArray(entry.getDeathEntry().getArmorContentsBase64());
                    ItemStack[] offhand = Serializer.base64ToItemStackArray(entry.getDeathEntry().getOffhandBase64());

                    player.getInventory().setContents(contents);
                    player.getInventory().setArmorContents(armor);
                    player.getInventory().setItemInOffHand(offhand[0]);
                },  OPTIONS));
    }

    private Component buildOpenInvComponent(LookupEntry entry){
        if(entry.isDamage()) return Component.text("");
        return Component.text(PvpSystem.getLang("commands.lookup.ui.info.openInvButton"))
                .hoverEvent(Component.text(PvpSystem.getLang("commands.lookup.ui.info.openInvHover")))
                .clickEvent(ClickEvent.callback((audience) -> {
                    player.performCommand("openinv " + entry.getUuid());
                },  OPTIONS));
    }

    private String getModeColor(int mode){
        return switch (mode) {
            case 1 -> PvpSystem.getLang("commands.lookup.ui.entry.mode.peaceful");
            case 0 -> PvpSystem.getLang("commands.lookup.ui.entry.mode.violent");
            default -> PvpSystem.getLang("commands.lookup.ui.entry.mode.non");
        };
    }

    private Component buildPaginationComponent(){
        Component prev = result.getPage() > 0 ? buildSwitchComponent(Component.text(PvpSystem.getLang("commands.lookup.ui.page.prev")), result.getPage() - 1) : Component.text("");
        Component next = result.getPage() < result.getMaxPages() - 1 ? buildSwitchComponent(Component.text(PvpSystem.getLang("commands.lookup.ui.page.next")), result.getPage() + 1) : Component.text("");

        TextComponent.Builder builder = Component.text();
        for(int i = Math.max(0, result.getPage() - 3); i < Math.min(result.getMaxPages(), result.getPage() + 3); i++){
            if(result.getPage() == i) {
                builder.append(buildSwitchComponent(Component.text(PvpSystem.getLang("commands.lookup.ui.page.selectedPage", "page", i + 1)), i));
            } else {
                builder.append(buildSwitchComponent(Component.text(PvpSystem.getLang("commands.lookup.ui.page.otherPage", "page", i + 1)), i));
            }
            if(i < Math.min(result.getMaxPages(), result.getPage() + 3) - 1){
                builder.append(Component.text(PvpSystem.getLang("commands.lookup.ui.page.separator")));
            }
        }
        Component pages = builder.build();

        return PlaceholderUtil.parseTemplate(PvpSystem.getLang("commands.lookup.ui.page.template", "current", result.getPage() + 1, "max", result.getMaxPages()), Map.of(
                "prev", prev,
                "pages", pages,
                "next", next
        ));
    }

    private Component buildSwitchComponent(Component text, int newPage){
        return text.clickEvent(ClickEvent.callback((audience) -> {
            result.setPage(newPage);
            display();
        }, OPTIONS));
    }

}
