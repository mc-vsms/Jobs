package com.gamingmesh.jobs.enchantments.executor;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.enchantments.data.EffectDefinition;
import com.gamingmesh.jobs.enchantments.data.EnchantmentEffect;
import com.gamingmesh.jobs.enchantments.util.EnchantmentUtils;
import net.Zrips.CMILib.Container.CMIAttribute;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Queue;
import java.util.LinkedList;
import java.util.HashSet;

/**
 * Executes enchantment effects based on their definitions
 */
public class EnchantmentEffectExecutor {

    private final Jobs plugin;

    public EnchantmentEffectExecutor(@NotNull Jobs plugin) {
        this.plugin = plugin;
    }

    /**
     * Execute effects for combat events (player attacking entity)
     */
    public void executeCombatEffects(@NotNull List<EffectDefinition> effects,
                                     @NotNull Player player,
                                     @NotNull ItemStack weapon,
                                     @NotNull LivingEntity victim,
                                     int level,
                                     @NotNull EntityDamageByEntityEvent event) {
        Map<String, Double> variables = new HashMap<>();
        variables.put("level", (double) level);
        variables.put("damage", event.getDamage());
        variables.put("baseDamage", event.getDamage());
        variables.put("health", player.getHealth());
        variables.put("maxHealth", CMIAttribute.MAX_HEALTH.getBaseValue(player));

        // Check for conditional effects first
        boolean conditionMet = true;
        for (EffectDefinition effect : effects) {
            if (effect.getType() == EnchantmentEffect.CONDITIONAL_CHECK) {
                conditionMet = checkCondition(effect, player, victim);
                if (!conditionMet) break;
            }
        }

        // Execute effects only if condition is met
        for (EffectDefinition effect : effects) {
            if (effect.getType() == EnchantmentEffect.CONDITIONAL_CHECK) continue;
            if (!conditionMet) break; // Skip remaining effects if condition failed

            executeEffect(effect, player, weapon, victim, event.getEntity().getLocation(), variables, event, null);
        }
    }

    /**
     * Execute effects for damage taken events
     */
    public void executeDamageEffects(@NotNull List<EffectDefinition> effects,
                                     @NotNull Player player,
                                     @NotNull ItemStack armor,
                                     int level,
                                     @NotNull EntityDamageEvent event) {
        for (EffectDefinition effect : effects) {
            Map<String, Double> variables = new HashMap<>();
            variables.put("level", (double) level);
            variables.put("damage", event.getDamage());
            variables.put("baseDamage", event.getDamage());
            variables.put("health", player.getHealth());
            variables.put("maxHealth", CMIAttribute.MAX_HEALTH.getBaseValue(player));

            executeEffect(effect, player, armor, null, player.getLocation(), variables, event, null);
        }
    }

    /**
     * Execute effects for block break events
     */
    public void executeBlockBreakEffects(@NotNull List<EffectDefinition> effects,
                                        @NotNull Player player,
                                        @NotNull ItemStack tool,
                                        @NotNull Block block,
                                        int level,
                                        @NotNull BlockBreakEvent event) {
        for (EffectDefinition effect : effects) {
            Map<String, Double> variables = new HashMap<>();
            variables.put("level", (double) level);
            variables.put("experience", (double) event.getExpToDrop());

            executeEffect(effect, player, tool, null, block.getLocation(), variables, null, event);
        }
    }

