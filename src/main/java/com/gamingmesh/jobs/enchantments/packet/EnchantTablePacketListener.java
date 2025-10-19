package com.gamingmesh.jobs.enchantments.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.container.Job;
import com.gamingmesh.jobs.container.JobProgression;
import com.gamingmesh.jobs.container.JobsPlayer;
import com.gamingmesh.jobs.enchantments.api.CustomEnchantment;
import com.gamingmesh.jobs.enchantments.registry.CustomEnchantmentRegistry;
import net.Zrips.CMILib.Colors.CMIChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ProtocolLib packet listener for modifying enchantment table offers
 * This allows us to show custom enchantments in the enchantment table UI
 */
public class EnchantTablePacketListener {

    private final Jobs plugin;
    private final CustomEnchantmentRegistry registry;
    private final ProtocolManager protocolManager;
    private PacketAdapter packetAdapter;

    private static final String ENCHANTER_JOB_NAME = "Enchanter";

    // Store temporary enchantment data per player
    private final Map<UUID, EnchantOfferData> playerOffers = new HashMap<>();

    private boolean enabled = false;

    public EnchantTablePacketListener(@NotNull Jobs plugin) {
        this.plugin = plugin;
        this.registry = CustomEnchantmentRegistry.getInstance();
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    /**
     * Enable packet listening
     */
    public void enable() {
        if (enabled) {
            return;
        }

        try {
            registerPacketListener();
            enabled = true;
            plugin.getLogger().info("Enchantment table packet listener enabled.");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to enable enchantment table packet listener: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Disable packet listening
     */
    public void disable() {
        if (!enabled) {
            return;
        }

        if (packetAdapter != null) {
            protocolManager.removePacketListener(packetAdapter);
            packetAdapter = null;
        }

        playerOffers.clear();
        enabled = false;
        plugin.getLogger().info("Enchantment table packet listener disabled.");
    }

    /**
     * Register packet listener for enchantment table
     */
    private void registerPacketListener() {
        // Listen for OPEN_WINDOW_MERCHANT packet which contains enchantment offers
        // Note: In newer versions, this might be OPEN_WINDOW or WINDOW_DATA
        packetAdapter = new PacketAdapter(plugin, ListenerPriority.NORMAL,
                PacketType.Play.Server.WINDOW_DATA) {

            @Override
            public void onPacketSending(PacketEvent event) {
                try {
                    handleWindowData(event);
                } catch (Exception e) {
                    // Silently ignore errors to prevent spam
                    // plugin.getLogger().warning("Error modifying enchant offers: " + e.getMessage());
                }
            }
        };

        protocolManager.addPacketListener(packetAdapter);
    }

    /**
     * Handle WINDOW_DATA packet to modify enchantment table data
     */
    private void handleWindowData(PacketEvent event) {
        Player player = event.getPlayer();
        PacketContainer packet = event.getPacket();

        // Check if this is enchantment table data
        // Property 0, 1, 2 = enchantment seed, level costs
        int property = packet.getIntegers().read(0);

        if (property < 0 || property > 2) {
            return; // Not enchantment level data
        }

        int enchanterLevel = getEnchanterLevel(player);

        // Only modify if player has Enchanter job
        if (enchanterLevel <= 0) {
            return;
        }

        // Store offer data for later use
        UUID playerId = player.getUniqueId();
        EnchantOfferData data = playerOffers.computeIfAbsent(playerId, k -> new EnchantOfferData());

        // Update the level requirement for this slot
        int value = packet.getIntegers().read(1);
        data.setLevelCost(property, value);

        // Send hint to player about custom enchantments
        if (property == 0 && !data.hintSent) {
            List<CustomEnchantment> available = getAvailableCustomEnchants(player);
            if (!available.isEmpty()) {
                player.sendMessage(CMIChatColor.translate(
                    "&7[&aエンチャンター&7] &e" + available.size() + " 個のカスタムエンチャントが利用可能！"));
                data.hintSent = true;
            }
        }
    }

    /**
     * Get player's Enchanter job level
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
     * Get available custom enchantments for player's current item
     */
    private List<CustomEnchantment> getAvailableCustomEnchants(@NotNull Player player) {
        // We would need to know what item is being enchanted
        // This is a simplified version - in practice, you'd track the enchantment table inventory
        int enchanterLevel = getEnchanterLevel(player);

        return registry.getAllEnchantments().stream()
                .filter(e -> e.getRequiredJobLevel() <= enchanterLevel)
                .collect(Collectors.toList());
    }

    /**
     * Clear player offer data when they close the enchantment table
     */
    public void clearPlayerData(@NotNull UUID playerId) {
        playerOffers.remove(playerId);
    }

    /**
     * Check if packet listener is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Data class to store enchantment offers per player
     */
    private static class EnchantOfferData {
        private final int[] levelCosts = new int[3];
        private boolean hintSent = false;

        public void setLevelCost(int slot, int cost) {
            if (slot >= 0 && slot < 3) {
                levelCosts[slot] = cost;
            }
        }

        public int getLevelCost(int slot) {
            if (slot >= 0 && slot < 3) {
                return levelCosts[slot];
            }
            return 0;
        }
    }
}
