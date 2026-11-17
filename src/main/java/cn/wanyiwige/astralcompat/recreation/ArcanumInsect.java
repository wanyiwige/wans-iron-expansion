package cn.wanyiwige.astralcompat.recreation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.ai.navigation.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ArcanumInsect extends Animal implements GeoEntity,FlyingAnimal {
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    public ArcanumInsect(EntityType<? extends Animal> type,Level level) {
        super(type,level);moveControl=new FlyingMoveControl(this,20,true);setNoGravity(true);xpReward=0;
    }
    public boolean dragonfly() { return getType()==ArcanumContent.DRAGONFLY.get(); }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        if(getType()==ArcanumContent.DRAGONFLY.get()) {
            goalSelector.addGoal(1,new MeleeAttackGoal(this,1,true));
            targetSelector.addGoal(0,new HurtByTargetGoal(this));
            targetSelector.addGoal(1,new NearestAttackableTargetGoal<>(this,Player.class,true));
            targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Astromancer.class,true));
            targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,Spider.class,true));
            targetSelector.addGoal(4,new NearestAttackableTargetGoal<>(this,Bee.class,true));
        } else {
            goalSelector.addGoal(3,new TemptGoal(this,1,Ingredient.of(net.minecraft.tags.ItemTags.FLOWERS),false));
        }
        goalSelector.addGoal(5,new WaterAvoidingRandomFlyingGoal(this,.5));
        goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,8));
    }
    public static AttributeSupplier.Builder attributes(boolean dragonfly) {
        var out=Mob.createMobAttributes().add(Attributes.MAX_HEALTH,dragonfly?20:15).add(Attributes.FOLLOW_RANGE,24)
                .add(Attributes.MOVEMENT_SPEED,dragonfly?.5:1).add(Attributes.FLYING_SPEED,dragonfly?.5:1.3)
                .add(Attributes.ATTACK_DAMAGE,2).add(Attributes.ARMOR,dragonfly?2:0);
        if(dragonfly)out.add(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_RESIST.get(),.1);
        return out;
    }
    @Override protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav=new FlyingPathNavigation(this,level);nav.setCanOpenDoors(false);nav.setCanPassDoors(true);return nav;
    }
    @Override public void travel(Vec3 input) {
        if(isControlledByLocalInstance()) {
            moveRelative(getSpeed(),input);move(MoverType.SELF,getDeltaMovement());setDeltaMovement(getDeltaMovement().scale(isInWater()?.8:.9));
        }
        calculateEntityAnimation(false);
    }
    @Override public boolean causeFallDamage(float distance,float multiplier,net.minecraft.world.damagesource.DamageSource source) { return false; }
    @Override public boolean isFood(ItemStack stack) { return false; }
    @Override public AgeableMob getBreedOffspring(ServerLevel level,AgeableMob other) { return null; }
    @Override public boolean isFlying() { return !onGround(); }
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return dragonfly()?net.minecraft.sounds.SoundEvents.BEE_LOOP:net.minecraft.sounds.SoundEvents.GLOW_SQUID_AMBIENT; }
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) { return dragonfly()?net.minecraft.sounds.SoundEvents.SILVERFISH_HURT:net.minecraft.sounds.SoundEvents.GLOW_SQUID_HURT; }
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return dragonfly()?net.minecraft.sounds.SoundEvents.SILVERFISH_DEATH:net.minecraft.sounds.SoundEvents.GLOW_SQUID_DEATH; }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    public String animation() { return dragonfly()?"idle_state":this instanceof SummonedMoth?"animation.big_moth.base":"animation.lunar_moth.base"; }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this,"flight",0,state->{state.getController().setAnimation(RawAnimation.begin().thenLoop(animation()));return PlayState.CONTINUE;}));
    }
}
