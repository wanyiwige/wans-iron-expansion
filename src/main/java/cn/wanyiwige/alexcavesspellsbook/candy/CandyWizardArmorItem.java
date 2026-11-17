package cn.wanyiwige.alexcavesspellsbook.candy;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.item.armor.IDisableHat;
import io.redspace.ironsspellbooks.item.armor.IDisableJacket;
import io.redspace.ironsspellbooks.item.armor.ImbuableChestplateArmorItem;
import io.redspace.ironsspellbooks.item.armor.IronsExtendedArmorMaterial;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/** School attire uses native Iron's armor upgrade and chest-imbue handling. */
public final class CandyWizardArmorItem extends ImbuableChestplateArmorItem implements IDisableHat, IDisableJacket {
    public CandyWizardArmorItem(Type type){super(Material.INSTANCE,type,new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));}
    @Override public Multimap<Attribute,AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot){
        if(slot!=getEquipmentSlot())return ImmutableMultimap.of();
        var out=ImmutableMultimap.<Attribute,AttributeModifier>builder().putAll(super.getDefaultAttributeModifiers(slot));
        int i=switch(getType()){case HELMET->0;case CHESTPLATE->1;case LEGGINGS->2;case BOOTS->3;};
        // A sustained-casting school set: additive base bonuses stack predictably per slot.
        add(out,CandyRegistry.POWER.get(),new double[]{.08,.16,.10,.06}[i],AttributeModifier.Operation.MULTIPLY_BASE);
        add(out,CandyRegistry.RESIST.get(),new double[]{.03,.04,.03,.02}[i],AttributeModifier.Operation.MULTIPLY_BASE);
        add(out,AttributeRegistry.MAX_MANA.get(),new double[]{60,110,80,50}[i],AttributeModifier.Operation.ADDITION);
        add(out,AttributeRegistry.MANA_REGEN.get(),new double[]{.03,.06,.04,.02}[i],AttributeModifier.Operation.MULTIPLY_BASE);
        return out.build();
    }
    private void add(ImmutableMultimap.Builder<Attribute,AttributeModifier> out,Attribute attribute,double value,AttributeModifier.Operation operation){
        String key="candy_wizard/"+getType().getName()+"/"+attribute.getDescriptionId();
        out.put(attribute,new AttributeModifier(UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)),key,value,operation));
    }
    @Override public ItemStack getDefaultInstance(){ItemStack stack=super.getDefaultInstance();initializeSpellContainer(stack);return stack;}
    @Override @OnlyIn(Dist.CLIENT) public GeoArmorRenderer<?> supplyRenderer(){return new cn.wanyiwige.alexcavesspellsbook.candy.client.CandyWizardArmorRenderer();}
    private enum Material implements IronsExtendedArmorMaterial {
        INSTANCE;
        public int getDurabilityForType(Type type){return 25*switch(type){case HELMET->11;case CHESTPLATE->16;case LEGGINGS->15;case BOOTS->13;};}
        public int getDefenseForType(Type type){return switch(type){case HELMET->2;case CHESTPLATE->6;case LEGGINGS->5;case BOOTS->2;};}
        public int getEnchantmentValue(){return 15;}
        public SoundEvent getEquipSound(){return SoundEvents.ARMOR_EQUIP_LEATHER;}
        public Ingredient getRepairIngredient(){return Ingredient.of(ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("irons_spellbooks","magic_cloth")));}
        public String getName(){return "alex_caves_spellsbook:candy_wizard";}
        public float getToughness(){return 0;}
        public float getKnockbackResistance(){return 0;}
        public Map<Attribute,AttributeModifier> getAdditionalAttributes(){return Map.of();}
    }
}
