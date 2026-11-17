package cn.wanyiwige.alexcavesspellsbook.toxic;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import java.util.*;

/** Server-owned collision and timelines; every visible phase is synchronized explicitly. */
public final class RadiationNewEffectEntity extends ToxicEffectEntity {
    private UUID targetId;
    private Vec3 velocity=Vec3.ZERO,direction=Vec3.ZERO;
    private final List<Vec3> points=new ArrayList<>();
    private final Map<UUID,List<Vec3>> trails=new LinkedHashMap<>();
    private final Set<UUID> areaTargets=new LinkedHashSet<>(),cloudPathHits=new HashSet<>(),cloudBurstHits=new HashSet<>();
    private static final class FilmRecord {
        final double height;final List<Vec3> shots=new ArrayList<>();
        Vec3 from=Vec3.ZERO,end=Vec3.ZERO;int flash=-100,fired;
        FilmRecord(double height){this.height=height;}
    }
    private final Map<UUID,FilmRecord> filmRecords=new LinkedHashMap<>();
    private final Map<UUID,Integer> wakeHits=new HashMap<>();
    private final Set<UUID> jumped=new HashSet<>(),contributed=new HashSet<>(),trailBursts=new HashSet<>();
    private ArmorStand lure;
    private int bounces,charges,step,filled,finishedAt=-1,dashEndedAt=-1;
    private boolean triggered,intersection;