    /**
     * Execute a single effect
     */
    private void executeEffect(@NotNull EffectDefinition effect,
                               @NotNull Player player,
                               @NotNull ItemStack item,
                               @Nullable LivingEntity target,
                               @NotNull Location location,
                               @NotNull Map<String, Double> variables,
                               @Nullable EntityDamageEvent damageEvent,
                               @Nullable BlockBreakEvent blockEvent) {
        EnchantmentEffect type = effect.getType();

        switch (type) {
            case HEAL_PLAYER:
                handleHealPlayer(effect, player, variables);
                break;

            case DAMAGE_NEARBY:
                handleDamageNearby(effect, player, target, location, variables);
                break;

            case DAMAGE_BONUS:
                handleDamageBonus(effect, damageEvent, variables);
                break;

            case APPLY_POTION:
                handleApplyPotion(effect, player, target, variables);
                break;

            case BREAK_BLOCKS:
                handleBreakBlocks(effect, player, item, location, variables);
                break;

            case AUTO_SMELT:
                handleAutoSmelt(effect, player, item, blockEvent);
                break;

            case SPAWN_LIGHTNING:
                handleSpawnLightning(effect, location, variables);
                break;

            case REFLECT_DAMAGE:
                handleReflectDamage(effect, player, damageEvent, variables);
                break;

            case INCREASE_DROPS:
                handleIncreaseDrops(effect, blockEvent, variables);
                break;

            case NEGATE_DAMAGE:
                handleNegateDamage(effect, damageEvent, variables);
                break;

            case VEIN_MINE:
                handleVeinMine(effect, player, item, blockEvent, variables);
                break;

            case MAGNETISM:
                // Passive effect, handled separately
                break;

            case AUTO_REPLANT:
                handleAutoReplant(effect, player, blockEvent);
                break;

            case SPEED_BOOST:
                handleSpeedBoost(effect, player, variables);
                break;

            case PREVENT_DEATH:
                handlePreventDeath(effect, player, damageEvent, variables);
                break;

            case CURSE_EFFECT:
                handleCurseEffect(effect, player, item, variables);
                break;

            case INCREASE_DURABILITY_DAMAGE:
                handleIncreaseDurabilityDamage(effect, item, variables);
                break;

            case SPAWN_PARTICLES:
                handleSpawnParticles(effect, location, variables);
                break;

            case DRAIN_HUNGER:
                handleDrainHunger(effect, player, variables);
                break;

            case INCREASE_DAMAGE_TAKEN:
                handleIncreaseDamageTaken(effect, player, damageEvent, variables);
                break;

            case CONDITIONAL_CHECK:
                // Handled inline by other effects
                break;

            case REDUCE_MAX_HEALTH:
                handleReduceMaxHealth(effect, player, variables);
                break;

            case APPLY_DEBUFF:
                handleApplyDebuff(effect, player, variables);
                break;

            case DAMAGE_SELF:
                handleDamageSelf(effect, player, variables);
                break;
        }
    }

    // Effect handlers

    private void handleHealPlayer(@NotNull EffectDefinition effect, @NotNull Player player, @NotNull Map<String, Double> variables) {
        String formula = effect.getParamAsString("amount");
        if (formula != null) {
            double healAmount = effect.evaluateFormula(formula, variables);
            double maxHealth = CMIAttribute.MAX_HEALTH.getBaseValue(player);
            player.setHealth(Math.min(maxHealth, player.getHealth() + healAmount));
        }
    }

    private void handleDamageNearby(@NotNull EffectDefinition effect, @NotNull Player player,
                                   @Nullable LivingEntity initialTarget, @NotNull Location location,
                                   @NotNull Map<String, Double> variables) {
        double radius = effect.getParamAsDouble("radius", 4.0);
        int maxTargets = effect.getParamAsInt("maxTargets", 3);
        String damageFormula = effect.getParamAsString("damage");

        if (damageFormula == null) return;

        double damage = effect.evaluateFormula(damageFormula, variables);
        int hitCount = 0;

        for (Entity entity : location.getWorld().getNearbyEntities(location, radius, radius, radius)) {
            if (hitCount >= maxTargets) break;

            if (entity instanceof LivingEntity && entity != player && entity != initialTarget) {
                LivingEntity target = (LivingEntity) entity;
                target.damage(damage, player);
                hitCount++;
            }
        }
    }

    private void handleDamageBonus(@NotNull EffectDefinition effect, @Nullable EntityDamageEvent event,
                                   @NotNull Map<String, Double> variables) {
        if (event == null) return;

        String formula = effect.getParamAsString("amount");
        if (formula != null) {
            double bonus = effect.evaluateFormula(formula, variables);
            event.setDamage(event.getDamage() + bonus);
        }
    }

    private void handleApplyPotion(@NotNull EffectDefinition effect, @NotNull Player player,
                                  @Nullable LivingEntity target, @NotNull Map<String, Double> variables) {
        String potionTypeName = effect.getParamAsString("type");
        if (potionTypeName == null) return;

        PotionEffectType potionType = PotionEffectType.getByName(potionTypeName.toUpperCase());
        if (potionType == null) return;

        int duration = effect.getParamAsInt("duration", 100);
        int amplifier = effect.getParamAsInt("amplifier", 0);
        boolean ambient = effect.getParamAsBoolean("ambient", true);
        boolean particles = effect.getParamAsBoolean("particles", false);

        String targetType = effect.getParamAsString("target");
        LivingEntity recipient = "victim".equals(targetType) && target != null ? target : player;

        recipient.addPotionEffect(new PotionEffect(potionType, duration, amplifier, ambient, particles));
    }

