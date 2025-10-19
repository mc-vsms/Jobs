package com.gamingmesh.jobs.enchantments.api;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Interface for custom enchantments in Jobs plugin
 * Only available to players with the Enchanter job
 */
public interface CustomEnchantment {

    /**
     * Get the unique identifier for this enchantment
     * @return The enchantment ID
     */
    @NotNull
    String getId();

    /**
     * Get the display name of this enchantment
     * @return The display name
     */
    @NotNull
    String getDisplayName();

    /**
     * Get the description of this enchantment
     * @return List of description lines
     */
    @NotNull
    List<String> getDescription();

    /**
     * Get the color for this enchantment's tooltip
     * @return The color code (e.g., "&6", "&b")
     */
    @NotNull
    String getColor();

    /**
     * Get the maximum level for this enchantment
     * @return The maximum level
     */
    int getMaxLevel();

    /**
     * Get the minimum level required in the Enchanter job to use this enchantment
     * @return The required job level
     */
    int getRequiredJobLevel();

    /**
     * Check if this is a curse enchantment
     * @return true if this is a curse
     */
    boolean isCurse();

    /**
     * Check if this enchantment has charges
     * @return true if chargeable
     */
    boolean isChargeable();

    /**
     * Get the maximum charges for this enchantment
     * @return The maximum charges, or 0 if not chargeable
     */
    int getMaxCharges();

    /**
     * Check if this enchantment has visual effects
     * @return true if visual effects are enabled
     */
    boolean hasVisualEffects();

    /**
     * Get the materials this enchantment can be applied to (primary items)
     * @return Set of applicable materials
     */
    @NotNull
    Set<Material> getPrimaryMaterials();

    /**
     * Get all materials this enchantment supports (including cross-support)
     * @return Set of supported materials
     */
    @NotNull
    Set<Material> getSupportedMaterials();

    /**
     * Check if this enchantment can be applied to the given item
     * @param item The item to check
     * @return true if applicable
     */
    boolean canEnchantItem(@NotNull ItemStack item);

    /**
     * Get the Bukkit enchantment this custom enchantment is based on
     * Can be null for completely custom enchantments
     * @return The base enchantment or null
     */
    @Nullable
    Enchantment getBaseEnchantment();

    /**
     * Get enchantments that conflict with this one
     * @return Set of conflicting enchantments
     */
    @NotNull
    Set<Enchantment> getConflicts();

    /**
     * Get the rarity/weight of this enchantment for random selection
     * @return The rarity weight (higher = more common)
     */
    int getRarity();

    /**
     * Check if this enchantment is hidden from the enchantments GUI
     * @return true if hidden
     */
    boolean isHiddenFromGui();
}
