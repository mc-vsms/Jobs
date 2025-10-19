package com.gamingmesh.jobs.enchantments.discovery;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.container.JobProgression;
import com.gamingmesh.jobs.container.JobsPlayer;
import net.Zrips.CMILib.Colors.CMIChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Manages enchantment discovery rewards
 * Players earn experience when discovering new enchantments or effects
 */
public class EnchantmentDiscoveryManager {

    private final Jobs plugin;
    private final NamespacedKey discoveryKey;

    // Reward configuration
    private static final double ENCHANTMENT_DISCOVERY_JOBS_EXP = 100.0;
    private static final int ENCHANTMENT_DISCOVERY_VANILLA_EXP = 350; // ~15 levels worth
    private static final double EFFECT_DISCOVERY_JOBS_EXP = 50.0;
    private static final int EFFECT_DISCOVERY_VANILLA_EXP = 175; // ~7.5 levels worth
    private static final String ENCHANTER_JOB_NAME = "Enchanter";

    public EnchantmentDiscoveryManager(@NotNull Jobs plugin) {
        this.plugin = plugin;
        this.discoveryKey = new NamespacedKey(plugin, "enchant_discoveries");
    }

    /**
     * Check if player has discovered this enchantment
     * @param player The player
     * @param enchantmentId The enchantment ID
     * @return true if already discovered
     */
    public boolean hasDiscovered(@NotNull Player player, @NotNull String enchantmentId) {
        Set<String> discoveries = getDiscoveries(player);
        return discoveries.contains(enchantmentId);
    }

    /**
     * Mark enchantment as discovered and reward player if new
     * @param player The player
     * @param enchantmentId The enchantment ID
     * @param enchantmentName The enchantment display name
     * @return true if this was a new discovery
     */
    public boolean discoverEnchantment(@NotNull Player player, @NotNull String enchantmentId, @NotNull String enchantmentName) {
        if (hasDiscovered(player, enchantmentId)) {
            return false; // Already discovered
        }

        // Add to discoveries
        Set<String> discoveries = getDiscoveries(player);
        discoveries.add(enchantmentId);
        saveDiscoveries(player, discoveries);

        // Give rewards
        giveDiscoveryRewards(player, enchantmentName, true);

        return true;
    }

    /**
     * Mark effect as discovered and reward player if new
     * @param player The player
     * @param effectId The effect ID (e.g., "enchantment:effect_name")
     * @param effectName The effect display name
     * @return true if this was a new discovery
     */
    public boolean discoverEffect(@NotNull Player player, @NotNull String effectId, @NotNull String effectName) {
        if (hasDiscovered(player, effectId)) {
            return false; // Already discovered
        }

        // Add to discoveries
        Set<String> discoveries = getDiscoveries(player);
        discoveries.add(effectId);
        saveDiscoveries(player, discoveries);

        // Give rewards
        giveDiscoveryRewards(player, effectName, false);

        return true;
    }

    /**
     * Give rewards for discovery
     * @param player The player
     * @param discoveryName The name of what was discovered
     * @param isEnchantment true if enchantment, false if effect
     */
    private void giveDiscoveryRewards(@NotNull Player player, @NotNull String discoveryName, boolean isEnchantment) {
        double jobsExp = isEnchantment ? ENCHANTMENT_DISCOVERY_JOBS_EXP : EFFECT_DISCOVERY_JOBS_EXP;
        int vanillaExp = isEnchantment ? ENCHANTMENT_DISCOVERY_VANILLA_EXP : EFFECT_DISCOVERY_VANILLA_EXP;

        // Give Jobs experience to Enchanter job only
        JobsPlayer jPlayer = Jobs.getPlayerManager().getJobsPlayer(player);
        boolean hasEnchanterJob = false;
        if (jPlayer != null) {
            // Find Enchanter job and add experience
            for (JobProgression jobProg : jPlayer.getJobProgression()) {
                if (jobProg != null && jobProg.getJob() != null) {
                    if (ENCHANTER_JOB_NAME.equalsIgnoreCase(jobProg.getJob().getName())) {
                        // Add experience to Enchanter job
                        jobProg.addExperience(jobsExp);
                        hasEnchanterJob = true;
                        break;
                    }
                }
            }
        }

        // Give vanilla experience
        player.giveExp(vanillaExp);

        // Play discovery sound
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

        // Send discovery message
        String type = isEnchantment ? "エンチャント" : "効果";
        player.sendMessage(CMIChatColor.translate("&8&m                                        "));
        player.sendMessage(CMIChatColor.translate("&d&l✦ 新しい" + type + "を発見！ &d&l✦"));
        player.sendMessage("");
        player.sendMessage(CMIChatColor.translate("  &f" + discoveryName));
        player.sendMessage("");
        player.sendMessage(CMIChatColor.translate("  &7報酬:"));
        if (hasEnchanterJob) {
            player.sendMessage(CMIChatColor.translate("  &a+ " + (int)jobsExp + " エンチャンター経験値"));
        }
        player.sendMessage(CMIChatColor.translate("  &b+ " + vanillaExp + " 経験値"));
        player.sendMessage(CMIChatColor.translate("&8&m                                        "));
    }

    /**
     * Get all discoveries for a player
     * @param player The player
     * @return Set of discovered enchantment/effect IDs
     */
    @NotNull
    private Set<String> getDiscoveries(@NotNull Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();

        String discoveryData = pdc.get(discoveryKey, PersistentDataType.STRING);
        if (discoveryData == null || discoveryData.isEmpty()) {
            return new HashSet<>();
        }

        Set<String> discoveries = new HashSet<>();
        for (String discovery : discoveryData.split(",")) {
            if (!discovery.isEmpty()) {
                discoveries.add(discovery);
            }
        }
        return discoveries;
    }

    /**
     * Save discoveries for a player
     * @param player The player
     * @param discoveries Set of discovered enchantment/effect IDs
     */
    private void saveDiscoveries(@NotNull Player player, @NotNull Set<String> discoveries) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();

        String discoveryData = String.join(",", discoveries);
        pdc.set(discoveryKey, PersistentDataType.STRING, discoveryData);
    }

    /**
     * Get total number of discoveries for a player
     * @param player The player
     * @return Number of discoveries
     */
    public int getDiscoveryCount(@NotNull Player player) {
        return getDiscoveries(player).size();
    }

    /**
     * Reset all discoveries for a player (admin command)
     * @param player The player
     */
    public void resetDiscoveries(@NotNull Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        pdc.remove(discoveryKey);
    }
}