    private void handleBreakBlocks(@NotNull EffectDefinition effect, @NotNull Player player,
                                  @NotNull ItemStack tool, @NotNull Location location,
                                  @NotNull Map<String, Double> variables) {
        int radius = effect.getParamAsInt("radius", 1);
        Block center = location.getBlock();
        Material centerType = center.getType();

        // Check if the tool is appropriate for the center block
        // If not, don't do area mining at all
        if (!isPreferredTool(centerType, tool.getType())) {
            return; // Exit early - no area mining for wrong tool
        }

        // Calculate maximum blocks based on radius (for 3x3x3 area, etc.)
        int maxBlocks = (int) Math.pow((radius * 2 + 1), 3) - 1; // Exclude center

        // Use BFS to find connected blocks of the same type
        Set<Block> toBreak = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();
        queue.add(center);
        toBreak.add(center);

        while (!queue.isEmpty() && toBreak.size() < maxBlocks) {
            Block current = queue.poll();

            // Check all 6 adjacent blocks (no diagonals)
            Block[] neighbors = {
                current.getRelative(1, 0, 0),
                current.getRelative(-1, 0, 0),
                current.getRelative(0, 1, 0),
                current.getRelative(0, -1, 0),
                current.getRelative(0, 0, 1),
                current.getRelative(0, 0, -1)
            };

            for (Block neighbor : neighbors) {
                if (toBreak.size() >= maxBlocks) break;

                // Check if within radius from center
                if (Math.abs(neighbor.getX() - center.getX()) > radius ||
                    Math.abs(neighbor.getY() - center.getY()) > radius ||
                    Math.abs(neighbor.getZ() - center.getZ()) > radius) {
                    continue;
                }

                Material neighborType = neighbor.getType();

                // Skip if already visited
                if (toBreak.contains(neighbor)) {
                    continue;
                }

                // Skip air and bedrock
                if (neighborType == Material.AIR || neighborType == Material.BEDROCK) {
                    continue;
                }

                // Must be same type as center block
                if (neighborType != centerType) {
                    continue;
                }

                // Check if the tool is appropriate for this block
                if (!isPreferredTool(neighborType, tool.getType())) {
                    continue;
                }

                // Add to break set and queue
                toBreak.add(neighbor);
                queue.add(neighbor);
            }
        }

        // Break all found blocks (except the original which is already being broken)
        toBreak.remove(center);
        for (Block block : toBreak) {
            // Simulate BlockBreakEvent for Jobs plugin to register
            BlockBreakEvent breakEvent = new BlockBreakEvent(block, player);
            plugin.getServer().getPluginManager().callEvent(breakEvent);

            if (!breakEvent.isCancelled()) {
                block.breakNaturally(tool);
            }
        }
    }

    /**
     * Check if a tool is the preferred tool for a block type
     */
    private boolean isPreferredTool(@NotNull Material blockType, @NotNull Material toolType) {
        // Pickaxe blocks
        if (isPickaxeBlock(blockType)) {
            return isPickaxe(toolType);
        }

        // Axe blocks
        if (isAxeBlock(blockType)) {
            return isAxe(toolType);
        }

        // Shovel blocks
        if (isShovelBlock(blockType)) {
            return isShovel(toolType);
        }

        // Hoe blocks
        if (isHoeBlock(blockType)) {
            return isHoe(toolType);
        }

        return true; // For blocks that don't require a specific tool
    }

    private boolean isPickaxe(@NotNull Material material) {
        return material == Material.WOODEN_PICKAXE ||
               material == Material.STONE_PICKAXE ||
               material == Material.IRON_PICKAXE ||
               material == Material.GOLDEN_PICKAXE ||
               material == Material.DIAMOND_PICKAXE ||
               material == Material.NETHERITE_PICKAXE;
    }

    private boolean isAxe(@NotNull Material material) {
        return material == Material.WOODEN_AXE ||
               material == Material.STONE_AXE ||
               material == Material.IRON_AXE ||
               material == Material.GOLDEN_AXE ||
               material == Material.DIAMOND_AXE ||
               material == Material.NETHERITE_AXE;
    }

    private boolean isShovel(@NotNull Material material) {
        return material == Material.WOODEN_SHOVEL ||
               material == Material.STONE_SHOVEL ||
               material == Material.IRON_SHOVEL ||
               material == Material.GOLDEN_SHOVEL ||
               material == Material.DIAMOND_SHOVEL ||
               material == Material.NETHERITE_SHOVEL;
    }

