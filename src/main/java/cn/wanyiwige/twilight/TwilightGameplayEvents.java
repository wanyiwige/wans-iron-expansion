package cn.wanyiwige.twilight;

import io.redspace.ironsspellbooks.api.events.*;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.*;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.*;

public final class TwilightGameplayEvents {
    private static final UUID CROUCH_SPEED=UUID.fromString("c6f1f05b-70db-4aec-83f5-332467c61b8e");
    private static final ThreadLocal<Boolean> REFLECTING=ThreadLocal.withInitial(()->false);
    public static boolean equipped(LivingEntity entity,String id){return CuriosApi.getCuriosInventory(entity).map(inv->inv.findFirstCurio(stack->stack.is(TwilightEquipment.get(id))).isPresent()).orElse(false);}
    private static boolean ready(Player player,String id){return !player.getCooldowns().isOnCooldown(TwilightEquipment.get(id));}
    private static void cooldown(Player player,String id,int ticks){player.getCooldowns().addCooldown(TwilightEquipment.get(id),ticks);}
    private static boolean school(net.minecraft.world.damagesource.DamageSource source,String name){return source.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,new net.minecraft.resources.ResourceLocation("irons_spellbooks",name+"_magic")));}
    @SubscribeEvent public static void incoming(LivingHurtEvent event){
        LivingEntity target=event.getEntity();Entity attacker=event.getSource().getEntity();
        if(target instanceof Player player){
            if(equipped(player,"ironwood_spellbook"))event.setAmount(event.getAmount()*.85F);
            if(equipped(player,"naga_mantle")&&(event.getSource().is(DamageTypeTags.IS_FIRE)||school(event.getSource(),"fire"))){player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,100));player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,100));}
        }
        if(!(attacker instanceof LivingEntity caster))return;
        if(event.getSource() instanceof SpellDamageSource){
            if(event.getSource().getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile && caster.getAttribute(TwilightAttributes.MAGIC_PROJECTILE_DAMAGE.get())!=null)
                event.setAmount(event.getAmount()*(float)caster.getAttributeValue(TwilightAttributes.MAGIC_PROJECTILE_DAMAGE.get()));
            if(caster.getMainHandItem().is(TwilightEquipment.get("steeleaf_staff"))&&caster.getRandom().nextFloat()<.05F)
                event.setAmount(event.getAmount()*(float)caster.getAttributeValue(TwilightAttributes.MAGIC_CRIT_DAMAGE.get()));
        }
        if(!(caster instanceof Player player))return;
        boolean melee=event.getSource().is(DamageTypes.PLAYER_ATTACK);
        if(melee&&equipped(player,"fiery_ring")&&target.isOnFire())event.setAmount(event.getAmount()*1.25F);
        if(melee&&player.isSprinting()&&player.getMainHandItem().is(TwilightEquipment.get("maze_butcher")))event.setAmount(event.getAmount()+10);
        if(event.getSource() instanceof SpellDamageSource){
            if(equipped(player,"knightmetal_spellbook")&&target.getArmorValue()>0)event.setAmount(event.getAmount()*1.2F);
            if(player.getMainHandItem().is(TwilightEquipment.get("knightmetal_staff"))&&target.getArmorValue()==0)event.setAmount(event.getAmount()*1.2F);
            if(equipped(player,"steeleaf_sheath")&&ready(player,"steeleaf_sheath")&&school(event.getSource(),"nature")&&player.getRandom().nextInt(5)==1){event.setAmount(event.getAmount()*1.5F);cooldown(player,"steeleaf_sheath",100);}
            if(equipped(player,"exanimated_spell_book")&&ready(player,"exanimated_spell_book")){
                if(school(event.getSource(),"blood")||school(event.getSource(),"ender")){target.addEffect(new MobEffectInstance(TwilightFoundation.OMINOUS_BURN.get(),100));cooldown(player,"exanimated_spell_book",100);}
                if(school(event.getSource(),"fire")||school(event.getSource(),"ender")){player.heal(event.getAmount()*.25F);cooldown(player,"exanimated_spell_book",200);}
            }
        }
        if(melee&&equipped(player,"yeti_brace")&&ready(player,"yeti_brace")){
            TwilightProjectile ice=TwilightEntities.PROJECTILES.get("ice_chunk").get().create(player.level());ice.setOwner(player);ice.setDamage(5);ice.setExplosionRadius(4.5F);ice.setPos(target.position().add(0,7,0));ice.shoot(new net.minecraft.world.phys.Vec3(.05,-.85,0).normalize());player.level().addFreshEntity(ice);cooldown(player,"yeti_brace",100);
        }
    }
    @SubscribeEvent public static void penetration(SpellDamageEvent event){
        if(!(event.getSpellDamageSource().getEntity() instanceof LivingEntity caster))return;
        var penetration=caster.getAttribute(TwilightAttributes.RESIST_PENETRATION.get());var resistance=event.getEntity().getAttribute(AttributeRegistry.SPELL_RESIST.get());
        if(penetration==null||penetration.getValue()<=0||resistance==null||resistance.getValue()<=1)return;
        double multiplier=io.redspace.ironsspellbooks.damage.DamageSources.getResist(event.getEntity(),event.getSpellDamageSource().spell().getSchoolType());
        double reduced=2-multiplier,pen=1+penetration.getValue();double adjustment=2-(pen>reduced?1:reduced/pen);
        if(multiplier>0)event.setAmount((float)(event.getAmount()*adjustment/multiplier));
    }
    @SubscribeEvent public static void damaged(LivingDamageEvent event){
        LivingEntity target=event.getEntity();Entity direct=event.getSource().getDirectEntity();Entity attacker=event.getSource().getEntity();
        if(attacker instanceof Player player && event.getSource() instanceof SpellDamageSource){
            if(equipped(player,"fiery_spell_book"))target.setSecondsOnFire(2);
            if(equipped(player,"snow_queen_spell_book")&&ready(player,"snow_queen_spell_book")&&school(event.getSource(),"ice")){
                target.setTicksFrozen(Math.max(100,target.getTicksFrozen()));target.addEffect(new MobEffectInstance(twilightforest.init.TFMobEffects.FROSTY.get(),100,2));cooldown(player,"snow_queen_spell_book",200);
            }
        }
        if(direct instanceof IMagicSummon summon&&summon.getSummoner() instanceof Player owner&&equipped(owner,"ominous_blazer"))target.addEffect(new MobEffectInstance(TwilightFoundation.OMINOUS_BURN.get(),60));
        if(attacker instanceof LivingEntity caster&&event.getSource() instanceof SpellDamageSource){
            double steal=caster.getAttribute(TwilightAttributes.MANA_STEAL.get())==null?0:caster.getAttributeValue(TwilightAttributes.MANA_STEAL.get());
            if(steal>0)MagicData.getPlayerMagicData(caster).addMana((float)(event.getAmount()*steal));
            double rend=caster.getAttribute(TwilightAttributes.MANA_REND.get())==null?0:caster.getAttributeValue(TwilightAttributes.MANA_REND.get());
            if(rend>0)MagicData.getPlayerMagicData(target).addMana((float)(-event.getAmount()*rend));
        }
        if(!REFLECTING.get()&&target instanceof Player player&&equipped(player,"thorn_rose_ring")&&direct instanceof LivingEntity enemy&&enemy!=target){
            try{REFLECTING.set(true);enemy.hurt(enemy.damageSources().thorns(player),4);}finally{REFLECTING.set(false);}
        }
    }
    @SubscribeEvent public static void cast(SpellOnCastEvent event){
        Player player=event.getEntity();if(player.level().isClientSide)return;
        if(event.getSchoolType()==SchoolRegistry.LIGHTNING.get()&&equipped(player,"carminite_mantle")&&ready(player,"carminite_mantle")){
            player.addEffect(new MobEffectInstance(MobEffectRegistry.TRUE_INVISIBILITY.get(),60));player.addEffect(new MobEffectInstance(MobEffectRegistry.CHARGED.get(),60));cooldown(player,"carminite_mantle",80);
        }
    }
    @SubscribeEvent public static void spellLevel(ModifySpellLevelEvent event){
        LivingEntity caster=event.getEntity();if(caster==null)return;
        Item main=caster.getMainHandItem().getItem();String spell=event.getSpell().getSpellResource().toString();
        if(main==TwilightEquipment.get("lich_greatsword")&&spell.startsWith(TwilightFoundation.NAMESPACE+":exanimated_"))event.addLevels(1);
        if(main==TwilightEquipment.get("ultimate_scepter")&&Set.of("twilight_bolt","life_sacrifice","fortifying_shields","summon_loyal_zombie").contains(event.getSpell().getSpellName())&&event.getSpell().getSpellResource().getNamespace().equals(TwilightFoundation.NAMESPACE))event.addLevels(1);
    }
    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent event){
        LivingEntity entity=event.getEntity();if(entity.level().isClientSide)return;
        if(entity instanceof Player player){
            var movement=player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            boolean crouched=player.isShiftKeyDown()&&equipped(player,"carminite_mantle");
            if(crouched&&movement.getModifier(CROUCH_SPEED)==null)movement.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(CROUCH_SPEED,"Twilight crouch speed",.15,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_BASE));
            if(!crouched&&movement.getModifier(CROUCH_SPEED)!=null)movement.removeModifier(CROUCH_SPEED);
            int fire=player.getRemainingFireTicks(),previous=player.getPersistentData().getInt("TwilightPreviousFire");
            if(fire>0&&fire>previous&&equipped(player,"naga_mantle")){fire=(int)Math.ceil(fire*.85);player.setRemainingFireTicks(fire);}
            player.getPersistentData().putInt("TwilightPreviousFire",Math.max(0,fire));
        }
        if(entity instanceof Player player && entity.tickCount%20==0){
            boolean full=true;for(EquipmentSlot slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)){
                if(!(player.getItemBySlot(slot).getItem() instanceof TwilightEquipment.Armor armor)||!armor.set.equals("fiery_mage")){full=false;break;}
            }
            if(full&&!player.hasEffect(TwilightFoundation.HYDRA_REVENGE.get()))player.addEffect(new MobEffectInstance(TwilightFoundation.HYDRA_REVENGE.get(),200,0,false,false,false));
        }
    }
    private TwilightGameplayEvents(){}
}
