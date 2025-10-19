package com.gamingmesh.jobs.enchantments.listener;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.enchantments.api.CustomEnchantment;
import com.gamingmesh.jobs.enchantments.registry.CustomEnchantmentRegistry;
import com.gamingmesh.jobs.enchantments.util.EnchantmentUtils;
import net.Zrips.CMILib.Colors.CMIChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Listener for custom enchantment effects
 */
public class EnchantmentEffectListener implements Listener {

    private final Jobs plugin;
    private final CustomEnchantmentRegistry registry;

    // Ore types for vein miner
    private static final Set<Material> ORES = EnumSet.of(
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE
    );

    // Smeltable blocks and their results
    private static final Map<Material, Material> SMELT_MAP = new HashMap<>();

    static {
        // Ores
        SMELT_MAP.put(Material.IRON_ORE, Material.IRON_INGOT);
        SMELT_MAP.put(Material.DEEPSLATE_IRON_ORE, Material.IRON_INGOT);
        SMELT_MAP.put(Material.GOLD_ORE, Material.GOLD_INGOT);
        SMELT_MAP.put(Material.DEEPSLATE_GOLD_ORE, Material.GOLD_INGOT);
        SMELT_MAP.put(Material.COPPER_ORE, Material.COPPER_INGOT);
        SMELT_MAP.put(Material.DEEPSLATE_COPPER_ORE, Material.COPPER_INGOT);
        SMELT_MAP.put(Material.NETHER_GOLD_ORE, Material.GOLD_INGOT);

        // Stone
        SMELT_MAP.put(Material.COBBLESTONE, Material.STONE);
        SMELT_MAP.put(Material.DEEPSLATE, Material.DEEPSLATE);
        SMELT_MAP.put(Material.COBBLED_DEEPSLATE, Material.DEEPSLATE);

        // Sand
        SMELT_MAP.put(Material.SAND, Material.GLASS);
        SMELT_MAP.put(Material.RED_SAND, Material.GLASS);

        // Wood logs
        SMELT_MAP.put(Material.OAK_LOG, Material.CHARCOAL);
        SMELT_MAP.put(Material.BIRCH_LOG, Material.CHARCOAL);
        SMELT_MAP.put(Material.SPRUCE_LOG, Material.CHARCOAL);
        SMELT_MAP.put(Material.JUNGLE_LOG, Material.CHARCOAL);
        SMELT_MAP.put(Material.ACACIA_LOG, Material.CHARCOAL);
        SMELT_MAP.put(Material.DARK_OAK_LOG, Material.CHARCOAL);
        SMELT_MAP.put(Material.MANGROVE_LOG, Material.CHARCOAL);
        SMELT_MAP.put(Material.CHERRY_LOG, Material.CHARCOAL);
    }

    public EnchantmentEffectListener(@NotNull Jobs plugin) {
        this.plugin = plugin;
        this.registry = CustomEnchantmentRegistry.getInstance();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        Map<String, Integer> enchantments = EnchantmentUtils.getCustomEnchantments(item);
        if (enchantments.isEmpty()) {
            return;
        }

        Block block = event.getBlock();

        // Process each custom enchantment
        for (Map.Entry<String, Integer> entry : enchantments.entrySet()) {
            String enchantId = entry.getKey();
            int level = entry.getValue();

            CustomEnchantment enchantment = registry.getEnchantment(enchantId);
            if (enchantment == null) {
                continue;
            }

            // Check charges if chargeable
            if (enchantment.isChargeable()) {
                int charges = EnchantmentUtils.getCurrentCharges(item, enchantId);
                if (charges <= 0) {
                    continue; // No charges left
                }
            }

            // Apply effect based on enchantment
            switch (enchantId) {
                case "vein_miner":
                    handleVeinMiner(player, item, block, level, enchantment);
                    break;
                case "auto_smelt":
                    handleAutoSmelt(player, item, block, level, enchantment);
                    break;
                case "lightning_striker":
                    handleLightningStriker(player, item, block, level, enchantment);
                    break;
                case "experience_boost":
                    handleExperienceBoost(player, item, block, level, enchantment, event);
                    break;
                case "curse_of_fragility":
                    handleCurseOfFragility(player, item, level);
                    break;
                case "area_mining":
                    handleAreaMining(player, item, block, level, enchantment);
                    break;
                case "magnetism":
                    handleMagnetism(player, item, block, level);
                    break;
                case "supreme_fortune":
                    handleSupremeFortune(player, item, block, level, enchantment, event);
                    break;
                case "explosive_mining":
                    handleExplosiveMining(player, item, block, level, enchantment);
                    break;
            }
        }
    }

