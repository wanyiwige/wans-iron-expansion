package cn.wanyiwige.twilight;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import twilightforest.capabilities.CapabilityList;

import java.util.List;

public final class FortifyingShieldsSpell extends AbstractSpell {
    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY).setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
            .setMaxLevel(1).setCooldownSeconds(25).setAllowCrafting(false).build();

    public FortifyingShieldsSpell() {
        baseManaCost = 120;
        manaCostPerLevel = 0;
        baseSpellPower = 4;
        spellPowerPerLevel = 1;
        castTime = 30;
    }
    @Override public ResourceLocation getSpellResource() { return TwilightFoundation.id("fortifying_shields"); }
    @Override public DefaultConfig getDefaultConfig() { return config; }
    @Override public CastType getCastType() { return CastType.LONG; }
    @Override public boolean allowLooting() { return false; }
    @Override public AnimationHolder getCastStartAnimation() { return SpellAnimations.SELF_CAST_ANIMATION; }
    @Override public List<MutableComponent> getUniqueInfo(int level, LivingEntity caster) {
        return List.of(Component.translatable("ui.wans_twilight_spellbooks.shields",
                "6"));
    }
    @Override public void onCast(Level world, int level, LivingEntity caster, CastSource source, MagicData data) {
        if (!world.isClientSide) {
            caster.getCapability(CapabilityList.SHIELDS).ifPresent(shields -> shields.setShields(6, true));
            if (world instanceof ServerLevel server) {
                for (int i = 0; i < 16; i++) {
                    double angle = Math.PI * 2 * i / 16;
                    server.sendParticles(ParticleTypes.END_ROD,
                            caster.getX() + Math.cos(angle) * 1.25, caster.getY() + .5,
                            caster.getZ() + Math.sin(angle) * 1.25, 1, 0, 0, 0, .1);
                }
            }
        }
        super.onCast(world, level, caster, source, data);
    }
}
