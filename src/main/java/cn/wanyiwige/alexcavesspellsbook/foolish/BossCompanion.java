package cn.wanyiwige.alexcavesspellsbook.foolish;

import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.syncher.*;
import net.minecraft.nbt.CompoundTag;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Destructible, owned summons using Foolish geometry, with explicit AI and expiration. */
public final class BossCompanion extends PathfinderMob implements GeoEntity,IMagicSummon {
    public enum Kind{BUG,ASSISTANT,SENTINEL,LEAF,VINE}
    private static final EntityDataAccessor<Integer> KIND=SynchedEntityData.defineId(BossCompanion.class,EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    public BossCast cast;
    private int lifetime=300;
    public BossCompanion(EntityType<? extends PathfinderMob> type,Level level){super(type,level);}
    public static AttributeSupplier.Builder createAttributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,30)
            .add(Attributes.MOVEMENT_SPEED,0.28).add(Attributes.ATTACK_DAMAGE,6).add(Attributes.FOLLOW_RANGE,24);}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(KIND,0);}
    public Kind kind(){return Kind.values()[Math.max(0,Math.min(4,entityData.get(KIND)))];}
    public void attach(BossCast cast,Kind kind,int lifetime){this.cast=cast;this.lifetime=lifetime;entityData.set(KIND,kind.ordinal());
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(kind==Kind.LEAF?16:kind==Kind.VINE?24:30+cast.rank*10);setHealth(getMaxHealth());}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));}
    @Override public LivingEntity getSummoner(){return cast==null?null:cast.owner();}
    @Override public boolean isAlliedTo(Entity entity){return entity==getSummoner()||isAlliedHelper(entity)||super.isAlliedTo(entity);}
    @Override public void onUnSummon(){discard();}
    @Override public boolean shouldBeSaved(){return false;}
    @Override public boolean doHurtTarget(Entity target){return cast!=null&&target instanceof LivingEntity living&&cast.damage(this,living,1,20,100);}
    @Override protected void dropAllDeathLoot(net.minecraft.world.damagesource.DamageSource source){/* No source-mob loot from player summons. */}
    @Override public void tick(){
        super.tick();if(level().isClientSide)return;
        LivingEntity owner=getSummoner();if(owner==null||tickCount>lifetime||owner.distanceToSqr(this)>10000){discard();return;}
        if(kind()==Kind.LEAF){
            getNavigation().stop();setDeltaMovement(new Vec3(0,getDeltaMovement().y,0));
            if(tickCount%20==0&&owner.distanceToSqr(this)<400&&cast.visible(position(),owner)){
                // Only the actual owner or its owned summons may receive the leaf's healing.
                LivingEntity heal=owner;
                for(LivingEntity friend:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(6),
                        e->e instanceof IMagicSummon summon&&summon.getSummoner()==owner&&e.isAlive()))
                    if(friend.getHealth()/friend.getMaxHealth()<heal.getHealth()/heal.getMaxHealth())heal=friend;
                heal.heal(cast.power);cast.vanilla(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,heal.position().add(0,1,0),4,0.3);
                if(owner.hasEffect(MobEffects.DAMAGE_RESISTANCE))addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,40,2));
            }return;
        }
        if(kind()==Kind.VINE){
            getNavigation().stop();setDeltaMovement(new Vec3(0,getDeltaMovement().y,0));
            for(LivingEntity victim:cast.enemies(position().add(0,1,0),2.5))if(cast.visible(position(),victim)){
                cast.status(victim,"minecraft:slowness",10,4);
                if(cast.movable(victim))cast.impulse(victim,new Vec3(0,Math.min(0,victim.getDeltaMovement().y),0));
                if(tickCount%20==0)cast.damage(this,victim,1,20,10);
            }return;
        }
        LivingEntity target=cast.target();
        if(target==null&&getTarget()!=null&&cast.enemy(getTarget())&&distanceToSqr(getTarget())<576)target=getTarget();
        if(target==null&&tickCount%10==0){var candidates=cast.enemies(position(),20);target=candidates.isEmpty()?null:candidates.get(0);}
        if(target==null){setTarget(null);if(distanceToSqr(owner)>9)getNavigation().moveTo(owner,1);return;}
        setTarget(target);getLookControl().setLookAt(target,30,30);
        if(distanceToSqr(target)>3)getNavigation().moveTo(target,1.2);
        else{getNavigation().stop();if(tickCount%20==0&&cast.visible(position(),target)){
            swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            float strength=hasEffect(MobEffects.DAMAGE_BOOST)?1.5F:1;
            cast.damage(this,target,strength,20,100);}}
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar registrar){
        registrar.add(new AnimationController<>(this,"main",2,state->{
            String prefix=switch(kind()){case BUG->"scarlant";case ASSISTANT->"scarlantassistant";case SENTINEL->"scarlantsentinel";default->"vine";};
            String action=kind()==Kind.VINE?"tangleidle":swinging?"attack":state.isMoving()&&kind().ordinal()<3?"walk":"idle";
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation."+prefix+"."+action));}));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
