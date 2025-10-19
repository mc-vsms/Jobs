package com.gamingmesh.jobs.enchantments.listener;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.container.Job;
import com.gamingmesh.jobs.container.JobProgression;
import com.gamingmesh.jobs.container.JobsPlayer;
import com.gamingmesh.jobs.enchantments.api.CustomEnchantment;
import com.gamingmesh.jobs.enchantments.registry.CustomEnchantmentRegistry;
import com.gamingmesh.jobs.enchantments.util.EnchantmentUtils;
import net.Zrips.CMILib.Colors.CMIChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Listener for recharging custom enchantments using anvil
 * Only Enchanter job can recharge items
 */
public class EnchantmentRechargeListener implements Listener {

    private final Jobs plugin;
    private final CustomEnchantmentRegistry registry;

    private static final String ENCHANTER_JOB_NAME = "Enchanter";

    // Materials that can be used to recharge (consume 1 per 10 charges)
    private static final Map<Material, Integer> RECHARGE_MATERIALS = new HashMap<>();

    static {
        // Gems and valuable materials for recharging
        RECHARGE_MATERIALS.put(Material.DIAMOND, 20);           // 20 charges per diamond
        RECHARGE_MATERIALS.put(Material.EMERALD, 15);           // 15 charges per emerald
        RECHARGE_MATERIALS.put(Material.LAPIS_LAZULI, 5);       // 5 charges per lapis
        RECHARGE_MATERIALS.put(Material.AMETHYST_SHARD, 10);    // 10 charges per amethyst
        RECHARGE_MATERIALS.put(Material.ECHO_SHARD, 25);        // 25 charges per echo shard
        RECHARGE_MATERIALS.put(Material.NETHERITE_INGOT, 50);   // 50 charges per netherite
    }