    private boolean isHoe(@NotNull Material material) {
        return material == Material.WOODEN_HOE ||
               material == Material.STONE_HOE ||
               material == Material.IRON_HOE ||
               material == Material.GOLDEN_HOE ||
               material == Material.DIAMOND_HOE ||
               material == Material.NETHERITE_HOE;
    }

    private boolean isPickaxeBlock(@NotNull Material material) {
        String name = material.name();
        return name.contains("_ORE") ||
               name.contains("STONE") ||
               name.contains("COBBLESTONE") ||
               name.contains("DEEPSLATE") ||
               name.contains("GRANITE") ||
               name.contains("DIORITE") ||
               name.contains("ANDESITE") ||
               name.contains("NETHERRACK") ||
               name.contains("BLACKSTONE") ||
               name.contains("BASALT") ||
               material == Material.OBSIDIAN ||
               material == Material.CRYING_OBSIDIAN ||
               material == Material.ANCIENT_DEBRIS ||
               material == Material.IRON_BLOCK ||
               material == Material.GOLD_BLOCK ||
               material == Material.DIAMOND_BLOCK ||
               material == Material.EMERALD_BLOCK ||
               material == Material.NETHERITE_BLOCK ||
               material == Material.QUARTZ_BLOCK ||
               material == Material.TERRACOTTA ||
               name.contains("CONCRETE") ||
               name.contains("BRICK") ||
               material == Material.END_STONE;
    }

    private boolean isAxeBlock(@NotNull Material material) {
        String name = material.name();
        return name.contains("LOG") ||
               name.contains("WOOD") ||
               name.contains("PLANKS") ||
               name.contains("FENCE") && !name.contains("NETHER_BRICK") ||
               name.contains("STAIRS") && name.contains("WOOD") ||
               material == Material.CRAFTING_TABLE ||
               material == Material.CHEST ||
               material == Material.BARREL ||
               material == Material.BOOKSHELF ||
               material == Material.MUSHROOM_STEM ||
               name.contains("HYPHAE");
    }

    private boolean isShovelBlock(@NotNull Material material) {
        return material == Material.DIRT ||
               material == Material.GRASS_BLOCK ||
               material == Material.SAND ||
               material == Material.RED_SAND ||
               material == Material.GRAVEL ||
               material == Material.CLAY ||
               material == Material.SOUL_SAND ||
               material == Material.SOUL_SOIL ||
               material == Material.SNOW ||
               material == Material.SNOW_BLOCK ||
               material == Material.MYCELIUM ||
               material == Material.PODZOL ||
               material == Material.COARSE_DIRT ||
               material == Material.ROOTED_DIRT;
    }

    private boolean isHoeBlock(@NotNull Material material) {
        String name = material.name();
        return name.contains("LEAVES") ||
               material == Material.HAY_BLOCK ||
               material == Material.DRIED_KELP_BLOCK ||
               material == Material.TARGET ||
               material == Material.SPONGE ||
               material == Material.WET_SPONGE ||
               name.contains("WART_BLOCK");
    }

    private void handleAutoSmelt(@NotNull EffectDefinition effect, @NotNull Player player,
                                 @NotNull ItemStack tool, @Nullable BlockBreakEvent event) {
        if (event == null) return;

        // Implementation similar to existing auto-smelt logic
        // This would use a smelt map to convert blocks
    }

    private void handleSpawnLightning(@NotNull EffectDefinition effect, @NotNull Location location,
                                     @NotNull Map<String, Double> variables) {
        boolean damaging = effect.getParamAsBoolean("damaging", false);

        if (damaging) {
            location.getWorld().strikeLightning(location);
        } else {
            location.getWorld().strikeLightningEffect(location);
        }
    }

    private void handleReflectDamage(@NotNull EffectDefinition effect, @NotNull Player player,
                                    @Nullable EntityDamageEvent event, @NotNull Map<String, Double> variables) {
        if (!(event instanceof EntityDamageByEntityEvent)) return;

        EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;
        if (!(damageEvent.getDamager() instanceof LivingEntity)) return;

        String formula = effect.getParamAsString("percent");
        if (formula != null) {
            double percent = effect.evaluateFormula(formula, variables);
            double reflectDamage = event.getFinalDamage() * percent;

            LivingEntity attacker = (LivingEntity) damageEvent.getDamager();
            attacker.damage(reflectDamage, player);
        }
    }

