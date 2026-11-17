package cn.wanyiwige.alexcavesspellsbook.toxic;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.item.armor.IDisableHat;
import io.redspace.ironsspellbooks.item.armor.IDisableJacket;
import io.redspace.ironsspellbooks.item.armor.ImbuableChestplateArmorItem;
import io.redspace.ironsspellbooks.item.armor.IronsExtendedArmorMaterial;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/** Heavy radiation-school attire using Iron's native armor upgrades and chest imbue slot. */
public final class CriticalWardenArmorItem extends ImbuableChestplateArmorItem implements IDisableHat, IDisableJacket {
    public CriticalWardenArmorItem(Type type) {
        super(Material.INSTANCE, type, new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot != getEquipmentSlot()) return ImmutableMultimap.of();
        var attributes = ImmutableMultimap.<Attribute, AttributeModifier>builder()
                .putAll(super.getDefaultAttributeModifiers(slot));
        int index = switch (getType()) { case HELMET -> 0; case CHESTPLATE -> 1; case LEGGINGS -> 2; case BOOTS -> 3; };
        add(attributes, ToxicRegistry.POWER.get(), new double[]{.10, .18, .14, .08}[index], AttributeModifier.Operation.MULTIPLY_BASE);
        add(attributes, AttributeRegistry.MAX_MANA.get(), new double[]{80, 150, 110, 60}[index], AttributeModifier.Operation.ADDITION);
        add(attributes, AttributeRegistry.MANA_REGEN.get(), new double[]{.02, .04, .02, .02}[index], AttributeModifier.Operation.MULTIPLY_BASE);
        add(attributes, ToxicRegistry.RESIST.get(), new double[]{.04, .06, .04, .02}[index], AttributeModifier.Operation.MULTIPLY_BASE);
        return attributes.build();
    }

    private void add(ImmutableMultimap.Builder<Attribute, AttributeModifier> attributes, Attribute attribute,
                     double amount, AttributeModifier.Operation operation) {
        String key = "critical_warden/" + getType().getName() + "/" + attribute.getDescriptionId();
        attributes.put(attribute, new AttributeModifier(UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)), key, amount, operation));
    }

    @Override public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        initializeSpellContainer(stack);
        return stack;
    }

    @Override @OnlyIn(Dist.CLIENT) public GeoArmorRenderer<?> supplyRenderer() {
        return new cn.wanyiwige.alexcavesspellsbook.toxic.client.CriticalWardenArmorRenderer();
    }

    private enum Material implements IronsExtendedArmorMaterial {
        INSTANCE;
        public int getDurabilityForType(Type type) { return 33 * switch (type) { case HELMET -> 11; case CHESTPLATE -> 16; case LEGGINGS -> 15; case BOOTS -> 13; }; }
        public int getDefenseForType(Type type) { return switch (type) { case HELMET -> 3; case CHESTPLATE -> 8; case LEGGINGS -> 6; case BOOTS -> 3; }; }
        public int getEnchantmentValue() { return 15; }
        public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_IRON; }
        public Ingredient getRepairIngredient() { return Ingredient.of(Items.IRON_INGOT); }
        public String getName() { return "alex_caves_spellsbook:critical_warden"; }
        public float getToughness() { return 2; }
        public float getKnockbackResistance() { return .025F; }
        public Map<Attribute, AttributeModifier> getAdditionalAttributes() { return Map.of(); }
    }
}
