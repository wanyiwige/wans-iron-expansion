package cn.wanyiwige.alexcavesspellsbook.toxic;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.SpellBook;
import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import io.redspace.ironsspellbooks.item.weapons.StaffItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Native ISS equipment attributes; the eye is a spellbook-slot focus with its own item silhouette. */
public final class RadiationEquipment {
    public static final class Focus extends SpellBook {
        public Focus(int slots,double power,int mana){
            super(slots,new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
            withSpellbookAttributes(
                    new AttributeContainer(ToxicRegistry.POWER,power,AttributeModifier.Operation.ADDITION),
                    new AttributeContainer(AttributeRegistry.MAX_MANA,mana,AttributeModifier.Operation.ADDITION));
        }
        @Override public ItemStack getDefaultInstance(){
            ItemStack stack=super.getDefaultInstance();initializeSpellContainer(stack);return stack;
        }
        @Override public void inventoryTick(ItemStack stack,Level level,Entity holder,int slot,boolean selected){
            if(!ISpellContainer.isSpellContainer(stack))initializeSpellContainer(stack);
            super.inventoryTick(stack,level,holder,slot,selected);
        }
    }
    public static final class FissionStaff extends StaffItem implements IPresetSpellContainer {
        public FissionStaff(){
            super(new Item.Properties().stacksTo(1).durability(1561).rarity(Rarity.RARE),1.5,-2.5,attributes());
        }
        private static Map<Attribute,AttributeModifier> attributes(){
            Map<Attribute,AttributeModifier> result=new LinkedHashMap<>();
            result.put(ToxicRegistry.POWER.get(),modifier("fission_staff_power",.10));
            result.put(AttributeRegistry.MAX_MANA.get(),modifier("fission_staff_mana",75));
            return result;
        }
        private static AttributeModifier modifier(String name,double amount){
            return new AttributeModifier(UUID.nameUUIDFromBytes(("alex_caves_spellsbook:"+name).getBytes(StandardCharsets.UTF_8)),name,amount,AttributeModifier.Operation.ADDITION);
        }
        @Override public void initializeSpellContainer(ItemStack stack){
            if(!ISpellContainer.isSpellContainer(stack))ISpellContainer.set(stack,ISpellContainer.create(1,false,true));
        }
        @Override public ItemStack getDefaultInstance(){
            ItemStack stack=super.getDefaultInstance();initializeSpellContainer(stack);return stack;
        }
        @Override public void inventoryTick(ItemStack stack,Level level,Entity holder,int slot,boolean selected){
            initializeSpellContainer(stack);super.inventoryTick(stack,level,holder,slot,selected);
        }
    }
    public static void addCreative(CreativeModeTab.Output output){
        output.accept(ToxicRegistry.RADIATION_GRIMOIRE.get().getDefaultInstance());
        output.accept(ToxicRegistry.MECHANICAL_RADIATION_EYE.get().getDefaultInstance());
        output.accept(ToxicRegistry.FISSION_STAFF.get().getDefaultInstance());
        output.accept(ToxicRegistry.CRITICAL_WARDEN_HELMET.get().getDefaultInstance());
        output.accept(ToxicRegistry.CRITICAL_WARDEN_CHESTPLATE.get().getDefaultInstance());
        output.accept(ToxicRegistry.CRITICAL_WARDEN_LEGGINGS.get().getDefaultInstance());
        output.accept(ToxicRegistry.CRITICAL_WARDEN_BOOTS.get().getDefaultInstance());
    }
    private RadiationEquipment(){}
}
