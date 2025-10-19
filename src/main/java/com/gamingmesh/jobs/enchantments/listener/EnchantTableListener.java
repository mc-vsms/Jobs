package com.gamingmesh.jobs.enchantments.listener;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.container.Job;
import com.gamingmesh.jobs.container.JobProgression;
import com.gamingmesh.jobs.container.JobsPlayer;
import com.gamingmesh.jobs.enchantments.CustomEnchantmentManager;
import com.gamingmesh.jobs.enchantments.api.CustomEnchantment;
import com.gamingmesh.jobs.enchantments.registry.CustomEnchantmentRegistry;
import net.Zrips.CMILib.Colors.CMIChatColor;
import net.Zrips.CMILib.Version.Schedulers.CMIScheduler;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Listener for enchantment table interactions
 * Adds custom enchantments to enchantment table offers
 */
public class EnchantTableListener implements Listener {

    private final Jobs plugin;
    private final CustomEnchantmentRegistry registry;
    private CustomEnchantmentManager enchantmentManager;

    private static final String ENCHANTER_JOB_NAME = "Enchanter";
    private static final double CUSTOM_ENCHANT_CHANCE = 0.3; // 30% chance to get custom enchant

    public EnchantTableListener(@NotNull Jobs plugin) {
        this.plugin = plugin;
        this.registry = CustomEnchantmentRegistry.getInstance();
        this.enchantmentManager = null; // Will be set later
    }

    /**
     * Set the enchantment manager (called after initialization)
     */
    public void setEnchantmentManager(@Nullable CustomEnchantmentManager manager) {
        this.enchantmentManager = manager;
    }

    /**
     * Get the player's current Enchanter job level
     */
    private int getEnchanterLevel(@NotNull Player player) {
        JobsPlayer jPlayer = Jobs.getPlayerManager().getJobsPlayer(player);
        if (jPlayer == null) {
            return 0;
        }

        for (JobProgression prog : jPlayer.progression) {
            Job job = prog.getJob();
            if (job != null && (ENCHANTER_JOB_NAME.equalsIgnoreCase(job.getName()) ||
                    ENCHANTER_JOB_NAME.equalsIgnoreCase(job.getShortName()))) {
                return prog.getLevel();
            }
        }

        return 0;
    }

