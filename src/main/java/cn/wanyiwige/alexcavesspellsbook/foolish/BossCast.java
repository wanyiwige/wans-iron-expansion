package cn.wanyiwige.alexcavesspellsbook.foolish;

import cn.wanyiwige.alexcavesspellsbook.candy.CandyCombat;
import cn.wanyiwige.alexcavesspellsbook.util.SaciliteSpellSupport;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import net.minecraft.core.particles.*;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** One immutable cast context shared by all descendants; limits are per victim per cast. */
public final class BossCast {
    public final ServerLevel level;
    public final UUID ownerId;
    public UUID targetId;
    public final BossSpellSpec spec;
    public final int rank;
    public final float power;
    public final Vec3 aim, look;
    private int spawned;
    private final Map<UUID,Long> nextHit=new HashMap<>();
    private final Map<UUID,Integer> hits=new HashMap<>();
    private BossCast(ServerLevel level,LivingEntity owner,BossSpellSpec spec,int rank,float power){
        this.level=level;ownerId=owner.getUUID();this.spec=spec;this.rank=rank;this.power=power;
        LivingEntity target=SaciliteSpellSupport.selectTarget(level,owner,32);
        targetId=target==null?null:target.getUUID();look=owner.getLookAngle();
        aim=target==null?SaciliteSpellSupport.aimPoint(level,owner,32):target.position();
    }
    public static void start(ServerLevel level,LivingEntity owner,BossSpellSpec spec,int rank,float power){
        if(!Float.isFinite(power)||power<0)return;
        // Retire the previous cast of this spell, including its summons, before recasting.
        for(BossEffect e:level.getEntitiesOfClass(BossEffect.class,owner.getBoundingBox().inflate(128),
                e->e.cast!=null&&e.cast.ownerId.equals(owner.getUUID())&&e.cast.spec==spec))e.discard();
        for(BossCompanion e:level.getEntitiesOfClass(BossCompanion.class,owner.getBoundingBox().inflate(128),
                e->e.cast!=null&&e.cast.ownerId.equals(owner.getUUID())&&e.cast.spec==spec))e.discard();
        BossCast cast=new BossCast(level,owner,spec,rank,power);
        cast.effect(BossEffect.Kind.DIRECTOR,owner.position(),Vec3.ZERO,0,260);
    }
    public LivingEntity owner(){Entity e=level.getEntity(ownerId);return e instanceof LivingEntity l&&l.isAlive()?l:null;}
    public LivingEntity target(){Entity e=targetId==null?null:level.getEntity(targetId);return e instanceof LivingEntity l&&owner()!=null&&enemy(l)?l:null;}
    public boolean enemy(LivingEntity target){LivingEntity owner=owner();return owner!=null&&CandyCombat.enemy(owner,target);}
    public List<LivingEntity> enemies(Vec3 center,double radius){
        return level.getEntitiesOfClass(LivingEntity.class,new AABB(center,center).inflate(radius),
                e->enemy(e)&&e.getBoundingBox().getCenter().distanceToSqr(center)<=radius*radius)
                .stream().sorted(Comparator.comparingDouble(e->e.distanceToSqr(center))).limit(32).toList();
    }
    public boolean visible(Vec3 from,LivingEntity target){return CandyCombat.visible(level,from.add(0,0.1,0),target);}
    public boolean damage(Entity source,LivingEntity victim,float factor,int interval,int maxHits){
        if(!enemy(victim)||!visible(source.position(),victim))return false;
        long now=level.getGameTime();UUID id=victim.getUUID();
        if(now<nextHit.getOrDefault(id,0L)||hits.getOrDefault(id,0)>=maxHits)return false;
        boolean hit=DamageSources.applyDamage(victim,power*factor,
                FoolishSpells.ENTRIES.get(spec).get().getDamageSource(source,owner()).setIFrames(0));
        if(hit){nextHit.put(id,now+interval);hits.merge(id,1,Integer::sum);}return hit;
    }
    public void blast(Entity source,double radius,float factor){
        burst(source.position(),radius);
        for(LivingEntity victim:enemies(source.position(),radius))damage(source,victim,factor,0,12);
    }
    public void burst(Vec3 center,double radius){
        level.sendParticles(radius>2?ParticleTypes.EXPLOSION_EMITTER:ParticleTypes.EXPLOSION,
                center.x,center.y,center.z,1,0,0,0,0);
        level.playSound(null,center.x,center.y,center.z,SoundEvents.GENERIC_EXPLODE,SoundSource.PLAYERS,0.7F,1);
    }
    public void lightning(Vec3 center){
        LightningBolt bolt=EntityType.LIGHTNING_BOLT.create(level);
        if(bolt!=null){bolt.setPos(center);bolt.setVisualOnly(true);level.addFreshEntity(bolt);}
    }
    public void particle(String name,Vec3 center,int count,double radius){
        var type=ForgeRegistries.PARTICLE_TYPES.getValue(ResourceLocation.fromNamespaceAndPath("foolish",name));
        if(type instanceof SimpleParticleType simple)level.sendParticles(simple,center.x,center.y,center.z,count,radius,radius,radius,0.02);
    }
    public void vanilla(ParticleOptions particle,Vec3 pos,int count,double radius){
        level.sendParticles(particle,pos.x,pos.y,pos.z,count,radius,radius,radius,0.025);
    }
    public void sound(String id,Vec3 pos){
        var sound=ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.parse(id));
        if(sound!=null)level.playSound(null,pos.x,pos.y,pos.z,sound,SoundSource.PLAYERS,0.7F,1);
    }
    public void status(LivingEntity victim,String id,int duration,int amplifier){
        if(!enemy(victim))return;
        var effect=ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.parse(id));
        if(effect!=null)victim.addEffect(new MobEffectInstance(effect,duration,amplifier),owner());
    }
    public void impulse(LivingEntity entity,Vec3 velocity){
        entity.setDeltaMovement(velocity);entity.hasImpulse=true;entity.hurtMarked=true;
        if(entity instanceof ServerPlayer player)player.connection.send(new ClientboundSetEntityMotionPacket(player));
    }
    public boolean movable(LivingEntity target){return !target.getType().is(net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.ENTITY_TYPE,ResourceLocation.fromNamespaceAndPath("alex_caves_spellsbook","boss_control_immune")));}
    public BossEffect effect(BossEffect.Kind kind,Vec3 pos,Vec3 motion,int delay,int lifetime){
        if(spawned++>=48||!level.hasChunkAt(net.minecraft.core.BlockPos.containing(pos)))return null;
        if(kind==BossEffect.Kind.FIREBALL||kind==BossEffect.Kind.RADIUM||kind==BossEffect.Kind.GAS_BOLT||kind==BossEffect.Kind.POLLEN
                ||kind==BossEffect.Kind.CAPSULE||kind==BossEffect.Kind.SHOCK||kind==BossEffect.Kind.THUNDER){
            var wall=level.clip(new ClipContext(owner().getEyePosition(),pos,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,owner()));
            if(wall.getType()!=HitResult.Type.MISS)pos=wall.getLocation().subtract(motion.normalize().scale(0.05));
        }
        BossEffect e=FoolishSpells.EFFECT.get().create(level);if(e==null)return null;
        e.attach(this,kind,delay,lifetime);e.setOwner(owner());e.setPos(pos);e.setDeltaMovement(motion);e.face(motion);
        level.addFreshEntity(e);return e;
    }
    public void companion(BossCompanion.Kind kind,Vec3 pos,int lifetime){
        if(spawned++>=48||!level.hasChunkAt(net.minecraft.core.BlockPos.containing(pos)))return;
        BossCompanion e=FoolishSpells.COMPANION.get().create(level);if(e==null)return;
        e.attach(this,kind,lifetime);e.setPos(pos);level.addFreshEntity(e);
    }
    public Vec3 ground(Vec3 center){
        Vec3 from=center.add(0,2,0),to=center.add(0,-8,0);
        var hit=level.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,owner()));
        return hit.getType()==HitResult.Type.MISS?center:hit.getLocation().add(0,0.03,0);
    }
    public void director(BossEffect host,int age){
        LivingEntity owner=owner();if(owner==null){host.discard();return;}
        host.setPos(owner.position().add(0,0.6,0));
        LivingEntity target=target();Vec3 muzzle=owner.getEyePosition().add(owner.getLookAngle().scale(0.7));
        if(age==1)sound(switch(spec){
            case INCINERATOR_STREAM->"minecraft:entity.firework_rocket.blast";case WHITE_FIRE_RAIN->"foolish:pyreambient";
            case TIDAL_SPIN->"minecraft:entity.player.attack.sweep";case TIDAL_GRIP->"foolish:thrasherbite";
            case RADIUM_BOLT,CORROSIVE_CLOUD->"minecraft:entity.wither.shoot";
            case STORM_MINEFIELD,PARALYZING_FEATHERS->"foolish:thunderbirdambient";
            case THUNDER_WINGBEAT->"minecraft:entity.ender_dragon.flap";case SCARLANT_CAPSULE->"minecraft:entity.shulker.shoot";
            case SCARLET_EDICT->"foolish:scarlantqueenambient";case NOURISHING_LEAVES,TANGLE_ROOTS->"foolish:terrorfloraambient";
            case BLINDING_POLLEN->"foolish:terraflorahit";case EXPLOSIVE_SPORES->"minecraft:block.beehive.exit";
            case ASTRAL_CHAIN_SLASH->"foolish:astralisdraw";case METEOR_JUDGMENT->"foolish:astralisroar";
            case SEEKING_STAR_BLADE->"foolish:starcast";case FALLING_STAR_BOMB->"foolish:swordcollapse";},owner.position());
        switch(spec){
            case INCINERATOR_STREAM -> {
                if(age<=20)effect(BossEffect.Kind.FIREBALL,muzzle,owner.getLookAngle().scale(1.35),0,60);
                if(age>=22)host.discard();
            }
            case WHITE_FIRE_RAIN -> {
                if(age==1)for(int i=0;i<3+rank*2;i++){
                    Vec3 start=aim.add((level.random.nextDouble()-0.5)*8,6+level.random.nextDouble()*2,(level.random.nextDouble()-0.5)*8);
                    effect(BossEffect.Kind.SMALL_FIREBALL,start,new Vec3(0,-0.8,0),i*2,80);
                }if(age>=2)host.discard();
            }
            case RADIUM_BOLT,CORROSIVE_CLOUD,BLINDING_POLLEN,SCARLANT_CAPSULE -> {
                if(age==1)effect(switch(spec){case RADIUM_BOLT->BossEffect.Kind.RADIUM;case CORROSIVE_CLOUD->BossEffect.Kind.GAS_BOLT;
                    case BLINDING_POLLEN->BossEffect.Kind.POLLEN;default->BossEffect.Kind.CAPSULE;},muzzle,owner.getLookAngle().scale(1.2),0,80);
                host.discard();
            }
            case STORM_MINEFIELD -> {
                if(age==1)for(int i=0;i<5+rank;i++){
                    double angle=i*Math.PI*2/(5+rank);
                    effect(BossEffect.Kind.MINE,owner.position().add(Math.cos(angle)*5,1+(i%3)*0.8,Math.sin(angle)*5),Vec3.ZERO,i,160);
                }host.discard();
            }
            case PARALYZING_FEATHERS -> {
                if(age==1)for(int i=0;i<2+rank;i++){
                    Vec3 dir=owner.getLookAngle().yRot((float)((i-(1+rank)/2.0)*0.12));
                    effect(BossEffect.Kind.SHOCK,muzzle,dir.scale(1.2),i*2,80);
                    effect(BossEffect.Kind.THUNDER,muzzle.add(0,0.25,0),dir.scale(1.1),i*2+1,80);
                }host.discard();
            }
            case EXPLOSIVE_SPORES -> {
                if(age==1){
                    owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,140,2));
                    for(int i=0;i<4+rank*2;i++)effect(BossEffect.Kind.SEED,owner.position().add((level.random.nextDouble()-0.5)*6,6,
                            (level.random.nextDouble()-0.5)*6),new Vec3((level.random.nextDouble()-0.5)*0.25,-0.1,(level.random.nextDouble()-0.5)*0.25),i,180);
                }host.discard();
            }
            case NOURISHING_LEAVES -> {
                if(age==1)for(int i=0;i<rank;i++)companion(BossCompanion.Kind.LEAF,ground(owner.position().add(Math.cos(i*2.1)*2,0,Math.sin(i*2.1)*2)),240+rank*80);
                host.discard();
            }
            case TANGLE_ROOTS -> {if(age==1)companion(BossCompanion.Kind.VINE,ground(aim),60+rank*20);host.discard();}
            case SCARLET_EDICT -> {
                if(age==1){
                    for(LivingEntity friend:level.getEntitiesOfClass(LivingEntity.class,owner.getBoundingBox().inflate(8)))
                        if(friend!=owner&&!enemy(friend)&&(friend.isAlliedTo(owner)||friend instanceof IMagicSummon summon&&summon.getSummoner()==owner))
                            friend.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,200+rank*100,rank-1));
                    for(LivingEntity victim:enemies(host.position(),5))if(visible(host.position(),victim)){
                        damage(host,victim,1,0,1);if(movable(victim))impulse(victim,victim.position().subtract(owner.position()).normalize().scale(1.2).add(0,0.2,0));
                    }
                    particle("scarlet_firefly",host.position(),24,3);
                }host.discard();
            }
            case TIDAL_SPIN -> {
                if(age<=20){owner.setYRot(owner.getYRot()+36);vanilla(ParticleTypes.SPLASH,host.position(),16,3);
                    for(LivingEntity victim:enemies(host.position(),4.5))if(damage(host,victim,1,10,2)&&movable(victim))
                        impulse(victim,victim.position().subtract(owner.position()).normalize().scale(0.65).add(0,0.15,0));}
                else host.discard();
            }
            case TIDAL_GRIP -> {
                if(age<=40){
                    Vec3 forward=owner.getLookAngle();impulse(owner,new Vec3(forward.x,0,forward.z).normalize().scale(0.6).add(0,owner.getDeltaMovement().y,0));
                    vanilla(ParticleTypes.SPLASH,host.position(),8,0.8);
                    if(target!=null&&owner.distanceToSqr(target)<144&&visible(owner.getEyePosition(),target)){
                        Vec3 hand=owner.position().add(forward.scale(1.4)).add(0,0.4,0);
                        if(movable(target)){Vec3 pull=hand.subtract(target.position());impulse(target,pull.length()>0.45?pull.normalize().scale(0.45):pull);}
                        damage(host,target,1,10,4);
                    }
                }else {if(target!=null&&movable(target)&&owner.distanceToSqr(target)<16)impulse(target,owner.getLookAngle().scale(1.2).add(0,0.8,0));host.discard();}
            }
            case THUNDER_WINGBEAT -> {
                if(age==1){
                    impulse(owner,owner.getLookAngle().scale(0.8));vanilla(ParticleTypes.ELECTRIC_SPARK,host.position(),40,3);
                    for(LivingEntity victim:enemies(owner.getEyePosition(),7))if(victim.position().subtract(owner.position()).normalize().dot(owner.getLookAngle())>0.25){
                        if(damage(host,victim,1,0,1)&&movable(victim))impulse(victim,owner.getLookAngle().scale(1.5).add(0,0.3,0));
                    }
                }host.discard();
            }
            case ASTRAL_CHAIN_SLASH -> {
                if(age==1||age==9||age==17||age==25||age==33){
                    boolean finalHit=age==33;impulse(owner,owner.getLookAngle().scale(finalHit?0.2:0.9));
                    sound(finalHit?"foolish:astralisheavyslash":"foolish:astralislightslash",owner.position());
                    particle("starsplitter_pierce",muzzle,12,1.5);
                    effect(BossEffect.Kind.SLASH,muzzle,owner.getLookAngle(),0,6);
                    for(LivingEntity victim:enemies(owner.getEyePosition(),finalHit?5:3.5))
                        if(victim.position().subtract(owner.position()).normalize().dot(owner.getLookAngle())>0.2)
                            damage(host,victim,finalHit?2.5F:1,6,5);
                }if(age>36)host.discard();
            }
            case METEOR_JUDGMENT -> {
                if(age<=40&&age%4==1){
                    Vec3 center=target==null?aim:target.position();
                    effect(BossEffect.Kind.METEOR,center.add((level.random.nextDouble()-0.5)*10,30+level.random.nextDouble()*20,
                            (level.random.nextDouble()-0.5)*10),new Vec3(0,-2,0),0,80);
                }if(age>40)host.discard();
            }
            case SEEKING_STAR_BLADE -> {if(age==1)effect(BossEffect.Kind.BLADE,owner.position().add(0,6,0),look.scale(0.5),10,140);host.discard();}
            case FALLING_STAR_BOMB -> {if(age==1)effect(BossEffect.Kind.STARBOMB,owner.position().add(0,7,0),look.scale(0.2),20,160);host.discard();}
        }
    }
}
