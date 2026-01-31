package me.hasenzahn1.pvp.menu;

import me.hasenzahn1.pvp.PvpSystem;
import me.hasenzahn1.pvp.commands.lookup.LookupEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;

public class MenuButton {

    private final String name;
    private final String displayTemplate;
    private final String displayHoverTemplate;
    private final String commandToExecute;
    private final DisplayCondition condition;

    public MenuButton(String name, String displayTemplate, String displayHoverTemplate, String commandToExecute, DisplayCondition condition) {
        this.name = name;
        this.displayTemplate = displayTemplate;
        this.displayHoverTemplate = displayHoverTemplate;
        this.commandToExecute = commandToExecute;
        this.condition = condition;
    }

    public Component getAsTextComponent(Player player, LookupEntry entry) {
        if(!player.hasPermission("pvpsystem.commands.lookup." + name)) return Component.text("");
        if(condition == DisplayCondition.HAS_TRIGGER && entry.getTriggerKey().isEmpty()) return Component.text("");
        return Component.text(PvpSystem.parseReplacements(displayTemplate, entry.getReplacementParameters()))
                .hoverEvent(Component.text(PvpSystem.parseReplacements(displayHoverTemplate, entry.getReplacementParameters())))
                .clickEvent(ClickEvent.callback((a) -> {
                    player.performCommand(PvpSystem.parseReplacements(commandToExecute, entry.getReplacementParameters()));
                        }, PaginatedMenu.OPTIONS));
    }

    public enum DisplayCondition{
        ALL,
        HAS_TRIGGER,
    }
}
