package cn.wanyiwige.astralcompat.recreation;

import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.entity.mobs.goals.*;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.util.OwnerHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SummonedMoth extends ArcanumInsect implements IMagicSummon {
    private java.util.UUID ownerId;
    public SummonedMoth(EntityType<? extends Animal> type,Level level) { super(type,level); }
    @Override protected void registerGoals() {
        goalSelector.addGoal(1,new MeleeAttackGoal(this,1,false));
        goalSelector.addGoal(4,new GenericFollowOwnerGoal(this,this::getSummoner,1,24,2,true,50));
        targetSelector.addGoal(1,new GenericOwnerHurtByTargetGoal(this,this::getSummoner));
        targetSelector.addGoal(2,new GenericOwnerHurtTargetGoal(this,this::getSummoner));
        targetSelector.addGoal(3,new GenericCopyOwnerTargetGoal(this,this::getSummoner));
        super.registerGoals();
    }
    @Override public boolean isAlliedTo(Entity target) { return super.isAlliedTo(target)||isAlliedHelper(target); }
    @Override public boolean hurt(DamageSource source,float amount) { return !shouldIgnoreDamage(source)&&super.hurt(source,amount); }
    @Override public boolean doHurtTarget(Entity target) {
        if(!super.doHurtTarget(target))return false;
        if(target instanceof LivingEntity living)living.addEffect(new MobEffectInstance(MobEffects.LEVITATION,40));
        return true;
    }
    @Override public InteractionResult mobInteract(Player player,InteractionHand hand) {
        if(player==getSummoner() && !isVehicle() && !player.isSecondaryUseActive()) {
            if(!level().isClientSide)player.startRiding(this);return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player,hand);
    }
    @Override public LivingEntity getControllingPassenger() { return getFirstPassenger() instanceof LivingEntity living?living:null; }
    @Override protected void tickRidden(Player player,Vec3 input) {
        super.tickRidden(player,input);setYRot(player.getYRot());setXRot(player.getXRot());yBodyRot=getYRot();yHeadRot=getYRot();
    }
    @Override protected Vec3 getRiddenInput(Player player,Vec3 input) {
        float forward=player.zza;
        return new Vec3(player.xxa*.5,forward>0?player.getLookAngle().y:0,forward>0?forward:forward*.25);
    }
    @Override protected float getRiddenSpeed(Player player) { return (float)getAttributeValue(Attributes.FLYING_SPEED)*.25F; }
    @Override public double getPassengersRidingOffset() { return 1.6; }
    @Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger) { return position().add(0,getBbHeight()+.2,0); }
    @Override public void onUnSummon() { if(!level().isClientSide) { ejectPassengers();discard(); } }
    @Override public void die(DamageSource source) { onDeathHelper();super.die(source); }
    @Override public void onRemovedFromWorld() { onRemovedHelper(this);super.onRemovedFromWorld(); }
    public void setSummoner(LivingEntity owner) { ownerId=owner.getUUID();SummonManager.setOwner(this,owner); }
    @Override public Entity getSummoner() {
        Entity owner=SummonManager.getOwner(this);
        return owner!=null?owner:ownerId!=null && level() instanceof net.minecraft.server.level.ServerLevel server?server.getEntity(ownerId):null;
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag);OwnerHelper.serializeOwner(tag,ownerId); }
    @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag);ownerId=OwnerHelper.deserializeOwner(tag); }
}
