package cn.wanyiwige.twilight;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

public final class DruidBoltSpell extends AbstractSpell {
    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON).setSchoolResource(SchoolRegistry.NATURE_RESOURCE)
            .setMaxLevel(10).setCooldownSeconds(1).build();

    public DruidBoltSpell() {
        baseManaCost = 10;
        manaCostPerLevel = 2;
        baseSpellPower = 12;
        spellPowerPerLevel = 1;
        castTime = 0;
    }
    @Override public ResourceLocation getSpellResource() { return TwilightFoundation.id("druid_bolt"); }
    @Override public DefaultConfig getDefaultConfig() { return config; }
    @Override public CastType getCastType() { return CastType.INSTANT; }
    @Override public List<MutableComponent> getUniqueInfo(int level, LivingEntity caster) {
        return List.of(Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getSpellPower(level, caster) * .5F, 2)));
    }
    @Override public void onCast(Level world, int level, LivingEntity caster, CastSource source, MagicData data) {
        if (!world.isClientSide) {
            DruidBoltProjectile bolt = new DruidBoltProjectile(world, caster);
            bolt.setPos(caster.position().add(0, caster.getEyeHeight() - bolt.getBbHeight() * .5, 0));
            bolt.shoot(caster.getLookAngle());
            bolt.setDamage(getSpellPower(level, caster) * .5F);
            world.addFreshEntity(bolt);
        }
        super.onCast(world, level, caster, source, data);
    }
}
