package cn.wanyiwige.twilight;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import twilightforest.entity.EnforcedHomePoint;
import org.joml.Vector3f;

public final class TwilightSoul extends Entity implements AntiMagicSusceptible {
    private static final EntityDataAccessor<Vector3f> HOME=SynchedEntityData.defineId(TwilightSoul.class,EntityDataSerializers.VECTOR3);
    public TwilightSoul(EntityType<?> type,Level world){super(type,world);noPhysics=true;setNoGravity(true);}
    public String kind(){return ForgeRegistries.ENTITY_TYPES.getKey(getType()).getPath();}
    @Override protected void defineSynchedData(){entityData.define(HOME,new Vector3f());}
    public static void spawn(Level world,String kind,Vec3 position,Vec3 home){if(world.isClientSide)return;TwilightSoul soul=TwilightEntities.SOULS.get(kind).get().create(world);soul.setPos(position);soul.entityData.set(HOME,home.toVector3f());world.addFreshEntity(soul);}
    @Override public void tick(){super.tick();Vec3 home=new Vec3(entityData.get(HOME)),to=home.subtract(position());if(to.lengthSqr()>.01)setPos(position().add(to.normalize().scale(Math.min(.12,to.length()))));if(level().isClientSide)level().addParticle(ParticleTypes.SOUL_FIRE_FLAME,getX(),getY()+.25,getZ(),0,.03,0);}
    @Override public boolean isPickable(){return true;}
    @Override public InteractionResult interactAt(Player player,Vec3 point,InteractionHand hand){
        if(new Vec3(entityData.get(HOME)).distanceToSqr(position())>.01)return InteractionResult.PASS;
        String key=switch(kind()){case "snow_queen_soul"->"aurora_phylactery";case "urghast_soul"->"carminite_heart";case "hydra_soul"->"regenerating_chop";default->"lich_phylactery";};
        ItemStack held=player.getItemInHand(hand);var item=ForgeRegistries.ITEMS.getValue(TwilightFoundation.id(key));if(!held.is(item))return InteractionResult.PASS;
        if(level() instanceof ServerLevel server){
            Mob boss;
            if(kind().equals("lich_soul")&&player.hasEffect(MobEffects.BAD_OMEN)){TwilightCaster lich=TwilightEntities.OMINOUS_LICH.get().create(server);lich.setRespawn(position());boss=lich;}
            else{String id=switch(kind()){case "snow_queen_soul"->"snow_queen";case "urghast_soul"->"ur_ghast";case "hydra_soul"->"hydra";default->"lich";};EntityType<?> type=ForgeRegistries.ENTITY_TYPES.getValue(new net.minecraft.resources.ResourceLocation("twilightforest",id));boss=(Mob)type.create(server);}
            if(boss==null)return InteractionResult.FAIL;
            boss.moveTo(getX(),getY(),getZ(),player.getYRot()+180,0);boss.finalizeSpawn(server,server.getCurrentDifficultyAt(blockPosition()),MobSpawnType.TRIGGERED,null,null);boss.setPersistenceRequired();
            if(boss instanceof EnforcedHomePoint home)home.setRestrictionPoint(GlobalPos.of(server.dimension(),blockPosition()));
            if(server.addFreshEntity(boss)){if(!player.isCreative())held.shrink(1);TwilightVisuals.burst(server,position(),TwilightParticles.EXANIMATED_FIRE.get(),50,.3);discard();}
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
    @Override public void onAntiMagic(MagicData data){if(level() instanceof ServerLevel server){TwilightVisuals.burst(server,position(),TwilightParticles.EXANIMATED_FIRE.get(),50,.3);playSound(net.minecraft.sounds.SoundEvents.ELDER_GUARDIAN_CURSE,2,1);}discard();}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
    @Override protected void addAdditionalSaveData(CompoundTag tag){Vector3f home=entityData.get(HOME);tag.putFloat("HomeX",home.x);tag.putFloat("HomeY",home.y);tag.putFloat("HomeZ",home.z);}
    @Override protected void readAdditionalSaveData(CompoundTag tag){entityData.set(HOME,new Vector3f(tag.getFloat("HomeX"),tag.getFloat("HomeY"),tag.getFloat("HomeZ")));}
}
