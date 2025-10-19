package com.gamingmesh.jobs.enchantments.listener;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.container.Job;
import com.gamingmesh.jobs.container.JobProgression;
import com.gamingmesh.jobs.container.JobsPlayer;
import com.gamingmesh.jobs.enchantments.api.CustomEnchantment;
import com.gamingmesh.jobs.enchantments.registry.CustomEnchantmentRegistry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.jetbrains.annotations.NotNull;

import net.Zrips.CMILib.Colors.CMIChatColor;

import java.util.List;

/**
 * Listener that restricts custom enchantments to players with the Enchanter job
 *
 * IMPORTANT: This listener ONLY restricts CUSTOM enchantments.
 * Vanilla Minecraft enchantments are available to all players regardless of job.
 */
public class EnchantRestrictionListener implements Listener {

    private final Jobs plugin;
    private final CustomEnchantmentRegistry registry;

    private static final String ENCHANTER_JOB_NAME = "Enchanter";

    public EnchantRestrictionListener(@NotNull Jobs plugin) {
        this.plugin = plugin;
        this.registry = CustomEnchantmentRegistry.getInstance();
    }

    /**
     * Check if a player has the Enchanter job and meets the level requirement
     * @param player The player to check
     * @param requiredLevel The required enchanter job level
     * @return true if player meets requirements
     */
    private boolean hasEnchanterJob(@NotNull Player player, int requiredLevel) {
        JobsPlayer jPlayer = Jobs.getPlayerManager().getJobsPlayer(player);
        if (jPlayer == null) {
            return false;
        }

        // Find Enchanter job progression
        for (JobProgression prog : jPlayer.progression) {
            Job job = prog.getJob();
            if (job != null && isEnchanterJob(job)) {
                return prog.getLevel() >= requiredLevel;
            }
        }

        return false;
    }

    /**
     * Check if a job is the Enchanter job
     * @param job The job to check
     * @return true if this is the Enchanter job
     */
    private boolean isEnchanterJob(@NotNull Job job) {
        return ENCHANTER_JOB_NAME.equalsIgnoreCase(job.getName()) ||
               ENCHANTER_JOB_NAME.equalsIgnoreCase(job.getShortName());
    }

    /**
     * Get the player's current Enchanter job level
     * @param player The player
     * @return The enchanter level, or 0 if not an enchanter
     */
    private int getEnchanterLevel(@NotNull Player player) {
        JobsPlayer jPlayer = Jobs.getPlayerManager().getJobsPlayer(player);
        if (jPlayer == null) {
            return 0;
        }

        for (JobProgression prog : jPlayer.progression) {
            Job job = prog.getJob();
            if (job != null && isEnchanterJob(job)) {
                return prog.getLevel();
            }
        }

        return 0;
    }

    /**
     * Event handler for enchanting items at an enchantment table
     * NOTE: Vanilla enchantments are NOT restricted - only custom enchantments require the job
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnchantItem(EnchantItemEvent event) {
        // Vanilla enchantments are always allowed for everyone
        // Custom enchantments are handled by EnchantTableListener
        // This listener is primarily for validation and future custom logic
    }

    /**
     * Event handler for preparing enchantments at an enchantment table
     * NOTE: This does not prevent vanilla enchantments - they work for everyone
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPrepareItemEnchant(PrepareItemEnchantEvent event) {
        // Vanilla enchantments are always shown for everyone
        // Custom enchantments are handled by EnchantTableListener based on job level
    }

    /**
     * Event handler for anvil enchanting
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (event.getResult() == null) {
            return;
        }

        Player player = null;
        if (event.getView().getPlayer() instanceof Player) {
            player = (Player) event.getView().getPlayer();
        }

        if (player == null) {
            return;
        }

        // Check if custom enchantments are being applied via anvil
        // This will be implemented when we add enchanted book support

        // TODO: Implement anvil custom enchantment restriction
    }

    /**
     * Send a message to the player about job requirements
     * @param player The player
     * @param requiredLevel The required job level
     */
    private void sendJobRequirementMessage(@NotNull Player player, int requiredLevel) {
        String message = CMIChatColor.translate("&cこのエンチャントを使用するには、エンチャンタージョブのレベル " + requiredLevel +
                " が必要です！");
        player.sendMessage(message);
    }

    /**
     * Check if a player can use a specific custom enchantment
     * @param player The player
     * @param enchantment The enchantment
     * @return true if player can use it
     */
    public boolean canUseEnchantment(@NotNull Player player, @NotNull CustomEnchantment enchantment) {
        return hasEnchanterJob(player, enchantment.getRequiredJobLevel());
    }
}
