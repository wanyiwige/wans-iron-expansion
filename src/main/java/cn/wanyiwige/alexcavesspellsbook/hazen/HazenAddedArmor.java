package cn.wanyiwige.alexcavesspellsbook.hazen;

import com.google.gson.Gson;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.item.armor.ImbuableChestplateArmorItem;
import io.redspace.ironsspellbooks.item.armor.IronsExtendedArmorMaterial;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Data-backed original armor. Activation is deferred until set-effect validation is complete. */
public final class HazenAddedArmor {
    public record Bonus(String attribute, double original, double value, String operation) {}
    public record MaterialData(int[] defense, int enchantment, String sound, String repair,
                               String repair_namespace, float toughness, float knockback) {}
    public record Spec(String id, String family, String source_class, String slot, String rarity,
                       int durability, boolean fire_resistant, boolean flight, boolean imbuable,
                       boolean glowing, String model, String texture, String animation,
                       MaterialData material, List<Bonus> resolved_attributes, List<EffectSpec> applied_effects, Map<String,String> required_pieces, boolean frostbite_affinity, boolean hide_hat, boolean hide_jacket, boolean glint, boolean unbreakable, String animation_name, List<String> animation_names) {
        public Spec {
            animation_names=animation_names==null?List.of():List.copyOf(animation_names);
        }
        public String animationClip(boolean flying){
            if(flying&&animation_names.contains("flying"))return "flying";
            if(animation_name!=null&&animation_names.contains(animation_name))return animation_name;
            return animation_names.contains("idle")?"idle":"";
        }
    }
    public record EffectSpec(String id,int duration,int amplifier) {}
    private static final DeferredRegister<Item> REGISTER=DeferredRegister.create(ForgeRegistries.ITEMS,"alex_caves_spellsbook");
    public static final Map<String,RegistryObject<Item>> ITEMS=new LinkedHashMap<>();
    public static final List<Spec> SPECS;
    static {
        try(var reader=new InputStreamReader(Objects.requireNonNull(HazenAddedArmor.class.getResourceAsStream("/hazen-added-armor.json")),StandardCharsets.UTF_8)) {
            SPECS=List.of(new Gson().fromJson(reader,Spec[].class));
        } catch(Exception e){throw new IllegalStateException("Cannot load added Hazen armor",e);}
        for(var spec:SPECS)ITEMS.put(spec.id(),REGISTER.register(spec.id(),()->new Armor(spec)));
    }
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus){REGISTER.register(bus);HazenAddedArmorEffects.register(bus);}
    public static void addToTab(CreativeModeTab.Output output){ITEMS.values().forEach(item->{if(((Armor)item.get()).enabled())output.accept(item.get().getDefaultInstance());});}
    public static class Armor extends ImbuableChestplateArmorItem {
        public final Spec spec;
        public boolean enabled(){return HazenConfig.armorEnabled(spec.id().substring("hazen_".length()),spec.family());}
        public Armor(Spec spec){
            super(new Material(spec),Type.valueOf(spec.slot()),properties(spec));this.spec=spec;
        }
        private static Properties properties(Spec s){
            var p=new Properties().stacksTo(1).durability(Math.max(1,s.durability()))
                .rarity(HazenRarities.get(s.rarity().equals("CINDEROUS_RARITY_PROXY")?"FIREBLOSSOM_RARITY":s.rarity()));
            if(s.fire_resistant())p.fireResistant();return p;
        }
        @Override public Multimap<Attribute,AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot){
            if(spec==null||slot!=getEquipmentSlot())return super.getDefaultAttributeModifiers(slot);
            if(!enabled())return ImmutableMultimap.of();
            var out=ImmutableMultimap.<Attribute,AttributeModifier>builder().putAll(super.getDefaultAttributeModifiers(slot));
            int i=0;
            for(var b:spec.resolved_attributes()){
                var attribute=resolve(b.attribute());if(attribute==null){if(b.attribute().startsWith("optional:"))continue;throw new IllegalStateException("Missing armor attribute "+b.attribute());}
                String key=spec.id()+"/"+slot+"/"+(i++);
                var op=switch(b.operation()){
                    case "ADD_VALUE"->AttributeModifier.Operation.ADDITION;
                    case "ADD_MULTIPLIED_BASE"->AttributeModifier.Operation.MULTIPLY_BASE;
                    case "ADD_MULTIPLIED_TOTAL"->AttributeModifier.Operation.MULTIPLY_TOTAL;
                    default->throw new IllegalArgumentException(b.operation());
                };
                out.put(attribute,new AttributeModifier(UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)),key,b.value(),op));
            }
            return out.build();
        }
        @Override public ItemStack getDefaultInstance(){var s=super.getDefaultInstance();if(spec.imbuable())initializeSpellContainer(s);return s;}
        @Override public void initializeSpellContainer(ItemStack stack){if(spec!=null&&spec.imbuable())super.initializeSpellContainer(stack);}
        @Override public int getMaxDamage(ItemStack stack){return spec.unbreakable()?0:super.getMaxDamage(stack);}
        @Override public boolean isDamageable(ItemStack stack){return !spec.unbreakable()&&super.isDamageable(stack);}
        @Override public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers){
            if(!spec.animation_names().isEmpty())controllers.add(new software.bernie.geckolib.core.animation.AnimationController<>(this,"original",0,state->{
                var entity=state.getData(software.bernie.geckolib.constant.DataTickets.ENTITY);
                boolean flying=entity instanceof net.minecraft.world.entity.player.Player p&&(p.getAbilities().flying||p.isFallFlying()&&!p.onGround());
                String clip=spec.animationClip(flying);
                if(clip.isEmpty())return software.bernie.geckolib.core.object.PlayState.STOP;
                return state.setAndContinue(software.bernie.geckolib.core.animation.RawAnimation.begin().thenLoop(clip));
            }));
        }
        @Override public boolean isFoil(ItemStack stack){return spec.glint()||super.isFoil(stack);}
        @Override public void inventoryTick(ItemStack stack,net.minecraft.world.level.Level level,net.minecraft.world.entity.Entity entity,int slot,boolean selected){super.inventoryTick(stack,level,entity,slot,selected);if(spec.imbuable())initializeSpellContainer(stack);}
        @Override public boolean canElytraFly(ItemStack stack,net.minecraft.world.entity.LivingEntity entity){return spec.flight()&&getEquipmentSlot()==EquipmentSlot.CHEST&&(!stack.isDamageableItem()||stack.getDamageValue()<stack.getMaxDamage()-1);}
        @Override public boolean elytraFlightTick(ItemStack stack,net.minecraft.world.entity.LivingEntity entity,int ticks){return canElytraFly(stack,entity);}
        @Override @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
        public software.bernie.geckolib.renderer.GeoArmorRenderer<?> supplyRenderer(){return new cn.wanyiwige.alexcavesspellsbook.hazen.client.HazenAddedArmorRenderer(spec.glowing());}
    }
    public static Attribute resolve(String name){
        if(name.startsWith("optional:"))return ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(name.substring(9)));
        if(name.equals("Attributes.MINING_EFFICIENCY"))return HazenAddedArmorEffects.MINING.get();
        if(name.equals("Attributes.SUBMERGED_MINING_SPEED"))return HazenAddedArmorEffects.SUBMERGED.get();
        if(name.equals("IronsLibRegistries.AttributeRegistry.ARROW_DAMAGE"))return HazenAddedArmorEffects.ARROW.get();
        if(name.startsWith("Attributes."))return switch(name){
            case "Attributes.ATTACK_DAMAGE"->Attributes.ATTACK_DAMAGE;
            case "Attributes.ATTACK_SPEED"->Attributes.ATTACK_SPEED;
            case "Attributes.MAX_HEALTH"->Attributes.MAX_HEALTH;
            case "Attributes.MOVEMENT_SPEED"->Attributes.MOVEMENT_SPEED;
            default->throw new IllegalArgumentException(name);
        };
        if(name.equals("ASAttributeRegistry.HYDRO_MAGIC_POWER"))return HazenAttributes.get("hydro_spell_power");
        if(name.equals("local.crit_damage"))return HazenAttributes.get("crit_damage");
        if(name.equals("local.arrow_damage")||name.equals("IronsLibRegistries.AttributeRegistry.ARROW_DAMAGE"))
            return ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("attributeslib:arrow_damage"));
        return HazenAttributes.resolve(name);
    }
    private record Material(Spec spec) implements IronsExtendedArmorMaterial {
        private int index(Type t){return switch(t){case HELMET->0;case CHESTPLATE->1;case LEGGINGS->2;case BOOTS->3;};}
        public int getDurabilityForType(Type t){return Math.max(1,spec.durability());}
        public int getDefenseForType(Type t){return spec.material().defense()[index(t)];}
        public int getEnchantmentValue(){return spec.material().enchantment();}
        public SoundEvent getEquipSound(){return switch(spec.material().sound()){
            case "SoundEvents.ARMOR_EQUIP_LEATHER"->SoundEvents.ARMOR_EQUIP_LEATHER;
            case "SoundEvents.ARMOR_EQUIP_DIAMOND"->SoundEvents.ARMOR_EQUIP_DIAMOND;
            default->SoundEvents.ARMOR_EQUIP_NETHERITE;
        };}
        public Ingredient getRepairIngredient(){
            var m=spec.material();String namespace=m.repair_namespace().equals("hazen_equipment")?"alex_caves_spellsbook":m.repair_namespace();
            String path=m.repair_namespace().equals("hazen_equipment")?"hazen_"+m.repair():m.repair();
            var key=new ResourceLocation(namespace,path);return ForgeRegistries.ITEMS.containsKey(key)?Ingredient.of(ForgeRegistries.ITEMS.getValue(key)):Ingredient.EMPTY;
        }
        public String getName(){return "alex_caves_spellsbook:"+spec.id();}
        public float getToughness(){return spec.material().toughness();}
        public float getKnockbackResistance(){return spec.material().knockback();}
        public Map<Attribute,AttributeModifier> getAdditionalAttributes(){return Map.of();}
    }
    private HazenAddedArmor(){}
}

