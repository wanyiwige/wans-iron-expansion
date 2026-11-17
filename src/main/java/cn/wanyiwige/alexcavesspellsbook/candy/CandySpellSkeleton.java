package cn.wanyiwige.alexcavesspellsbook.candy;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** Deliberately unavailable until the individual spell is implemented. */
public final class CandySpellSkeleton extends AbstractSpell {
    private final CandySpecs.Spec spec;
    private final DefaultConfig config;
    public CandySpellSkeleton(CandySpecs.Spec spec) {
        this.spec = spec;
        config = new DefaultConfig().setSchoolResource(CandyRegistry.SCHOOL_ID).setMinRarity(spec.rarity())
                .setMaxLevel(spec.maxLevel()).setCooldownSeconds(spec.cooldown()).setAllowCrafting(false).setDeprecated(true).build();
        baseManaCost = spec.mana(); manaCostPerLevel = spec.manaGrowth();
        baseSpellPower = 1; spellPowerPerLevel = 0; castTime = spec.castTicks();
    }
    @Override public ResourceLocation getSpellResource() { return CandyRegistry.id(spec.id()); }
    @Override public DefaultConfig getDefaultConfig() { return config; }
    @Override public CastType getCastType() { return spec.castType(); }
    @Override public boolean allowLooting() { return false; }
    @Override public boolean allowCrafting() { return false; }
    @Override public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) { return false; }
    @Override public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) { }
}
