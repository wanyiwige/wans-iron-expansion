package cn.wanyiwige.twilight;

import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import twilightforest.entity.projectile.NatureBolt;

/** Keeps Twilight Forest's nature bolt projectile and adds the source spell's damage and poison. */
public final class DruidBoltProjectile extends NatureBolt {
    private float spellDamage;

    public DruidBoltProjectile(EntityType<? extends NatureBolt> type, Level level) {
        super(type, level);
    }
    public DruidBoltProjectile(Level level, LivingEntity caster) {
        this(TwilightEntities.DRUID_BOLT.get(), level);
        setOwner(caster);
    }
    public void setDamage(float value) { spellDamage = value; }
    public void shoot(Vec3 direction) { setDeltaMovement(direction.normalize().scale(1.75)); }
    @Override protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide) {
            DamageSources.applyDamage(result.getEntity(), spellDamage,
                    TwilightSpells.DRUID_BOLT.get().getDamageSource(this, getOwner()));
            if (result.getEntity() instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.POISON, 140, 1));
            }
        }
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("SpellDamage", spellDamage);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        spellDamage = tag.getFloat("SpellDamage");
    }
}
