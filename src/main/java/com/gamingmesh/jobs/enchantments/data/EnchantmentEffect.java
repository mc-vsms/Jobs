package com.gamingmesh.jobs.enchantments.data;

/**
 * Defines what effect an enchantment should apply
 */
public enum EnchantmentEffect {
    /**
     * Heals the player
     * Parameters: amount (double or formula)
     */
    HEAL_PLAYER,

    /**
     * Damages entities near the target/player
     * Parameters: radius (double), damage (double), maxTargets (int), excludeSelf (boolean)
     */
    DAMAGE_NEARBY,

    /**
     * Adds bonus damage to the attack
     * Parameters: amount (double or formula), condition (optional)
     */
    DAMAGE_BONUS,

    /**
     * Applies a potion effect
     * Parameters: type (PotionEffectType), duration (int), amplifier (int), target (SELF/TARGET)
     */
    APPLY_POTION,

    /**
     * Breaks blocks in an area
     * Parameters: radius (int), shape (SPHERE/CUBE), preventDropLoss (boolean)
     */
    BREAK_BLOCKS,

    /**
     * Auto-smelts broken blocks
     * No parameters needed
     */
    AUTO_SMELT,

    /**
     * Spawns lightning at location
     * Parameters: damage (boolean), visual (boolean)
     */
    SPAWN_LIGHTNING,

    /**
     * Reflects damage back to attacker
     * Parameters: percent (double), cap (double, optional)
     */
    REFLECT_DAMAGE,

    /**
     * Increases exp/item drops
     * Parameters: multiplier (double), type (EXP/ITEMS/BOTH)
     */
    INCREASE_DROPS,

    /**
     * Negates damage
     * Parameters: chance (double), amount (ALL/PARTIAL), partialAmount (double, if PARTIAL)
     */
    NEGATE_DAMAGE,

    /**
     * Finds and breaks connected blocks of the same type (vein miner)
     * Parameters: maxBlocks (int)
     */
    VEIN_MINE,

    /**
     * Auto-pickup items around player
     * Parameters: radius (double)
     */
    MAGNETISM,

    /**
     * Replaces broken block with another block
     * Parameters: blockType (Material), delay (int, ticks)
     */
    AUTO_REPLANT,

    /**
     * Increases player speed
     * Parameters: amplifier (int)
     */
    SPEED_BOOST,

    /**
     * Prevents death and restores health
     * Parameters: healthAmount (double)
     */
    PREVENT_DEATH,

    /**
     * Applies a curse effect (slowness, hunger, etc.)
     * Parameters: type (PotionEffectType), amplifier (int)
     */
    CURSE_EFFECT,

    /**
     * Increases durability damage
     * Parameters: extraDamage (int)
     */
    INCREASE_DURABILITY_DAMAGE,

    /**
     * Spawns particles
     * Parameters: type (Particle), count (int), spread (double)
     */
    SPAWN_PARTICLES,

    /**
     * Drains player hunger/food level
     * Parameters: amount (int), saturation (float, optional)
     */
    DRAIN_HUNGER,

    /**
     * Increases damage taken by player
     * Parameters: multiplier (double or formula), duration (int, optional)
     */
    INCREASE_DAMAGE_TAKEN,

    /**
     * Conditional effect - only works if condition is met
     * Parameters: condition (string), entityType (string, for mob checks), armorType (string, for armor checks)
     */
    CONDITIONAL_CHECK,

    /**
     * Reduces player max health temporarily
     * Parameters: amount (double), duration (int)
     */
    REDUCE_MAX_HEALTH,

    /**
     * Applies negative potion effects to self
     * Parameters: type (PotionEffectType), duration (int), amplifier (int)
     */
    APPLY_DEBUFF,

    /**
     * Damages the player
     * Parameters: amount (double or formula), ignoreArmor (boolean)
     */
    DAMAGE_SELF
}
