package cn.wanyiwige.illusion;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.UUID;
public final class TrapCaster extends AbstractSpellCastingMob implements IMagicSummon {
    public UUID owner;public int life=60;private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    public TrapCaster(EntityType<? extends TrapCaster> type,Level level){super(type,level);setInvisible(true);setInvulnerable(true);setNoGravity(true);setSilent(true);noPhysics=true;}
    public static AttributeSupplier.Builder attributes(){return createMobAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.MOVEMENT_SPEED,0).add(Attributes.ATTACK_DAMAGE,0);}
    @Override public void tick(){super.tick();if(!level().isClientSide&&tickCount>life)discard();}
    @Override protected void registerGoals(){}
    @Override public boolean isNoAi(){return true;}
    @Override public Entity getSummoner(){return owner==null?null:level().getPlayerByUUID(owner);}
    @Override public boolean isAlliedTo(Entity e){Entity p=getSummoner();return e!=null&&(e==p||p!=null&&p.isAlliedTo(e)||super.isAlliedTo(e));}
    @Override public void onUnSummon(){discard();}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override protected boolean shouldDropLoot(){return false;}
    @Override public boolean shouldDropExperience(){return false;}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);if(owner!=null)tag.putUUID("TrapOwner",owner);tag.putInt("Life",Math.max(0,life-tickCount));}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);owner=tag.hasUUID("TrapOwner")?tag.getUUID("TrapOwner"):null;life=tag.getInt("Life");}
}
