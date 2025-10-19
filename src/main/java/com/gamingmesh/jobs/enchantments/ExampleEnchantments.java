package com.gamingmesh.jobs.enchantments;

import com.gamingmesh.jobs.enchantments.api.CustomEnchantment;
import com.gamingmesh.jobs.enchantments.data.*;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

import java.util.*;

/**
 * Example custom enchantments using the data-driven trigger+effect system
 * These can be loaded as examples or replaced with configuration-based enchantments
 */
public class ExampleEnchantments {

    /**
     * Create example enchantments for demonstration
     * @return List of example enchantments
     */
    public static List<CustomEnchantment> createExamples() {
        List<CustomEnchantment> examples = new ArrayList<>();

        examples.add(createVampiric());
        examples.add(createChainStrike());
        examples.add(createExecute());
        examples.add(createFrost());
        examples.add(createFeatherFall());
        examples.add(createUndying());
        examples.add(createReflection());
        examples.add(createNightVision());
        examples.add(createLightweight());
        examples.add(createAreaMining());
        examples.add(createAutoPlant());
        examples.add(createVeinMiner());

        return examples;
    }

    /**
     * Example 1: Vampiric - Heals player based on damage dealt
     * TRADE-OFF: Only works on living creatures (not undead like skeletons/zombies)
     */
    public static CustomEnchantment createVampiric() {
        return new EnchantmentData.Builder("vampiric", "吸血")
                .description(Arrays.asList(
                        "&7攻撃時、ダメージの一部を体力として吸収",
                        "&8アンデッド系モンスターからは吸収不可"))
                .color("&c")
                .maxLevel(3)
                .requiredJobLevel(15)
                .chargeable(true)
                .maxCharges(100)
                .visualEffects(true)
                .primaryMaterials(getSwordMaterials())
                .supportedMaterials(getSwordMaterials())
                .rarity(6)
                .addTrigger(EnchantmentTrigger.ON_ATTACK)
                // Conditional check: only works on non-undead entities
                .addEffect(new EffectDefinition(EnchantmentEffect.CONDITIONAL_CHECK)
                        .param("condition", "entity_not_undead"))
                .addEffect(new EffectDefinition(EnchantmentEffect.HEAL_PLAYER)
                        .param("amount", "damage * (0.15 + {level} * 0.1)"))
                .build();
    }

    /**
     * Example 2: Chain Strike - Spreads damage to nearby enemies
     */
    public static CustomEnchantment createChainStrike() {
        return new EnchantmentData.Builder("chain_strike", "連鎖攻撃")
                .description(Arrays.asList("&7攻撃が周囲の敵にも伝播"))
                .color("&e")
                .maxLevel(3)
                .requiredJobLevel(25)
                .chargeable(true)
                .maxCharges(50)
                .visualEffects(true)
                .primaryMaterials(getSwordMaterials())
                .supportedMaterials(getSwordMaterials())
                .rarity(4)
                .addTrigger(EnchantmentTrigger.ON_ATTACK)
                .addEffect(new EffectDefinition(EnchantmentEffect.DAMAGE_NEARBY)
                        .param("radius", "3.0 + {level} * 1.0")  // 4, 5, 6 blocks
                        .param("damage", "damage * (0.3 + {level} * 0.1)")  // 40%, 50%, 60%
                        .param("maxTargets", "2 + {level}"))  // 3, 4, 5 targets
                .addEffect(new EffectDefinition(EnchantmentEffect.SPAWN_LIGHTNING)
                        .param("damaging", false))
                .build();
    }

    /**
     * Example 3: Execute - Extra damage to low health enemies
     */
    public static CustomEnchantment createExecute() {
        return new EnchantmentData.Builder("execute", "処刑")
                .description(Arrays.asList("&7体力が低い敵に追加ダメージ"))
                .color("&4")
                .maxLevel(3)
                .requiredJobLevel(30)
                .chargeable(true)
                .maxCharges(30)
                .visualEffects(true)
                .primaryMaterials(getSwordMaterials())
                .supportedMaterials(getSwordMaterials())
                .rarity(3)
                .addTrigger(EnchantmentTrigger.ON_ATTACK)
                .addEffect(new EffectDefinition(EnchantmentEffect.DAMAGE_BONUS)
                        .param("amount", "4.0 + {level} * 2.0")  // 6, 8, 10 extra damage
                        .param("condition", "targetHealth < 0.3"))  // Only when target below 30% HP
                .addEffect(new EffectDefinition(EnchantmentEffect.SPAWN_LIGHTNING)
                        .param("damaging", false))
                .build();
    }

