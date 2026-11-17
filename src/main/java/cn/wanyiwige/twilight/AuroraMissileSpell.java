package cn.wanyiwige.twilight;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class AuroraMissileSpell extends AbstractSpell {
    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC).setSchoolResource(SchoolRegistry.ICE_RESOURCE)
            .setMaxLevel(6).setCooldownSeconds(1).build();
    public AuroraMissileSpell() {
        baseManaCost = 10;
        manaCostPerLevel = 2;
        baseSpellPower = 12;
        spellPowerPerLevel = 1;
        castTime = 0;
    }
    @Override public ResourceLocation getSpellResource() { return TwilightFoundation.id("aurora_missile"); }
    @Override public DefaultConfig getDefaultConfig() { return config; }
    @Override public CastType getCastType() { return CastType.INSTANT; }
    @Override public List<MutableComponent> getUniqueInfo(int level, LivingEntity caster) {
        return List.of(Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(damage(level, caster), 2)));
    }
    private float damage(int level, LivingEntity caster) {
        if (caster == null) return getSpellPower(level, null) * 7F;
        double ice = caster.getAttributeValue(AttributeRegistry.ICE_SPELL_POWER.get());
        double holy = caster.getAttributeValue(AttributeRegistry.HOLY_SPELL_POWER.get());
        return (float) ((ice + holy) * (ice == holy ? 1.5 : .5));
    }
    @Override public SpellDamageSource getDamageSource(@Nullable Entity projectile, Entity attacker) {
        return super.getDamageSource(projectile, attacker).setFreezeTicks(80);
    }
    @Override public void onCast(Level world, int level, LivingEntity caster, CastSource source, MagicData data) {
        if (!world.isClientSide) {
            AuroraMissileProjectile missile = new AuroraMissileProjectile(world, caster);
            missile.setCursorHoming(true);
            missile.setPos(caster.position().add(0, caster.getEyeHeight() - missile.getBbHeight() * .5, 0));
            missile.shoot(caster.getLookAngle());
            missile.setDamage(damage(level, caster));
            world.addFreshEntity(missile);
        }
        super.onCast(world, level, caster, source, data);
    }
}