    /**
     * Vein Miner - mines connected ores of the same type
     */
    private void handleVeinMiner(@NotNull Player player, @NotNull ItemStack tool,
                                  @NotNull Block block, int level, @NotNull CustomEnchantment enchantment) {
        if (!ORES.contains(block.getType())) {
            return;
        }

        // Calculate max blocks based on level
        int maxBlocks = 8 + (level * 4); // 12, 16, 20 blocks for levels 1, 2, 3

        Set<Block> vein = findVein(block, block.getType(), maxBlocks);

        if (vein.size() <= 1) {
            EnchantmentUtils.consumeCharge(tool, enchantment.getId());
            return;
        }

        // Mine the vein
        vein.remove(block); // Don't break the original block twice

        for (Block oreBlock : vein) {
            oreBlock.breakNaturally(tool);
        }

        EnchantmentUtils.consumeCharge(tool, enchantment.getId());
    }

    /**
     * Find connected blocks of the same type (vein)
     */
    private Set<Block> findVein(@NotNull Block start, @NotNull Material type, int maxBlocks) {
        Set<Block> vein = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();

        queue.add(start);
        vein.add(start);

        while (!queue.isEmpty() && vein.size() < maxBlocks) {
            Block current = queue.poll();

            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;

                        Block relative = current.getRelative(x, y, z);
                        if (relative.getType() == type && !vein.contains(relative)) {
                            vein.add(relative);
                            queue.add(relative);

                            if (vein.size() >= maxBlocks) {
                                return vein;
                            }
                        }
                    }
                }
            }
        }

        return vein;
    }

    /**
     * Auto Smelt - automatically smelts broken blocks
     */
    private void handleAutoSmelt(@NotNull Player player, @NotNull ItemStack tool,
                                  @NotNull Block block, int level, @NotNull CustomEnchantment enchantment) {
        Material smelted = SMELT_MAP.get(block.getType());
        if (smelted == null) {
            return;
        }

        // Cancel normal drops and give smelted item
        Collection<ItemStack> drops = block.getDrops(tool);
        block.setType(Material.AIR);

        for (ItemStack drop : drops) {
            ItemStack smeltedItem = new ItemStack(smelted, drop.getAmount());
            player.getInventory().addItem(smeltedItem);
        }

        EnchantmentUtils.consumeCharge(tool, enchantment.getId());
    }

    /**
     * Lightning Striker - strikes nearby entities with lightning
     */
    private void handleLightningStriker(@NotNull Player player, @NotNull ItemStack tool,
                                        @NotNull Block block, int level, @NotNull CustomEnchantment enchantment) {
        Location loc = block.getLocation().add(0.5, 0.5, 0.5);
        double radius = 3.0 + (level * 1.0); // 4, 5, 6 blocks radius

        int hitCount = 0;
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity living = (LivingEntity) entity;
                double damage = 4.0 + (level * 2.0); // 6, 8, 10 damage
                living.damage(damage, player);
                living.getWorld().strikeLightningEffect(living.getLocation());
                hitCount++;
            }
        }

        if (hitCount > 0) {
            EnchantmentUtils.consumeCharge(tool, enchantment.getId());
        }
    }

    /**
     * Experience Boost - increases experience gained
     */
    private void handleExperienceBoost(@NotNull Player player, @NotNull ItemStack tool,
                                       @NotNull Block block, int level, @NotNull CustomEnchantment enchantment,
                                       @NotNull BlockBreakEvent event) {
        int currentExp = event.getExpToDrop();
        if (currentExp > 0) {
            double multiplier = 1.0 + (level * 0.5); // 1.5x, 2.0x, 2.5x
            int bonusExp = (int) (currentExp * multiplier) - currentExp;
            event.setExpToDrop(currentExp + bonusExp);

            EnchantmentUtils.consumeCharge(tool, enchantment.getId());
        }
    }

    /**
     * Curse of Fragility - increases durability damage
     */
    private void handleCurseOfFragility(@NotNull Player player, @NotNull ItemStack tool, int level) {
        if (!(tool.getItemMeta() instanceof Damageable)) {
            return;
        }

        Damageable damageable = (Damageable) tool.getItemMeta();

        // Add extra damage (1-3 based on level)
        int extraDamage = level;
        damageable.setDamage(damageable.getDamage() + extraDamage);
        tool.setItemMeta((ItemMeta) damageable);

        if (damageable.getDamage() >= tool.getType().getMaxDurability()) {
            tool.setAmount(0); // Break the item
            player.sendMessage(CMIChatColor.translate("&c&l呪いによってアイテムが壊れました！"));
        }
    }

    /**
     * Area Mining - Breaks 3x3 area
     */
    private void handleAreaMining(@NotNull Player player, @NotNull ItemStack tool,
                                  @NotNull Block block, int level, @NotNull CustomEnchantment enchantment) {
        int radius = level; // 1, 2, 3 (3x3, 5x5, 7x7 area)

        // Get the face the player is looking at
        org.bukkit.block.BlockFace face = getTargetedFace(player);

        List<Block> blocksToBreak = new ArrayList<>();

        // Get blocks in area based on face
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;

                    Block relative = block.getRelative(x, y, z);
                    if (relative.getType() != Material.AIR && relative.getType() != Material.BEDROCK) {
                        // Check if block can be broken with this tool
                        if (canBreakWith(relative.getType(), tool.getType())) {
                            blocksToBreak.add(relative);
                        }
                    }
                }
            }
        }

        // Break all blocks
        for (Block b : blocksToBreak) {
            b.breakNaturally(tool);
        }

        EnchantmentUtils.consumeCharge(tool, enchantment.getId());
    }

    /**
     * Magnetism - Auto-pickup dropped items
     */
    private void handleMagnetism(@NotNull Player player, @NotNull ItemStack tool,
                                @NotNull Block block, int level) {
        // This is handled passively, items are already picked up automatically
        // No action needed on block break
    }

    /**
     * Supreme Fortune - Enhanced fortune effect
     */
    private void handleSupremeFortune(@NotNull Player player, @NotNull ItemStack tool,
                                     @NotNull Block block, int level, @NotNull CustomEnchantment enchantment,
                                     @NotNull BlockBreakEvent event) {
        // Add bonus drops based on level (in addition to vanilla fortune)
        Collection<ItemStack> drops = block.getDrops(tool);

        for (ItemStack drop : drops) {
            int bonusAmount = level; // +1, +2, +3 items per level
            if (bonusAmount > 0) {
                ItemStack bonus = drop.clone();
                bonus.setAmount(bonusAmount);
                player.getInventory().addItem(bonus);
            }
        }

        EnchantmentUtils.consumeCharge(tool, enchantment.getId());
    }

    /**
     * Explosive Mining - TNT-like mining
     */
    private void handleExplosiveMining(@NotNull Player player, @NotNull ItemStack tool,
                                       @NotNull Block block, int level, @NotNull CustomEnchantment enchantment) {
        int radius = 1 + level; // 2, 3, 4 block radius
        Location center = block.getLocation().add(0.5, 0.5, 0.5);

        // Create explosion effect
        center.getWorld().createExplosion(center, 0.0f, false, false);

        // Break blocks in radius
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block b = block.getRelative(x, y, z);
                    if (b.getType() != Material.AIR && b.getType() != Material.BEDROCK) {
                        b.breakNaturally(tool);
                    }
                }
            }
        }

        EnchantmentUtils.consumeCharge(tool, enchantment.getId());
    }

    /**
     * Get the face the player is looking at
     */
    private org.bukkit.block.BlockFace getTargetedFace(@NotNull Player player) {
        // Simple approximation based on player's pitch
        float pitch = player.getLocation().getPitch();
        if (pitch > 45) {
            return org.bukkit.block.BlockFace.DOWN;
        } else if (pitch < -45) {
            return org.bukkit.block.BlockFace.UP;
        }
        return org.bukkit.block.BlockFace.NORTH; // Default to horizontal
    }

    /**
     * Check if a material can be broken with a specific tool
     */
    private boolean canBreakWith(@NotNull Material block, @NotNull Material tool) {
        // Simple check - pickaxes can break stone-like blocks
        String toolName = tool.name();
        if (toolName.contains("PICKAXE")) {
            return block.name().contains("STONE") || block.name().contains("ORE") ||
                   block.name().contains("DEEPSLATE") || block.name().contains("OBSIDIAN");
        }
        return true; // Allow other tools to break any block for now
    }

}
