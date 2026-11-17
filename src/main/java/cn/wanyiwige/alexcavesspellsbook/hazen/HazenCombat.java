package cn.wanyiwige.alexcavesspellsbook.hazen;

import io.redspace.ironsspellbooks.api.events.ModifySpellLevelEvent;
import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import top.theillusivec4.curios.api.CuriosApi;
import java.util.HashSet;

public final class HazenCombat {
    private static final DeferredRegister<MobEffect> EFFECTS=DeferredRegister.create(ForgeRegistries.MOB_EFFECTS,"alex_caves_spellsbook");
    public static final RegistryObject<MobEffect> CHAOS_STATE=EFFECTS.register("hazen_chaos_state",()->new Effect(MobEffectCategory.HARMFUL,0x2E2EFF));
    public static final RegistryObject<MobEffect> HARMONIZED=EFFECTS.register("hazen_harmonized",()->new Effect(MobEffectCategory.BENEFICIAL,3311322)
            .addAttributeModifier(HazenAttributes.resolve("IronsLibRegistries.AttributeRegistry.CRIT_DAMAGE"),
                    "72f8672b-e28a-476b-91d0-a01576ec5b1f",.05,AttributeModifier.Operation.MULTIPLY_TOTAL));
    private static final TagKey<Item> HARMONIOUS=TagKey.create(Registries.ITEM,HazenEquipment.id("hazen/harmonious_equipment"));
    private static final class Effect extends MobEffect { private Effect(MobEffectCategory category,int color){super(category,color);} }
    public static void register(IEventBus bus){EFFECTS.register(bus);}
    public static boolean hasHarmoniousEquipment(LivingEntity entity){
        for(EquipmentSlot slot:EquipmentSlot.values())if(entity.getItemBySlot(slot).is(HARMONIOUS))return true;
        return CuriosApi.getCuriosInventory(entity).map(inv->inv.findFirstCurio(stack->stack.is(HARMONIOUS)).isPresent()).orElse(false);
    }
    @SubscribeEvent public static void crafted(PlayerEvent.ItemCraftedEvent event){
        if(event.getCrafting().getItem() instanceof HazenWeaponItem weapon)
            weapon.initializeSpellContainer(event.getCrafting());
    }
    @SubscribeEvent public static void affinity(ModifySpellLevelEvent event){
        var caster=event.getEntity();if(caster==null||!HazenConfig.ENABLED.get())return;
        // One bonus per weapon definition, even when holding two copies.
        var seen=new HashSet<String>();
        for(var stack:new net.minecraft.world.item.ItemStack[]{caster.getMainHandItem(),caster.getOffhandItem()}){
            if(!(stack.getItem() instanceof HazenWeaponItem item)||!HazenConfig.itemEnabled(item.spec.id)||!seen.add(item.spec.id))continue;
            for(var affinity:item.spec.affinity)if(HazenSpells.resolve(affinity.source_field())==event.getSpell())event.addLevels(affinity.bonus());
        }
    }
    @SubscribeEvent public static void manaRend(LivingHurtEvent event){
        if(!HazenConfig.ENABLED.get()||!(event.getSource().getEntity() instanceof LivingEntity attacker))return;
        var target=event.getEntity();var mana=target.getAttribute(AttributeRegistry.MAX_MANA.get());
        var rendAttribute=attacker.getAttribute(HazenAttributes.get("mana_rend"));
        if(mana!=null&&rendAttribute!=null){
            double rend=rendAttribute.getValue(),extra=Math.max(0,mana.getValue()-mana.getBaseValue());
            if(rend>0&&extra>0)event.setAmount((float)(event.getAmount()*(1+rend*extra/1000)));
        }
        var pierce=attacker.getAttribute(HazenAttributes.get("armor_pierce"));
        if(pierce!=null&&pierce.getValue()>0&&event.getAmount()>0&&!event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)){
            float amount=event.getAmount(),armor=target.getArmorValue(),tough=(float)target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS);
            float desired=net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(amount,armor*(1-(float)Math.min(1,pierce.getValue())),tough);
            float low=amount,high=Math.max(1,amount)*5;
            for(int i=0;i<24;i++){float mid=(low+high)*.5f;if(net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(mid,armor,tough)<desired)low=mid;else high=mid;}
            event.setAmount((low+high)*.5f);
        }
    }
    @SubscribeEvent public static void penetration(SpellDamageEvent event){
        if(!HazenConfig.ENABLED.get()||!(event.getSpellDamageSource().getEntity() instanceof LivingEntity caster))return;
        var attr=caster.getAttribute(HazenAttributes.get("spell_res_penetration"));if(attr==null||attr.getValue()<=0)return;
        var resistance=event.getEntity().getAttribute(AttributeRegistry.SPELL_RESIST.get());if(resistance==null||resistance.getValue()<=1)return;
        // Apply the original library's bounded resistance-penetration formula.
        double multiplier=io.redspace.ironsspellbooks.damage.DamageSources.getResist(event.getEntity(),event.getSpellDamageSource().spell().getSchoolType());
        double reduced=2-multiplier,pen=1+attr.getValue();
        double adjustment=2-(pen>reduced?1:reduced/pen);
        if(multiplier>0)event.setAmount((float)(event.getOriginalAmount()*adjustment/multiplier));
    }
    @SubscribeEvent public static void projectileCritical(SpellDamageEvent event){
        if(!HazenConfig.ENABLED.get()||!(event.getSpellDamageSource().getEntity() instanceof LivingEntity caster)
                ||!(event.getSpellDamageSource().getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile))return;
        boolean equipped=false;
        for(var stack:new net.minecraft.world.item.ItemStack[]{caster.getMainHandItem(),caster.getOffhandItem()})
            if(stack.getItem() instanceof HazenWeaponItem weapon&&HazenConfig.itemEnabled(weapon.spec.id)
                    &&weapon.spec.attributes.stream().anyMatch(a->a.source_attribute().equals("ASAttributeRegistry.MAGIC_PROJECTILE_CRIT_CHANCE")))equipped=true;
        if(equipped&&caster.getRandom().nextDouble()<caster.getAttributeValue(HazenAttributes.get("magic_projectile_crit_chance")))
            event.setAmount(event.getAmount()*1.5f);
    }
    @SubscribeEvent public static void nativeCritical(net.minecraftforge.event.entity.player.CriticalHitEvent event){
        if(!HazenConfig.ENABLED.get()||!event.isVanillaCritical()
                ||ForgeRegistries.ATTRIBUTES.containsKey(new net.minecraft.resources.ResourceLocation("attributeslib:crit_damage")))return;
        var attr=event.getEntity().getAttribute(HazenAttributes.get("crit_damage"));
        if(attr!=null)event.setDamageModifier((float)(event.getDamageModifier()*attr.getValue()/1.5));
    }
    private HazenCombat(){}
}
