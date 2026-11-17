package cn.wanyiwige.alexcavesspellsbook.toxic;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import java.util.*;

public abstract class ToxicEffectEntity extends Entity {
    private static final EntityDataAccessor<CompoundTag> STATE=SynchedEntityData.defineId(ToxicEffectEntity.class,EntityDataSerializers.COMPOUND_TAG);
    protected final ToxicSpec spec;
    protected int age,rank=1,life;
    protected float power;
    private UUID ownerUUID;
    private Vec3 castOrigin=Vec3.ZERO;
    private Vec3 damageOrigin;
    protected final Set<UUID> marked=new HashSet<>();
    protected ToxicEffectEntity(EntityType<?> type,Level level,ToxicSpec spec) {super(type,level);this.spec=spec;life=spec.life;setNoGravity(true);}
    public void configure(LivingEntity owner,int rank,float power) {
        this.ownerUUID=owner.getUUID();this.rank=spec.clampLevel(rank);this.power=Float.isFinite(power)?Math.max(0,power):0;
        castOrigin=owner.getEyePosition();
        owner.getPersistentData().putUUID("WanToxicCast_"+spec.id,getUUID());
        setPos(owner.position());
        CompoundTag s=visual().copy();s.putUUID("Owner",ownerUUID);s.putInt("OwnerId",owner.getId());s.putInt("Life",life);entityData.set(STATE,s);
        setup(owner);
    }
    protected void setup(LivingEntity owner){}
    public final ToxicSpec spec(){return spec;}
    public final float power(){return power;}
    public final Vec3 damageOrigin(){return damageOrigin!=null?damageOrigin:castOrigin;}
    final void damageOrigin(Vec3 origin){damageOrigin=origin;}
    public final int visualAge(){return level().isClientSide?age:visual().getInt("Age");}
    public final UUID ownerUUID(){return ownerUUID!=null?ownerUUID:visual().hasUUID("Owner")?visual().getUUID("Owner"):null;}
    public final AbstractSpell spell(){return ToxicRegistry.ENTRIES.get(spec.id).get();}
    public final boolean isCurrentCast(LivingEntity owner){return owner.getPersistentData().hasUUID("WanToxicCast_"+spec.id)
            &&getUUID().equals(owner.getPersistentData().getUUID("WanToxicCast_"+spec.id));}
    public LivingEntity owner() {
        Entity e=level() instanceof ServerLevel s&&ownerUUID()!=null?s.getEntity(ownerUUID()):level().getEntity(visual().getInt("OwnerId"));
        return e instanceof LivingEntity l&&l.isAlive()&&l.getUUID().equals(ownerUUID())?l:null;
    }
    public CompoundTag visual(){return entityData.get(STATE);}
    protected final void entityDataState(CompoundTag state){entityData.set(STATE,state);}
    public void value(String key,int value){CompoundTag s=visual().copy();s.putInt(key,value);entityData.set(STATE,s);}
    public void vector(String key,Vec3 v){CompoundTag s=visual().copy();s.putDouble(key+"X",v.x);s.putDouble(key+"Y",v.y);s.putDouble(key+"Z",v.z);entityData.set(STATE,s);}
    public Vec3 vector(String key){CompoundTag s=visual();return new Vec3(s.getDouble(key+"X"),s.getDouble(key+"Y"),s.getDouble(key+"Z"));}
    protected void mark(LivingEntity target) {
        if(marked.size()<32&&marked.add(target.getUUID())&&owner()!=null) {
            ToxicMarks.add(target,ownerUUID(),level().getGameTime());
            ReactorWardEntity.charge((ServerLevel)level(),owner());
        }
    }
    @Override public final void tick() {
        super.tick();
        if(!(level() instanceof ServerLevel server)){age++;return;}
        LivingEntity owner=owner();if(owner==null||owner.isSpectator()||!isCurrentCast(owner)){discard();return;}
        age++;
        if(age>life){expire(owner);discard();return;}
        serverTick(server,owner);
        if((age&1)==0||age==1)value("Age",age);
    }
    protected void expire(LivingEntity owner){}
    protected abstract void serverTick(ServerLevel level,LivingEntity owner);
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){
        super.onSyncedDataUpdated(key);
        if(STATE.equals(key)&&level().isClientSide)age=Math.max(age,visual().getInt("Age"));
    }
    @Override protected void defineSynchedData(){entityData.define(STATE,new CompoundTag());}
    @Override protected void readAdditionalSaveData(CompoundTag tag){}
    @Override protected void addAdditionalSaveData(CompoundTag tag){}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
