package cn.wanyiwige.alexcavesspellsbook.hazen;

import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.Arrays;

/** Native summon ownership and a timed five-move sword AI; no transplanted library. */
public final class HazenSummonedTerraprisma extends PathfinderMob implements IMagicSummon, GeoEntity {
    private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(HazenSummonedTerraprisma.class, EntityDataSerializers.INT);
    private static final String[] ANIMATIONS = {"animation.terraprisma.swing_a", "animation.terraprisma.dash", "animation.terraprisma.swing_b",
            "animation.terraprisma.swing_barrage", "animation.terraprisma.down_slash"};
    private static final int[] LENGTHS = {40, 40, 40, 90, 30};
    private static final int[][] HIT_FRAMES = {{15}, {20,22,24,26,28,30,32,34,36,38,40}, {24,28},
            {22,28,35,42,48,58,63,66,69,75,78,81,85}, {21}};
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int attackAge, recovery = 10;
    private boolean dashHit;
    public HazenSummonedTerraprisma(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
    }
    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.ATTACK_DAMAGE,2).add(Attributes.MAX_HEALTH,25)
                .add(Attributes.ATTACK_KNOCKBACK,1).add(Attributes.FOLLOW_RANGE,40).add(Attributes.FLYING_SPEED,2.5)
                .add(Attributes.MOVEMENT_SPEED,.5);
    }
    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(ACTION,-1); }
    @Override protected PathNavigation createNavigation(Level level) {
        var navigation = new FlyingPathNavigation(this,level); navigation.setCanFloat(true); navigation.setCanPassDoors(true); return navigation;
    }
    @Override public Entity getSummoner() { return SummonManager.getOwner(this); }
    @Override public boolean isAlliedTo(Entity other) { return other == getSummoner() || isAlliedHelper(other) || super.isAlliedTo(other); }
    @Override public boolean hurt(DamageSource source, float amount) { return !shouldIgnoreDamage(source) && super.hurt(source,amount); }
    @Override public void onUnSummon() { discard(); }
    @Override public void die(DamageSource source) { onDeathHelper(); super.die(source); }
    @Override public void remove(RemovalReason reason) {
        boolean firstRemoval=!isRemoved();
        super.remove(reason);
        if(firstRemoval&&!level().isClientSide)onRemovedHelper(this);
    }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        if (!HazenConfig.spellEnabled("call_forth_terraprisma")) { onUnSummon(); return; }
        Entity owner = getSummoner();
        if (!(owner instanceof LivingEntity caster) || !owner.isAlive()) { onUnSummon(); return; }
        if (getTarget() == null || !getTarget().isAlive() || isAlliedTo(getTarget())) {
            LivingEntity candidate = caster.getLastHurtMob();
            if (candidate == null || !candidate.isAlive()) candidate = caster.getLastHurtByMob();
            if (candidate == null && caster instanceof Mob mob) candidate = mob.getTarget();
            setTarget(candidate != null && candidate.isAlive() && !isAlliedTo(candidate) ? candidate : null);
        }
        var target = getTarget();
        if (target == null) {
            entityData.set(ACTION,-1);
            if (distanceToSqr(owner)>49) moveControl.setWantedPosition(owner.getX(),owner.getY()+1,owner.getZ(),1);
            if (distanceToSqr(owner)>400 && level().noCollision(this, getBoundingBox().move(owner.position().subtract(position())))) setPos(owner.position().add(0,1,0));
        } else {
            double distance = distanceTo(target);
            lookControl.setLookAt(target);
            if (distance>4) moveControl.setWantedPosition(target.getX(),target.getY()+1,target.getZ(),1.3);
            int action = entityData.get(ACTION);
            if (action<0) {
                if (--recovery<=0 && distance<12 && hasLineOfSight(target)) {
                    int next = distance>6 && random.nextFloat()<.25f ? 1 : random.nextInt(ANIMATIONS.length);
                    entityData.set(ACTION,next); attackAge=0; dashHit=false;
                }
            } else {
                attackAge++;
                if (Arrays.binarySearch(HIT_FRAMES[action],attackAge)>=0 && hasLineOfSight(target)) {
                    if (action==1) {
                        if (attackAge==20) setDeltaMovement(target.position().subtract(position()).normalize().multiply(2.4,.5,2.4).add(0,.15,0));
                        if (!dashHit && distance<1.8) { dashHit=attack(target); }
                    } else {
                        setDeltaMovement(getDeltaMovement().add(target.position().subtract(position()).normalize().scale(.55)));
                        if (distance<4) attack(target);
                    }
                }
                if (attackAge>=LENGTHS[action]) { entityData.set(ACTION,-1); recovery=10+random.nextInt(21); }
            }
        }
        if (tickCount%8==0) {
            double y=(target==null?owner.getY():target.getY())+1-getY();
            setDeltaMovement(getDeltaMovement().add(0,net.minecraft.util.Mth.clamp(y*.05,-.15,.15),0));
        }
        if (tickCount%80==0) heal(1);
    }
    private boolean attack(LivingEntity victim) {
        if (isAlliedTo(victim)) return false;
        victim.invulnerableTime=0;
        return DamageSources.applyDamage(victim,(float)getAttributeValue(Attributes.ATTACK_DAMAGE),
                HazenSpells.get("call_forth_terraprisma").getDamageSource(this,getSummoner()));
    }
    @Override public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this,"sword",0,event -> {
            int action=entityData.get(ACTION);
            if (action<0) event.getController().setAnimation(RawAnimation.begin().thenLoop("animation.terraprisma.idle"));
            else event.getController().setAnimation(RawAnimation.begin().thenPlay(ANIMATIONS[action]));
            return PlayState.CONTINUE;
        }));
    }
}
