package cn.wanyiwige.twilight;

import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import net.minecraft.core.particles.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;
import java.util.function.Supplier;

/** Distinct registered projectiles share collision, persistence, and explosion rules. */
public final class TwilightProjectile extends AbstractMagicProjectile implements software.bernie.geckolib.animatable.GeoEntity {
    private final software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache cache=software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);
    @Override public software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar registrar){registrar.add(new software.bernie.geckolib.core.animation.AnimationController<>(this,"twilight_idle",0,state->state.setAndContinue(software.bernie.geckolib.core.animation.RawAnimation.begin().thenLoop("idle"))));}
    public float aoeDamage;
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> EMBEDDED=net.minecraft.network.syncher.SynchedEntityData.defineId(TwilightProjectile.class,net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private int fuse;
    public TwilightProjectile(EntityType<? extends TwilightProjectile> type,Level world) {
        super(type,world);setNoGravity(!kind().equals("extended_ice_bomb") && !kind().equals("ice_chunk"));
    }
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(EMBEDDED,false);}
    public String kind() { return ForgeRegistries.ENTITY_TYPES.getKey(getType()).getPath(); }
    private String spell() { return switch(kind()) {case "hydra_fireball" -> "hydra_mortar";case "ice_chunk" -> "avalanche";case "extended_ice_bomb" -> "ice_bomb";case "ice_arrow" -> "aurora_missile";default -> kind();}; }
    @Override public float getSpeed() { return switch(kind()) {case "extended_ice_bomb" -> .65F;case "exanimated_trident", "ice_chunk" -> 1.5F;case "ice_arrow" -> 2.5F;default -> 1.15F;}; }
    @Override public Optional<Supplier<SoundEvent>> getImpactSound() { return Optional.of(()->kind().contains("ice")?SoundEvents.GLASS_BREAK:SoundEvents.GENERIC_EXPLODE); }
    @Override public void tick() {
        if(entityData.get(EMBEDDED)) {
            if(level().isClientSide)trailParticles();
            if(!level().isClientSide && --fuse<=0){if(kind().equals("ice_arrow"))discard();else explode(position());}
        } else super.tick();
    }
    @Override protected void onHit(HitResult hit) {
        if(level().isClientSide)return;
        if(kind().equals("exanimated_trident") && hit instanceof BlockHitResult) {
            entityData.set(EMBEDDED,true);fuse=300;setPos(hit.getLocation());setDeltaMovement(Vec3.ZERO);
            return;
        }
        if(kind().equals("extended_ice_bomb")) {
            if(hit instanceof EntityHitResult result && result.getEntity() instanceof LivingEntity living && !living.getType().is(net.minecraft.tags.EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES)) {
                DamageSources.applyDamage(living,getDamage(),TwilightSpells.get("ice_bomb").getDamageSource(this,getOwner()));
                chill(living);
            }
            freezeTerrain(hit.getLocation());
            TwilightAreaEntity.spawn(getOwner(),level(),"ice_bomb_zone",hit.getLocation(),aoeDamage,3,101);
            discard();return;
        }
        if(kind().equals("ice_arrow")) {
            if(hit instanceof EntityHitResult result) {
                boolean hitTarget=DamageSources.applyDamage(result.getEntity(),getDamage(),io.redspace.ironsspellbooks.api.registry.SpellRegistry.ICICLE_SPELL.get().getDamageSource(this,getOwner()));
                if(!hitTarget){setDeltaMovement(getDeltaMovement().scale(-.1));return;}
                if(result.getEntity().getType()!=EntityType.ENDERMAN){frostField(hit.getLocation());if(result.getEntity() instanceof LivingEntity living)living.setArrowCount(living.getArrowCount()+1);}
                consumeEntityImpact(result,true);
            }else{
                frostField(hit.getLocation());entityData.set(EMBEDDED,true);fuse=300;setPos(hit.getLocation());setDeltaMovement(Vec3.ZERO);
            }
            impactParticles(hit.getLocation().x,hit.getLocation().y,hit.getLocation().z);return;
        }
        explode(hit.getLocation());
    }
    private void frostField(Vec3 position){
        var field=new io.redspace.ironsspellbooks.entity.spells.snowball.FrostField(level());field.setOwner(getOwner());field.setDuration((int)getDamage());field.setRadius(Math.max(1,getExplosionRadius()));field.setCircular();field.moveTo(position);level().addFreshEntity(field);
    }
    private void explode(Vec3 center) {
        if(!(level() instanceof ServerLevel server))return;
        float radius=Math.max(.1F,getExplosionRadius());
        for(LivingEntity target:level().getEntitiesOfClass(LivingEntity.class,new AABB(center,center).inflate(radius),t->TwilightRecreatedSpell.hostile(getOwner(),t))) {
            double distance=target.distanceToSqr(center);
            if(distance>=radius*radius || !Utils.hasLineOfSight(level(),center.add(0,.5,0),target.getBoundingBox().getCenter(),true))continue;
            float dealt=kind().equals("ice_chunk")?getDamage():(float)(getDamage()*(1-distance/(radius*radius)));
            DamageSources.applyDamage(target,dealt,TwilightSpells.get(spell()).getDamageSource(this,getOwner()));
            if(kind().equals("exanimated_fireball"))target.addEffect(new MobEffectInstance(TwilightFoundation.OMINOUS_BURN.get(),100));
        }
        if(kind().equals("hydra_fireball")) TwilightAreaEntity.spawn(getOwner(),level(),"hydra_fire_field",Utils.moveToRelativeGroundLevel(level(),center,2,6),aoeDamage,radius,200);
        TwilightVisuals.burst(server,center,particle(),40,radius*.3);
        server.playSound(null,center.x,center.y,center.z,kind().equals("ice_chunk")?SoundEvents.GLASS_BREAK:SoundEvents.GENERIC_EXPLODE,SoundSource.NEUTRAL,2,.85F);
        discard();
    }
    public static void chill(LivingEntity target) {
        target.setTicksFrozen(Math.max(target.getTicksFrozen(),100));
        target.addEffect(new MobEffectInstance(twilightforest.init.TFMobEffects.FROSTY.get(),100,0));
    }
    private void freezeTerrain(Vec3 center){
        var origin=net.minecraft.core.BlockPos.containing(center);var snow=net.minecraft.world.level.block.Blocks.SNOW.defaultBlockState();
        for(var position:net.minecraft.core.BlockPos.betweenClosed(origin.offset(-3,-3,-3),origin.offset(3,3,3))){
            var state=level().getBlockState(position);
            if(state.is(net.minecraft.world.level.block.Blocks.WATER))level().setBlockAndUpdate(position,net.minecraft.world.level.block.Blocks.ICE.defaultBlockState());
            else if(state.is(net.minecraft.world.level.block.Blocks.LAVA)&&state.getFluidState().isSource())level().setBlockAndUpdate(position,net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState());
            else if(level().isEmptyBlock(position)&&snow.canSurvive(level(),position))level().setBlockAndUpdate(position,snow);
            else if(state.is(twilightforest.data.tags.BlockTagGenerator.ICE_BOMB_REPLACEABLES))level().setBlockAndUpdate(position,snow.canSurvive(level(),position)?snow:net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            else if(state.is(net.minecraft.world.level.block.Blocks.SNOW)&&state.getValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS)<8)level().setBlockAndUpdate(position,state.setValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS,state.getValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS)+1));
        }
    }
    public ParticleOptions particle() { return switch(kind()) {
        case "ice_chunk", "extended_ice_bomb", "ice_arrow" -> ParticleTypes.SNOWFLAKE;
        case "hydra_fireball" -> TwilightParticles.HYDRA_FIRE.get();
        case "exanimated_trident" -> TwilightParticles.EXANIMATED_FIRE.get();
        default -> TwilightParticles.EXANIMATED_SMOKE.get();
    }; }
    @Override public void trailParticles() {
        for(int i=0;i<4;i++)level().addParticle(particle(),getX()+random.nextGaussian()*.15,getY()+.2,getZ()+random.nextGaussian()*.15,0,0,0);
    }
    @Override public void impactParticles(double x,double y,double z) {
        if(level() instanceof ServerLevel server)TwilightVisuals.burst(server,new Vec3(x,y,z),particle(),20,.2);
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);tag.putFloat("AoeDamage",aoeDamage);tag.putBoolean("Embedded",entityData.get(EMBEDDED));tag.putInt("Fuse",fuse);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);aoeDamage=tag.getFloat("AoeDamage");entityData.set(EMBEDDED,tag.getBoolean("Embedded"));fuse=tag.getInt("Fuse");
    }
}
