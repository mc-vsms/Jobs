package com.gamingmesh.jobs.enchantments.registry;

import com.gamingmesh.jobs.enchantments.api.CustomEnchantment;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry for all custom enchantments in the Jobs plugin
 * Thread-safe singleton implementation
 */
public class CustomEnchantmentRegistry {

    private static CustomEnchantmentRegistry instance;

    private final Map<String, CustomEnchantment> enchantments;
    private final Map<Material, List<CustomEnchantment>> enchantmentsByMaterial;

    private CustomEnchantmentRegistry() {
        this.enchantments = new ConcurrentHashMap<>();
        this.enchantmentsByMaterial = new ConcurrentHashMap<>();
    }

    /**
     * Get the singleton instance of the registry
     * @return The registry instance
     */
    public static CustomEnchantmentRegistry getInstance() {
        if (instance == null) {
            synchronized (CustomEnchantmentRegistry.class) {
                if (instance == null) {
                    instance = new CustomEnchantmentRegistry();
                }
            }
        }
        return instance;
    }

    /**
     * Register a custom enchantment
     * @param enchantment The enchantment to register
     * @return true if registered successfully, false if ID already exists
     */
    public boolean register(@NotNull CustomEnchantment enchantment) {
        if (enchantments.containsKey(enchantment.getId())) {
            return false;
        }

        enchantments.put(enchantment.getId(), enchantment);

        // Update material mapping
        for (Material material : enchantment.getSupportedMaterials()) {
            enchantmentsByMaterial.computeIfAbsent(material, k -> new ArrayList<>())
                    .add(enchantment);
        }

        return true;
    }

    /**
     * Unregister a custom enchantment
     * @param id The ID of the enchantment to unregister
     * @return The unregistered enchantment, or null if not found
     */
    @Nullable
    public CustomEnchantment unregister(@NotNull String id) {
        CustomEnchantment enchantment = enchantments.remove(id);

        if (enchantment != null) {
            // Remove from material mapping
            for (Material material : enchantment.getSupportedMaterials()) {
                List<CustomEnchantment> list = enchantmentsByMaterial.get(material);
                if (list != null) {
                    list.remove(enchantment);
                    if (list.isEmpty()) {
                        enchantmentsByMaterial.remove(material);
                    }
                }
            }
        }

        return enchantment;
    }

    /**
     * Get a custom enchantment by ID
     * @param id The enchantment ID
     * @return The enchantment, or null if not found
     */
    @Nullable
    public CustomEnchantment getEnchantment(@NotNull String id) {
        return enchantments.get(id);
    }

    /**
     * Get all registered custom enchantments
     * @return Unmodifiable collection of all enchantments
     */
    @NotNull
    public Collection<CustomEnchantment> getAllEnchantments() {
        return Collections.unmodifiableCollection(enchantments.values());
    }

    /**
     * Get all custom enchantments that can be applied to the given material
     * @param material The material to check
     * @return List of applicable enchantments
     */
    @NotNull
    public List<CustomEnchantment> getEnchantsForMaterial(@NotNull Material material) {
        List<CustomEnchantment> list = enchantmentsByMaterial.get(material);
        return list != null ? new ArrayList<>(list) : Collections.emptyList();
    }

    /**
     * Get all custom enchantments that can be applied to the given item
     * @param item The item to check
     * @return List of applicable enchantments
     */
    @NotNull
    public List<CustomEnchantment> getEnchantsForItem(@NotNull ItemStack item) {
        return getEnchantsForMaterial(item.getType()).stream()
                .filter(ench -> ench.canEnchantItem(item))
                .collect(Collectors.toList());
    }

    /**
     * Get all visible enchantments (not hidden from GUI)
     * @return List of visible enchantments
     */
    @NotNull
    public List<CustomEnchantment> getVisibleEnchantments() {
        return enchantments.values().stream()
                .filter(e -> !e.isHiddenFromGui())
                .collect(Collectors.toList());
    }

    /**
     * Get all enchantments that require a specific job level or less
     * @param jobLevel The player's enchanter job level
     * @return List of available enchantments
     */
    @NotNull
    public List<CustomEnchantment> getEnchantsForJobLevel(int jobLevel) {
        return enchantments.values().stream()
                .filter(e -> e.getRequiredJobLevel() <= jobLevel)
                .collect(Collectors.toList());
    }

    /**
     * Check if an enchantment ID is registered
     * @param id The enchantment ID
     * @return true if registered
     */
    public boolean isRegistered(@NotNull String id) {
        return enchantments.containsKey(id);
    }

    /**
     * Clear all registered enchantments
     */
    public void clear() {
        enchantments.clear();
        enchantmentsByMaterial.clear();
    }

    /**
     * Get the number of registered enchantments
     * @return The count
     */
    public int size() {
        return enchantments.size();
    }
}