    /**
     * Prepare enchantment offers - modify to show custom enchantments
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareItemEnchant(PrepareItemEnchantEvent event) {
        Player player = event.getEnchanter();
        int enchanterLevel = getEnchanterLevel(player);

        // Only modify offers if player has Enchanter job
        if (enchanterLevel <= 0) {
            return; // No job, vanilla enchantments only
        }

        // Get available custom enchantments for this item
        List<CustomEnchantment> availableEnchants = registry.getEnchantsForItem(event.getItem())
                .stream()
                .filter(e -> e.getRequiredJobLevel() <= enchanterLevel)
                .collect(Collectors.toList());

        if (availableEnchants.isEmpty()) {
            return; // No custom enchantments available
        }

        // Get the enchantment offers
        EnchantmentOffer[] offers = event.getOffers();

        // Try to add custom enchantment to one of the offers
        Random random = new Random();
        if (random.nextDouble() < CUSTOM_ENCHANT_CHANCE && offers.length > 0) {
            // Pick a random slot to modify (prefer higher slots for better enchantments)
            int slot = random.nextInt(offers.length);

            // Pick a random custom enchantment
            CustomEnchantment customEnch = availableEnchants.get(random.nextInt(availableEnchants.size()));

            // If the offer exists, we'll mark it for later
            // We can't directly add custom enchantments to the offers here,
            // but we can store information for the actual enchant event
        }
    }

    /**
     * Actually apply custom enchantments when enchanting
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchantItem(EnchantItemEvent event) {
        Player player = event.getEnchanter();
        int enchanterLevel = getEnchanterLevel(player);

        // Allow vanilla enchanting for everyone
        if (enchanterLevel <= 0) {
            return; // No job, vanilla enchantments only - this is OK!
        }

        // Get available custom enchantments for this item
        List<CustomEnchantment> availableEnchants = registry.getEnchantsForItem(event.getItem())
                .stream()
                .filter(e -> e.getRequiredJobLevel() <= enchanterLevel)
                .collect(Collectors.toList());

        if (availableEnchants.isEmpty()) {
            return; // No custom enchantments available
        }

        // Random chance to add a custom enchantment
        Random random = new Random();
        if (random.nextDouble() < CUSTOM_ENCHANT_CHANCE) {
            // Pick a random custom enchantment
            CustomEnchantment customEnch = availableEnchants.get(random.nextInt(availableEnchants.size()));

            // Determine the level based on enchant button clicked
            int maxLevel = Math.min(customEnch.getMaxLevel(),
                    event.getExpLevelCost() / 10 + 1); // Higher cost = higher level
            int level = Math.max(1, random.nextInt(maxLevel) + 1);

            // Notify player first
            String enchantName = CMIChatColor.translate(
                    customEnch.getColor() + customEnch.getDisplayName() +
                    (level > 1 ? " " + getRomanNumeral(level) : ""));

            player.sendMessage(CMIChatColor.translate(
                    "&7[&aエンチャンター&7] &eボーナスエンチャントを獲得: " + enchantName));

            // Add custom enchantment to the item with a slight delay
            // This ensures the vanilla enchantments are already applied
            // Use CMIScheduler for Folia compatibility
            CMIScheduler.get().runTaskLater(plugin, () -> {
                try {
                    // Try to get the item from player's cursor first (most likely location)
                    ItemStack cursorItem = player.getItemOnCursor();
                    ItemStack targetItem = null;

                    if (cursorItem != null && !cursorItem.getType().isAir() && cursorItem.hasItemMeta()) {
                        // Player is holding the enchanted item
                        targetItem = cursorItem;
                        plugin.getLogger().info("Found enchanted item on cursor");
                    } else {
                        // Try to find it in inventory
                        ItemStack resultItem = event.getInventory().getItem(0);
                        if (resultItem != null && !resultItem.getType().isAir() && resultItem.hasItemMeta()) {
                            targetItem = resultItem;
                            plugin.getLogger().info("Found enchanted item in enchantment table");
                        }
                    }

                    if (targetItem != null) {
                        addCustomEnchantToItem(targetItem, customEnch, level, player);

                        // Update based on where the item is
                        if (cursorItem != null && cursorItem == targetItem) {
                            player.setItemOnCursor(targetItem);
                        } else {
                            event.getInventory().setItem(0, targetItem);
                        }

                        // Don't call updateInventory() - it's deprecated and can cause issues
                        plugin.getLogger().info("Successfully added custom enchantment to item!");
                    } else {
                        plugin.getLogger().warning("Could not find enchanted item to add custom enchantment!");
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error adding custom enchantment to item: " + e.getMessage());
                    e.printStackTrace();
                }
            }, 2L); // 2 tick delay to ensure item is in place
        }
    }

    /**
     * Add a custom enchantment to an item via PDC and lore
     */
    private void addCustomEnchantToItem(@NotNull ItemStack item,
                                        @NotNull CustomEnchantment enchantment,
                                        int level,
                                        @NotNull Player player) {
        // Save enchantment data to PersistentDataContainer
        com.gamingmesh.jobs.enchantments.util.EnchantmentUtils.addCustomEnchantment(item, enchantment.getId(), level);

        // Check for discovery and give rewards
        if (enchantmentManager != null && enchantmentManager.getDiscoveryManager() != null) {
            enchantmentManager.getDiscoveryManager()
                .discoverEnchantment(player, enchantment.getId(), enchantment.getDisplayName());
        }

        // Set initial charges if chargeable (this will also update lore via setCharges)
        if (enchantment.isChargeable()) {
            // Don't call setCharges here - it will update lore prematurely
            // Just save the charge data to PDC
            if (!item.hasItemMeta()) {
                return;
            }
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                NamespacedKey key = new NamespacedKey("jobs", "charges_" + enchantment.getId());
                pdc.set(key, PersistentDataType.INTEGER, enchantment.getMaxCharges());
                item.setItemMeta(meta);
            }
        }

        // Note: Lore will be updated when the item is moved from enchantment table to inventory
        // by the ItemLoreUpdateListener

        // Add visual glow effect
        // Note: This uses a workaround with hidden enchantments
        if (item.getEnchantments().isEmpty()) {
            try {
                // Add a fake low-level enchantment to create glow
                item.addUnsafeEnchantment(Enchantment.UNBREAKING, 0);
            } catch (Exception e) {
                // Ignore if it fails
            }
        }

        // Log for debugging
        plugin.getLogger().info("Added custom enchantment to item: " + enchantment.getId() + " (Level " + level + ")");
    }

    /**
     * Convert number to Roman numerals
     */
    private String getRomanNumeral(int number) {
        if (number <= 0 || number > 10) {
            return String.valueOf(number);
        }

        String[] romanNumerals = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return romanNumerals[number];
    }
}
