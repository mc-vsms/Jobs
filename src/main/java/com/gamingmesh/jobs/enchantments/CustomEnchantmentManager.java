package com.gamingmesh.jobs.enchantments;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.enchantments.api.CustomEnchantment;
import com.gamingmesh.jobs.enchantments.discovery.EnchantmentDiscoveryManager;
import com.gamingmesh.jobs.enchantments.listener.ElytraEnchantListener;
import com.gamingmesh.jobs.enchantments.listener.EnchantRestrictionListener;
import com.gamingmesh.jobs.enchantments.listener.EnchantTableListener;
import com.gamingmesh.jobs.enchantments.listener.EnchantmentEffectListener;
import com.gamingmesh.jobs.enchantments.listener.EnchantmentRechargeListener;
import com.gamingmesh.jobs.enchantments.listener.ItemLoreUpdateListener;
import com.gamingmesh.jobs.enchantments.packet.EnchantTablePacketListener;
import com.gamingmesh.jobs.enchantments.registry.CustomEnchantmentRegistry;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Main manager class for custom enchantments in Jobs plugin
 * Handles initialization, configuration, and lifecycle management
 */
public class CustomEnchantmentManager {

    private final Jobs plugin;
    private final CustomEnchantmentRegistry registry;
    private EnchantmentDiscoveryManager discoveryManager;
    private EnchantRestrictionListener restrictionListener;
    private EnchantTableListener enchantTableListener;
    private EnchantmentEffectListener effectListener;
    private EnchantmentRechargeListener rechargeListener;
    private ElytraEnchantListener elytraEnchantListener;
    private EnchantTablePacketListener packetListener;
    private ItemLoreUpdateListener loreUpdateListener;

    private boolean enabled = false;
    private boolean protocolLibAvailable = false;

    public CustomEnchantmentManager(@NotNull Jobs plugin) {
        this.plugin = plugin;
        this.registry = CustomEnchantmentRegistry.getInstance();
    }

    /**
     * Initialize the custom enchantment system
     */
    public void enable() {
        if (enabled) {
            plugin.getLogger().warning("Custom enchantment system is already enabled!");
            return;
        }

        plugin.getLogger().info("Initializing custom enchantment system...");

        // Check for ProtocolLib
        protocolLibAvailable = checkProtocolLib();
        if (!protocolLibAvailable) {
            plugin.getLogger().warning("ProtocolLib not found! Enchantment tooltips will be disabled.");
        }

        // Load enchantments from configuration
        loadEnchantments();

        // Initialize discovery manager
        discoveryManager = new EnchantmentDiscoveryManager(plugin);

        // Initialize packet listener if ProtocolLib is available
        if (protocolLibAvailable) {
            packetListener = new EnchantTablePacketListener(plugin);
            packetListener.enable();
        }

        // Register event listeners
        restrictionListener = new EnchantRestrictionListener(plugin);
        plugin.getServer().getPluginManager().registerEvents(restrictionListener, plugin);

        enchantTableListener = new EnchantTableListener(plugin);
        enchantTableListener.setEnchantmentManager(this);
        plugin.getServer().getPluginManager().registerEvents(enchantTableListener, plugin);

        // Register enchantment effect listener
        effectListener = new EnchantmentEffectListener(plugin);
        plugin.getServer().getPluginManager().registerEvents(effectListener, plugin);

        // Register recharge listener
        rechargeListener = new EnchantmentRechargeListener(plugin);
        plugin.getServer().getPluginManager().registerEvents(rechargeListener, plugin);

        // Register elytra enchant listener
        elytraEnchantListener = new ElytraEnchantListener(plugin);
        plugin.getServer().getPluginManager().registerEvents(elytraEnchantListener, plugin);

        // Register item lore update listener
        loreUpdateListener = new ItemLoreUpdateListener(plugin);
        plugin.getServer().getPluginManager().registerEvents(loreUpdateListener, plugin);

        enabled = true;
        plugin.getLogger().info("Custom enchantment system enabled! Loaded " + registry.size() + " enchantments.");
        plugin.getLogger().info("  - Vanilla enchantments: Available to all players");
        plugin.getLogger().info("  - Custom enchantments: Require Enchanter job");
        if (protocolLibAvailable) {
            plugin.getLogger().info("  - ProtocolLib integration: Active (packet modification enabled)");
        }
    }

    /**
     * Disable the custom enchantment system
     */
    public void disable() {
        if (!enabled) {
            return;
        }

        plugin.getLogger().info("Disabling custom enchantment system...");

        // Disable packet listener
        if (packetListener != null) {
            packetListener.disable();
            packetListener = null;
        }

        // Clear registry
        registry.clear();

        enabled = false;
        plugin.getLogger().info("Custom enchantment system disabled.");
    }

    /**
     * Reload enchantments from configuration
     */
    public void reload() {
        plugin.getLogger().info("Reloading custom enchantments...");

        // Clear existing enchantments
        registry.clear();

        // Reload from config
        loadEnchantments();

        plugin.getLogger().info("Reloaded " + registry.size() + " custom enchantments.");
    }

    /**
     * Load enchantments from configuration
     * This will be implemented to load from config files
     */
    private void loadEnchantments() {
        // TODO: Implement configuration loading
        // For now, load example enchantments for testing
        plugin.getLogger().info("Loading enchantments from configuration...");

        loadExampleEnchantments();
    }

    /**
     * Load example enchantments for testing
     */
    private void loadExampleEnchantments() {
        List<CustomEnchantment> examples = ExampleEnchantments.createExamples();

        for (CustomEnchantment enchantment : examples) {
            boolean success = registry.register(enchantment);
            if (success) {
                plugin.getLogger().info("  Registered: " + enchantment.getId() +
                        " (" + enchantment.getDisplayName() + ")");
            } else {
                plugin.getLogger().warning("  Failed to register: " + enchantment.getId());
            }
        }
    }

    /**
     * Check if ProtocolLib is available
     * @return true if ProtocolLib is loaded
     */
    private boolean checkProtocolLib() {
        Plugin protocolLib = Bukkit.getPluginManager().getPlugin("ProtocolLib");
        return protocolLib != null && protocolLib.isEnabled();
    }

    /**
     * Register a custom enchantment programmatically
     * @param enchantment The enchantment to register
     * @return true if registered successfully
     */
    public boolean registerEnchantment(@NotNull CustomEnchantment enchantment) {
        boolean success = registry.register(enchantment);
        if (success) {
            plugin.getLogger().info("Registered custom enchantment: " + enchantment.getId());
        } else {
            plugin.getLogger().warning("Failed to register enchantment: " + enchantment.getId() + " (already exists)");
        }
        return success;
    }

    /**
     * Unregister a custom enchantment
     * @param id The enchantment ID
     * @return true if unregistered successfully
     */
    public boolean unregisterEnchantment(@NotNull String id) {
        CustomEnchantment removed = registry.unregister(id);
        if (removed != null) {
            plugin.getLogger().info("Unregistered custom enchantment: " + id);
            return true;
        }
        return false;
    }

    /**
     * Get the enchantment registry
     * @return The registry instance
     */
    @NotNull
    public CustomEnchantmentRegistry getRegistry() {
        return registry;
    }

    /**
     * Get the discovery manager
     * @return The discovery manager
     */
    public EnchantmentDiscoveryManager getDiscoveryManager() {
        return discoveryManager;
    }

    /**
     * Check if the system is enabled
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Check if ProtocolLib is available
     * @return true if ProtocolLib is loaded
     */
    public boolean isProtocolLibAvailable() {
        return protocolLibAvailable;
    }
}
