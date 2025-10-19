package com.gamingmesh.jobs.enchantments.data;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gamingmesh.jobs.enchantments.api.CustomEnchantment;
import net.Zrips.CMILib.Colors.CMIChatColor;

import java.util.*;

/**
 * Default implementation of CustomEnchantment interface
 */
public class EnchantmentData implements CustomEnchantment {

    private final String id;
    private final String displayName;
    private final List<String> description;
    private final String color;
    private final int maxLevel;
    private final int requiredJobLevel;
    private final boolean curse;
    private final boolean chargeable;
    private final int maxCharges;
    private final boolean visualEffects;
    private final Set<Material> primaryMaterials;
    private final Set<Material> supportedMaterials;
    private final Enchantment baseEnchantment;
    private final Set<Enchantment> conflicts;
    private final int rarity;
    private final boolean hiddenFromGui;

    // New trigger and effect system
    private final List<EnchantmentTrigger> triggers;
    private final List<EffectDefinition> effects;

    private EnchantmentData(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.description = Collections.unmodifiableList(builder.description);
        this.color = builder.color;
        this.maxLevel = builder.maxLevel;
        this.requiredJobLevel = builder.requiredJobLevel;
        this.curse = builder.curse;
        this.chargeable = builder.chargeable;
        this.maxCharges = builder.maxCharges;
        this.visualEffects = builder.visualEffects;
        this.primaryMaterials = Collections.unmodifiableSet(builder.primaryMaterials);
        this.supportedMaterials = Collections.unmodifiableSet(builder.supportedMaterials);
        this.baseEnchantment = builder.baseEnchantment;
        this.conflicts = Collections.unmodifiableSet(builder.conflicts);
        this.rarity = builder.rarity;
        this.hiddenFromGui = builder.hiddenFromGui;
        this.triggers = Collections.unmodifiableList(builder.triggers);
        this.effects = Collections.unmodifiableList(builder.effects);
    }

    @Override
    @NotNull
    public String getId() {
        return id;
    }

    @Override
    @NotNull
    public String getDisplayName() {
        return CMIChatColor.translate(color + displayName);
    }

    @Override
    @NotNull
    public List<String> getDescription() {
        List<String> translatedDescription = new ArrayList<>();
        for (String line : description) {
            translatedDescription.add(CMIChatColor.translate(line));
        }
        return translatedDescription;
    }

    @Override
    @NotNull
    public String getColor() {
        return color;
    }

    @Override
    public int getMaxLevel() {
        return maxLevel;
    }

    @Override
    public int getRequiredJobLevel() {
        return requiredJobLevel;
    }

    @Override
    public boolean isCurse() {
        return curse;
    }

    @Override
    public boolean isChargeable() {
        return chargeable;
    }

    @Override
    public int getMaxCharges() {
        return maxCharges;
    }

    @Override
    public boolean hasVisualEffects() {
        return visualEffects;
    }

    @Override
    @NotNull
    public Set<Material> getPrimaryMaterials() {
        return primaryMaterials;
    }

    @Override
    @NotNull
    public Set<Material> getSupportedMaterials() {
        return supportedMaterials;
    }

    @Override
    public boolean canEnchantItem(@NotNull ItemStack item) {
        return supportedMaterials.contains(item.getType());
    }

    @Override
    @Nullable
    public Enchantment getBaseEnchantment() {
        return baseEnchantment;
    }

    @Override
    @NotNull
    public Set<Enchantment> getConflicts() {
        return conflicts;
    }

    @Override
    public int getRarity() {
        return rarity;
    }

    @Override
    public boolean isHiddenFromGui() {
        return hiddenFromGui;
    }

    /**
     * Get the triggers for this enchantment
     */
    @NotNull
    public List<EnchantmentTrigger> getTriggers() {
        return triggers;
    }

    /**
     * Get the effects for this enchantment
     */
    @NotNull
    public List<EffectDefinition> getEffects() {
        return effects;
    }

    /**
     * Check if this enchantment has a specific trigger
     */
    public boolean hasTrigger(@NotNull EnchantmentTrigger trigger) {
        return triggers.contains(trigger);
    }

    /**
     * Check if this enchantment has any effects
     */
    public boolean hasEffects() {
        return !effects.isEmpty();
    }

    public static class Builder {
        private String id;
        private String displayName;
        private List<String> description = new ArrayList<>();
        private String color = "&7";
        private int maxLevel = 1;
        private int requiredJobLevel = 0;
        private boolean curse = false;
        private boolean chargeable = false;
        private int maxCharges = 0;
        private boolean visualEffects = true;
        private Set<Material> primaryMaterials = new HashSet<>();
        private Set<Material> supportedMaterials = new HashSet<>();
        private Enchantment baseEnchantment = null;
        private Set<Enchantment> conflicts = new HashSet<>();
        private int rarity = 10;
        private boolean hiddenFromGui = false;
        private List<EnchantmentTrigger> triggers = new ArrayList<>();
        private List<EffectDefinition> effects = new ArrayList<>();

        public Builder(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public Builder description(List<String> description) {
            this.description = new ArrayList<>(description);
            return this;
        }

        public Builder color(String color) {
            this.color = color;
            return this;
        }

        public Builder maxLevel(int maxLevel) {
            this.maxLevel = maxLevel;
            return this;
        }

        public Builder requiredJobLevel(int requiredJobLevel) {
            this.requiredJobLevel = requiredJobLevel;
            return this;
        }

        public Builder curse(boolean curse) {
            this.curse = curse;
            return this;
        }

        public Builder chargeable(boolean chargeable) {
            this.chargeable = chargeable;
            return this;
        }

        public Builder maxCharges(int maxCharges) {
            this.maxCharges = maxCharges;
            return this;
        }

        public Builder visualEffects(boolean visualEffects) {
            this.visualEffects = visualEffects;
            return this;
        }

        public Builder primaryMaterials(Set<Material> materials) {
            this.primaryMaterials = new HashSet<>(materials);
            return this;
        }

        public Builder supportedMaterials(Set<Material> materials) {
            this.supportedMaterials = new HashSet<>(materials);
            return this;
        }

        public Builder baseEnchantment(Enchantment enchantment) {
            this.baseEnchantment = enchantment;
            return this;
        }

        public Builder conflicts(Set<Enchantment> conflicts) {
            this.conflicts = new HashSet<>(conflicts);
            return this;
        }

        public Builder rarity(int rarity) {
            this.rarity = rarity;
            return this;
        }

        public Builder hiddenFromGui(boolean hidden) {
            this.hiddenFromGui = hidden;
            return this;
        }

        /**
         * Add a trigger to this enchantment
         */
        public Builder addTrigger(EnchantmentTrigger trigger) {
            this.triggers.add(trigger);
            return this;
        }

        /**
         * Add multiple triggers to this enchantment
         */
        public Builder triggers(EnchantmentTrigger... triggers) {
            this.triggers.addAll(Arrays.asList(triggers));
            return this;
        }

        /**
         * Add an effect to this enchantment
         */
        public Builder addEffect(EffectDefinition effect) {
            this.effects.add(effect);
            return this;
        }

        /**
         * Add multiple effects to this enchantment
         */
        public Builder effects(EffectDefinition... effects) {
            this.effects.addAll(Arrays.asList(effects));
            return this;
        }

        public EnchantmentData build() {
            if (id == null || id.isEmpty()) {
                throw new IllegalStateException("Enchantment ID cannot be null or empty");
            }
            if (displayName == null || displayName.isEmpty()) {
                throw new IllegalStateException("Enchantment display name cannot be null or empty");
            }
            return new EnchantmentData(this);
        }
    }
}
