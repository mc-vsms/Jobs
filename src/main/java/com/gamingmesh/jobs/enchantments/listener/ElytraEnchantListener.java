package com.gamingmesh.jobs.enchantments.listener;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.container.Job;
import com.gamingmesh.jobs.container.JobProgression;
import com.gamingmesh.jobs.container.JobsPlayer;
import net.Zrips.CMILib.Colors.CMIChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Allows players with Jobs to enchant elytras with chest plate enchantments
 */
public class ElytraEnchantListener implements Listener {

    private final Jobs plugin;

    private static final String ENCHANTER_JOB_NAME = "Enchanter";
    private static final int REQUIRED_ENCHANTER_LEVEL = 30;

    // Chest plate enchantments that can be applied to elytra
    private static final Set<Enchantment> ALLOWED_ENCHANTMENTS;

    static {
        ALLOWED_ENCHANTMENTS = new java.util.HashSet<>();
        ALLOWED_ENCHANTMENTS.add(Enchantment.PROTECTION);
        ALLOWED_ENCHANTMENTS.add(Enchantment.FIRE_PROTECTION);
        ALLOWED_ENCHANTMENTS.add(Enchantment.BLAST_PROTECTION);
        ALLOWED_ENCHANTMENTS.add(Enchantment.PROJECTILE_PROTECTION);
        ALLOWED_ENCHANTMENTS.add(Enchantment.THORNS);
        ALLOWED_ENCHANTMENTS.add(Enchantment.UNBREAKING);
        ALLOWED_ENCHANTMENTS.add(Enchantment.MENDING);
        ALLOWED_ENCHANTMENTS.add(Enchantment.VANISHING_CURSE);
        ALLOWED_ENCHANTMENTS.add(Enchantment.BINDING_CURSE);
    }

    public ElytraEnchantListener(@NotNull Jobs plugin) {
        this.plugin = plugin;
    }

    /**
     * Handle taking result from anvil - send message
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAnvilClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        if (event.getInventory().getType() != InventoryType.ANVIL) {
            return;
        }

        // Check if clicking result slot
        if (event.getRawSlot() != 2) {
            return;
        }

        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType() != Material.ELYTRA) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        // Check if this was an elytra combination
        AnvilInventory inv = (AnvilInventory) event.getInventory();
        ItemStack left = inv.getItem(0);
        ItemStack right = inv.getItem(1);

        if (left == null || right == null || left.getType() != Material.ELYTRA) {
            return;
        }

        // Send appropriate message based on what was combined
        if (right.getType() == Material.ENCHANTED_BOOK) {
            player.sendMessage(CMIChatColor.translate(
                    "&7[&6Jobs&7] &eエリトラにエンチャントを適用しました！"));
        } else if (right.getType() == Material.ELYTRA) {
            player.sendMessage(CMIChatColor.translate(
                    "&7[&6Jobs&7] &eエリトラのエンチャントを統合しました！"));
        } else if (isChestplate(right.getType())) {
            player.sendMessage(CMIChatColor.translate(
                    "&7[&6Jobs&7] &eチェストプレートのエンチャントをエリトラに転送しました！"));
        }
    }

    /**
     * Handle anvil combining - allow elytra combinations
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAnvilPrepare(PrepareAnvilEvent event) {
        AnvilInventory inv = event.getInventory();
        ItemStack left = inv.getItem(0);
        ItemStack right = inv.getItem(1);

        if (left == null || right == null) {
            return;
        }

        // Check if left item is elytra
        if (left.getType() != Material.ELYTRA) {
            return;
        }

        Player player = (Player) event.getView().getPlayer();

        // Check if player has Enchanter job at required level
        int enchanterLevel = getEnchanterLevel(player);
        if (enchanterLevel < REQUIRED_ENCHANTER_LEVEL) {
            return;
        }

        // Handle elytra + enchanted book
        if (right.getType() == Material.ENCHANTED_BOOK) {
            handleEnchantedBookCombination(event, left, right, player);
        }
        // Handle elytra + elytra combination (merge enchantments)
        else if (right.getType() == Material.ELYTRA) {
            handleElytraCombination(event, left, right, player);
        }
        // Handle elytra + chestplate combination (transfer enchantments)
        else if (isChestplate(right.getType())) {
            handleChestplateCombination(event, left, right, player);
        }
    }

    /**
     * Check if material is a chestplate
     */
    private boolean isChestplate(@NotNull Material material) {
        return material == Material.LEATHER_CHESTPLATE ||
               material == Material.CHAINMAIL_CHESTPLATE ||
               material == Material.IRON_CHESTPLATE ||
               material == Material.GOLDEN_CHESTPLATE ||
               material == Material.DIAMOND_CHESTPLATE ||
               material == Material.NETHERITE_CHESTPLATE;
    }

    /**
     * Handle elytra + enchanted book combination
     */
    private void handleEnchantedBookCombination(@NotNull PrepareAnvilEvent event,
                                                 @NotNull ItemStack elytra,
                                                 @NotNull ItemStack book,
                                                 @NotNull Player player) {
        if (!(book.getItemMeta() instanceof EnchantmentStorageMeta)) {
            return;
        }

        EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) book.getItemMeta();
        Map<Enchantment, Integer> storedEnchants = bookMeta.getStoredEnchants();

