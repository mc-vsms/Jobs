package com.gamingmesh.jobs.enchantments.listener;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.enchantments.util.EnchantmentUtils;
import net.Zrips.CMILib.Version.Schedulers.CMIScheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Listener that updates item lore when items with custom enchantments are moved in inventory
 * This ensures the lore is persisted when items are taken from enchantment tables
 */
public class ItemLoreUpdateListener implements Listener {

    private final Jobs plugin;

    public ItemLoreUpdateListener(@NotNull Jobs plugin) {
        this.plugin = plugin;
    }

    /**
     * Update lore when items are moved between inventory slots
     * This ensures enchantment lore is persisted when items are moved
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        InventoryAction action = event.getAction();

        // Ignore actions that don't move items
        if (action == InventoryAction.NOTHING ||
            action == InventoryAction.CLONE_STACK ||
            action == InventoryAction.UNKNOWN) {
            return;
        }

        // Only update lore on PLACE actions to avoid double-updates
        // PICKUP happens first, then PLACE - we only want to update after the item is placed
        if (action == InventoryAction.PICKUP_ALL ||
            action == InventoryAction.PICKUP_HALF ||
            action == InventoryAction.PICKUP_ONE ||
            action == InventoryAction.PICKUP_SOME) {
            return;
        }

        // Check if there's actually an item involved
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        if ((clickedItem == null || clickedItem.getType().isAir()) &&
            (cursorItem == null || cursorItem.getType().isAir())) {
            return; // No items involved, skip
        }

        // Find which item has custom enchantments
        ItemStack itemToUpdate = null;

        if (cursorItem != null && !cursorItem.getType().isAir()) {
            Map<String, Integer> cursorEnchants = EnchantmentUtils.getCustomEnchantments(cursorItem);
            if (!cursorEnchants.isEmpty()) {
                itemToUpdate = cursorItem;
            }
        }

        if (itemToUpdate == null && clickedItem != null && !clickedItem.getType().isAir()) {
            Map<String, Integer> clickedEnchants = EnchantmentUtils.getCustomEnchantments(clickedItem);
            if (!clickedEnchants.isEmpty()) {
                itemToUpdate = clickedItem;
            }
        }

        if (itemToUpdate == null && event.getHotbarButton() >= 0) {
            ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());
            if (hotbarItem != null && !hotbarItem.getType().isAir()) {
                Map<String, Integer> hotbarEnchants = EnchantmentUtils.getCustomEnchantments(hotbarItem);
                if (!hotbarEnchants.isEmpty()) {
                    itemToUpdate = hotbarItem;
                }
            }
        }

        if (itemToUpdate == null) {
            return; // No items with enchantments found
        }

        // Update lore to persist enchantment information
        EnchantmentUtils.updateLoreDisplay(itemToUpdate);
    }
}
