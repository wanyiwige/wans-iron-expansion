package cn.wanyiwige.alexcavesspellsbook.hazen;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.tags.DamageTypeTags;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.registries.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import java.util.*;

public final class HazenAddedArmorEffects {
    private static final String NS="alex_caves_spellsbook";
    private static final DeferredRegister<Attribute> ATTR=DeferredRegister.create(ForgeRegistries.ATTRIBUTES,NS);
    public static final RegistryObject<Attribute> MINING=ATTR.register("hazen_added_mining_efficiency",()->new RangedAttribute("attribute."+NS+".hazen_added_mining_efficiency",0,0,1024).setSyncable(true));
    public static final RegistryObject<Attribute> SUBMERGED=ATTR.register("hazen_added_submerged_mining_speed",()->new RangedAttribute("attribute."+NS+".hazen_added_submerged_mining_speed",1,0,1024).setSyncable(true));
    public static final RegistryObject<Attribute> ARROW=ATTR.register("hazen_added_arrow_damage",()->new RangedAttribute("attribute."+NS+".hazen_added_arrow_damage",1,0,1024).setSyncable(true));
    private static final DeferredRegister<MobEffect> EFFECTS=DeferredRegister.create(ForgeRegistries.MOB_EFFECTS,NS);
    public static final Map<String,RegistryObject<MobEffect>> BUFFS=new LinkedHashMap<>();
    static{
        for(String name:List.of("MAGE_SET_BONUS","ARCHER_SET_BONUS","SWORDMASTER_SET_BONUS","TYRANTS_GRACE_EFFECT","TYROS_SOUL_STATE","AWAKENING","REVIVE_COOLDOWN"))
            BUFFS.put(name,EFFECTS.register("hazen_added_"+name.toLowerCase(Locale.ROOT),()->buff(name)));
    }
    private static class Effect extends MobEffect{Effect(){super(MobEffectCategory.BENEFICIAL,0xfbb741);}}
    private static MobEffect buff(String name){
        var e=new Effect();
        switch(name){
            case "MAGE_SET_BONUS"->{bonus(e,name,AttributeRegistry.CASTING_MOVESPEED.get(),.15,false);bonus(e,name,AttributeRegistry.SPELL_RESIST.get(),.1,false);}
            case "ARCHER_SET_BONUS"->{bonus(e,name,Attributes.MOVEMENT_SPEED,.15,false);bonus(e,name,ARROW.get(),.1,false);}
            case "SWORDMASTER_SET_BONUS"->bonus(e,name,Attributes.ATTACK_SPEED,.15,false);
            case "TYRANTS_GRACE_EFFECT"->{bonus(e,name,Attributes.ATTACK_DAMAGE,1.2,true);bonus(e,name,AttributeRegistry.CASTING_MOVESPEED.get(),.2,false);bonus(e,name,AttributeRegistry.FIRE_SPELL_POWER.get(),.144,false);bonus(e,name,Attributes.ATTACK_SPEED,1,true);bonus(e,name,ARROW.get(),1,false);}
        }return e;
    }
    private static void bonus(MobEffect e,String name,Attribute a,double amount,boolean addition){e.addAttributeModifier(a,UUID.nameUUIDFromBytes((NS+"/added/"+name+"/"+a.getDescriptionId()).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString(),amount,addition?AttributeModifier.Operation.ADDITION:AttributeModifier.Operation.MULTIPLY_BASE);}
    public static void register(IEventBus bus){
        ATTR.register(bus);EFFECTS.register(bus);
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeModificationEvent e)->e.getTypes().forEach(t->{e.add(t,MINING.get());e.add(t,SUBMERGED.get());e.add(t,ARROW.get());}));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(HazenAddedArmorEffects.class);
        HazenAddedArmorAbility.register();
    }
    public static boolean full(LivingEntity entity,HazenAddedArmor.Spec spec){
        if(spec.required_pieces().isEmpty())return false;
        for(var piece:spec.required_pieces().entrySet()){
            var stack=entity.getItemBySlot(ArmorItem.Type.valueOf(piece.getKey()).getSlot());
            if(!(stack.getItem() instanceof HazenAddedArmor.Armor a)||!a.enabled()||!a.spec.source_class().endsWith("."+piece.getValue()))return false;
        }return true;
    }
    public static boolean family(LivingEntity e,String family){
        for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))
            if(!(e.getItemBySlot(slot).getItem() instanceof HazenAddedArmor.Armor a)||!a.enabled()||!a.spec.family().equals(family))return false;
        return true;
    }
    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent event){
        var entity=event.getEntity();if(entity.level().isClientSide||!(entity instanceof Player))return;
        var done=new HashSet<String>();
        for(var stack:entity.getArmorSlots())if(stack.getItem() instanceof HazenAddedArmor.Armor armor&&full(entity,armor.spec)){
            for(var row:armor.spec.applied_effects()){
                if(!done.add(row.id()))continue;
                MobEffect effect=row.id().startsWith("MobEffects.")?vanilla(row.id()):BUFFS.get(row.id().substring(row.id().lastIndexOf('.')+1)).get();
                if(!entity.hasEffect(effect))entity.addEffect(new MobEffectInstance(effect,row.duration(),row.amplifier(),false,false,false));
            }
        }
    }
    private static MobEffect vanilla(String id){return switch(id){case "MobEffects.NIGHT_VISION"->MobEffects.NIGHT_VISION;case "MobEffects.MOVEMENT_SPEED"->MobEffects.MOVEMENT_SPEED;default->throw new IllegalArgumentException(id);};}
    @SubscribeEvent public static void breakSpeed(PlayerEvent.BreakSpeed e){
        var p=e.getEntity();if(e.getNewSpeed()>1)e.setNewSpeed((float)(e.getNewSpeed()+p.getAttributeValue(MINING.get())));
        if(p.isEyeInFluid(net.minecraft.tags.FluidTags.WATER))e.setNewSpeed((float)(e.getNewSpeed()*p.getAttributeValue(SUBMERGED.get())));
    }
    @SubscribeEvent public static void hurt(LivingHurtEvent event){
        if(family(event.getEntity(),"garments_of_the_first_flamebearer")&&event.getSource().is(DamageTypeTags.IS_FIRE)){event.getEntity().clearFire();event.setCanceled(true);return;}
        if(event.getSource().getDirectEntity() instanceof AbstractArrow&&event.getSource().getEntity() instanceof LivingEntity attacker)
            event.setAmount((float)(event.getAmount()*attacker.getAttributeValue(ARROW.get())));
        if(event.getSource().getEntity() instanceof Player attacker&&family(attacker,"garments_of_the_first_flamebearer")
            &&event.getSource().is(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,new ResourceLocation("irons_spellbooks:fire_magic")))
            &&!attacker.getCooldowns().isOnCooldown(attacker.getItemBySlot(EquipmentSlot.LEGS).getItem()))
            io.redspace.ironsspellbooks.effect.ImmolateEffect.addImmolateStack(event.getEntity(),attacker);
    }
    @SubscribeEvent public static void damage(LivingDamageEvent event){
        if(event.getAmount()<=0||!(event.getSource().getEntity() instanceof Player attacker)||!family(attacker,"garments_of_the_first_flamebearer"))return;
        var target=event.getEntity();var chest=attacker.getItemBySlot(EquipmentSlot.CHEST).getItem();
        if(!attacker.getCooldowns().isOnCooldown(chest)){target.setSecondsOnFire(5);attacker.getCooldowns().addCooldown(chest,20);}
        var head=attacker.getItemBySlot(EquipmentSlot.HEAD).getItem();
        if(!attacker.getCooldowns().isOnCooldown(head)){
            double power=attacker.getAttributeValue(AttributeRegistry.FIRE_SPELL_POWER.get());int amplifier=Math.max(0,(int)Math.floor(power<10?power:power/100)-1);
            boolean soul=attacker.hasEffect(BUFFS.get("TYROS_SOUL_STATE").get());
            var effect=ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("irons_spellbooks",soul?"soul_burn":"rend"));
            if(effect!=null)target.addEffect(new MobEffectInstance(effect,100,amplifier));attacker.getCooldowns().addCooldown(head,200);
        }
    }
    @SubscribeEvent public static void death(LivingDeathEvent event){
        var e=event.getEntity();var awaken=BUFFS.get("AWAKENING").get();var cooldown=BUFFS.get("REVIVE_COOLDOWN").get();
        if(!event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)&&family(e,"dead_king")&&!e.hasEffect(awaken)&&!e.hasEffect(cooldown)){
            e.setHealth(10);e.addEffect(new MobEffectInstance(awaken,3600,0,false,true,true));event.setCanceled(true);
            e.level().playSound(null,e.blockPosition(),io.redspace.ironsspellbooks.registries.SoundRegistry.DEAD_KING_EXPLODE.get(),net.minecraft.sounds.SoundSource.PLAYERS,1.25f,1);
        }
    }
    @SubscribeEvent public static void expiry(MobEffectEvent.Expired event){if(event.getEffectInstance()!=null&&event.getEffectInstance().getEffect()==BUFFS.get("AWAKENING").get())event.getEntity().addEffect(new MobEffectInstance(BUFFS.get("REVIVE_COOLDOWN").get(),12000,0,false,false,true));}
    @SubscribeEvent public static void remove(MobEffectEvent.Remove event){if(event.getEffect()==BUFFS.get("REVIVE_COOLDOWN").get())event.setCanceled(true);}
    @SubscribeEvent public static void affinity(io.redspace.ironsspellbooks.api.events.ModifySpellLevelEvent event){
        var e=event.getEntity();if(e==null||event.getSpell()!=io.redspace.ironsspellbooks.api.registry.SpellRegistry.FROSTBITE_SPELL.get())return;
        for(var stack:e.getArmorSlots())if(stack.getItem() instanceof HazenAddedArmor.Armor a&&a.spec.frostbite_affinity())event.addLevels(1);
    }
    private HazenAddedArmorEffects(){}
}