    /**
     * Example 4: Frost - Applies slowness and freezes enemy
     * TRADE-OFF: Without leather armor, user also gets frozen
     */
    public static CustomEnchantment createFrost() {
        return new EnchantmentData.Builder("frost", "氷結")
                .description(Arrays.asList(
                        "&7敵を凍らせて減速",
                        "&8革装備を着ていないと自分も凍傷になる"))
                .color("&b")
                .maxLevel(3)
                .requiredJobLevel(20)
                .chargeable(true)
                .maxCharges(80)
                .visualEffects(true)
                .primaryMaterials(getSwordMaterials())
                .supportedMaterials(getSwordMaterials())
                .rarity(7)
                .addTrigger(EnchantmentTrigger.ON_ATTACK)
                // Effect 1: Freeze enemy
                .addEffect(new EffectDefinition(EnchantmentEffect.APPLY_POTION)
                        .param("type", "SLOWNESS")
                        .param("duration", "40 + {level} * 20")  // 2, 3, 4 seconds
                        .param("amplifier", "{level} - 1")  // 0, 1, 2 (Slowness I, II, III)
                        .param("target", "victim"))
                // Effect 2: If not wearing leather armor, freeze self too
                .addEffect(new EffectDefinition(EnchantmentEffect.CONDITIONAL_CHECK)
                        .param("condition", "not_wearing_leather_armor"))
                .addEffect(new EffectDefinition(EnchantmentEffect.APPLY_DEBUFF)
                        .param("type", "SLOWNESS")
                        .param("duration", "20 + {level} * 10")  // 1, 1.5, 2 seconds
                        .param("amplifier", "0"))  // Slowness I
                .build();
    }

    /**
     * Example 5: Feather Fall - Negates fall damage
     */
    public static CustomEnchantment createFeatherFall() {
        return new EnchantmentData.Builder("feather_fall", "軽量落下")
                .description(Arrays.asList("&7落下ダメージを無効化"))
                .color("&f")
                .maxLevel(1)
                .requiredJobLevel(10)
                .chargeable(true)
                .maxCharges(50)
                .visualEffects(false)
                .primaryMaterials(getBootMaterials())
                .supportedMaterials(getBootMaterials())
                .rarity(8)
                .addTrigger(EnchantmentTrigger.ON_FALL)
                .addEffect(new EffectDefinition(EnchantmentEffect.NEGATE_DAMAGE)
                        .param("chance", 0.80))  // 80% chance
                .build();
    }

    /**
     * Example 6: Undying - Survive lethal damage once
     */
    public static CustomEnchantment createUndying() {
        return new EnchantmentData.Builder("undying", "不死")
                .description(Arrays.asList("&7致命的なダメージを一度だけ無効化"))
                .color("&6")
                .maxLevel(1)
                .requiredJobLevel(40)
                .chargeable(true)
                .maxCharges(1)  // Only one use
                .visualEffects(true)
                .primaryMaterials(getArmorMaterials())
                .supportedMaterials(getArmorMaterials())
                .rarity(2)
                .addTrigger(EnchantmentTrigger.ON_LETHAL_DAMAGE)
                .addEffect(new EffectDefinition(EnchantmentEffect.PREVENT_DEATH)
                        .param("health", 1.0))  // Survive with 1 HP
                .addEffect(new EffectDefinition(EnchantmentEffect.APPLY_POTION)
                        .param("type", "REGENERATION")
                        .param("duration", 100)
                        .param("amplifier", 1)
                        .param("target", "player"))
                .addEffect(new EffectDefinition(EnchantmentEffect.APPLY_POTION)
                        .param("type", "FIRE_RESISTANCE")
                        .param("duration", 100)
                        .param("amplifier", 0)
                        .param("target", "player"))
                .addEffect(new EffectDefinition(EnchantmentEffect.APPLY_POTION)
                        .param("type", "ABSORPTION")
                        .param("duration", 100)
                        .param("amplifier", 1)
                        .param("target", "player"))
                .build();
    }

