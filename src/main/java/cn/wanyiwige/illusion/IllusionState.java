package cn.wanyiwige.illusion;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.nbt.*;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.registries.ForgeRegistries;
public final class IllusionState {
    public static CompoundTag data(Entity e){CompoundTag p=e.getPersistentData();if(!p.contains("WanIllusion",10))p.put("WanIllusion",new CompoundTag());return p.getCompound("WanIllusion");}
    public static boolean tagged(LivingEntity e,String name){return e.getType().is(TagKey.create(Registries.ENTITY_TYPE,IllusionContent.id(name)));}
    public static double attribute(LivingEntity e,net.minecraft.world.entity.ai.attributes.Attribute a){return e.getAttribute(a)==null?1:e.getAttributeValue(a);}
    public static boolean mobImmune(LivingEntity e){return tagged(e,"illusion_immune")||attribute(e,AttributeRegistry.SPELL_RESIST.get())>1.2;}
    public static boolean immune(LivingEntity target,LivingEntity caster){
        if(tagged(target,"illusion_immune"))return true;
        if(target instanceof Player)return 1-attribute(target,AttributeRegistry.SPELL_RESIST.get())>Math.max(0,attribute(caster,AttributeRegistry.EVOCATION_SPELL_POWER.get())-1)/1.5;
        return mobImmune(target);
    }
    /** Only copy appearance data; never entity inventories, passengers, commands, or identity. */
    public static CompoundTag capture(LivingEntity target){
        CompoundTag result=new CompoundTag();result.putString("Type",ForgeRegistries.ENTITY_TYPES.getKey(target.getType()).toString());
        result.putString("Name",target.getName().getString());
        if(target instanceof Player player){result.put("Profile",NbtUtils.writeGameProfile(new CompoundTag(),player.getGameProfile()));}
        else{
            CompoundTag raw=new CompoundTag(),visual=new CompoundTag();target.saveWithoutId(raw);
            for(String key:java.util.List.of("Age","Variant","Color","CollarColor","Size","Sheared","Saddle","ChestedHorse","CatType","RabbitType","VillagerData","MainGene","HiddenGene","IsBaby","AngerTime"))if(raw.contains(key))visual.put(key,raw.get(key).copy());
            result.put("Appearance",visual);
        }
        return result;
    }
    public static void disguise(LivingEntity entity,CompoundTag appearance,int duration){data(entity).put("Disguise",appearance.copy());entity.addEffect(new MobEffectInstance(IllusionContent.effect("disguised"),duration,0,false,false));sync(entity);}
    public static int mask(LivingEntity e){int n=0,i=0;for(String name:IllusionContent.EFFECT_MAP.keySet()){if(e.hasEffect(IllusionContent.effect(name)))n|=1<<i;i++;}return n;}
    public static CompoundTag snapshot(LivingEntity e){CompoundTag tag=data(e).copy();CompoundTag effects=new CompoundTag();for(String name:IllusionContent.EFFECT_MAP.keySet()){var fx=e.getEffect(IllusionContent.effect(name));if(fx!=null)effects.putInt(name,fx.getDuration());}tag.put("Effects",effects);return tag;}
    public static void sync(LivingEntity e){if(!e.level().isClientSide)IllusionNetwork.tracking(e,new IllusionNetwork.State(e.getId(),e.getUUID(),snapshot(e)));}
    public static float[] scales(LivingEntity e){
        CompoundTag d=data(e);float[] s={1,1,1};if(!e.hasEffect(IllusionContent.effect("squish"))&&!e.level().isClientSide)return s;
        int axis=d.contains("Axis")?d.getInt("Axis"):-1;float amount=d.getFloat("Amount");if(axis<0||axis>2||amount<=0||amount>=1)return s;
        amount=Math.max(.125F,amount);float compensation=(float)Math.sqrt(1/amount);java.util.Arrays.fill(s,compensation);s[axis]=amount;return s;
    }
    private IllusionState(){}
}
