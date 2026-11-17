package cn.wanyiwige.twilight;

import com.google.common.collect.*;
import io.redspace.ironsspellbooks.api.item.weapons.MagicSwordItem;
import io.redspace.ironsspellbooks.api.registry.*;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.item.SpellBook;
import io.redspace.ironsspellbooks.item.armor.*;
import io.redspace.ironsspellbooks.item.curios.CurioBaseItem;
import io.redspace.ironsspellbooks.item.weapons.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.*;
import net.minecraftforge.registries.*;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import top.theillusivec4.curios.api.SlotContext;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;

public final class TwilightEquipment {
    public static final Map<String,RegistryObject<Item>> ITEMS=new LinkedHashMap<>();
    public record Bonus(Supplier<Attribute> attribute,double amount,AttributeModifier.Operation operation){}
    private static Bonus add(Supplier<Attribute> attr,double value){return new Bonus(attr,value,AttributeModifier.Operation.ADDITION);}
    private static Bonus mul(Supplier<Attribute> attr,double value){return new Bonus(attr,value,AttributeModifier.Operation.MULTIPLY_BASE);}
    private static Supplier<Attribute> attr(String id){return ()->ForgeRegistries.ATTRIBUTES.getValue(new net.minecraft.resources.ResourceLocation(id));}
    static {
        for(String name:List.of("ironwood_spellbook","knightmetal_spellbook","carminite_spell_book","fiery_spell_book","snow_queen_spell_book","exanimated_spell_book"))
            register(name,()->name.equals("fiery_spell_book")?new FieryBook():new Book(name));
        for(String name:List.of("ironwood_ring","knightmetal_ring","fiery_ring","thorn_rose_ring","naga_mantle","carminite_mantle","knightmetal_brace","yeti_brace","ominous_blazer","steeleaf_sheath"))
            register(name,()->new Accessory(name));
        for(String name:List.of("steeleaf_staff","knightmetal_staff","boreal_staff"))
            register(name,()->new StaffItem(properties(name),name.equals("knightmetal_staff")?9:6,name.equals("knightmetal_staff")?-3:-2.5,modifiers(name)));
        register("maze_butcher",()->new Weapon("maze_butcher",11,-3.2,"minotaur_lunge",3));
        register("lich_greatsword",()->new Weapon("lich_greatsword",13,-2.6,"exanimated_fireball",3));
        register("ultimate_scepter",Scepter::new);
        register("boreal_bow",TwilightBorealBow::new);
        register("exanimated_lich_loot_bag",TwilightLootBag::new);
        for(String set:List.of("knightmetal_mage","fiery_mage","exanimated_lich"))for(ArmorItem.Type type:ArmorItem.Type.values()){
            String name=set+"_"+type.getName();register(name,()->set.equals("fiery_mage")?new Armor(set,type):new CapeArmor(set,type));
        }
        register("tarnished_lich_crown",()->new Armor("tarnished_lich_crown",ArmorItem.Type.HELMET));
        register("tarnished_queen_crown",()->new Armor("tarnished_queen_crown",ArmorItem.Type.HELMET));
    }
    private static void register(String id,Supplier<Item> supplier){ITEMS.put(id,TwilightItems.ITEMS.register(id,supplier));}
    public static void bootstrap(){}
    public static Item get(String name){return ITEMS.get(name).get();}
    public static Item.Properties properties(String name){return new Item.Properties().stacksTo(1).fireResistant().rarity(name.contains("exanimated")||name.equals("ultimate_scepter")||name.equals("lich_greatsword")?Rarity.EPIC:Rarity.RARE);}
    public static List<Bonus> bonuses(String id){
        Supplier<Attribute> armorPierce=attr("irons_lib:armor_pierce");
        return switch(id){
            case "ironwood_ring"->List.of(new Bonus(()->Attributes.ARMOR,.05,AttributeModifier.Operation.MULTIPLY_TOTAL));
            case "knightmetal_ring","knightmetal_brace"->List.of(add(armorPierce,3));
            case "yeti_brace"->List.of(add(()->Attributes.ATTACK_DAMAGE,2));
            case "naga_mantle"->List.of(mul(AttributeRegistry.FIRE_MAGIC_RESIST,.1));
            case "carminite_mantle"->List.of(mul(AttributeRegistry.LIGHTNING_SPELL_POWER,.05));
            case "ominous_blazer"->List.of(mul(AttributeRegistry.BLOOD_SPELL_POWER,.05),mul(AttributeRegistry.FIRE_SPELL_POWER,.05),mul(AttributeRegistry.ENDER_SPELL_POWER,.05),add(AttributeRegistry.SUMMON_DAMAGE,.1));
            case "steeleaf_sheath"->List.of(mul(AttributeRegistry.NATURE_SPELL_POWER,.1),mul(()->Attributes.ATTACK_DAMAGE,.05),mul(attr("irons_lib:crit_damage"),.05));
            case "steeleaf_staff"->List.of(mul(AttributeRegistry.NATURE_SPELL_POWER,.15),add(TwilightAttributes.MAGIC_CRIT_DAMAGE,.25),mul(AttributeRegistry.SPELL_POWER,.05));
            case "knightmetal_staff"->List.of(add(armorPierce,2),mul(AttributeRegistry.SPELL_POWER,.1));
            case "boreal_staff"->List.of(mul(AttributeRegistry.ICE_SPELL_POWER,.1),mul(AttributeRegistry.HOLY_SPELL_POWER,.1),mul(AttributeRegistry.MANA_REGEN,.25),mul(AttributeRegistry.SPELL_POWER,.05));
            case "ultimate_scepter"->List.of(mul(AttributeRegistry.BLOOD_SPELL_POWER,.1),mul(AttributeRegistry.ENDER_SPELL_POWER,.05),mul(AttributeRegistry.HOLY_SPELL_POWER,.05),add(TwilightAttributes.MANA_STEAL,.25),mul(AttributeRegistry.SPELL_POWER,.05));
            case "lich_greatsword"->List.of(mul(AttributeRegistry.BLOOD_SPELL_POWER,.05),mul(AttributeRegistry.FIRE_SPELL_POWER,.05),mul(AttributeRegistry.ENDER_SPELL_POWER,.05));
            case "maze_butcher"->List.of(mul(()->Attributes.MOVEMENT_SPEED,.15));
            case "boreal_bow"->List.of(mul(AttributeRegistry.ICE_SPELL_POWER,.1),mul(TwilightAttributes.MAGIC_PROJECTILE_DAMAGE,.5));
            case "ironwood_spellbook"->List.of(mul(AttributeRegistry.NATURE_MAGIC_RESIST,.05),add(AttributeRegistry.MAX_MANA,150));
            case "knightmetal_spellbook"->List.of(add(armorPierce,2),add(AttributeRegistry.MAX_MANA,200));
            case "carminite_spell_book"->List.of(mul(AttributeRegistry.LIGHTNING_SPELL_POWER,.1),add(AttributeRegistry.MAX_MANA,200));
            case "fiery_spell_book"->List.of(mul(AttributeRegistry.FIRE_SPELL_POWER,.15),add(AttributeRegistry.MAX_MANA,250));
            case "snow_queen_spell_book"->List.of(mul(AttributeRegistry.ICE_SPELL_POWER,.1),mul(AttributeRegistry.HOLY_SPELL_POWER,.1),add(AttributeRegistry.MAX_MANA,250));
            case "exanimated_spell_book"->List.of(mul(AttributeRegistry.BLOOD_SPELL_POWER,.1),mul(AttributeRegistry.FIRE_SPELL_POWER,.1),mul(AttributeRegistry.ENDER_SPELL_POWER,.1),add(AttributeRegistry.MAX_MANA,250));
            case "fiery_mage"->List.of(add(AttributeRegistry.MAX_MANA,125),add(AttributeRegistry.FIRE_SPELL_POWER,.15),mul(AttributeRegistry.SPELL_POWER,.05));
            case "knightmetal_mage"->List.of(add(AttributeRegistry.MAX_MANA,125),add(TwilightAttributes.RESIST_PENETRATION,.05),mul(AttributeRegistry.SPELL_POWER,.05));
            case "exanimated_lich"->List.of(add(AttributeRegistry.MAX_MANA,125),mul(AttributeRegistry.BLOOD_SPELL_POWER,.1),mul(AttributeRegistry.ENDER_SPELL_POWER,.1),mul(AttributeRegistry.FIRE_SPELL_POWER,.1));
            case "tarnished_lich_crown","tarnished_queen_crown"->List.of(add(AttributeRegistry.MAX_MANA,250),mul(AttributeRegistry.MANA_REGEN,.3),mul(AttributeRegistry.ELDRITCH_SPELL_POWER,.05),mul(id.contains("queen")?AttributeRegistry.ICE_SPELL_POWER:AttributeRegistry.FIRE_SPELL_POWER,.05),mul(AttributeRegistry.SPELL_POWER,.05),mul(()->Attributes.ATTACK_DAMAGE,-.75));
            default->List.of();
        };
    }
    static Map<Attribute,AttributeModifier> modifiers(String name){
        Map<Attribute,AttributeModifier> map=new HashMap<>();for(Bonus bonus:bonuses(name)){
            Attribute attribute=bonus.attribute().get();if(attribute!=null)map.put(attribute,new AttributeModifier(UUID.nameUUIDFromBytes(("twilight/"+name+"/"+ForgeRegistries.ATTRIBUTES.getKey(attribute)).getBytes(StandardCharsets.UTF_8)),"twilight/"+name,bonus.amount(),bonus.operation()));
        }return map;
    }
    private static AttributeContainer[] containers(String name){return bonuses(name).stream().filter(b->b.attribute().get()!=null).map(b->new AttributeContainer(b.attribute(),b.amount(),b.operation())).toArray(AttributeContainer[]::new);}
    public static final class Accessory extends CurioBaseItem {
        public final String name;Accessory(String name){super(properties(name));this.name=name;}
        @Override public Multimap<Attribute,AttributeModifier> getAttributeModifiers(SlotContext context,UUID uuid,ItemStack stack){
            Multimap<Attribute,AttributeModifier> map=HashMultimap.create();for(Bonus bonus:bonuses(name)){
                Attribute attr=bonus.attribute().get();if(attr!=null)map.put(attr,new AttributeModifier(uuid,"twilight/"+name,bonus.amount(),bonus.operation()));
            }return map;
        }
        @Override public void appendHoverText(ItemStack stack,Level world,List<Component> lines,TooltipFlag flag){super.appendHoverText(stack,world,lines,flag);lines.add(Component.translatable(getDescriptionId()+".desc"));}
    }
    public static class Book extends SpellBook implements software.bernie.geckolib.animatable.GeoItem {
        private final software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache cache=software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);
        public final String name;
        Book(String name){super(name.equals("ironwood_spellbook")?8:name.equals("exanimated_spell_book")?15:name.equals("knightmetal_spellbook")||name.equals("carminite_spell_book")?10:12,properties(name));this.name=name;withSpellbookAttributes(containers(name));}
        @Override public boolean isUnique(){return name.equals("exanimated_spell_book");}
        @Override public void inventoryTick(ItemStack stack,Level world,Entity entity,int slot,boolean selected){super.inventoryTick(stack,world,entity,slot,selected);if(!world.isClientSide)initializeSpellContainer(stack);}
        @Override public void initializeSpellContainer(ItemStack stack){
            if(!ISpellContainer.isSpellContainer(stack)){
                var container=ISpellContainer.create(getMaxSpellSlots(),true,true);ISpellContainer.set(stack,container);
                if(isUnique())for(String spell:List.of("exanimated_ray","exanimated_step","exanimated_trident"))container.addSpell(TwilightSpells.get(spell),spell.equals("exanimated_ray")?6:spell.equals("exanimated_step")?4:5,true,stack);
            }
        }
        @Override public ItemStack getDefaultInstance(){ItemStack stack=super.getDefaultInstance();initializeSpellContainer(stack);return stack;}
        @Override public software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
        @Override public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar registrar){}
    }
    public static final class FieryBook extends Book {
        FieryBook(){super("fiery_spell_book");}
        @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){private TwilightFieryBookRenderer renderer;public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new TwilightFieryBookRenderer();return renderer;}});}
    }
    public static class Weapon extends MagicSwordItem implements software.bernie.geckolib.animatable.GeoItem {
        private final software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache cache=software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);
        public final String name;
        Weapon(String name,double damage,double speed,String spell,int rank){super(Tiers.NETHERITE,damage,speed,spell==null?SpellDataRegistryHolder.of():SpellDataRegistryHolder.of(new SpellDataRegistryHolder(TwilightSpells.RECREATED.get(spell),rank)),modifiers(name),properties(name));this.name=name;}
        @Override public boolean isDamageable(ItemStack stack){return false;}
        @Override public ItemStack getDefaultInstance(){ItemStack stack=super.getDefaultInstance();initializeSpellContainer(stack);return stack;}
        @Override public software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
        @Override public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar registrar){if(name.equals("ultimate_scepter"))registrar.add(new software.bernie.geckolib.core.animation.AnimationController<>(this,"twilight_idle",0,state->state.setAndContinue(software.bernie.geckolib.core.animation.RawAnimation.begin().thenLoop("idle"))));}
    }
    public static final class Scepter extends Weapon {
        Scepter(){super("ultimate_scepter",6,-2.5,null,0);}
        @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){private TwilightScepterRenderer renderer;public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new TwilightScepterRenderer();return renderer;}});}
    }
    public static class Armor extends ExtendedArmorItem implements IPresetSpellContainer {
        public final String set;
        Armor(String set,Type type){super(set.startsWith("tarnished")?ExtendedArmorMaterials.TARNISHED:ExtendedArmorMaterials.NETHERITE_BATTLEMAGE,type,properties(set).durability((type==Type.HELMET?11:type==Type.CHESTPLATE?16:type==Type.LEGGINGS?15:13)*48),containers(set));this.set=set;}
        @Override public void initializeSpellContainer(ItemStack stack){if((getType()==Type.CHESTPLATE||set.equals("exanimated_lich")||set.startsWith("tarnished"))&&!ISpellContainer.isSpellContainer(stack))ISpellContainer.set(stack,ISpellContainer.create(1,true,true));}
        @Override public ItemStack getDefaultInstance(){ItemStack stack=super.getDefaultInstance();initializeSpellContainer(stack);return stack;}
        @Override @OnlyIn(Dist.CLIENT) public GeoArmorRenderer<?> supplyRenderer(){return new TwilightArmorRenderer(set.equals("exanimated_lich")||set.equals("fiery_mage"));}
        @Override public void appendHoverText(ItemStack stack,Level world,List<Component> lines,TooltipFlag flag){super.appendHoverText(stack,world,lines,flag);if(set.equals("fiery_mage")&&getType()==Type.CHESTPLATE){lines.add(Component.translatable("tooltip.wans_twilight_spellbooks.on_full_set"));lines.add(Component.translatable("tooltip.wans_twilight_spellbooks.hydra_set_passive"));}}
    }
    public static final class CapeArmor extends Armor implements IArmorCapeProvider {
        CapeArmor(String set,Type type){super(set,type);}
        @Override public net.minecraft.resources.ResourceLocation getCapeResourceLocation(){return TwilightFoundation.id("textures/armor/"+(set.equals("exanimated_lich")?"exanimated_lich_cape.png":"knightmetal_mage_cape.png"));}
    }
    private TwilightEquipment(){}
}
