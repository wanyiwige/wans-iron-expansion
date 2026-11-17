package cn.wanyiwige.illusion;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;
public final class IllusionBlockEntity extends BlockEntity {
    public UUID owner;public double ownerPower=1;public long expiry;public ResourceLocation spell;public int rank=1,cooldown;private CompoundTag attributes=new CompoundTag();
    public IllusionBlockEntity(BlockPos pos,BlockState state){super(IllusionContent.BLOCK_ENTITY.get(),pos,state);}
    public static boolean spellTag(AbstractSpell spell,String name){return spell!=null&&SpellRegistry.REGISTRY.get().getHolder(spell.getSpellResource()).map(h->h.is(TagKey.create(SpellRegistry.SPELL_REGISTRY_KEY,IllusionContent.id(name)))).orElse(false);}
    public static boolean allowed(AbstractSpell spell){return spell!=null&&spell!=SpellRegistry.none()&&spell.getCastType()!=CastType.CONTINUOUS&&!spellTag(spell,"trap_spell_blacklist");}
    public void captureAttributes(Player player){for(var a:ForgeRegistries.ATTRIBUTES){var instance=player.getAttribute(a);if(instance!=null)attributes.putDouble(ForgeRegistries.ATTRIBUTES.getKey(a).toString(),instance.getValue());}}
    public static void tick(Level level,BlockPos pos,BlockState state,IllusionBlockEntity b){
        if(state.is(IllusionContent.WALL.get())){if(b.expiry==0){b.expiry=level.getGameTime()+600;b.setChanged();}if(level.getGameTime()>=b.expiry)level.removeBlock(pos,false);return;}
        if(b.cooldown>0){b.cooldown--;if(b.cooldown==0)b.setChanged();return;}
        if(level.getGameTime()%5!=0||b.spell==null||b.owner==null||!(level instanceof ServerLevel server))return;
        AbstractSpell spell=SpellRegistry.getSpell(b.spell);if(!allowed(spell))return;
        Player owner=server.getPlayerByUUID(b.owner);if(owner==null||!owner.isAlive())return;
        Vec3 start=Vec3.atCenterOf(pos),end=start.add(Vec3.atLowerCornerOf(state.getValue(IllusionBlock.FACING).getNormal()).scale(10));
        var block=level.clip(new net.minecraft.world.level.ClipContext(start,end,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,owner));end=block.getLocation();
        boolean support=spellTag(spell,"is_support_spell");final Vec3 rayEnd=end;
        var targets=level.getEntitiesOfClass(LivingEntity.class,new AABB(start,end).inflate(.4),e->{if(!e.isAlive()||e instanceof TrapCaster)return false;boolean ally=e==owner||owner.isAlliedTo(e)||e.isAlliedTo(owner)||(e instanceof OwnableEntity pet&&b.owner.equals(pet.getOwnerUUID()));return support==ally&&e.getBoundingBox().inflate(.4).clip(start,rayEnd).isPresent();});
        targets.sort(Comparator.comparingDouble(e->e.distanceToSqr(start)));
        if(targets.isEmpty())return;LivingEntity victim=targets.get(0);
        TrapCaster caster=IllusionContent.TRAP_CASTER.get().create(server);if(caster==null)return;
        caster.owner=b.owner;caster.life=Math.max(60,spell.getCastTime(b.rank)+40);caster.setPos(start);Vec3 direction=victim.getEyePosition().subtract(start).normalize();caster.setYRot((float)(Math.toDegrees(Math.atan2(direction.z,direction.x))-90));caster.setXRot((float)-Math.toDegrees(Math.asin(direction.y)));caster.yHeadRot=caster.getYRot();
        for(String key:b.attributes.getAllKeys()){var a=ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(key));if(a!=null&&caster.getAttribute(a)!=null)caster.getAttribute(a).setBaseValue(b.attributes.getDouble(key));}
        caster.setTarget(victim);caster.getPersistentData().putUUID(IllusionEvents.OWNER_KEY,b.owner);server.addFreshEntity(caster);caster.getMagicData().setAdditionalCastData(new TargetEntityCastData(victim));
        java.util.UUID previous=IllusionEvents.TRAP_OWNER.get();IllusionEvents.TRAP_OWNER.set(b.owner);
        try{spell.onCast(server,b.rank,caster,CastSource.NONE,caster.getMagicData());}finally{if(previous==null)IllusionEvents.TRAP_OWNER.remove();else IllusionEvents.TRAP_OWNER.set(previous);}
        b.cooldown=Math.max(5,spell.getSpellCooldown()*5);b.setChanged();level.sendBlockUpdated(pos,state,state,3);
    }
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);if(owner!=null)tag.putUUID("Owner",owner);tag.putDouble("OwnerPower",ownerPower);tag.putLong("Expiry",expiry);if(spell!=null)tag.putString("Spell",spell.toString());tag.putInt("Rank",rank);tag.putInt("Cooldown",cooldown);tag.put("Attributes",attributes);}
    @Override public void load(CompoundTag tag){super.load(tag);owner=tag.hasUUID("Owner")?tag.getUUID("Owner"):null;ownerPower=tag.getDouble("OwnerPower");expiry=tag.getLong("Expiry");spell=ResourceLocation.tryParse(tag.getString("Spell"));rank=Math.max(1,Math.min(100,tag.getInt("Rank")));cooldown=Math.max(0,tag.getInt("Cooldown"));attributes=tag.getCompound("Attributes");}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
