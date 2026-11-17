package cn.wanyiwige.illusion;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import java.util.*;
import java.util.function.Supplier;
public final class FearBolt extends AbstractMagicProjectile {
    public FearBolt(EntityType<? extends FearBolt> type,Level level){super(type,level);setNoGravity(true);}
    @Override public float getSpeed(){return 1.3F;}
    @Override public Optional<Supplier<SoundEvent>> getImpactSound(){return Optional.empty();}
    @Override protected void onHitBlock(BlockHitResult hit){super.onHitBlock(hit);discard();}
    @Override protected void onHitEntity(EntityHitResult hit){
        if(getOwner()!=null&&getOwner().isAlliedTo(hit.getEntity()))return;
        super.onHitEntity(hit);
        if(!level().isClientSide){
            if(DamageSources.applyDamage(hit.getEntity(),getDamage(),IllusionContent.SPELL_MAP.get("fear_bolt").get().getDamageSource(this,getOwner()))&&hit.getEntity() instanceof LivingEntity victim){
                victim.addEffect(new MobEffectInstance(IllusionContent.effect("feared"),120));
                victim.addEffect(new MobEffectInstance(io.redspace.ironsspellbooks.registries.MobEffectRegistry.GUIDING_BOLT.get(),500));IllusionState.sync(victim);
                if(victim instanceof net.minecraft.server.level.ServerPlayer p)p.playNotifySound(IllusionContent.FLASH.get(),net.minecraft.sounds.SoundSource.PLAYERS,1,1);
            }
            consumeEntityImpact(hit,true);
        }
    }
    @Override public void trailParticles(){level().addParticle(ParticleTypes.CRIT,getX(),getY(),getZ(),0,0,0);}
    @Override public void impactParticles(double x,double y,double z){}
}
