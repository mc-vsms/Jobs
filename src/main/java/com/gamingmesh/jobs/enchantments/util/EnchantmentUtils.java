package com.gamingmesh.jobs.enchantments.util;

import com.gamingmesh.jobs.enchantments.api.CustomEnchantment;
import com.gamingmesh.jobs.enchantments.registry.CustomEnchantmentRegistry;
import net.Zrips.CMILib.Colors.CMIChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Utility class for working with custom enchantments on items
 */
public class EnchantmentUtils {

    // PersistentDataContainer keys
    private static final String PDC_NAMESPACE = "jobs";
    private static final String PDC_ENCHANTS_KEY = "custom_enchants";
    private static final String PDC_CHARGES_PREFIX = "charges_";

    /**
     * Get all custom enchantments from an item (from PersistentDataContainer)
     * @param item The item to check
     * @return Map of enchantment ID to level
     */
    @NotNull
    public static Map<String, Integer> getCustomEnchantments(@Nullable ItemStack item) {
        Map<String, Integer> enchantments = new HashMap<>();

        if (item == null || !item.hasItemMeta()) {
            return enchantments;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return enchantments;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(PDC_NAMESPACE, PDC_ENCHANTS_KEY);

        // Read enchantments from PDC
        String data = pdc.get(key, PersistentDataType.STRING);
        if (data != null && !data.isEmpty()) {
            // Format: "enchant_id:level,enchant_id:level,..."
            String[] parts = data.split(",");
            for (String part : parts) {
                String[] enchantData = part.split(":");
                if (enchantData.length == 2) {
                    try {
                        String enchantId = enchantData[0];
                        int level = Integer.parseInt(enchantData[1]);
                        enchantments.put(enchantId, level);
                    } catch (NumberFormatException e) {
                        // Skip invalid data
                    }
                }
            }
        }

        return enchantments;
    }

    /**
     * Set custom enchantments on an item (save to PersistentDataContainer)
     * @param item The item to modify
     * @param enchantments Map of enchantment ID to level
     */
    public static void setCustomEnchantments(@NotNull ItemStack item, @NotNull Map<String, Integer> enchantments) {
        if (!item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(PDC_NAMESPACE, PDC_ENCHANTS_KEY);

        if (enchantments.isEmpty()) {
            pdc.remove(key);
        } else {
            // Format: "enchant_id:level,enchant_id:level,..."
            StringBuilder data = new StringBuilder();
            for (Map.Entry<String, Integer> entry : enchantments.entrySet()) {
                if (data.length() > 0) {
                    data.append(",");
                }
                data.append(entry.getKey()).append(":").append(entry.getValue());
            }
            pdc.set(key, PersistentDataType.STRING, data.toString());
        }

        item.setItemMeta(meta);
    }

    /**
     * Add a custom enchantment to an item
     * @param item The item to modify
     * @param enchantId The enchantment ID
     * @param level The enchantment level
     */
    public static void addCustomEnchantment(@NotNull ItemStack item, @NotNull String enchantId, int level) {
        Map<String, Integer> enchantments = getCustomEnchantments(item);
        enchantments.put(enchantId, level);
        setCustomEnchantments(item, enchantments);
    }

    /**
     * Get current charges for an enchantment (from PersistentDataContainer)
     * @param item The item to check
     * @param enchantmentId The enchantment ID
     * @return Current charges, or -1 if not found or not chargeable
     */
    public static int getCurrentCharges(@Nullable ItemStack item, @NotNull String enchantmentId) {
        if (item == null || !item.hasItemMeta()) {
            return -1;
        }

        CustomEnchantment enchantment = CustomEnchantmentRegistry.getInstance().getEnchantment(enchantmentId);
        if (enchantment == null || !enchantment.isChargeable()) {
            return -1;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return -1;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(PDC_NAMESPACE, PDC_CHARGES_PREFIX + enchantmentId);

        Integer charges = pdc.get(key, PersistentDataType.INTEGER);
        if (charges != null) {
            return charges;
        }

        // If no charges stored, return max charges (fully charged)
        return enchantment.getMaxCharges();
    }

    /**
     * Set charges for an enchantment on an item (save to PersistentDataContainer)
     * @param item The item to modify
     * @param enchantmentId The enchantment ID
     * @param charges New charge amount
     * @return true if updated successfully
     */
    public static boolean setCharges(@Nullable ItemStack item, @NotNull String enchantmentId, int charges) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        CustomEnchantment enchantment = CustomEnchantmentRegistry.getInstance().getEnchantment(enchantmentId);
        if (enchantment == null || !enchantment.isChargeable()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        // Clamp charges
        charges = Math.max(0, Math.min(charges, enchantment.getMaxCharges()));

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(PDC_NAMESPACE, PDC_CHARGES_PREFIX + enchantmentId);

        pdc.set(key, PersistentDataType.INTEGER, charges);
        item.setItemMeta(meta);

        // Update lore display to persist the enchantment info
        updateLoreDisplay(item);

        return true;
    }

    /**
     * Update the lore display to match PDC data
     * This updates the item's lore to show custom enchantments with their current charge status
     * @param item The item to update
     */
    public static void updateLoreDisplay(@NotNull ItemStack item) {
        if (!item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        Map<String, Integer> enchantments = getCustomEnchantments(item);
        if (enchantments.isEmpty()) {
            return;
        }

        // Get current lore (for vanilla enchantments, unbreakable, etc.)
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

        // Remove any old custom enchantment sections
        List<String> cleanedLore = new ArrayList<>();
        boolean inCustomSection = false;
        for (String line : lore) {
            String stripped = CMIChatColor.stripColor(line);

            // Check if we're entering a custom enchantment section
            if (stripped.contains("特殊効果")) {
                inCustomSection = true;
                continue; // Skip the header
            }

            // If we're in the custom section, skip until we find an empty line
            if (inCustomSection) {
                if (stripped.trim().isEmpty()) {
                    inCustomSection = false; // End of custom section
                }
                continue; // Skip all lines in custom section
            }

            // Preserve non-custom lore (vanilla enchantments, etc.)
            cleanedLore.add(line);
        }

        // Add new custom enchantment section
        if (!cleanedLore.isEmpty()) {
            cleanedLore.add("");
        }

        cleanedLore.add(CMIChatColor.translate("&6&l[ 特殊効果 ]"));

        CustomEnchantmentRegistry registry = CustomEnchantmentRegistry.getInstance();
        for (Map.Entry<String, Integer> entry : enchantments.entrySet()) {
            CustomEnchantment enchant = registry.getEnchantment(entry.getKey());
            if (enchant == null) continue;

            String levelStr = entry.getValue() > 1 ? " " + getRomanNumeral(entry.getValue()) : "";
            String enchantLine = enchant.getDisplayName() + levelStr;
            cleanedLore.add(CMIChatColor.translate(enchantLine));

            // Add description lines
            for (String descLine : enchant.getDescription()) {
                cleanedLore.add(CMIChatColor.translate("&7" + descLine));
            }

            // Add charges if chargeable (use current charges from PDC)
            if (enchant.isChargeable()) {
                int charges = getCurrentCharges(item, entry.getKey());
                cleanedLore.add(CMIChatColor.translate("&9⚡ チャージ: &f" + charges + "&7/&f" + enchant.getMaxCharges()));
            }
        }

        meta.setLore(cleanedLore);
        item.setItemMeta(meta);
    }

    /**
     * Convert integer to Roman numeral
     */
    private static String getRomanNumeral(int num) {
        if (num <= 0 || num > 10) return String.valueOf(num);
        String[] romans = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return romans[num];
    }

    /**
     * Check if an item has a specific custom enchantment
     * @param item The item to check
     * @param enchantmentId The enchantment ID
     * @return true if the item has this enchantment
     */
    public static boolean hasEnchantment(@Nullable ItemStack item, @NotNull String enchantmentId) {
        return getCustomEnchantments(item).containsKey(enchantmentId);
    }

    /**
     * Get the level of a custom enchantment on an item
     * @param item The item to check
     * @param enchantmentId The enchantment ID
     * @return The level (1-X), or 0 if not present
     */
    public static int getEnchantmentLevel(@Nullable ItemStack item, @NotNull String enchantmentId) {
        return getCustomEnchantments(item).getOrDefault(enchantmentId, 0);
    }

    /**
     * Consume one charge from an enchantment on an item
     * @param item The item to modify
     * @param enchantmentId The enchantment ID
     * @return true if charge was consumed successfully, false if no charges left or not chargeable
     */
    public static boolean consumeCharge(@NotNull ItemStack item, @NotNull String enchantmentId) {
        CustomEnchantment enchantment = CustomEnchantmentRegistry.getInstance().getEnchantment(enchantmentId);
        if (enchantment == null || !enchantment.isChargeable()) {
            return false;
        }

        int currentCharges = getCurrentCharges(item, enchantmentId);
        if (currentCharges > 0) {
            setCharges(item, enchantmentId, currentCharges - 1);
            return true;
        }

        return false;
    }
}
