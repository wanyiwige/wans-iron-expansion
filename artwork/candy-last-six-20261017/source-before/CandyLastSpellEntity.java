package cn.wanyiwige.alexcavesspellsbook.candy;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import java.util.*;

/** Server-authoritative second-half programs; each cast owns one bounded family and hit ledger. */
public final class CandyLastSpellEntity extends CandySpellEntity {
    private static final int ROOT=0,SHARD=1,SHOT=2,ICE=3;
    private boolean last,burst;
    private int role,emitted,travelTicks;
    private Vec3 goal=Vec3.ZERO,direction=Vec3.ZERO;
    private CandyLastSpellEntity root;
    private final Set<UUID> struck=new HashSet<>();
    public CandyLastSpellEntity(EntityType<?> type,net.minecraft.world.level.Level level){super(type,level);}
    public void beginLast(Vec3 goal,Vec3 direction){
        this.last=true;this.root=this;this.goal=goal;this.direction=direction.normalize();
        visualValue("LastProgram",1);visualValue("LastRole",ROOT);
        visualValue("Heading",(float)Math.atan2(this.direction.x,this.direction.z));
    }
    private CandyLastSpellEntity actor(int role,int index,Vec3 pos,Vec3 velocity){
        CandyEffectEntity created=child(role==ICE?Mode.PHANTOM:Mode.BALL,pos,velocity);
        if(!(created instanceof CandyLastSpellEntity child))return null;
        child.last=true;child.root=this;child.role=role;child.goal=goal;child.direction=direction;
        child.setDeltaMovement(velocity);child.visualValue("LastProgram",1);child.visualValue("LastRole",role);
        child.visualValue("Index",index);child.color(index);
        return child;
    }
    @Override protected void motion(ServerLevel level,LivingEntity owner){if(!last)super.motion(level,owner);}
    @Override protected void serverTimeline(ServerLevel level,LivingEntity owner,int age){
        if(!last){super.serverTimeline(level,owner,age);return;}
        if(role!=ROOT){projectile(level,owner);return;}
        switch(spec().id()){
            case "sugar_glass_stars" -> glass(owner,age);
            case "rainbow_leap" -> {if(age>=12)finish();}
            case "sweet_overdrive" -> overdrive(owner,age);
            case "rainbow_snare" -> snare(level,owner,age);
            case "candicorn_charge" -> candicorn(level,owner,age);
            case "gummy_guardian" -> {if(age==0)summonBear(level,owner,0);}
            case "gingerbread_march" -> {if(age==0)for(int i=0;i<3;i++)summonGinger(level,owner,i);}
            case "candy_cane_whirl" -> whirl(level,owner,age);
            case "gumball_sentry" -> {if(age==0)summonSentry(level,owner);}
            case "gum_worm_breach" -> worm(level,owner,age);
            case "candy_banquet" -> banquet(level,owner,age);
            case "ice_cream_storm" -> iceStorm(level,owner,age);
            case "licorice_grand_feast" -> grandFeast(level,owner,age);
            default -> finish();
        }
    }
    private void glass(LivingEntity owner,int age){
        if(age%5==0&&emitted<6){
            int i=emitted++;
            Vec3 vector=direction.yRot((float)((i-2.5)*0.14)).scale(1.05).add(0,(i%2==0?0.03:-0.03),0);
            actor(SHARD,i,position().add(direction.scale(0.4)),vector);
        }
    }
    private void projectile(ServerLevel level,LivingEntity owner){
        travelTicks++;
        Vec3 from=position(),velocity=getDeltaMovement(),to=from.add(velocity);
        if(role==ICE){velocity=velocity.add(0,-0.06,0);setDeltaMovement(velocity);to=from.add(velocity);}
        var wall=level.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
        Vec3 stop=wall.getLocation();
        List<LivingEntity> targets=level.getEntitiesOfClass(LivingEntity.class,new AABB(from,stop).inflate(role==ICE?0.45:0.3),
                e->CandyCombat.enemy(owner,e)&&e.getBoundingBox().inflate(0.35).clip(from,stop).isPresent());
        targets.sort(Comparator.comparingDouble((LivingEntity e)->e.getBoundingBox().inflate(0.35).clip(from,stop).orElse(stop).distanceToSqr(from)));
        if(!targets.isEmpty()){
            LivingEntity target=targets.get(0);setPos(target.getBoundingBox().getCenter());
            if(role==ICE){splash(level,owner,1.5,true);}else{hit(target,1);impact();finish();}
            return;
        }
        setPos(stop);
        if(wall.getType()!=HitResult.Type.MISS||travelTicks>28||position().distanceToSqr(root.position())>spec().range()*spec().range()){
            if(role==ICE)splash(level,owner,1.5,true);else{impact();finish();}
        }
    }
    private boolean hit(LivingEntity target,float scale){
        return CandyCombat.damage(this,target,CandyRegistry.ENTRIES.get(spec().id()).get(),spec().hitDamage(spellLevel())*scale);
    }
    private void splash(ServerLevel level,LivingEntity owner,double radius,boolean slow){
        if(burst)return;burst=true;
        for(LivingEntity target:CandyCombat.targets(level,owner,position(),radius))if(hit(target,1)&&slow)CandyCombat.slow(this,target,spec().controlTicks(),0);
        impact();finish();
    }
    private void overdrive(LivingEntity owner,int age){
        setPos(owner.position().add(0,owner.getBbHeight()*0.6,0));
        if(age==0)owner.addEffect(new MobEffectInstance(CandyRegistry.SWEET_OVERDRIVE.get(),spec().activeTicks(),spellLevel()-1),owner);
    }
    private void snare(ServerLevel level,LivingEntity owner,int age){
        if(!CandyLastRules.pulse(spec(),age))return;
        List<Integer> bound=new ArrayList<>();
        for(LivingEntity target:CandyCombat.targets(level,owner,position().add(0,0.6,0),spec().radius())){
            if(Math.abs(target.getY()-getY())>2)continue;
            if(hit(target,1)){
                boolean controlled=CandyCombat.slow(this,target,spec().controlTicks(),1);
                if(bound.size()<12&&controlled)bound.add(target.getId());
                Vec3 pull=position().subtract(target.position()).multiply(1,0,1);
                if(pull.lengthSqr()>0.25)CandyCombat.impulse(this,target,pull.normalize().scale(0.13));
            }
        }
        var cue=visual().copy();cue.putIntArray("BoundTargets",bound);cue.putInt("SnarePulseAge",cue.getInt("Age"));visualSnapshot(cue);
        impact();
    }
    private void candicorn(ServerLevel level,LivingEntity owner,int age){
        Vec3 from=position(),step=direction.multiply(1,0,1).normalize().scale(0.55),to=from.add(step);
        var wall=level.clip(new ClipContext(from.add(0,0.65,0),to.add(0,0.65,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
        Vec3 floor=CandyFirstCasting.ground(level,to.add(0,0.5,0),0.5,2,owner);
        if(wall.getType()!=HitResult.Type.MISS||floor==null||Math.abs(floor.y-from.y)>1){impact();finish();return;}
        setPos(floor);visualValue("ChargeAge",age);
        for(LivingEntity target:CandyCombat.targets(level,owner,position(),spec().radius()))if(!struck.contains(target.getUUID())){
            if(hit(target,1)){struck.add(target.getUUID());CandyCombat.impulse(this,target,direction.scale(0.3).add(0,0.12,0));}
        }
        if(age>=43||position().distanceToSqr(anchor("Anchor"))>=spec().range()*spec().range()){
            impact();finish();
        }
    }
    private void whirl(ServerLevel level,LivingEntity owner,int age){
        setPos(owner.position().add(0,owner.getBbHeight()*0.6,0));
        if(!CandyLastRules.pulse(spec(),age))return;
        for(LivingEntity target:CandyCombat.targets(level,owner,position(),spec().radius()))if(hit(target,1))
            CandyCombat.impulse(this,target,target.position().subtract(owner.position()).normalize().scale(0.18));
    }
    private void worm(ServerLevel level,LivingEntity owner,int age){
        if(age<8)return;
        Vec3 from=position(),to=from.add(direction.multiply(1,0,1).normalize().scale(0.55));
        var wall=level.clip(new ClipContext(from.add(0,0.5,0),to.add(0,0.5,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
        if(wall.getType()!=HitResult.Type.MISS){impact();finish();return;}
        Vec3 floor=CandyFirstCasting.ground(level,to,1,2,owner);
        if(floor==null||Math.abs(floor.y-from.y)>1){impact();finish();return;}
        setPos(floor);visualValue("WormAge",age);
        if(age%4==0)for(LivingEntity target:CandyCombat.targets(level,owner,position().add(0,1,0),spec().radius())){
            if(struck.add(target.getUUID())&&hit(target,1))CandyCombat.impulse(this,target,target.position().subtract(position()).normalize().scale(0.32).add(0,0.1,0));
        }
        if(age>=43||position().distanceToSqr(anchor("Anchor"))>spec().range()*spec().range()
                ||position().distanceToSqr(goal)<0.8){impact();finish();}
    }
    private void banquet(ServerLevel level,LivingEntity owner,int age){
        if(!CandyLastRules.pulse(spec(),age))return;
        for(LivingEntity target:CandyCombat.targets(level,owner,position().add(0,0.7,0),spec().radius())){
            if(Math.abs(target.getY()-getY())<=2&&hit(target,1))CandyCombat.slow(this,target,spec().controlTicks(),0);
        }
        // Owner-side feast reward stays in the domain and has no terrain or biome mutation.
        for(LivingEntity ally:level.getEntitiesOfClass(LivingEntity.class,new AABB(position(),position()).inflate(spec().radius()),
                e->e.isAlive()&&(e==owner||e.isAlliedTo(owner)||owner.isAlliedTo(e)||CandyCombat.principal(e)==CandyCombat.principal(owner)))){
            if(ally.position().distanceToSqr(position())<=spec().radius()*spec().radius())
                ally.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION,30,0),owner);
        }
        impact();
    }
    private void iceStorm(ServerLevel level,LivingEntity owner,int age){
        if(age%16!=0||emitted>=7)return;
        int i=emitted++;double angle=i*2.399963229728653,rad=Math.sqrt((i+0.5)/7.0)*spec().radius();
        Vec3 floor=position().add(Math.cos(angle)*rad,0,Math.sin(angle)*rad);
        var ceiling=level.clip(new ClipContext(floor.add(0,0.3,0),floor.add(0,6,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
        Vec3 top=ceiling.getLocation().subtract(0,0.4,0);
        if(top.y-floor.y>1)actor(ICE,i,top,new Vec3(0,-0.3,0));
    }
    private void grandFeast(ServerLevel level,LivingEntity owner,int age){
        if(age==0){summonBear(level,owner,0);summonGinger(level,owner,1);summonGinger(level,owner,2);summonSentry(level,owner);}
        if(!CandyLastRules.pulse(spec(),age))return;
        for(LivingEntity target:CandyCombat.targets(level,owner,position().add(0,0.7,0),spec().radius())){
            if(Math.abs(target.getY()-getY())<=2&&hit(target,1))CandyCombat.slow(this,target,spec().controlTicks(),0);
        }
        impact();
    }
    private Vec3 summonPosition(ServerLevel level,LivingEntity owner,int i){
        double angle=i*2.094,offset=i==0?1.5:2.2;
        Vec3 desired=position().add(Math.cos(angle)*offset,0,Math.sin(angle)*offset);
        Vec3 floor=CandyFirstCasting.ground(level,desired.add(0,0.5,0),0.5,3,owner);
        if(floor==null)return null;
        return level.clip(new ClipContext(owner.getEyePosition(),floor.add(0,0.6,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,owner)).getType()==HitResult.Type.MISS?floor:null;
    }
    private void summonBear(ServerLevel level,LivingEntity owner,int i){
        Vec3 pos=summonPosition(level,owner,i);if(pos==null)return;
        SummonedCandyBear mob=CandyRegistry.SUMMON_BEAR.get().create(level);if(mob==null)return;
        mob.setPos(pos);mob.attach(this,i);if(level.noCollision(mob)&&session().summon(mob,this)){visualValue("Summons",visual().getInt("Summons")+1);summonCue(pos);}
    }
    private void summonGinger(ServerLevel level,LivingEntity owner,int i){
        Vec3 pos=summonPosition(level,owner,i);if(pos==null)return;
        SummonedCandyGinger mob=CandyRegistry.SUMMON_GINGER.get().create(level);if(mob==null)return;
        mob.setPos(pos);mob.attach(this,i,0xDB397D);if(level.noCollision(mob)&&session().summon(mob,this)){visualValue("Summons",visual().getInt("Summons")+1);summonCue(pos);}
    }
    private void summonCue(Vec3 pos){
        if(!spec().id().equals("gummy_guardian")&&!spec().id().equals("gingerbread_march"))return;
        var cue=visual().copy();int i=cue.getInt("SummonCues");
        cue.putDouble("SummonX"+i,pos.x);cue.putDouble("SummonY"+i,pos.y);cue.putDouble("SummonZ"+i,pos.z);
        cue.putInt("SummonCues",i+1);cue.putInt("SummonCueAge",cue.getInt("Age"));visualSnapshot(cue);
    }
    private void summonSentry(ServerLevel level,LivingEntity owner){
        Vec3 pos=position();SummonedCandySentry mob=CandyRegistry.SUMMON_SENTRY.get().create(level);if(mob==null)return;
        mob.setPos(pos);mob.attach(this);if(level.noCollision(mob)&&session().summon(mob,this))visualValue("Summons",visual().getInt("Summons")+1);
    }
    public void sentryShot(Mob mob,LivingEntity target){
        if(isRemoved()||session()==null||session().ended()||!spec().id().equals("gumball_sentry")&&!spec().id().equals("licorice_grand_feast"))return;
        Vec3 start=mob.getEyePosition(),vector=target.getBoundingBox().getCenter().subtract(start);
        if(vector.lengthSqr()>64||vector.lengthSqr()<0.01)return;
        actor(SHOT,emitted++%12,start,vector.normalize().scale(0.7));
    }
}