    /**
     * Example 7: Reflection - Reflects damage back to attacker
     */
    public static CustomEnchantment createReflection() {
        return new EnchantmentData.Builder("reflection", "反射")
                .description(Arrays.asList("&7受けたダメージを攻撃者に反射"))
                .color("&d")
                .maxLevel(3)
                .requiredJobLevel(25)
                .chargeable(true)
                .maxCharges(100)
                .visualEffects(true)
                .primaryMaterials(getArmorMaterials())
                .supportedMaterials(getArmorMaterials())
                .rarity(5)
                .addTrigger(EnchantmentTrigger.ON_DAMAGED)
                .addEffect(new EffectDefinition(EnchantmentEffect.REFLECT_DAMAGE)
                        .param("percent", "0.15 + {level} * 0.1"))  // 25%, 35%, 45%
                .build();
    }

    /**
     * Example 8: Night Vision - Constant night vision while equipped
     */
    public static CustomEnchantment createNightVision() {
        return new EnchantmentData.Builder("night_vision", "暗視")
                .description(Arrays.asList("&7装備中は暗視効果"))
                .color("&9")
                .maxLevel(1)
                .requiredJobLevel(5)
                .chargeable(false)
                .visualEffects(false)
                .primaryMaterials(getHelmetMaterials())
                .supportedMaterials(getHelmetMaterials())
                .rarity(10)
                .addTrigger(EnchantmentTrigger.PASSIVE)
                .addEffect(new EffectDefinition(EnchantmentEffect.APPLY_POTION)
                        .param("type", "NIGHT_VISION")
                        .param("duration", 400)
                        .param("amplifier", 0)
                        .param("ambient", true)
                        .param("particles", false)
                        .param("target", "player"))
                .build();
    }

    /**
     * Example 9: Lightweight - Speed boost while wearing armor
     * TRADE-OFF: Increases damage taken by 10-30%
     */
    public static CustomEnchantment createLightweight() {
        return new EnchantmentData.Builder("lightweight", "軽量化")
                .description(Arrays.asList(
                        "&7装備中は移動速度上昇",
                        "&8代償：被ダメージが増加"))
                .color("&f")
                .maxLevel(3)
                .requiredJobLevel(15)
                .chargeable(false)
                .visualEffects(false)
                .primaryMaterials(getArmorMaterials())
                .supportedMaterials(getArmorMaterials())
                .rarity(8)
                // Passive speed boost
                .addTrigger(EnchantmentTrigger.PASSIVE)
                .addEffect(new EffectDefinition(EnchantmentEffect.SPEED_BOOST)
                        .param("amplifier", "{level} - 1")  // Speed I, II, III
                        .param("duration", 40))
                // Trade-off: increase damage taken when attacked
                .addTrigger(EnchantmentTrigger.ON_DAMAGED)
                .addEffect(new EffectDefinition(EnchantmentEffect.INCREASE_DAMAGE_TAKEN)
                        .param("multiplier", "1.0 + {level} * 0.1"))  // 1.1x, 1.2x, 1.3x
                .build();
    }

    /**
     * Example 10: Area Mining - Breaks 3x3 area
     * TRADE-OFF: Drains hunger/food level
     */
    public static CustomEnchantment createAreaMining() {
        return new EnchantmentData.Builder("area_mining", "範囲採掘")
                .description(Arrays.asList(
                        "&7ブロックを範囲破壊",
                        "&8代償：空腹度を消費"))
                .color("&6")
                .maxLevel(3)
                .requiredJobLevel(20)
                .chargeable(true)
                .maxCharges(100)
                .visualEffects(false)
                .primaryMaterials(getPickaxeMaterials())
                .supportedMaterials(getPickaxeMaterials())
                .rarity(5)
                .addTrigger(EnchantmentTrigger.ON_BLOCK_BREAK)
                // Effect 1: Break blocks in area
                .addEffect(new EffectDefinition(EnchantmentEffect.BREAK_BLOCKS)
                        .param("radius", "{level}"))  // 1, 2, 3 (3x3, 5x5, 7x7)
                // Effect 2: Drain hunger
                .addEffect(new EffectDefinition(EnchantmentEffect.DRAIN_HUNGER)
                        .param("amount", "{level}")  // 1, 2, 3 food points
                        .param("saturation", "0.5"))  // 0.5 saturation
                .build();
    }

