package me.hasenzahn1.pvp.listeners;

import me.hasenzahn1.pvp.commands.lookup.PaginatedMenu;
import me.hasenzahn1.pvp.debug.ObjectPrinter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;

public class InventoryListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event){
        if(!PaginatedMenu.playersWithInvOpen.contains(event.getWhoClicked().getUniqueId())) return;
        if(event.getClickedInventory() == null) return;

        boolean isTopInventory = event.getView().getTopInventory().equals(event.getClickedInventory());

        if (isTopInventory) {
            event.getCursor();
            if (!event.getCursor().getType().isAir()) {
                event.setCancelled(true);
            }
        }

        if (event.isShiftClick() && event.getClickedInventory().getType() == InventoryType.PLAYER) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event){
        PaginatedMenu.playersWithInvOpen.remove(event.getPlayer().getUniqueId());
    }

}
