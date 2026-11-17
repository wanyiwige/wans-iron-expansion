package cn.wanyiwige.alexcavesspellsbook.hazen;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import java.util.*;

@Mod.EventBusSubscriber(modid="alex_caves_spellsbook")
public final class HazenAddedArmorAudit {
    @SubscribeEvent public static void start(net.minecraftforge.event.server.ServerStartedEvent event){
        if(!Boolean.getBoolean("hazen.added.audit"))return;
        var server=event.getServer();
        try{
            var level=server.overworld();var p=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
            int items=0,effects=0;var families=new HashSet<String>();
            for(var spec:HazenAddedArmor.SPECS){
                require(spec.animation_names()!=null,"normalized animation list "+spec.id());
                for(boolean flying:new boolean[]{false,true}){
                    String clip=spec.animationClip(flying);require(clip.isEmpty()||spec.animation_names().contains(clip),"existing animation "+spec.id());
                }
                var gson=new com.google.gson.Gson();var legacy=gson.toJsonTree(spec).getAsJsonObject();legacy.remove("animation_names");
                var missing=gson.fromJson(legacy,HazenAddedArmor.Spec.class);
                require(missing.animation_names()!=null&&missing.animationClip(false).isEmpty()&&missing.animationClip(true).isEmpty(),"missing animation list regression "+spec.id());
                var item=(HazenAddedArmor.Armor)HazenAddedArmor.ITEMS.get(spec.id()).get();
                var stack=item.getDefaultInstance();require(stack.getItem()==item,"default item "+spec.id());
                var modifiers=item.getDefaultAttributeModifiers(item.getEquipmentSlot());
                require(!modifiers.isEmpty(),"attributes "+spec.id());
                for(var b:spec.resolved_attributes())if(!b.attribute().startsWith("optional:"))require(HazenAddedArmor.resolve(b.attribute())!=null,"missing required attribute "+b.attribute());
                var saved=ItemStack.of(stack.save(new net.minecraft.nbt.CompoundTag()));require(saved.getItem()==item,"NBT "+spec.id());
                if(spec.imbuable()&&item.getEquipmentSlot()==EquipmentSlot.CHEST)require(io.redspace.ironsspellbooks.api.spells.ISpellContainer.isSpellContainer(saved),"imbue "+spec.id());
                var mana=io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get();double before=p.getAttributeValue(mana);
                double expected=spec.resolved_attributes().stream().filter(b->b.attribute().equals("AttributeRegistry.MAX_MANA")&&b.operation().equals("ADD_VALUE")).mapToDouble(HazenAddedArmor.Bonus::value).sum();
                p.getAttributes().addTransientAttributeModifiers(modifiers);require(Math.abs(p.getAttributeValue(mana)-before-expected)<.001,"equipped mana "+spec.id());
                p.getAttributes().removeAttributeModifiers(modifiers);require(Math.abs(p.getAttributeValue(mana)-before)<.001,"unequip mana "+spec.id());families.add(spec.family());items++;
            }
            for(var spec:HazenAddedArmor.SPECS)if(!spec.applied_effects().isEmpty()&&!spec.required_pieces().isEmpty()){
                clear(p);
                for(var piece:spec.required_pieces().entrySet()){
                    var matching=HazenAddedArmor.SPECS.stream().filter(s->s.slot().equals(piece.getKey())&&s.source_class().endsWith("."+piece.getValue())).findFirst().orElseThrow();
                    p.setItemSlot(ArmorItem.Type.valueOf(piece.getKey()).getSlot(),HazenAddedArmor.ITEMS.get(matching.id()).get().getDefaultInstance());
                }
                require(HazenAddedArmorEffects.full(p,spec),"set identity "+spec.id());
                HazenAddedArmorEffects.tick(new net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent(p));
                for(var effect:spec.applied_effects())if(effect.id().startsWith("HnSEffects."))require(p.hasEffect(HazenAddedArmorEffects.BUFFS.get(effect.id().substring(11)).get()),"set effect "+spec.id());
                p.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);require(!HazenAddedArmorEffects.full(p,spec),"mixed set rejection "+spec.id());effects++;
            }
            clear(p);require(items==167&&families.size()==42,"inventory count");
            equip(p,"dead_king");
            var death=new net.minecraftforge.event.entity.living.LivingDeathEvent(p,p.damageSources().generic());HazenAddedArmorEffects.death(death);
            require(death.isCanceled()&&p.getHealth()==10&&p.hasEffect(HazenAddedArmorEffects.BUFFS.get("AWAKENING").get()),"dead king revival");
            var repeat=new net.minecraftforge.event.entity.living.LivingDeathEvent(p,p.damageSources().generic());HazenAddedArmorEffects.death(repeat);require(!repeat.isCanceled(),"revival awakening guard");
            HazenAddedArmorEffects.expiry(new net.minecraftforge.event.entity.living.MobEffectEvent.Expired(p,new net.minecraft.world.effect.MobEffectInstance(HazenAddedArmorEffects.BUFFS.get("AWAKENING").get(),1)));
            require(p.hasEffect(HazenAddedArmorEffects.BUFFS.get("REVIVE_COOLDOWN").get()),"revival cooldown");
            p.removeEffect(HazenAddedArmorEffects.BUFFS.get("AWAKENING").get());clear(p);
            equip(p,"garments_of_the_first_flamebearer");HazenAddedArmorAbility.activate(p);
            var soul=HazenAddedArmorEffects.BUFFS.get("TYROS_SOUL_STATE").get();var boots=p.getItemBySlot(EquipmentSlot.FEET).getItem();
            require(p.hasEffect(soul)&&p.getCooldowns().isOnCooldown(boots),"Tyros activation");
            p.removeEffect(soul);HazenAddedArmorAbility.activate(p);require(!p.hasEffect(soul),"ability cooldown guard");
            var fire=new net.minecraftforge.event.entity.living.LivingHurtEvent(p,p.damageSources().inFire(),4);HazenAddedArmorEffects.hurt(fire);require(fire.isCanceled(),"Tyros fire immunity");
            clear(p);p.getCooldowns().removeCooldown(boots);HazenAddedArmorAbility.activate(p);require(!p.hasEffect(soul),"ability requires full set");
            com.mojang.logging.LogUtils.getLogger().info("HAZEN_ADDED_ARMOR_AUDIT_PASS items={} families={} set-fixtures={} equipped-mana=pass revival=pass ability=pass fire-immunity=pass animation-null-regression=167",items,families.size(),effects);
            java.nio.file.Files.writeString(java.nio.file.Path.of("hazen-added-armor-audit.json"),"{\"items\":"+items+",\"families\":"+families.size()+",\"set_fixtures\":"+effects+",\"passed\":true}");
        }catch(Throwable failure){com.mojang.logging.LogUtils.getLogger().error("HAZEN_ADDED_ARMOR_AUDIT_FAILED",failure);}
        server.halt(false);
    }
    private static void clear(net.minecraft.world.entity.player.Player p){for(var s:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))p.setItemSlot(s,ItemStack.EMPTY);p.removeAllEffects();}
    private static void equip(net.minecraft.world.entity.player.Player p,String family){for(var spec:HazenAddedArmor.SPECS)if(spec.family().equals(family))p.setItemSlot(ArmorItem.Type.valueOf(spec.slot()).getSlot(),HazenAddedArmor.ITEMS.get(spec.id()).get().getDefaultInstance());}
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
}