    /**
     * Example 11: Auto Plant - Automatically replants trees
     */
    public static CustomEnchantment createAutoPlant() {
        return new EnchantmentData.Builder("auto_plant", "自動植林")
                .description(Arrays.asList("&7破壊した木を自動で植え直す"))
                .color("&2")
                .maxLevel(1)
                .requiredJobLevel(10)
                .chargeable(false)
                .visualEffects(false)
                .primaryMaterials(getAxeMaterials())
                .supportedMaterials(getAxeMaterials())
                .rarity(9)
                .addTrigger(EnchantmentTrigger.ON_BLOCK_BREAK)
                .addEffect(new EffectDefinition(EnchantmentEffect.AUTO_REPLANT)
                        .param("blockType", "leaves")
                        .param("replantType", "sapling"))
                .build();
    }

    /**
     * Example 12: Vein Miner - Mines connected ores
     */
    public static CustomEnchantment createVeinMiner() {
        return new EnchantmentData.Builder("vein_miner", "鉱脈採掘")
                .description(Arrays.asList("&7繋がった鉱石を一度に採掘"))
                .color("&b")
                .maxLevel(3)
                .requiredJobLevel(30)
                .chargeable(true)
                .maxCharges(50)
                .visualEffects(true)
                .primaryMaterials(getPickaxeMaterials())
                .supportedMaterials(getPickaxeMaterials())
                .rarity(3)
                .addTrigger(EnchantmentTrigger.ON_BLOCK_BREAK)
                .addEffect(new EffectDefinition(EnchantmentEffect.VEIN_MINE)
                        .param("maxBlocks", "8 + {level} * 4"))  // 12, 16, 20 blocks
                .build();
    }

    // Helper methods to get material sets

    private static Set<Material> getSwordMaterials() {
        return EnumSet.of(
                Material.WOODEN_SWORD,
                Material.STONE_SWORD,
                Material.IRON_SWORD,
                Material.GOLDEN_SWORD,
                Material.DIAMOND_SWORD,
                Material.NETHERITE_SWORD
        );
    }

    private static Set<Material> getPickaxeMaterials() {
        return EnumSet.of(
                Material.WOODEN_PICKAXE,
                Material.STONE_PICKAXE,
                Material.IRON_PICKAXE,
                Material.GOLDEN_PICKAXE,
                Material.DIAMOND_PICKAXE,
                Material.NETHERITE_PICKAXE
        );
    }

    private static Set<Material> getAxeMaterials() {
        return EnumSet.of(
                Material.WOODEN_AXE,
                Material.STONE_AXE,
                Material.IRON_AXE,
                Material.GOLDEN_AXE,
                Material.DIAMOND_AXE,
                Material.NETHERITE_AXE
        );
    }

    private static Set<Material> getBootMaterials() {
        return EnumSet.of(
                Material.LEATHER_BOOTS,
                Material.CHAINMAIL_BOOTS,
                Material.IRON_BOOTS,
                Material.GOLDEN_BOOTS,
                Material.DIAMOND_BOOTS,
                Material.NETHERITE_BOOTS
        );
    }

    private static Set<Material> getHelmetMaterials() {
        return EnumSet.of(
                Material.LEATHER_HELMET,
                Material.CHAINMAIL_HELMET,
                Material.IRON_HELMET,
                Material.GOLDEN_HELMET,
                Material.DIAMOND_HELMET,
                Material.NETHERITE_HELMET
        );
    }

    private static Set<Material> getArmorMaterials() {
        Set<Material> materials = new HashSet<>();
        materials.addAll(getHelmetMaterials());
        materials.addAll(EnumSet.of(
                Material.LEATHER_CHESTPLATE,
                Material.CHAINMAIL_CHESTPLATE,
                Material.IRON_CHESTPLATE,
                Material.GOLDEN_CHESTPLATE,
                Material.DIAMOND_CHESTPLATE,
                Material.NETHERITE_CHESTPLATE
        ));
        materials.addAll(EnumSet.of(
                Material.LEATHER_LEGGINGS,
                Material.CHAINMAIL_LEGGINGS,
                Material.IRON_LEGGINGS,
                Material.GOLDEN_LEGGINGS,
                Material.DIAMOND_LEGGINGS,
                Material.NETHERITE_LEGGINGS
        ));
        materials.addAll(getBootMaterials());
        return materials;
    }
}