    public EnchantmentRechargeListener(@NotNull Jobs plugin) {
        this.plugin = plugin;
        this.registry = CustomEnchantmentRegistry.getInstance();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAnvilPrepare(PrepareAnvilEvent event) {
        AnvilInventory inv = event.getInventory();
        ItemStack left = inv.getItem(0);  // Item with enchantment
        ItemStack right = inv.getItem(1); // Recharge material

        if (left == null || right == null) {
            return;
        }

        // Check if left item has custom enchantments
        Map<String, Integer> enchantments = EnchantmentUtils.getCustomEnchantments(left);
        if (enchantments.isEmpty()) {
            return;
        }

        // Check if right item is a valid recharge material
        Integer chargesPerItem = RECHARGE_MATERIALS.get(right.getType());
        if (chargesPerItem == null) {
            return;
        }

        // Check if player has Enchanter job
        Player player = (Player) event.getView().getPlayer();
        if (!hasEnchanterJob(player)) {
            // Set result to null to prevent recharging
            event.setResult(null);
            return;
        }

        // Find chargeable enchantments that need recharging
        boolean needsRecharge = false;
        for (String enchantId : enchantments.keySet()) {
            CustomEnchantment enchantment = registry.getEnchantment(enchantId);
            if (enchantment != null && enchantment.isChargeable()) {
                int currentCharges = EnchantmentUtils.getCurrentCharges(left, enchantId);
                if (currentCharges < enchantment.getMaxCharges()) {
                    needsRecharge = true;
                    break;
                }
            }
        }

        if (!needsRecharge) {
            return; // All enchantments are fully charged
        }

        // Create result item with recharged enchantments
        ItemStack result = left.clone();
        int materialsUsed = 0;
        int totalChargesAdded = 0;

        for (String enchantId : enchantments.keySet()) {
            CustomEnchantment enchantment = registry.getEnchantment(enchantId);
            if (enchantment != null && enchantment.isChargeable()) {
                int currentCharges = EnchantmentUtils.getCurrentCharges(result, enchantId);
                int maxCharges = enchantment.getMaxCharges();

                if (currentCharges < maxCharges) {
                    // Calculate how many charges to add
                    int chargesNeeded = maxCharges - currentCharges;
                    int materialsNeeded = (chargesNeeded + chargesPerItem - 1) / chargesPerItem; // Round up
                    int materialsAvailable = right.getAmount();

                    int materialsToUse = Math.min(materialsNeeded, materialsAvailable - materialsUsed);
                    int chargesToAdd = Math.min(materialsToUse * chargesPerItem, chargesNeeded);

                    EnchantmentUtils.setCharges(result, enchantId, currentCharges + chargesToAdd);
                    materialsUsed += materialsToUse;
                    totalChargesAdded += chargesToAdd;

                    if (materialsUsed >= materialsAvailable) {
                        break; // Used all available materials
                    }
                }
            }
        }

        if (totalChargesAdded > 0) {
            event.setResult(result);

            // Set repair cost to 0 to make it easier to take
            AnvilInventory anvilInv = (AnvilInventory) event.getInventory();
            anvilInv.setRepairCost(0);
        }
    }

    /**
     * Handle taking result from anvil - send message once and consume correct amount
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
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
        if (result == null) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        AnvilInventory inv = (AnvilInventory) event.getInventory();
        ItemStack left = inv.getItem(0);
        ItemStack right = inv.getItem(1);

        if (left == null || right == null) {
            return;
        }

        // Check if this was a recharge operation
        Integer chargesPerItem = RECHARGE_MATERIALS.get(right.getType());
        if (chargesPerItem == null) {
            return;
        }

        Map<String, Integer> enchantments = EnchantmentUtils.getCustomEnchantments(left);
        if (enchantments.isEmpty()) {
            return;
        }

        // Calculate how many materials were actually needed
        int materialsUsed = 0;
        int totalChargesAdded = 0;

        for (String enchantId : enchantments.keySet()) {
            CustomEnchantment enchantment = registry.getEnchantment(enchantId);
            if (enchantment != null && enchantment.isChargeable()) {
                int leftCharges = EnchantmentUtils.getCurrentCharges(left, enchantId);
                int resultCharges = EnchantmentUtils.getCurrentCharges(result, enchantId);
                int chargesAdded = resultCharges - leftCharges;

                if (chargesAdded > 0) {
                    totalChargesAdded += chargesAdded;
                    // Calculate materials used for this enchantment
                    int materialsForThis = (chargesAdded + chargesPerItem - 1) / chargesPerItem;
                    materialsUsed += materialsForThis;
                }
            }
        }

        if (totalChargesAdded > 0 && materialsUsed > 0) {
            // Cancel default behavior
            event.setCancelled(true);

            // Manually handle the transaction
            // Give result to player
            player.getInventory().addItem(result.clone());

            // Remove left item
            inv.setItem(0, null);

            // Reduce right item by correct amount
            int newAmount = right.getAmount() - materialsUsed;
            if (newAmount > 0) {
                right.setAmount(newAmount);
                inv.setItem(1, right);
            } else {
                inv.setItem(1, null);
            }

            // Clear result slot
            inv.setItem(2, null);

            player.sendMessage(CMIChatColor.translate(
                    "&7[&aエンチャンター&7] &e+" + totalChargesAdded + " チャージを回復！ " +
                    "&7(" + right.getType().name() + " x" + materialsUsed + " 使用)"));
        }
    }

    /**
     * Check if player has Enchanter job
     */
    private boolean hasEnchanterJob(@NotNull Player player) {
        JobsPlayer jPlayer = Jobs.getPlayerManager().getJobsPlayer(player);
        if (jPlayer == null) {
            return false;
        }

        for (JobProgression prog : jPlayer.progression) {
            Job job = prog.getJob();
            if (job != null && (ENCHANTER_JOB_NAME.equalsIgnoreCase(job.getName()) ||
                    ENCHANTER_JOB_NAME.equalsIgnoreCase(job.getShortName()))) {
                return true;
            }
        }

        return false;
    }
}
