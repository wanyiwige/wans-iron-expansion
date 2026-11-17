package cn.wanyiwige.twilight;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import twilightforest.init.TFSounds;

import java.util.List;
import java.util.Optional;

/** Recreates the supplied Twilight Scepter bolt's cast cost and projectile behavior. */
public final class TwilightBoltSpell extends AbstractSpell {
    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(1).setCooldownSeconds(0)
            .setAllowCrafting(false).build();

    public TwilightBoltSpell() {
        baseManaCost = 30;
        manaCostPerLevel = 5;
        baseSpellPower = 10;
        spellPowerPerLevel = 1;
        castTime = 0;
    }

    @Override public ResourceLocation getSpellResource() { return TwilightFoundation.id("twilight_bolt"); }
    @Override public DefaultConfig getDefaultConfig() { return config; }
    @Override public CastType getCastType() { return CastType.INSTANT; }
    @Override public boolean allowLooting() { return false; }
    @Override public Optional<SoundEvent> getCastFinishSound() { return Optional.of(TFSounds.SCEPTER_PEARL.get()); }
    @Override public List<MutableComponent> getUniqueInfo(int level, LivingEntity caster) {
        return List.of(Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getSpellPower(level, caster), 2)));
    }
    @Override public void onCast(Level world, int level, LivingEntity caster, CastSource source, MagicData data) {
        if (!world.isClientSide) {
            TwilightBoltProjectile bolt = new TwilightBoltProjectile(world, caster);
            bolt.setPos(caster.position().add(0, caster.getEyeHeight() - bolt.getBbHeight() * .5, 0));
            bolt.shoot(caster.getLookAngle());
            bolt.setDamage(getSpellPower(level, caster));
            world.addFreshEntity(bolt);
        }
        super.onCast(world, level, caster, source, data);
    }
}
