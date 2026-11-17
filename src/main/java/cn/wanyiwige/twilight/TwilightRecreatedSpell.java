package cn.wanyiwige.twilight;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.*;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.spells.ender.TeleportSpell;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

import java.util.*;

/** Server-authoritative recreation of the remaining source spells, using 1.20.1 APIs. */
public final class TwilightRecreatedSpell extends AbstractSpell {
    private final TwilightSpellDefinition definition;
    private final DefaultConfig config;
    public TwilightRecreatedSpell(TwilightSpellDefinition definition) {
        this.definition = definition;
        baseManaCost = definition.mana(); manaCostPerLevel = definition.manaStep();
        baseSpellPower = (int)definition.power(); spellPowerPerLevel = (int)definition.powerStep(); castTime = definition.ticks();
        config = new DefaultConfig().setSchoolResource(definition.school()).setMinRarity(definition.rarity())
                .setMaxLevel(definition.max()).setCooldownSeconds(definition.cooldown())
                .setAllowCrafting(definition.crafting()).build();
    }
    private String kind() { return definition.id(); }
    private boolean summon() { return kind().startsWith("summon_"); }
    @Override public ResourceLocation getSpellResource() { return TwilightFoundation.id(kind()); }
    @Override public DefaultConfig getDefaultConfig() { return config; }
    @Override public CastType getCastType() { return definition.cast(); }
    @Override public boolean allowLooting() {
        return !(kind().startsWith("exanimated_") || kind().equals("life_sacrifice")
                || kind().equals("summon_loyal_zombie") || kind().equals("summon_death_library"));
    }
    @Override public int getRecastCount(int rank, LivingEntity caster) {
        return summon() ? (kind().equals("summon_loyal_zombie") ? 5 : 2) : kind().equals("exanimated_trident") ? 2 + rank : 0;
    }
    @Override public ICastDataSerializable getEmptyCastData() {
        return summon() ? new SummonedEntitiesCastData() : kind().equals("minotaur_lunge") ? new ImpulseCastData() : super.getEmptyCastData();
    }
    @Override public void onRecastFinished(ServerPlayer player, RecastInstance instance, RecastResult result, ICastDataSerializable data) {
        if (!summon() || SummonManager.recastFinishedHelper(player, instance, result, data)) super.onRecastFinished(player, instance, result, data);
    }
    @Override public Optional<SoundEvent> getCastFinishSound() {
        return switch (kind()) {
            case "exanimated_step" -> Optional.of(io.redspace.ironsspellbooks.registries.SoundRegistry.LIGHTNING_WOOSH_01.get());
            case "exanimated_ray", "jet_eruption" -> Optional.of(io.redspace.ironsspellbooks.registries.SoundRegistry.FIRE_ERUPTION_SLAM.get());
            case "exanimated_trident" -> Optional.of(io.redspace.ironsspellbooks.registries.SoundRegistry.MAGIC_SPELL_REVERSE_3.get());
            case "hydra_mortar" -> Optional.of(twilightforest.init.TFSounds.HYDRA_SHOOT.get());
            case "avalanche" -> Optional.of(io.redspace.ironsspellbooks.registries.SoundRegistry.EARTHQUAKE_CAST.get());
            case "carminite_pull" -> Optional.of(io.redspace.ironsspellbooks.registries.SoundRegistry.ELECTROCUTE_LOOP.get());
            case "minotaur_lunge" -> Optional.of(twilightforest.init.TFSounds.MINOTAUR_ATTACK.get());
            case "life_sacrifice" -> Optional.empty();
            default -> super.getCastFinishSound();
        };
    }
    @Override public Optional<SoundEvent> getCastStartSound(){
        return switch(kind()){
            case "exanimated_fireball"->Optional.of(io.redspace.ironsspellbooks.registries.SoundRegistry.FIREBALL_START.get());
            case "exanimated_trident"->Optional.of(io.redspace.ironsspellbooks.registries.SoundRegistry.FIRE_ARROW_CHARGE.get());
            case "hydra_mortar"->Optional.of(twilightforest.init.TFSounds.HYDRA_GROWL.get());
            case "ice_bomb"->Optional.of(io.redspace.ironsspellbooks.registries.SoundRegistry.FROSTWAVE_PREPARE.get());
            case "jet_eruption"->Optional.of(io.redspace.ironsspellbooks.registries.SoundRegistry.HEAT_SURGE_PREPARE.get());
            case "avalanche"->Optional.of(io.redspace.ironsspellbooks.registries.SoundRegistry.EARTHQUAKE_LOOP.get());
            case "mosquito_swarm"->Optional.of(io.redspace.ironsspellbooks.registries.SoundRegistry.FIREFLY_SPELL_PREPARE.get());
            case "minotaur_lunge"->Optional.of(SoundEvents.COW_AMBIENT);
            case "exanimated_ray","exanimated_step","carminite_pull"->Optional.empty();
            default->super.getCastStartSound();
        };
    }
    @Override public io.redspace.ironsspellbooks.api.util.AnimationHolder getCastStartAnimation(){return switch(kind()){
        case "exanimated_step"->io.redspace.ironsspellbooks.api.util.AnimationHolder.none();
        case "ice_bomb","exanimated_trident"->SpellAnimations.ANIMATION_CHARGED_CAST;
        case "avalanche","jet_eruption"->SpellAnimations.CHARGE_RAISED_HAND;
        case "carminite_pull"->SpellAnimations.ANIMATION_CONTINUOUS_CAST_ONE_HANDED;
        default->super.getCastStartAnimation();
    };}
    @Override public io.redspace.ironsspellbooks.api.util.AnimationHolder getCastFinishAnimation(){return kind().equals("avalanche")||kind().equals("jet_eruption")?SpellAnimations.TOUCH_GROUND_ANIMATION:super.getCastFinishAnimation();}
    @Override public boolean stopSoundOnCancel(){return kind().equals("avalanche")||kind().equals("jet_eruption")||super.stopSoundOnCancel();}
    @Override public boolean canBeInterrupted(net.minecraft.world.entity.player.Player player){return !kind().equals("minotaur_lunge")&&super.canBeInterrupted(player);}
    @Override public int getEffectiveCastTime(int rank,LivingEntity caster){return kind().equals("minotaur_lunge")?getCastTime(rank):super.getEffectiveCastTime(rank,caster);}
    @Override public SpellDamageSource getDamageSource(Entity projectile, Entity attacker) {
        SpellDamageSource source = super.getDamageSource(projectile, attacker);
        return switch (kind()) {
            case "exanimated_fireball", "exanimated_step" -> source.setLifestealPercent(.1F);
            case "exanimated_ray", "mosquito_swarm" -> source.setLifestealPercent(.25F);
            case "life_sacrifice" -> source.setLifestealPercent(.5F).indirect();
            case "exanimated_trident" -> source.setFireTicks(60).setLifestealPercent(.05F).indirect();
            case "jet_eruption" -> source.setFireTicks(80);
            case "avalanche", "ice_bomb" -> source.setFreezeTicks(80);
            default -> source;
        };
    }
    public float damage(int rank, LivingEntity caster) {
        float power = getSpellPower(rank, caster);
        if (kind().startsWith("exanimated_") && caster != null) {
            double f = caster.getAttributeValue(AttributeRegistry.FIRE_SPELL_POWER.get());
            double b = caster.getAttributeValue(AttributeRegistry.BLOOD_SPELL_POWER.get());
            double e = caster.getAttributeValue(AttributeRegistry.ENDER_SPELL_POWER.get());
            boolean balanced = f == b && b == e;
            return (float) switch (kind()) {
                case "exanimated_fireball" -> 5 + 5 * (f + b + e) * (balanced ? 2 : 1);
                case "exanimated_ray" -> 3 * (f + b + e) * (balanced ? 1.5 : 1);
                default -> 5 * (f + b + e) * (balanced ? .75 : .5);
            };
        }
        return switch (kind()) {
            case "exanimated_fireball" -> power * 7;
            case "exanimated_ray", "exanimated_step", "exanimated_trident" -> power * 2;
            case "hydra_mortar" -> {
                if (caster == null) yield power * 7;
                double f = caster.getAttributeValue(AttributeRegistry.FIRE_SPELL_POWER.get());
                double n = caster.getAttributeValue(AttributeRegistry.NATURE_SPELL_POWER.get());
                yield (float) (5 + 5 * (f + n) * (f == n ? 1.5 : 1));
            }
            case "mosquito_swarm" -> power / 3;
            case "avalanche", "jet_eruption" -> power * .5F;
            case "ice_bomb" -> baseSpellPower * getEntityPowerMultiplier(caster);
            case "carminite_pull" -> 1 + power * .25F;
            case "minotaur_lunge" -> power * (1 + (caster == null ? 0 : (float)caster.getAttributeValue(Attributes.MOVEMENT_SPEED)));
            default -> power;
        };
    }
    public float radius(int rank, LivingEntity caster) {
        return switch (kind()) {
            case "exanimated_fireball" -> 2 + (int)getSpellPower(rank, caster);
            case "exanimated_trident" -> 5;
            case "exanimated_step" -> 2.5F + rank * .5F;
            case "jet_eruption", "avalanche" -> 6 + rank * .5F;
            case "hydra_mortar" -> 1 + rank * .25F;
            case "ice_bomb", "mosquito_swarm" -> 2;
            default -> 0;
        };
    }
    @Override public List<MutableComponent> getUniqueInfo(int rank, LivingEntity caster) {
        if (summon()) return List.of(Component.translatable("ui.irons_spellbooks.summon_count", rank),Component.translatable("ui.irons_spellbooks.duration",kind().equals("summon_loyal_zombie")?5:600));
        List<MutableComponent> info = new ArrayList<>();
        info.add(Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(damage(rank, caster), 2)));
        if (radius(rank,caster) > 0) info.add(Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(radius(rank,caster), 1)));
        if(kind().equals("exanimated_ray")||kind().equals("fire_bettle_spit"))info.add(Component.translatable("ui.irons_spellbooks.range",Utils.stringTruncation(4+getSpellPower(rank,caster)*1.5F,1)));
        if(kind().equals("life_sacrifice")){info.add(Component.translatable("ui.irons_spellbooks.range",25));info.add(Component.translatable("ui.wans_twilight_spellbooks.sacrifice_healing",Utils.stringTruncation(getSpellPower(rank,caster),1)));}
        if(kind().equals("mosquito_swarm"))info.add(Component.translatable("ui.irons_spellbooks.duration",10));
        return info;
    }
    @Override public boolean checkPreCastConditions(Level world, int rank, LivingEntity caster, MagicData data) {
        if (kind().equals("life_sacrifice")) return Utils.preCastTargetHelper(world,caster,data,this,25,.25F);
        return super.checkPreCastConditions(world,rank,caster,data);
    }
    @Override public void onClientCast(Level world, int rank, LivingEntity caster, ICastData data) {
        if (kind().equals("minotaur_lunge") && data instanceof ImpulseCastData impulse) {
            caster.setDeltaMovement(caster.getDeltaMovement().add(impulse.x,impulse.y,impulse.z));
            caster.hasImpulse = true;
        }
        super.onClientCast(world,rank,caster,data);
    }
    @Override public void onCast(Level world, int rank, LivingEntity caster, CastSource source, MagicData data) {
        if (!(world instanceof ServerLevel server)) return;
        float power = getSpellPower(rank,caster), damage = damage(rank,caster), radius = radius(rank,caster);
        Vec3 eye = caster.getEyePosition(), look = caster.getLookAngle();
        switch (kind()) {
            case "fire_bettle_spit", "exanimated_ray" -> {
                HitResult hit = Utils.raycastForEntity(world,caster,4 + power * 1.5F,true,.15F);
                if (hit instanceof EntityHitResult entityHit) {
                    SpellDamageSource ds = getDamageSource(caster).indirect();
                    if (kind().equals("fire_bettle_spit")) ds.setFireTicks((int)(power * 15));
                    DamageSources.applyDamage(entityHit.getEntity(),damage,ds);
                    if (kind().equals("exanimated_ray") && entityHit.getEntity() instanceof LivingEntity target)
                        target.addEffect(new MobEffectInstance(TwilightFoundation.OMINOUS_BURN.get(),(int)(power*20)));
                }
                if (kind().equals("exanimated_ray")) TwilightAreaEntity.beam(caster,eye,hit.getLocation());
                else TwilightVisuals.line(server,eye,hit.getLocation(),ParticleTypes.FLAME,.5);
                TwilightVisuals.burst(server,hit.getLocation(),kind().equals("exanimated_ray") ? TwilightParticles.EXANIMATED_FIRE.get() : ParticleTypes.SMOKE,50,.3);
            }
            case "exanimated_fireball", "exanimated_trident", "ice_bomb", "hydra_mortar" -> {
                if (kind().equals("exanimated_trident") && !data.getPlayerRecasts().hasRecastForSpell(getSpellId()))
                    data.getPlayerRecasts().addRecast(new RecastInstance(getSpellId(),rank,2+rank,100,source,null),data);
                int[] angles = kind().equals("hydra_mortar") ? new int[]{0,-15,15} : new int[]{0};
                for (int angle : angles) {
                    String id = switch(kind()) { case "ice_bomb" -> "extended_ice_bomb"; case "hydra_mortar" -> "hydra_fireball"; default -> kind(); };
                    TwilightProjectile projectile = TwilightEntities.PROJECTILES.get(id).get().create(world);
                    projectile.setOwner(caster); projectile.setDamage(damage); projectile.setExplosionRadius(radius);
                    projectile.aoeDamage = kind().equals("hydra_mortar") ? power * .185F : 1 + power * .1F;
                    projectile.setPos(eye.add(look.scale(kind().equals("hydra_mortar") ? 0 : 1)));
                    projectile.shoot(look.yRot((float)Math.toRadians(angle)));
                    if(kind().equals("hydra_mortar"))projectile.setDeltaMovement(look.yRot((float)Math.toRadians(angle)).normalize().scale(1.25));
                    if(kind().equals("ice_bomb")) projectile.setDeltaMovement(projectile.getDeltaMovement().add(0,.2,0));
                    world.addFreshEntity(projectile);
                }
            }
            case "avalanche", "jet_eruption" -> {
                for (LivingEntity target : world.getEntitiesOfClass(LivingEntity.class,caster.getBoundingBox().inflate(radius,4,radius),t -> hostile(caster,t))) {
                    if (caster.distanceToSqr(target) > radius*radius || !caster.hasLineOfSight(target)) continue;
                    if(kind().equals("avalanche")) {
                        TwilightProjectile chunk = TwilightEntities.PROJECTILES.get("ice_chunk").get().create(world);
                        chunk.setOwner(caster); chunk.setDamage(damage);chunk.setExplosionRadius(2.5F);
                        chunk.setPos(target.position().add(0,7,0)); chunk.shoot(new Vec3(.05,-.85,0).normalize());world.addFreshEntity(chunk);
                    } else TwilightAreaEntity.spawn(caster,"jet_eruption",target.position(),damage,1,96);
                }
                TwilightVisuals.ring(server,caster.position().add(0,.1,0),radius,
                        kind().equals("avalanche") ? ParticleTypes.SNOWFLAKE : ParticleTypes.FLAME);
            }
            case "carminite_pull" -> {
                TwilightAreaEntity cone = TwilightAreaEntity.findOrCreate(caster,"carminite_pull",20);
                cone.pulse(damage,power * .1F);
            }
            case "mosquito_swarm" -> {
                HitResult hit = Utils.raycastForEntity(world,caster,32,true,.35F);
                TwilightAreaEntity swarm = TwilightAreaEntity.spawn(caster,"mosquito_swarm",hit.getLocation().add(0,.5,0),damage,2,200);
                if(hit instanceof EntityHitResult target) swarm.setTarget(target.getEntity());
            }
            case "exanimated_step" -> {
                Vec3 destination = TeleportSpell.findTeleportLocation(world,caster,(float)(Utils.softCapFormula(getEntityPowerMultiplier(caster))*getSpellPower(rank,null)));
                for (LivingEntity target : world.getEntitiesOfClass(LivingEntity.class,new AABB(destination,destination).inflate(radius,radius*2,radius),t->hostile(caster,t))) {
                    if(Utils.hasLineOfSight(world,destination.add(0,1,0),target.getBoundingBox().getCenter(),true)
                            && DamageSources.applyDamage(target,damage,getDamageSource(caster))) {
                        target.setSecondsOnFire(5);target.addEffect(new MobEffectInstance(TwilightFoundation.OMINOUS_BURN.get(),100));
                    }
                }
                TwilightVisuals.burst(server,caster.position(),TwilightParticles.EXANIMATED_SMOKE.get(),50,.5);
                TwilightVisuals.burst(server,destination,TwilightParticles.EXANIMATED_FIRE.get(),80,.7);
                if(caster.isPassenger())caster.stopRiding();
                Utils.handleSpellTeleport(this,caster,destination);caster.resetFallDistance();
            }
            case "life_sacrifice" -> {
                LivingEntity target = data.getAdditionalCastData() instanceof TargetEntityCastData cast ? cast.getTarget(server) : null;
                if(target == null || !target.isAlive())break;
                if(target instanceof IMagicSummon summon && summon.getSummoner() == caster) {
                    caster.heal(power + target.getHealth()*.5F);summon.onUnSummon();
                } else DamageSources.applyDamage(target,damage + target.getHealth()*.05F,getDamageSource(caster));
                TwilightVisuals.burst(server,target.getBoundingBox().getCenter(),ParticleTypes.CRIMSON_SPORE,100,.4);
                TwilightVisuals.line(server,target.getBoundingBox().getCenter(),caster.getBoundingBox().getCenter(),ParticleTypes.CRIMSON_SPORE,.2);
                server.playSound(null,target.blockPosition(),io.redspace.ironsspellbooks.registries.SoundRegistry.BLOOD_EXPLOSION.get(),net.minecraft.sounds.SoundSource.PLAYERS,3,1);
            }
            case "minotaur_lunge" -> {
                HitResult hit=Utils.raycastForEntity(world,caster,8,true,2);
                if(hit instanceof EntityHitResult entityHit) for(LivingEntity target:world.getEntitiesOfClass(LivingEntity.class,entityHit.getEntity().getBoundingBox().inflate(1.25),t->hostile(caster,t))) {
                    if(caster.hasLineOfSight(target) && DamageSources.applyDamage(target,damage,getDamageSource(caster))) {
                        target.setDeltaMovement(target.getDeltaMovement().add(target.position().subtract(caster.position()).normalize().add(0,.5,0).normalize()));target.hurtMarked=true;
                    }
                }
                Vec3 impulse=look.multiply(3,1,3).normalize().add(0,.25,0).scale((15+power)/12F);
                data.setAdditionalCastData(new ImpulseCastData((float)impulse.x,(float)impulse.y,(float)impulse.z,true));
                caster.setDeltaMovement(caster.getDeltaMovement().scale(.25).add(impulse.scale(.75)));caster.hurtMarked=true;caster.invulnerableTime=20;
            }
            default -> { if(summon())TwilightSummons.cast(this,kind(),server,rank,caster,source,data); else throw new IllegalStateException(kind()); }
        }
        super.onCast(world,rank,caster,source,data);
    }
    public static boolean hostile(Entity caster, Entity target) {
        return target != caster && target.isAlive() && !target.isSpectator() && (caster==null||!DamageSources.isFriendlyFireBetween(caster,target));
    }
}
