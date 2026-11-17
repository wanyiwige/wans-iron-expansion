package cn.wanyiwige.alexcavesspellsbook.candy;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import java.util.*;

/** Server programs for spells 1-13; actors and their controller share one cast ledger. */
public class CandySpellEntity extends CandyEffectEntity {
    public static final int CONTROL=0,BALL=1,MINT=2,SPEAR=3,ROCKET=4,AREA=5;
    private int role=CONTROL,index,bounces=3,flightTicks,launched;
    private boolean programmed,detonated,split,locked;
    private double travelled;
    private Vec3 direction=Vec3.ZERO,goal=Vec3.ZERO;
    private CandySpellEntity controller;
    private UUID target;
    private final Set<UUID> visited=new HashSet<>();
    private Vec3 pixelImpactNormal=Vec3.ZERO;
    private final int[] pixelPalette={0,1,2,3,4,5};
    private boolean pixelPaletteShuffled;
    private int pixelPaletteCursor;
    private int nextPixelColor(){
        if(!pixelPaletteShuffled){
            for(int i=pixelPalette.length-1;i>0;i--){int j=random.nextInt(i+1),color=pixelPalette[i];pixelPalette[i]=pixelPalette[j];pixelPalette[j]=color;}
            pixelPaletteShuffled=true;
        }
        return pixelPalette[pixelPaletteCursor++%pixelPalette.length];
    }
    private boolean pixelSpell(){return switch(spec().id()){
        case "rainbow_ricochet","sugar_hunt","crystal_split","popping_cluster","peppermint_guard",
             "peppermint_hunt","peppermint_wheels","licorice_hex_wave","sugar_seal_bloom","frostmint_rain"->true;
        default->false;};}
    private void pixelNormal(Vec3 normal){if(pixelSpell())pixelImpactNormal=normal;}
    @Override public void impact(){
        super.impact();
        if(level().isClientSide||!pixelSpell()||role==CONTROL)return;
        var s=visual().copy();
        s.putInt("PixelHitSequence",s.getInt("PixelHitSequence")+1);
        s.putInt("PixelHitAge",s.getInt("Age"));
        s.putInt("PixelHitKind",detonated?2:s.getInt("PixelFractured")==1?3:1);
        s.putDouble("PixelHitX",getX());s.putDouble("PixelHitY",getY());s.putDouble("PixelHitZ",getZ());
        s.putFloat("PixelNormalX",(float)pixelImpactNormal.x);s.putFloat("PixelNormalY",(float)pixelImpactNormal.y);s.putFloat("PixelNormalZ",(float)pixelImpactNormal.z);
        visualSnapshot(s);
        pixelImpactNormal=Vec3.ZERO;
    }
    public CandySpellEntity(EntityType<?> type,net.minecraft.world.level.Level level){super(type,level);}
    public void begin(Vec3 goal,Vec3 direction){
        programmed=true;this.goal=goal;this.direction=direction.normalize();controller=this;
        role=CandyFirstCasting.groundSpell(spec().id())?AREA:CONTROL;
        if(spec().id().equals("soda_meteor")){role=ROCKET;setDeltaMovement(direction.scale(0.8).add(0,0.15,0));}
        visualValue("Role",role);visualValue("Program",1);
        if(pixelSpell())visualValue("PixelHeading",(float)Math.atan2(this.direction.x,this.direction.z));
    }
    private void actor(CandySpellEntity root,int role,int index,Vec3 velocity){
        programmed=true;controller=root;this.role=role;this.index=index;direction=root.direction;goal=root.goal;
        setDeltaMovement(velocity);visualValue("Role",role);visualValue("Index",index);visualValue("Program",1);color(index*2);
        if(spec().id().equals("rainbow_ricochet")||spec().id().equals("peppermint_guard")||spec().id().equals("peppermint_hunt")||spec().id().equals("peppermint_wheels"))
            visualValue("PixelPalette",root.nextPixelColor());
    }
    @Override protected void motion(ServerLevel level,LivingEntity owner){if(!programmed)super.motion(level,owner);}
    @Override protected void serverTimeline(ServerLevel level,LivingEntity owner,int age){
        if(!programmed)return;
        if(controller!=this){actorTick(level,owner,age);return;}
        String id=spec().id();
        switch(id){
            case "rainbow_ricochet","sugar_hunt","crystal_split","popping_cluster","peppermint_guard","peppermint_hunt","peppermint_wheels","frostmint_rain" -> launch(level,owner,age);
            case "licorice_hex_wave" -> hex(level,owner,age);
            case "sugar_seal_bloom" -> bloom(level,owner,age);
            case "soda_meteor" -> rocket(level,owner);
            case "soda_frostburst" -> reaction(level,owner,age);
            case "caramel_bog" -> caramel(level,owner,age);
            default -> finish();
        }
    }
    private CandySpellEntity spawn(int role,Mode mode,int index,Vec3 pos,Vec3 velocity){
        CandyEffectEntity e=child(mode,pos,velocity);
        if(!(e instanceof CandySpellEntity actor))return null;
        actor.actor(this,role,index,velocity);return actor;
    }
    private void launch(ServerLevel level,LivingEntity owner,int age){
        String id=spec().id();int count=CandyFirstRules.launchCount(id);
        while(launched<count&&age>=CandyFirstRules.launchTick(id,launched)){
            int i=launched++;
            Vec3 origin=position(),velocity=direction.scale(0.95);int role=BALL;Mode mode=Mode.BALL;
            if(id.startsWith("peppermint")){
                role=MINT;mode=Mode.MINT;
                origin=owner.position().add(0,owner.getBbHeight()*0.6,0);
                if(id.equals("peppermint_wheels")){
                    velocity=direction.multiply(1,0,1).normalize().yRot((float)((i-1)*0.26)).scale(0.6);
                    Vec3 floor=CandyFirstCasting.ground(level,owner.position().add(velocity),0.8,2,owner);
                    if(floor==null)continue;origin=floor.add(0,0.35,0);
                }
            }else if(id.equals("popping_cluster")) velocity=direction.yRot((float)((i-1.5)*0.18)).scale(0.5).add(0,0.3,0);
            else if(id.equals("rainbow_ricochet"))velocity=direction.yRot((float)((i-1)*0.09)).scale(0.95);
            else if(id.equals("frostmint_rain")){
                role=SPEAR;mode=Mode.PHANTOM;
                double a=i*Math.PI*2/6;
                Vec3 offset=goal.add(Math.cos(a)*1.6,0,Math.sin(a)*1.6);
                Vec3 floor=CandyFirstCasting.ground(level,offset,0.7,2,owner);
                if(floor==null)continue;
                // Reject lateral sites behind a wall rather than conjuring a spear through it.
                if(block(level,goal.add(0,0.2,0),floor.add(0,0.2,0)).getType()!=HitResult.Type.MISS)continue;
                origin=sky(level,floor,5);velocity=new Vec3(0,-0.9,0);
                if(origin.y-floor.y<0.7)continue;
            }
            CandySpellEntity actor=spawn(role,mode,i,origin,velocity);
            if(actor!=null&&(id.equals("peppermint_guard")||id.equals("peppermint_hunt")))actor.orbit(2.5F,0.18F,(float)(i*Math.PI*2/5));
        }
        if(id.equals("peppermint_guard")||id.equals("peppermint_hunt"))setPos(owner.position().add(0,owner.getBbHeight()*0.6,0));
    }
    private void actorTick(ServerLevel level,LivingEntity owner,int age){
        flightTicks++;
        switch(role){
            case BALL -> ball(level,owner);
            case MINT -> mint(level,owner,age);
            case SPEAR -> spear(level,owner);
            default -> finish();
        }
    }
    private void ball(ServerLevel level,LivingEntity owner){
        String id=spec().id();
        if(id.equals("popping_cluster")){
            visualValue("Fuse",flightTicks);
            if(flightTicks>=CandyFirstRules.fuseTick(index)){burst(level,owner,position(),spec().radius(),1,false);return;}
            setDeltaMovement(getDeltaMovement().add(0,-0.035,0));
        }
        sweep(level,owner,getDeltaMovement(),true);
    }
    private void mint(ServerLevel level,LivingEntity owner,int age){
        String id=spec().id();
        if(id.equals("peppermint_wheels")){
            setDeltaMovement(getDeltaMovement().add(0,-0.05,0));sweep(level,owner,getDeltaMovement(),false);return;
        }
        if(id.equals("peppermint_guard")||age<20+index*6){
            double phase=visual().getFloat("Phase")+age*visual().getFloat("AngularSpeed");
            double radius=visual().getFloat("Radius");Vec3 center=owner.position().add(0,owner.getBbHeight()*0.6,0);
            Vec3 desired=center.add(Math.cos(phase)*radius,0,Math.sin(phase)*radius);
            Vec3 next=block(level,center,desired).getLocation();
            // The orbit itself is swept: server damage uses the displayed blade path.
            collideEntities(level,owner,position(),next,0.35,false);setPos(next);
            visualValue("Orbiting",1);return;
        }
        if(!locked){locked=true;seek(level,owner);visualValue("Orbiting",0);}
        LivingEntity enemy=resolve(level,target);
        if(enemy!=null&&CandyCombat.enemy(owner,enemy)&&CandyCombat.visible(level,position(),enemy)){
            Vec3 aim=enemy.getBoundingBox().getCenter().subtract(position()).normalize().scale(0.9);
            setDeltaMovement(getDeltaMovement().scale(0.4).add(aim.scale(0.6)));
        }else if(getDeltaMovement().lengthSqr()<0.02)setDeltaMovement(direction.scale(0.9));
        sweep(level,owner,getDeltaMovement(),false);
    }
    private void spear(ServerLevel level,LivingEntity owner){
        sweep(level,owner,getDeltaMovement(),false);
    }
    private void rocket(ServerLevel level,LivingEntity owner){
        flightTicks++;visualValue("RocketAge",flightTicks);
        if(flightTicks>4){
            Vec3 wanted=goal.subtract(position());
            if(wanted.lengthSqr()<0.5){burst(level,owner,position(),spec().radius(),1,false);return;}
            setDeltaMovement(getDeltaMovement().scale(0.65).add(wanted.normalize().scale(0.4)));
        }
        sweep(level,owner,getDeltaMovement(),false);
    }
    private void sweep(ServerLevel level,LivingEntity owner,Vec3 velocity,boolean bounce){
        Vec3 from=position(),to=from.add(velocity);
        BlockHitResult wall=block(level,from,to);Vec3 stop=wall.getLocation();
        LivingEntity victim=collideEntities(level,owner,from,stop,role==MINT?0.35:0.16,true);
        if(victim!=null){
            Vec3 hit=victim.getBoundingBox().inflate(0.18).clip(from,stop).orElse(stop);
            setPos(hit);contact(level,owner,victim);return;
        }
        travelled+=from.distanceTo(stop);setPos(stop);
        if(travelled>=spec().range()){if(spec().id().equals("popping_cluster"))setDeltaMovement(Vec3.ZERO);else finish();return;}
        if(wall.getType()==HitResult.Type.MISS)return;
        Vec3 normal=Vec3.atLowerCornerOf(wall.getDirection().getNormal());setPos(stop.add(normal.scale(0.03)));
        pixelNormal(normal);
        if(role==ROCKET){burst(level,owner,position(),spec().radius(),1,false);return;}
        if(role==SPEAR){landing(level,owner);return;}
        if(spec().id().equals("peppermint_wheels")){
            if(wall.getDirection()==Direction.UP){setDeltaMovement(new Vec3(velocity.x,0.16,velocity.z));impact();return;}
            Vec3 step=CandyFirstCasting.ground(level,from.add(velocity.x,0,velocity.z),1.05,0.7,owner);
            if(step!=null&&step.y-from.y<=0.75&&block(level,from.add(0,0.8,0),step.add(0,0.8,0)).getType()==HitResult.Type.MISS){
                setPos(step.add(0,0.35,0));setDeltaMovement(new Vec3(velocity.x,0.12,velocity.z));return;
            }
            impact();finish();return;
        }
        if(bounce&&bounces-->0){
            double damp=spec().id().equals("popping_cluster")?0.6:1;
            setDeltaMovement(velocity.subtract(normal.scale(2*velocity.dot(normal))).scale(damp));
            CandySounds.bounce(level,position());impact();
            if(spec().id().equals("sugar_hunt"))seek(level,owner);
        }else if(spec().id().equals("popping_cluster")){setDeltaMovement(Vec3.ZERO);}
        else if(spec().id().equals("crystal_split")&&!split){setDeltaMovement(velocity.subtract(normal.scale(2*velocity.dot(normal))));split(level,owner,null);}
        else{impact();finish();}
    }
    private LivingEntity collideEntities(ServerLevel level,LivingEntity owner,Vec3 from,Vec3 to,double radius,boolean firstOnly){
        List<LivingEntity> list=level.getEntitiesOfClass(LivingEntity.class,new AABB(from,to).inflate(radius+0.5),e->
                CandyCombat.enemy(owner,e)&&!visited.contains(e.getUUID())&&e.getBoundingBox().inflate(radius).clip(from,to).isPresent());
        list.sort(Comparator.comparingDouble((LivingEntity e)->e.getBoundingBox().inflate(radius).clip(from,to).orElse(to).distanceToSqr(from)).thenComparing(Entity::getUUID));
        if(firstOnly)return list.isEmpty()?null:list.get(0);
        for(LivingEntity e:list){if(hit(e,1)){impact();}}
        return null;
    }
    private void contact(ServerLevel level,LivingEntity owner,LivingEntity victim){
        String id=spec().id();
        if(role==ROCKET){burst(level,owner,position(),spec().radius(),1,false);return;}
        if(role==SPEAR){landing(level,owner);return;}
        if(id.equals("popping_cluster")){
            // Land/stick and retain a visible fuse; no contact damage before the explosion.
            visited.add(victim.getUUID());setDeltaMovement(Vec3.ZERO);return;
        }
        hit(victim,split?0.5F:1);visited.add(victim.getUUID());impact();
        if(id.equals("crystal_split")&&!split){split(level,owner,victim);return;}
        if(id.equals("sugar_hunt")&&visited.size()<4&&seek(level,owner)){
            setPos(position().add(getDeltaMovement().normalize().scale(0.25)));return;
        }
        finish();
    }
    private boolean seek(ServerLevel level,LivingEntity owner){
        LivingEntity next=CandyCombat.targets(level,owner,position(),8).stream().filter(e->!visited.contains(e.getUUID())).findFirst().orElse(null);
        target=next==null?null:next.getUUID();
        if(next==null)return false;
        setDeltaMovement(next.getBoundingBox().getCenter().subtract(position()).normalize().scale(0.95));return true;
    }
    private void split(ServerLevel level,LivingEntity owner,LivingEntity victim){
        split=true;
        if(pixelSpell())visualValue("PixelFractured",1);
        Vec3 velocity=getDeltaMovement().lengthSqr()>0.01?getDeltaMovement().normalize():direction;
        for(int i=0;i<3;i++){
            CandySpellEntity child=controller.spawn(BALL,Mode.BALL,i,position().add(velocity.scale(0.1)),velocity.yRot((float)((i-1)*0.45)).scale(0.85));
            if(child!=null){child.split=true;if(pixelSpell())child.visualValue("PixelShard",1);if(victim!=null)child.visited.add(victim.getUUID());}
        }
        impact();finish();
    }
    private boolean hit(LivingEntity target,float multiplier){
        return CandyCombat.damage(this,target,CandyRegistry.ENTRIES.get(spec().id()).get(),spec().hitDamage(spellLevel())*multiplier);
    }
    private void landing(ServerLevel level,LivingEntity owner){
        if(spec().id().equals("soda_frostburst")){controller.burst(level,owner,controller.position(),spec().radius(),1,true);impact();finish();}
        else burst(level,owner,position(),2.4,1,true);
    }
    private void burst(ServerLevel level,LivingEntity owner,Vec3 center,double radius,float scale,boolean freeze){
        if(detonated)return;detonated=true;setPos(center);visualValue("BurstRadius",(float)radius);
        if(pixelSpell())visualValue("PixelDetonated",1);
        for(LivingEntity e:CandyCombat.targets(level,owner,center,radius)){
            if(hit(e,scale)){
                if(freeze)chill(e);
                CandyCombat.impulse(this,e,e.position().subtract(center).normalize().scale(0.3).add(0,0.12,0));
            }
        }
        impact();finish();
    }
    private void chill(LivingEntity target){
        if(target.getType().is(CandyRegistry.CONTROL_IMMUNE))return;
        target.addEffect(new MobEffectInstance(CandyRegistry.FROSTMINT_CHILL.get(),spec().controlTicks(),0),owner());
        if(target.canFreeze())target.setTicksFrozen(Math.max(target.getTicksFrozen(),spec().controlTicks()));
    }
    private void hex(ServerLevel level,LivingEntity owner,int age){
        Vec3 next=position().add(direction.multiply(1,0,1).normalize().scale(0.13));
        Vec3 floor=CandyFirstCasting.ground(level,next,0.65,1.5,owner);
        if(floor!=null&&floor.y-position().y<=0.6&&block(level,position().add(0,0.35,0),floor.add(0,0.35,0)).getType()==HitResult.Type.MISS){setPos(floor);travelled+=0.13;}
        else if(age<80){finish();return;}
        if(CandyFirstRules.hexPulse(age)){
            for(LivingEntity e:area(level,owner,spec().radius()))if(hit(e,CandyFirstRules.hexScale(age)))CandyCombat.slow(this,e,spec().controlTicks(),0);
            impact();visualValue("FinalPulse",age==80?1:0);
        }
    }
    private void bloom(ServerLevel level,LivingEntity owner,int age){
        visualValue("Bloom",Math.max(0,age-20));
        for(int ring=0;ring<3;ring++)if(age==CandyFirstRules.bloomTick(ring)){
            double outer=spec().radius()*(ring+1)/3.0;
            for(LivingEntity e:area(level,owner,outer)){
                double d=e.getBoundingBox().getCenter().multiply(1,0,1).distanceTo(position().multiply(1,0,1));
                if(CandyFirstRules.bloomBand(spec().radius(),d,ring)&&hit(e,1))CandyCombat.slow(this,e,spec().controlTicks(),0);
            }
            impact();visualValue("BloomRing",ring);
        }
        if(age>=32)finish();
    }
    private void reaction(ServerLevel level,LivingEntity owner,int age){
        visualValue("Reaction",age);
        if(age==18){Vec3 top=sky(level,position(),4);if(top.y-position().y<0.7){finish();return;}spawn(SPEAR,Mode.PHANTOM,0,top,new Vec3(0,-0.65,0));}
    }
    private void caramel(ServerLevel level,LivingEntity owner,int age){
        double radius=CandyFirstRules.growingRadius(spec().radius(),age);visualValue("Radius",(float)radius);
        for(LivingEntity e:area(level,owner,radius)){
            if(!e.getType().is(CandyRegistry.CONTROL_IMMUNE))e.addEffect(new MobEffectInstance(CandyRegistry.CARAMEL_STICKY.get(),2,0,false,true,true),owner);
            if(age%spec().hitInterval()==0&&hit(e,1))impact();
        }
    }
    private List<LivingEntity> area(ServerLevel level,LivingEntity owner,double radius){
        return level.getEntitiesOfClass(LivingEntity.class,new AABB(position().add(-radius,-0.3,-radius),position().add(radius,2.5,radius)),e->
                CandyCombat.enemy(owner,e)&&Math.abs(e.getY()-getY())<=1.25&&e.position().multiply(1,0,1).distanceToSqr(position().multiply(1,0,1))<=radius*radius
                        &&CandyCombat.visible(level,position().add(0,0.2,0),e)).stream()
                .sorted(Comparator.comparingDouble((LivingEntity e)->e.distanceToSqr(this)).thenComparing(Entity::getUUID)).limit(32).toList();
    }
    private BlockHitResult block(ServerLevel level,Vec3 from,Vec3 to){return level.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));}
    private Vec3 sky(ServerLevel level,Vec3 floor,double height){
        BlockHitResult ceiling=block(level,floor.add(0,0.15,0),floor.add(0,height,0));
        return ceiling.getType()==HitResult.Type.MISS?ceiling.getLocation():ceiling.getLocation().add(0,-0.3,0);
    }
    private static LivingEntity resolve(ServerLevel level,UUID id){return id!=null&&level.getEntity(id) instanceof LivingEntity e&&e.isAlive()?e:null;}
}
