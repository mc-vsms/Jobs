package com.gamingmesh.jobs.enchantments.listener;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.enchantments.api.CustomEnchantment;
import com.gamingmesh.jobs.enchantments.data.EnchantmentData;
import com.gamingmesh.jobs.enchantments.data.EnchantmentTrigger;
import com.gamingmesh.jobs.enchantments.executor.EnchantmentEffectExecutor;
import com.gamingmesh.jobs.enchantments.registry.CustomEnchantmentRegistry;
import com.gamingmesh.jobs.enchantments.util.EnchantmentUtils;
import net.Zrips.CMILib.Version.Schedulers.CMIScheduler;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Universal listener that processes all enchantment triggers and executes effects
 */
public class UniversalEnchantmentListener implements Listener {

    private final Jobs plugin;
    private final CustomEnchantmentRegistry registry;
    private final EnchantmentEffectExecutor executor;

    public UniversalEnchantmentListener(@NotNull Jobs plugin) {
        this.plugin = plugin;
        this.registry = CustomEnchantmentRegistry.getInstance();
        this.executor = new EnchantmentEffectExecutor(plugin);

        // Start periodic task for PERIODIC and PASSIVE triggers
        startPeriodicTask();
    }

    /**
     * Handle ON_ATTACK trigger
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }

        Player player = (Player) event.getDamager();
        LivingEntity victim = (LivingEntity) event.getEntity();
        ItemStack weapon = player.getInventory().getItemInMainHand();

        if (weapon == null || weapon.getType() == Material.AIR) {
            return;
        }

        processItemEnchantments(weapon, player, EnchantmentTrigger.ON_ATTACK, (enchantment, level) -> {
            if (enchantment instanceof EnchantmentData) {
                EnchantmentData data = (EnchantmentData) enchantment;
                executor.executeCombatEffects(data.getEffects(), player, weapon, victim, level, event);
                EnchantmentUtils.consumeCharge(weapon, enchantment.getId());
            }
        });
    }

    /**
     * Handle ON_DAMAGED trigger
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        // Check armor pieces
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor == null || armor.getType() == Material.AIR) {
                continue;
            }

            processItemEnchantments(armor, player, EnchantmentTrigger.ON_DAMAGED, (enchantment, level) -> {
                if (enchantment instanceof EnchantmentData) {
                    EnchantmentData data = (EnchantmentData) enchantment;
                    executor.executeDamageEffects(data.getEffects(), player, armor, level, event);
                    EnchantmentUtils.consumeCharge(armor, enchantment.getId());
                }
            });
        }

        // Handle ON_FALL trigger
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            ItemStack boots = player.getInventory().getBoots();
            if (boots != null && boots.getType() != Material.AIR) {
                processItemEnchantments(boots, player, EnchantmentTrigger.ON_FALL, (enchantment, level) -> {
                    if (enchantment instanceof EnchantmentData) {
                        EnchantmentData data = (EnchantmentData) enchantment;
                        executor.executeDamageEffects(data.getEffects(), player, boots, level, event);
                        EnchantmentUtils.consumeCharge(boots, enchantment.getId());
                    }
                });
            }
        }

        // Handle ON_LETHAL_DAMAGE trigger
        if (event.getFinalDamage() >= player.getHealth()) {
            for (ItemStack armor : player.getInventory().getArmorContents()) {
                if (armor == null || armor.getType() == Material.AIR) {
                    continue;
                }

                processItemEnchantments(armor, player, EnchantmentTrigger.ON_LETHAL_DAMAGE, (enchantment, level) -> {
                    if (enchantment instanceof EnchantmentData) {
                        EnchantmentData data = (EnchantmentData) enchantment;
                        executor.executeDamageEffects(data.getEffects(), player, armor, level, event);
                        EnchantmentUtils.consumeCharge(armor, enchantment.getId());
                    }
                });
            }
        }
    }

    /**
     * Handle ON_BLOCK_BREAK trigger
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        // Prevent infinite loop from vein mining
        if (event.getBlock().hasMetadata("vein_mining")) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (tool == null || tool.getType() == Material.AIR) {
            return;
        }

        processItemEnchantments(tool, player, EnchantmentTrigger.ON_BLOCK_BREAK, (enchantment, level) -> {
            if (enchantment instanceof EnchantmentData) {
                EnchantmentData data = (EnchantmentData) enchantment;
                executor.executeBlockBreakEffects(data.getEffects(), player, tool, event.getBlock(), level, event);
                EnchantmentUtils.consumeCharge(tool, enchantment.getId());
            }
        });
    }

    /**
     * Handle ON_KILL trigger
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity().getKiller() instanceof Player)) {
            return;
        }

        Player player = event.getEntity().getKiller();
        ItemStack weapon = player.getInventory().getItemInMainHand();

        if (weapon == null || weapon.getType() == Material.AIR) {
            return;
        }

        processItemEnchantments(weapon, player, EnchantmentTrigger.ON_KILL, (enchantment, level) -> {
            if (enchantment instanceof EnchantmentData) {
                EnchantmentData data = (EnchantmentData) enchantment;
                // Effects would be executed here (similar to combat effects)
                EnchantmentUtils.consumeCharge(weapon, enchantment.getId());
            }
        });
    }

    /**
     * Handle ON_MOVE trigger
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only trigger if player actually moved (not just head rotation)
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();

        // Check boots for movement enchantments
        ItemStack boots = player.getInventory().getBoots();
        if (boots != null && boots.getType() != Material.AIR) {
            processItemEnchantments(boots, player, EnchantmentTrigger.ON_MOVE, (enchantment, level) -> {
                if (enchantment instanceof EnchantmentData) {
                    // Effects would be executed here
                    EnchantmentUtils.consumeCharge(boots, enchantment.getId());
                }
            });
        }
    }

    /**
     * Handle ON_BLOCK_PLACE trigger
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();

        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        processItemEnchantments(item, player, EnchantmentTrigger.ON_BLOCK_PLACE, (enchantment, level) -> {
            if (enchantment instanceof EnchantmentData) {
                // Effects would be executed here
                EnchantmentUtils.consumeCharge(item, enchantment.getId());
            }
        });
    }

    /**
     * Start periodic task for PERIODIC and PASSIVE triggers
     */
    private void startPeriodicTask() {
        CMIScheduler.scheduleSyncRepeatingTask(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                // Run on player's region thread for Folia compatibility
                CMIScheduler.runAtLocation(plugin, player.getLocation(), () -> {
                    processPassiveEffects(player);
                });
            }
        }, 20L, 20L); // Run every second
    }

    /**
     * Process passive and periodic effects for a player
     */
    private void processPassiveEffects(@NotNull Player player) {
        // Check all equipment
        ItemStack[] equipment = new ItemStack[5];
        equipment[0] = player.getInventory().getItemInMainHand();
        equipment[1] = player.getInventory().getHelmet();
        equipment[2] = player.getInventory().getChestplate();
        equipment[3] = player.getInventory().getLeggings();
        equipment[4] = player.getInventory().getBoots();

        for (ItemStack item : equipment) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            // Process PASSIVE triggers
            processItemEnchantments(item, player, EnchantmentTrigger.PASSIVE, (enchantment, level) -> {
                if (enchantment instanceof EnchantmentData) {
                    // Passive effects would be executed here
                }
            });

            // Process PERIODIC triggers
            processItemEnchantments(item, player, EnchantmentTrigger.PERIODIC, (enchantment, level) -> {
                if (enchantment instanceof EnchantmentData) {
                    // Periodic effects would be executed here
                    EnchantmentUtils.consumeCharge(item, enchantment.getId());
                }
            });
        }
    }

    /**
     * Process enchantments on an item for a specific trigger
     */
    private void processItemEnchantments(@NotNull ItemStack item, @NotNull Player player,
                                        @NotNull EnchantmentTrigger trigger,
                                        @NotNull EnchantmentProcessor processor) {
        Map<String, Integer> enchantments = EnchantmentUtils.getCustomEnchantments(item);
        if (enchantments.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Integer> entry : enchantments.entrySet()) {
            String enchantId = entry.getKey();
            int level = entry.getValue();

            CustomEnchantment enchantment = registry.getEnchantment(enchantId);
            if (enchantment == null) {
                continue;
            }

            // Check if this enchantment has the specified trigger
            if (enchantment instanceof EnchantmentData) {
                EnchantmentData data = (EnchantmentData) enchantment;
                if (!data.hasTrigger(trigger)) {
                    continue;
                }

                // Check if enchantment has effects
                if (!data.hasEffects()) {
                    continue;
                }

                // Check charges if chargeable
                if (enchantment.isChargeable()) {
                    int charges = EnchantmentUtils.getCurrentCharges(item, enchantId);
                    if (charges <= 0) {
                        continue; // No charges left
                    }
                }

                // Process the enchantment
                processor.process(enchantment, level);
            }
        }
    }

    /**
     * Functional interface for processing enchantments
     */
    @FunctionalInterface
    private interface EnchantmentProcessor {
        void process(@NotNull CustomEnchantment enchantment, int level);
    }
}