        // Check if any stored enchantments are chest plate enchantments
        Map<Enchantment, Integer> applicableEnchants = new HashMap<>();
        for (Map.Entry<Enchantment, Integer> entry : storedEnchants.entrySet()) {
            if (ALLOWED_ENCHANTMENTS.contains(entry.getKey())) {
                applicableEnchants.put(entry.getKey(), entry.getValue());
            }
        }

        if (applicableEnchants.isEmpty()) {
            return;
        }

        // Create result elytra with new enchantments
        ItemStack result = elytra.clone();
        Map<Enchantment, Integer> currentEnchants = result.getEnchantments();

        boolean modified = false;
        for (Map.Entry<Enchantment, Integer> entry : applicableEnchants.entrySet()) {
            Enchantment enchant = entry.getKey();
            int newLevel = entry.getValue();

            // If elytra already has this enchantment, combine levels
            if (currentEnchants.containsKey(enchant)) {
                int currentLevel = currentEnchants.get(enchant);
                if (currentLevel == newLevel) {
                    // Same level = level up (up to max)
                    newLevel = Math.min(currentLevel + 1, enchant.getMaxLevel());
                } else {
                    // Different level = use higher level
                    newLevel = Math.max(currentLevel, newLevel);
                }
            }

            result.addUnsafeEnchantment(enchant, newLevel);
            modified = true;
        }

        if (modified) {
            event.setResult(result);
        }
    }

    /**
     * Handle elytra + chestplate combination (transfer enchantments from chestplate to elytra)
     */
    private void handleChestplateCombination(@NotNull PrepareAnvilEvent event,
                                             @NotNull ItemStack elytra,
                                             @NotNull ItemStack chestplate,
                                             @NotNull Player player) {
        Map<Enchantment, Integer> chestplateEnchants = chestplate.getEnchantments();

        if (chestplateEnchants.isEmpty()) {
            return;
        }

        // Filter to only allowed enchantments
        Map<Enchantment, Integer> applicableEnchants = new HashMap<>();
        for (Map.Entry<Enchantment, Integer> entry : chestplateEnchants.entrySet()) {
            if (ALLOWED_ENCHANTMENTS.contains(entry.getKey())) {
                applicableEnchants.put(entry.getKey(), entry.getValue());
            }
        }

        if (applicableEnchants.isEmpty()) {
            return;
        }

        // Create result elytra with enchantments from chestplate
        ItemStack result = elytra.clone();
        Map<Enchantment, Integer> currentEnchants = result.getEnchantments();

        boolean modified = false;
        for (Map.Entry<Enchantment, Integer> entry : applicableEnchants.entrySet()) {
            Enchantment enchant = entry.getKey();
            int newLevel = entry.getValue();

            // If elytra already has this enchantment, combine levels
            if (currentEnchants.containsKey(enchant)) {
                int currentLevel = currentEnchants.get(enchant);
                if (currentLevel == newLevel) {
                    // Same level = level up (up to max)
                    newLevel = Math.min(currentLevel + 1, enchant.getMaxLevel());
                } else {
                    // Different level = use higher level
                    newLevel = Math.max(currentLevel, newLevel);
                }
            }

            result.addUnsafeEnchantment(enchant, newLevel);
            modified = true;
        }

        if (modified) {
            event.setResult(result);

            // Set repair cost to 0
            AnvilInventory inv = event.getInventory();
            inv.setRepairCost(0);
        }
    }

    /**
     * Handle elytra + elytra combination (merge enchantments)
     */
    private void handleElytraCombination(@NotNull PrepareAnvilEvent event,
                                         @NotNull ItemStack left,
                                         @NotNull ItemStack right,
                                         @NotNull Player player) {
        Map<Enchantment, Integer> leftEnchants = left.getEnchantments();
        Map<Enchantment, Integer> rightEnchants = right.getEnchantments();

        // Check if either elytra has chest plate enchantments
        boolean hasAllowedEnchants = false;
        for (Enchantment enchant : leftEnchants.keySet()) {
            if (ALLOWED_ENCHANTMENTS.contains(enchant)) {
                hasAllowedEnchants = true;
                break;
            }
        }
        for (Enchantment enchant : rightEnchants.keySet()) {
            if (ALLOWED_ENCHANTMENTS.contains(enchant)) {
                hasAllowedEnchants = true;
                break;
            }
        }

        if (!hasAllowedEnchants) {
            return;
        }

        // Merge enchantments
        ItemStack result = left.clone();
        boolean modified = false;

        for (Map.Entry<Enchantment, Integer> entry : rightEnchants.entrySet()) {
            Enchantment enchant = entry.getKey();
            int rightLevel = entry.getValue();

            if (!ALLOWED_ENCHANTMENTS.contains(enchant)) {
                continue;
            }

            if (leftEnchants.containsKey(enchant)) {
                int leftLevel = leftEnchants.get(enchant);
                if (leftLevel == rightLevel) {
                    // Same level = level up
                    int newLevel = Math.min(leftLevel + 1, enchant.getMaxLevel());
                    result.addUnsafeEnchantment(enchant, newLevel);
                } else {
                    // Different level = use higher
                    int newLevel = Math.max(leftLevel, rightLevel);
                    result.addUnsafeEnchantment(enchant, newLevel);
                }
            } else {
                // Add new enchantment
                result.addUnsafeEnchantment(enchant, rightLevel);
            }
            modified = true;
        }

        if (modified) {
            event.setResult(result);
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
}