    private void handleIncreaseDrops(@NotNull EffectDefinition effect, @Nullable BlockBreakEvent event,
                                    @NotNull Map<String, Double> variables) {
        if (event == null) return;

        String type = effect.getParamAsString("dropType");
        if ("experience".equals(type)) {
            String formula = effect.getParamAsString("multiplier");
            if (formula != null) {
                double multiplier = effect.evaluateFormula(formula, variables);
                int currentExp = event.getExpToDrop();
                event.setExpToDrop((int) (currentExp * multiplier));
            }
        }
    }

    private void handleNegateDamage(@NotNull EffectDefinition effect, @Nullable EntityDamageEvent event,
                                   @NotNull Map<String, Double> variables) {
        if (event == null) return;

        double chance = effect.getParamAsDouble("chance", 1.0);
        if (Math.random() < chance) {
            event.setCancelled(true);
        }
    }

    private void handleVeinMine(@NotNull EffectDefinition effect, @NotNull Player player,
                               @NotNull ItemStack tool, @Nullable BlockBreakEvent event,
                               @NotNull Map<String, Double> variables) {
        if (event == null) return;

        int maxBlocks = effect.getParamAsInt("maxBlocks", 16);
        Block startBlock = event.getBlock();
        Material targetType = startBlock.getType();

        // Check if the tool is appropriate for this block
        if (!isPreferredTool(targetType, tool.getType())) {
            return;
        }

        Set<Block> toBreak = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();
        queue.add(startBlock);
        toBreak.add(startBlock);

        // BFS to find connected blocks of the same type
        while (!queue.isEmpty() && toBreak.size() < maxBlocks) {
            Block current = queue.poll();

            // Check 6 adjacent blocks (no diagonals)
            Block[] neighbors = {
                current.getRelative(1, 0, 0),
                current.getRelative(-1, 0, 0),
                current.getRelative(0, 1, 0),
                current.getRelative(0, -1, 0),
                current.getRelative(0, 0, 1),
                current.getRelative(0, 0, -1)
            };

            for (Block neighbor : neighbors) {
                if (toBreak.size() >= maxBlocks) break;

                // Check if same type and not already in set
                if (neighbor.getType() == targetType && !toBreak.contains(neighbor)) {
                    toBreak.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        // Break all found blocks (except the original which is already being broken)
        toBreak.remove(startBlock);
        for (Block block : toBreak) {
            // Mark this block as being broken by vein mining to prevent infinite loop
            block.setMetadata("vein_mining", new org.bukkit.metadata.FixedMetadataValue(plugin, true));

            // Simulate BlockBreakEvent for Jobs plugin to register
            BlockBreakEvent breakEvent = new BlockBreakEvent(block, player);
            plugin.getServer().getPluginManager().callEvent(breakEvent);

            if (!breakEvent.isCancelled()) {
                block.breakNaturally(tool);
            }

            // Clean up metadata
            block.removeMetadata("vein_mining", plugin);
        }
    }

    private void handleAutoReplant(@NotNull EffectDefinition effect, @NotNull Player player,
                                  @Nullable BlockBreakEvent event) {
        if (event == null) return;

        // Implementation similar to existing auto-plant logic
    }

    private void handleSpeedBoost(@NotNull EffectDefinition effect, @NotNull Player player,
                                 @NotNull Map<String, Double> variables) {
        int amplifier = effect.getParamAsInt("amplifier", 0);
        int duration = effect.getParamAsInt("duration", 40);

        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, amplifier, true, false));
    }

    private void handlePreventDeath(@NotNull EffectDefinition effect, @NotNull Player player,
                                   @Nullable EntityDamageEvent event, @NotNull Map<String, Double> variables) {
        if (event == null) return;

        double healthToSet = effect.getParamAsDouble("health", 1.0);
        event.setDamage(player.getHealth() - healthToSet);

        // Apply additional effects
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 100, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 1));
    }

    private void handleCurseEffect(@NotNull EffectDefinition effect, @NotNull Player player,
                                   @NotNull ItemStack item, @NotNull Map<String, Double> variables) {
        // Curse effects vary by type
        String curseType = effect.getParamAsString("curseType");
        // Implementation would vary based on curse type
    }

    private void handleIncreaseDurabilityDamage(@NotNull EffectDefinition effect, @NotNull ItemStack item,
                                               @NotNull Map<String, Double> variables) {
        // Implementation for curse of fragility
        // Increases durability damage
    }

    private void handleSpawnParticles(@NotNull EffectDefinition effect, @NotNull Location location,
                                     @NotNull Map<String, Double> variables) {
        // Spawn visual particles
        // Implementation would use Particle API
    }

    // New debuff/trade-off handlers

    private void handleDrainHunger(@NotNull EffectDefinition effect, @NotNull Player player,
                                   @NotNull Map<String, Double> variables) {
        int amount = effect.getParamAsInt("amount", 1);
        float saturation = (float) effect.getParamAsDouble("saturation", 0.0);

        int currentFood = player.getFoodLevel();
        player.setFoodLevel(Math.max(0, currentFood - amount));

        if (saturation > 0) {
            float currentSat = player.getSaturation();
            player.setSaturation(Math.max(0, currentSat - saturation));
        }
    }

    private void handleIncreaseDamageTaken(@NotNull EffectDefinition effect, @NotNull Player player,
                                          @Nullable EntityDamageEvent event,
                                          @NotNull Map<String, Double> variables) {
        if (event == null) return;

        String multiplierFormula = effect.getParamAsString("multiplier");
        if (multiplierFormula != null) {
            double multiplier = effect.evaluateFormula(multiplierFormula, variables);
            double currentDamage = event.getDamage();
            event.setDamage(currentDamage * multiplier);
        }
    }

    private void handleReduceMaxHealth(@NotNull EffectDefinition effect, @NotNull Player player,
                                      @NotNull Map<String, Double> variables) {
        double amount = effect.getParamAsDouble("amount", 2.0);
        int duration = effect.getParamAsInt("duration", 100);

        // Apply health boost with negative value to reduce max health
        player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, duration,
                                                -(int)(amount / 4), true, false));
    }

    private void handleApplyDebuff(@NotNull EffectDefinition effect, @NotNull Player player,
                                   @NotNull Map<String, Double> variables) {
        String potionTypeName = effect.getParamAsString("type");
        if (potionTypeName == null) return;

        PotionEffectType potionType = PotionEffectType.getByName(potionTypeName.toUpperCase());
        if (potionType == null) return;

        int duration = effect.getParamAsInt("duration", 100);
        int amplifier = effect.getParamAsInt("amplifier", 0);

        player.addPotionEffect(new PotionEffect(potionType, duration, amplifier, true, false));
    }

    private void handleDamageSelf(@NotNull EffectDefinition effect, @NotNull Player player,
                                  @NotNull Map<String, Double> variables) {
        String amountFormula = effect.getParamAsString("amount");
        if (amountFormula == null) return;

        double damage = effect.evaluateFormula(amountFormula, variables);
        boolean ignoreArmor = effect.getParamAsBoolean("ignoreArmor", false);

        if (ignoreArmor) {
            player.setHealth(Math.max(0, player.getHealth() - damage));
        } else {
            player.damage(damage);
        }
    }

    /**
     * Check if a condition is met for conditional effects
     */
    public boolean checkCondition(@NotNull EffectDefinition effect, @NotNull Player player,
                                  @Nullable LivingEntity target) {
        String condition = effect.getParamAsString("condition");
        if (condition == null) return true;

        switch (condition.toLowerCase()) {
            case "entity_not_undead":
                if (target == null) return false;
                String entityType = target.getType().name();
                return !entityType.equals("SKELETON") &&
                       !entityType.equals("ZOMBIE") &&
                       !entityType.equals("WITHER_SKELETON") &&
                       !entityType.equals("ZOMBIE_VILLAGER") &&
                       !entityType.equals("HUSK") &&
                       !entityType.equals("DROWNED") &&
                       !entityType.equals("PHANTOM") &&
                       !entityType.equals("WITHER");

            case "wearing_leather_armor":
                return player.getInventory().getChestplate() != null &&
                       player.getInventory().getChestplate().getType() == Material.LEATHER_CHESTPLATE;

            case "not_wearing_leather_armor":
                return player.getInventory().getChestplate() == null ||
                       player.getInventory().getChestplate().getType() != Material.LEATHER_CHESTPLATE;

            case "full_health":
                return player.getHealth() >= CMIAttribute.MAX_HEALTH.getBaseValue(player);

            case "low_health":
                return player.getHealth() < CMIAttribute.MAX_HEALTH.getBaseValue(player) * 0.3;

            default:
                return true;
        }
    }
}