    public RadiationNewEffectEntity(EntityType<?> type,Level level,ToxicSpec spec){super(type,level,spec);}
    public boolean canTrigger(){return !triggered&&finishedAt<0&&switch(spec){
        case FILM,CLOUD,DECOY,MATRIX -> age<life-4;default -> false;
    };}
    public void trigger(){
        if(triggered||finishedAt>=0)return;
        triggered=true;value("Triggered",1);value("ResolvedAt",age);step=0;
        life=Math.max(life,age+32);value("Life",life);
    }
    private void autoTrigger(){trigger();closeRecast(RecastResult.TIMEOUT);}
    public void cancel(){finish(6);if(lure!=null)lure.discard();}
    private void finish(int linger){
        if(finishedAt<0){finishedAt=age;value("FinishedAt",age);}
        life=age+linger;value("Life",life);
    }
    private void closeRecast(RecastResult result){
        if(owner() instanceof ServerPlayer player&&isCurrentCast(player)){
            var recasts=MagicData.getPlayerMagicData(player).getPlayerRecasts();
            var instance=recasts.getRecastInstance(spell().getSpellId());
            if(instance!=null)recasts.removeRecast(instance,result);
        }
    }
    private void sound(SoundEvent sound,float volume,float pitch){
        level().playSound(null,getX(),getY(),getZ(),sound,SoundSource.PLAYERS,volume,pitch);
    }
    @Override protected void setup(LivingEntity owner){
        ServerLevel server=(ServerLevel)level();direction=owner.getLookAngle().normalize();vector("Direction",direction);
        value("Rank",rank);value("FinishedAt",-1);value("Flash",-100);
        if(spec==ToxicSpec.ISOTOPE||spec==ToxicSpec.FILM||spec==ToxicSpec.CHAIN){
            String key="WanRadiationAim_"+spec.id;
            Entity locked=owner.getPersistentData().hasUUID(key)?server.getEntity(owner.getPersistentData().getUUID(key)):null;
            LivingEntity target=locked instanceof LivingEntity living&&ToxicCombat.enemy(owner,living)
                    &&owner.distanceToSqr(living)<=24*24&&ToxicCombat.visible(server,owner.getEyePosition(),living.getBoundingBox().getCenter(),owner)
                    ?living:ToxicCombat.aimedEnemy(server,owner,24);
            owner.getPersistentData().remove(key);
            if(spec==ToxicSpec.ISOTOPE||spec==ToxicSpec.FILM){
                setPos(target!=null?target.getBoundingBox().getCenter():areaAim(server,owner));
                vector("AreaCenter",position());value("AreaRadius",(int)RadiationRules.AREA_RADIUS);
                BlockHitResult floor=server.clip(new ClipContext(position(),position().add(0,-8,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,owner));
                vector("AreaFloor",floor.getType()==HitResult.Type.BLOCK?floor.getLocation().add(0,.035,0):position());
                for(LivingEntity enemy:ToxicCombat.sphere(server,owner,position(),RadiationRules.AREA_RADIUS)){
                    if(areaTargets.size()>=RadiationRules.AREA_TARGET_LIMIT)break;
                    areaTargets.add(enemy.getUUID());
                    if(spec==ToxicSpec.ISOTOPE)mark(enemy);
                    else filmRecords.put(enemy.getUUID(),new FilmRecord(enemy.getBbHeight()));
                }
                value("Count",areaTargets.size());
            }else if(target!=null){
                targetId=target.getUUID();setPos(target.getBoundingBox().getCenter());value("Target",target.getId());
                value("Width",(int)(target.getBbWidth()*1000));value("Height",(int)(target.getBbHeight()*1000));
            }
        }
        switch(spec){
            case NEUTRON -> {setPos(owner.getEyePosition().add(direction.scale(.35)));velocity=direction.scale(.85);points.add(position());}
            case MODERATOR,CLOUD,MATRIX -> {Vec3 ground=groundAim(server,owner);if(ground!=null)setPos(ground);}
            case LEAD -> {
                direction=new Vec3(direction.x,0,direction.z).normalize();
                if(direction.lengthSqr()<.1)direction=new Vec3(0,0,1);
                vector("Direction",direction);setPos(owner.position().add(direction.scale(2)).add(0,RadiationRules.LEAD_CENTER_HEIGHT,0));
                charges=RadiationRules.shieldCharges(rank);value("Charges",charges);value("Capacity",charges);
                sound(SoundEvents.IRON_TRAPDOOR_CLOSE,.65F,.65F);
            }
            case DECOY -> {
                setPos(owner.position());vector("Shell",position());
                lure=new ArmorStand(EntityType.ARMOR_STAND,server){@Override public boolean shouldBeSaved(){return false;}};
                if(lure!=null){
                    lure.setPos(position());lure.setInvisible(true);lure.setNoGravity(true);lure.setSilent(true);
                    lure.setHealth(Math.min(20,5+rank*2));
                    lure.getPersistentData().putUUID("WanRadiationLureOwner",owner.getUUID());
                    server.addFreshEntity(lure);value("Lure",lure.getId());
                }
                owner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,80,0,false,false));
            }
            case CHERENKOV -> {setPos(owner.position());points.add(owner.position());publishPoints("Path",points);life=64;value("Life",life);}
            default -> {}
        }
        if(spec==ToxicSpec.MODERATOR){life=80+10*(rank-1);value("Life",life);}
    }
    public static Vec3 groundAim(ServerLevel server,LivingEntity owner){
        Vec3 from=owner.getEyePosition(),end=from.add(owner.getLookAngle().scale(12));
        Vec3 at=server.clip(new ClipContext(from,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,owner)).getLocation();
        BlockHitResult ground=server.clip(new ClipContext(at.add(0,1,0),at.add(0,-12,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,owner));
        if(ground.getType()!=HitResult.Type.BLOCK||ground.getDirection()!=Direction.UP)return null;
        return ground.getLocation().add(0,.035,0);
    }
    public static Vec3 areaAim(ServerLevel server,LivingEntity owner){
        LivingEntity target=ToxicCombat.aimedEnemy(server,owner,24);
        return target!=null?target.getBoundingBox().getCenter():ToxicCombat.end(server,owner,16).add(0,.05,0);
    }
    private LivingEntity areaTarget(ServerLevel server,LivingEntity owner,UUID id){
        Entity entity=server.getEntity(id);
        return entity instanceof LivingEntity living&&ToxicCombat.enemy(owner,living)&&owner.distanceToSqr(living)<1600?living:null;
    }
    private LivingEntity target(ServerLevel server,LivingEntity owner){
        Entity entity=targetId==null?null:server.getEntity(targetId);
        return entity instanceof LivingEntity living&&ToxicCombat.enemy(owner,living)&&owner.distanceToSqr(living)<1600?living:null;
    }
    @Override protected void serverTick(ServerLevel server,LivingEntity owner){
        if(finishedAt>=0)return;
        switch(spec){
            case ISOTOPE -> isotope(server,owner);case NEUTRON -> neutron(server,owner);
            case MODERATOR -> moderator(server,owner);case LEAD -> lead(server,owner);
            case CHAIN -> chain(server,owner);case FILM -> film(server,owner);
            case CLOUD -> cloud(server,owner);case CHERENKOV -> cherenkov(server,owner);
            case DECOY -> decoy(server,owner);case MATRIX -> matrix(server,owner);default -> {}
        }
    }
    private void isotope(ServerLevel server,LivingEntity owner){
        areaTargets.removeIf(id->areaTarget(server,owner,id)==null);
        if(areaTargets.isEmpty()){finish(6);return;}
        if(RadiationRules.isotopePulse(age)){
            for(UUID id:areaTargets){
                LivingEntity target=areaTarget(server,owner,id);
                if(target!=null)ToxicCombat.damage(this,target,.2F);
            }
            value("Flash",age);value("Pulses",(age-1)/20+1);
            sound(SoundEvents.AMETHYST_BLOCK_HIT,.32F,1.2F+(age-1)/200F);
        }
    }
    private void neutron(ServerLevel server,LivingEntity owner){
        Vec3 start=position(),end=start.add(velocity);
        BlockHitResult block=server.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
        Vec3 stop=block.getType()==HitResult.Type.BLOCK?block.getLocation():end;
        LivingEntity victim=null;Vec3 nearest=stop;double distance=start.distanceToSqr(stop);
        for(LivingEntity target:server.getEntitiesOfClass(LivingEntity.class,new AABB(start,stop).inflate(.45),t->ToxicCombat.enemy(owner,t))){
            var box=target.getBoundingBox().inflate(.22);
            Vec3 hit=box.contains(start)?start:box.clip(start,stop).orElse(null);
            if(hit!=null&&start.distanceToSqr(hit)<=distance){victim=target;nearest=hit;distance=start.distanceToSqr(hit);}
        }
        setPos(nearest);points.add(position());if(points.size()>7)points.remove(0);publishPoints("Trail",points);
        if(victim!=null){
            if(ToxicCombat.damageFrom(this,victim,(float)Math.pow(.85,bounces),damageOrigin())){
                mark(victim);if(bounces>0)ToxicCombat.irradiate(owner,victim,60);
            }
            value("Flash",age);finish(7);sound(SoundEvents.FIREWORK_ROCKET_TWINKLE,.5F,1.6F);return;
        }
        if(block.getType()==HitResult.Type.BLOCK){
            vector("Impact",stop);vector("Normal",Vec3.atLowerCornerOf(block.getDirection().getNormal()));value("Flash",age);
            if(bounces>=RadiationRules.maxBounces(rank)){finish(7);return;}
            Direction.Axis axis=block.getDirection().getAxis();
            velocity=new Vec3(axis==Direction.Axis.X?-velocity.x:velocity.x,axis==Direction.Axis.Y?-velocity.y:velocity.y,axis==Direction.Axis.Z?-velocity.z:velocity.z);
            setPos(stop.add(Vec3.atLowerCornerOf(block.getDirection().getNormal()).scale(.08)));
            value("Bounce",++bounces);sound(SoundEvents.AMETHYST_BLOCK_HIT,.6F,1.3F+bounces*.2F);
        }
        vector("Direction",velocity.normalize());
    }
    private void moderator(ServerLevel server,LivingEntity owner){
        if(age%5!=1)return;int affected=0;
        for(LivingEntity target:server.getEntitiesOfClass(LivingEntity.class,new AABB(position(),position()).inflate(3,2,3),t->ToxicCombat.enemy(owner,t))){
            Vec3 d=target.position().subtract(position());
            if(d.x*d.x+d.z*d.z>9||!ToxicCombat.visible(server,position().add(0,.5,0),target.getBoundingBox().getCenter(),this))continue;
            int amp=(rank>=4?1:0)+(ToxicMarks.count(target,ownerUUID(),server.getGameTime())>0?1:0);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,12,Math.min(2,amp),true,false),owner);affected++;
        }
        value("Affected",Math.min(32,affected));
    }
    private void lead(ServerLevel server,LivingEntity owner){
        Vec3 right=new Vec3(direction.z,0,-direction.x);
        for(Projectile projectile:server.getEntitiesOfClass(Projectile.class,new AABB(position(),position()).inflate(6,3,6))){
            if(!(projectile instanceof AbstractArrow||projectile instanceof ThrowableProjectile||projectile instanceof Fireball)
                    ||projectile.getOwner()==owner||projectile.getDeltaMovement().dot(direction)>=-.005)continue;
            Vec3 end=projectile.position().subtract(position()),start=end.subtract(projectile.getDeltaMovement());
            double crossing=RadiationRules.shieldCrossing(start.dot(direction),end.dot(direction));
            if(!Double.isFinite(crossing))continue;Vec3 hit=start.lerp(end,crossing);
            if(Math.abs(hit.dot(right))>RadiationRules.LEAD_HALF_WIDTH||Math.abs(hit.y)>RadiationRules.LEAD_HALF_HEIGHT)continue;
            vector("Hit",hit.add(position()));projectile.discard();value("Charges",--charges);value("Flash",age);
            sound(SoundEvents.SHIELD_BLOCK,.7F,.7F+charges*.1F);
            if(charges<=0){finish(8);sound(SoundEvents.IRON_TRAPDOOR_OPEN,.65F,.6F);return;}
        }
    }
    private void chain(ServerLevel server,LivingEntity owner){
        if(age%5!=1)return;
        LivingEntity next=step==0?target(server,owner):ToxicCombat.sphere(server,owner,position(),4).stream()
                .filter(t->!jumped.contains(t.getUUID())&&ToxicMarks.count(t,ownerUUID(),server.getGameTime())>0).findFirst().orElse(null);
        if(next==null||ToxicMarks.count(next,ownerUUID(),server.getGameTime())<=0){finish(6);return;}
        Vec3 from=step==0?owner.getEyePosition().add(owner.getLookAngle().scale(.4)):position(),to=next.getBoundingBox().getCenter();
        if(!ToxicCombat.visible(server,from,to,this)){finish(6);return;}
        vector("From",from);vector("End",to);setPos(to);value("Flash",age);
        if(ToxicCombat.damageFrom(this,next,.65F,from))ToxicMarks.consumeOne(next,ownerUUID(),server.getGameTime());
        jumped.add(next.getUUID());value("Step",++step);sound(SoundEvents.AMETHYST_BLOCK_BREAK,.55F,1+step*.13F);
        if(step>=Math.min(5,rank+1))finish(6);
    }
    private void film(ServerLevel server,LivingEntity owner){
        filmRecords.keySet().removeIf(id->areaTarget(server,owner,id)==null);
        if(filmRecords.isEmpty()){closeRecast(RecastResult.COUNTERSPELL);finish(6);return;}
        if(!triggered&&RadiationRules.filmCapture(age)){
            for(var entry:filmRecords.entrySet()){
                LivingEntity target=areaTarget(server,owner,entry.getKey());
                if(target!=null&&entry.getValue().shots.size()<4)entry.getValue().shots.add(target.getBoundingBox().getCenter());
            }
            publishFilm(server);value("CaptureAt",age);sound(SoundEvents.AMETHYST_BLOCK_HIT,.38F,.8F+(age/15+1)*.2F);
        }
        if(!triggered&&age>=61)autoTrigger();if(!triggered)return;
        int elapsed=age-visual().getInt("ResolvedAt");
        int shots=filmRecords.values().stream().mapToInt(record->record.shots.size()).max().orElse(0);
        if(elapsed%5==0&&step<shots){
            for(var entry:filmRecords.entrySet()){
                FilmRecord record=entry.getValue();LivingEntity target=areaTarget(server,owner,entry.getKey());
                if(target==null||step>=record.shots.size())continue;
                Vec3 from=record.shots.get(step),to=target.getBoundingBox().getCenter();
                BlockHitResult hit=server.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
                record.from=from;record.end=hit.getLocation();record.flash=age;record.fired=step+1;
                if(hit.getType()==HitResult.Type.MISS)ToxicCombat.damageFrom(this,target,RadiationRules.filmDamage(step),from);
            }
            publishFilm(server);value("Flash",age);value("Step",step+1);
            sound(SoundEvents.TRIDENT_RETURN,.42F,1.6F);step++;
        }
        if(step>=shots)finish(7);
    }
    private void cloud(ServerLevel server,LivingEntity owner){
        if(!triggered&&age%3==1){
            for(LivingEntity target:ToxicCombat.sphere(server,owner,position().add(0,.8,0),rank>=5?4:3)){
                if(!trails.containsKey(target.getUUID())&&trails.size()>=8)continue;
                List<Vec3> path=trails.computeIfAbsent(target.getUUID(),id->new ArrayList<>());
                Vec3 point=target.position().add(0,.35,0);
                if(path.isEmpty()||path.get(path.size()-1).distanceToSqr(point)>.10){path.add(point);if(path.size()>12)path.remove(0);}
            }
            publishTrails();
        }
        if(!triggered&&age>=80)autoTrigger();if(!triggered)return;
        int elapsed=age-visual().getInt("ResolvedAt"),longest=0;
        for(var entry:trails.entrySet()){
            List<Vec3> path=entry.getValue();longest=Math.max(longest,path.size());
            if(RadiationRules.cloudPathStep(elapsed,path.size())){
                int index=(elapsed-1)/2;Vec3 from=path.get(Math.max(0,index-1)),to=path.get(index);
                // Sweep the whole segment, including stationary one-node trails.
                for(LivingEntity target:server.getEntitiesOfClass(LivingEntity.class,new AABB(from,to).inflate(1.1),t->ToxicCombat.enemy(owner,t))){
                    Vec3 center=target.getBoundingBox().getCenter();AABB box=target.getBoundingBox().inflate(.8);
                    Vec3 closest=box.contains(from)?from:box.clip(from,to).orElse(null);
                    if(cloudPathHits.contains(target.getUUID())||closest==null
                            ||!ToxicCombat.visible(server,closest.add(0,.1,0),center,this))continue;
                    if(ToxicCombat.damageFrom(this,target,.3F,from)){cloudPathHits.add(target.getUUID());mark(target);}
                }
            }
            if(elapsed>=Math.max(0,path.size()-1)*2+1&&trailBursts.add(entry.getKey())&&!path.isEmpty()){
                Vec3 end=path.get(path.size()-1);
                for(LivingEntity target:server.getEntitiesOfClass(LivingEntity.class,new AABB(end,end).inflate(1.5),t->ToxicCombat.enemy(owner,t)
                        &&ToxicCombat.visible(server,end.add(0,.1,0),t.getBoundingBox().getCenter(),this)))
                    if(!cloudBurstHits.contains(target.getUUID())&&ToxicCombat.damageFrom(this,target,.55F,path.get(0))){
                        cloudBurstHits.add(target.getUUID());mark(target);
                    }
                sound(SoundEvents.FIREWORK_ROCKET_TWINKLE,.4F,1.7F);
            }
        }
        if(elapsed>=Math.max(1,longest)*2)finish(8);
    }
    private void cherenkov(ServerLevel server,LivingEntity owner){
        if(dashEndedAt<0){
            if(age<=16&&owner.onGround()&&!owner.isInWater()&&!owner.horizontalCollision){
                Vec3 look=owner.getLookAngle(),forward=new Vec3(look.x,0,look.z).normalize();
                owner.setDeltaMovement(forward.scale(.65).add(0,owner.getDeltaMovement().y,0));owner.hurtMarked=true;
                Vec3 at=owner.position();
                if(points.isEmpty()||points.get(points.size()-1).distanceToSqr(at)>.09){
                    points.add(at);if(points.size()>16)points.remove(0);publishPoints("Path",points);value("Count",points.size());
                }
                setPos(at);
                if(!intersection&&points.size()>7)for(int i=0;i<points.size()-5;i++)if(points.get(i).distanceToSqr(at)<.64){
                    intersection=true;value("IntersectionAt",age);
                    for(LivingEntity target:ToxicCombat.sphere(server,owner,at.add(0,.5,0),2))ToxicCombat.damageFrom(this,target,.25F,points.get(0));break;
                }
                return;
            }
            dashEndedAt=age;value("DashEndedAt",age);sound(SoundEvents.TRIDENT_RIPTIDE_1,.65F,1.4F);
        }
        int index=(age-dashEndedAt)/2;if(index>=points.size()){finish(8);return;}
        value("Wave",index+1);if((age-dashEndedAt)%2!=0)return;
        Vec3 at=points.get(index),before=points.get(Math.max(0,index-1)),after=points.get(Math.min(points.size()-1,index+1));
        Vec3 d=after.subtract(before),side=new Vec3(-d.z,0,d.x).normalize();if(side.lengthSqr()<.01)side=new Vec3(1,0,0);
        for(int sign:new int[]{-1,1})for(LivingEntity target:ToxicCombat.sphere(server,owner,at.add(side.scale(sign*1.2)).add(0,.6,0),1.25))
            if(wakeHits.getOrDefault(target.getUUID(),0)<2&&ToxicCombat.damageFrom(this,target,.38F,at))wakeHits.merge(target.getUUID(),1,Integer::sum);
    }
    private void decoy(ServerLevel server,LivingEntity owner){
        if(!triggered&&lure!=null&&!lure.isAlive()){value("Broken",1);autoTrigger();}
        if(!triggered){
            if(lure!=null&&age%10==1)for(Mob mob:server.getEntitiesOfClass(Mob.class,new AABB(position(),position()).inflate(7),m->ToxicCombat.enemy(owner,m)))
                if(mob.getTarget()==owner&&ToxicCombat.visible(server,mob.getEyePosition(),position().add(0,1,0),this))mob.setTarget(lure);
            if(age>=RadiationRules.DECOY_RECAST_TICKS){closeRecast(RecastResult.USER_CANCEL);finish(7);}return;
        }
        if(step++>0)return;if(lure!=null)lure.discard();
        Vec3 shell=position(),origin=owner.position();vector("Shell",shell);vector("Departure",origin);
        AABB destination=owner.getBoundingBox().move(shell.subtract(origin));
        boolean safe=visual().getInt("Broken")==0&&owner.distanceToSqr(shell)<=576
                &&server.hasChunkAt(BlockPos.containing(shell))&&server.getWorldBorder().isWithinBounds(BlockPos.containing(shell))
                &&ToxicCombat.visible(server,owner.getEyePosition(),shell.add(0,1,0),owner)
                &&server.noCollision(owner,destination)&&!server.noCollision(owner,destination.move(0,-.12,0))
                &&!server.containsAnyLiquid(destination);
        if(safe){
            if(owner instanceof ServerPlayer player)player.teleportTo(server,shell.x,shell.y,shell.z,owner.getYRot(),owner.getXRot());
            else owner.teleportTo(shell.x,shell.y,shell.z);
            owner.setDeltaMovement(Vec3.ZERO);owner.fallDistance=0;setPos(origin);value("Swapped",1);
            for(LivingEntity target:ToxicCombat.sphere(server,owner,origin.add(0,.7,0),2.5))ToxicCombat.damageFrom(this,target,.4F,shell);
            sound(SoundEvents.ENDERMAN_TELEPORT,.7F,1.2F);
        }else if(visual().getInt("Broken")>0){
            for(LivingEntity target:ToxicCombat.sphere(server,owner,shell.add(0,.7,0),2.5))ToxicCombat.damageFrom(this,target,.4F,origin);
        }else if(owner instanceof ServerPlayer player)player.displayClientMessage(net.minecraft.network.chat.Component.translatable("ui.alex_caves_spellsbook.radiation.unsafe_swap"),true);
        value("Flash",age);finish(10);
    }
    private void matrix(ServerLevel server,LivingEntity owner){
        if(!triggered&&age>=RadiationRules.MATRIX_RECAST_TICKS)autoTrigger();if(!triggered)return;
        int pattern=visual().getInt("Pattern"),elapsed=age-visual().getInt("ResolvedAt");
        if(step++==0){
            if(pattern==0)pulse(server,owner,.3F);else resolveMatrix(server,owner,pattern);
            sound(SoundEvents.BEACON_DEACTIVATE,.6F,1.5F);
        }
        if(pattern>=4&&pattern<=6&&(elapsed==4||elapsed==8||elapsed==12)){
            Vec3 fragment=position().add((pattern-5)*1.2,.4,(elapsed/4-2)*1.2);
            for(LivingEntity target:ToxicCombat.sphere(server,owner,fragment,1.1))ToxicCombat.damageFrom(this,target,.16F,position().add(0,4,0));
        }
        if(elapsed>=17)finish(6);
    }
    public void registerRadiationHit(ServerLevel server,LivingEntity owner,LivingEntity victim,UUID castId,Vec3 origin){
        if(spec!=ToxicSpec.MATRIX||triggered||contributed.contains(castId)||contributed.size()>=64
                ||victim.distanceToSqr(position())>RadiationRules.MATRIX_RADIUS*RadiationRules.MATRIX_RADIUS||!ToxicCombat.enemy(owner,victim))return;
        Vec3 d=victim.getBoundingBox().getCenter().subtract(origin);
        int cell=RadiationRules.matrixCell(d.x,d.y,d.z),bit=1<<cell;if((filled&bit)!=0)return;
        contributed.add(castId);filled|=bit;value("Cells",filled);value("LastCell",cell);value("Flash",age);
        sound(SoundEvents.AMETHYST_BLOCK_CHIME,.5F,.8F+Integer.bitCount(filled)*.1F);
        int pattern=RadiationRules.matrixPattern(filled);if(pattern>0){value("Pattern",pattern);autoTrigger();}
    }
    private void resolveMatrix(ServerLevel server,LivingEntity owner,int pattern){
        for(LivingEntity target:ToxicCombat.sphere(server,owner,position().add(0,.8,0),5)){
            Vec3 d=target.position().subtract(position());
            boolean hit=pattern<=3?Math.abs(d.z-(pattern-2)*1.2)<1&&Math.abs(d.x)<=4
                    :pattern<=6?Math.abs(d.x-(pattern-5)*1.2)<1.2&&Math.abs(d.z)<=2.6
                    :(Math.abs(d.x-d.z)<1.1||Math.abs(d.x+d.z)<1.1)&&Math.abs(d.x)<=3.8&&Math.abs(d.z)<=3.8;
            if(hit)ToxicCombat.damageFrom(this,target,pattern<=3?.9F:pattern<=6?.7F:1.05F,position());
        }
    }
    private void pulse(ServerLevel server,LivingEntity owner,float factor){
        for(LivingEntity target:ToxicCombat.sphere(server,owner,position().add(0,.7,0),4))ToxicCombat.damageFrom(this,target,factor,position());
    }
    private static ListTag encodePoints(List<Vec3> points){
        ListTag list=new ListTag();for(Vec3 point:points){CompoundTag p=new CompoundTag();p.putDouble("X",point.x);p.putDouble("Y",point.y);p.putDouble("Z",point.z);list.add(p);}return list;
    }
    private void publishPoints(String key,List<Vec3> points){CompoundTag state=visual().copy();state.put(key,encodePoints(points));entityDataState(state);}
    private void publishFilm(ServerLevel server){
        ListTag records=new ListTag();
        for(var entry:filmRecords.entrySet()){
            Entity target=server.getEntity(entry.getKey());if(target==null)continue;
            FilmRecord record=entry.getValue();CompoundTag row=new CompoundTag();
            row.putInt("Target",target.getId());row.putInt("Height",(int)(record.height*1000));
            row.put("Snapshots",encodePoints(record.shots));row.putInt("Step",record.fired);row.putInt("Flash",record.flash);
            for(var pair:Map.of("From",record.from,"End",record.end).entrySet()){
                Vec3 point=pair.getValue();String key=pair.getKey();
                row.putDouble(key+"X",point.x);row.putDouble(key+"Y",point.y);row.putDouble(key+"Z",point.z);
            }
            records.add(row);
        }
        CompoundTag state=visual().copy();state.put("FilmTargets",records);state.putInt("Count",records.size());entityDataState(state);
    }
    private void publishTrails(){
        ListTag list=new ListTag();for(List<Vec3> path:trails.values()){CompoundTag trail=new CompoundTag();trail.put("Points",encodePoints(path));list.add(trail);}
        CompoundTag state=visual().copy();state.put("CloudPaths",list);state.putInt("Count",trails.size());entityDataState(state);
    }
    @Override protected void expire(LivingEntity owner){closeRecast(RecastResult.TIMEOUT);if(lure!=null)lure.discard();}
    @Override public void remove(RemovalReason reason){
        if(!level().isClientSide){if(lure!=null)lure.discard();closeRecast(RecastResult.COUNTERSPELL);}super.remove(reason);
    }
}
