package com.gamingmesh.jobs.enchantments.data;

/**
 * Defines when an enchantment effect should trigger
 */
public enum EnchantmentTrigger {
    /**
     * Triggers when the player attacks an entity
     */
    ON_ATTACK,

    /**
     * Triggers when the player takes damage
     */
    ON_DAMAGED,

    /**
     * Triggers when the player breaks a block
     */
    ON_BLOCK_BREAK,

    /**
     * Triggers when the player kills an entity
     */
    ON_KILL,

    /**
     * Triggers passively (constant effect while equipped)
     */
    PASSIVE,

    /**
     * Triggers when the player moves
     */
    ON_MOVE,

    /**
     * Triggers when the player falls (fall damage)
     */
    ON_FALL,

    /**
     * Triggers when the player is about to die from lethal damage
     */
    ON_LETHAL_DAMAGE,

    /**
     * Triggers when a block is placed
     */
    ON_BLOCK_PLACE,

    /**
     * Triggers periodically (timed effect)
     */
    PERIODIC
}
