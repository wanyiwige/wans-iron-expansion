package cn.wanyiwige.alexcavesspellsbook.hazen;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.item.armor.IDisableHat;
import io.redspace.ironsspellbooks.item.armor.IDisableJacket;
import io.redspace.ironsspellbooks.item.armor.ImbuableChestplateArmorItem;
import io.redspace.ironsspellbooks.item.armor.IronsExtendedArmorMaterial;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Native Iron's Spellbooks armor and imbue handling, with original piece statistics. */
public final class HazenArmorItem extends ImbuableChestplateArmorItem implements IDisableHat, IDisableJacket {
    public final HazenArmor.SetSpec spec;
    public HazenArmorItem(HazenArmor.SetSpec spec, Type type) {
        super(new Material(spec), type, new Properties().stacksTo(1).rarity(HazenRarities.get(spec.rarity()))
                .fireResistant().durability(baseDurability(type) * spec.durability_multiplier()));
        this.spec = spec;
    }
    public String shortId() { return spec.id() + "_" + getType().getName(); }
    public boolean enabled() { return HazenConfig.armorEnabled(shortId(), spec.id()); }
    @Override public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (!enabled() || slot != getEquipmentSlot()) return ImmutableMultimap.of();
        var out = ImmutableMultimap.<Attribute, AttributeModifier>builder().putAll(super.getDefaultAttributeModifiers(slot));
        if (!spec.melee()) {
            add(out, AttributeRegistry.MAX_MANA.get(), 125, AttributeModifier.Operation.ADDITION);
            add(out, HazenAttributes.get(spec.school() + "_spell_power"), .1, AttributeModifier.Operation.MULTIPLY_BASE);
            add(out, AttributeRegistry.SPELL_POWER.get(), .05, AttributeModifier.Operation.MULTIPLY_BASE);
        } else if (spec.id().equals("frostbite_knight")) {
            add(out, AttributeRegistry.MAX_MANA.get(), 125, AttributeModifier.Operation.ADDITION);
            add(out, AttributeRegistry.ICE_SPELL_POWER.get(), .15, AttributeModifier.Operation.MULTIPLY_BASE);
            add(out, Attributes.ATTACK_SPEED, .1, AttributeModifier.Operation.MULTIPLY_TOTAL);
            add(out, Attributes.ATTACK_DAMAGE, 1, AttributeModifier.Operation.ADDITION);
            add(out, AttributeRegistry.SPELL_POWER.get(), .1, AttributeModifier.Operation.MULTIPLY_BASE);
        } else {
            add(out, Attributes.ATTACK_SPEED, .15, AttributeModifier.Operation.MULTIPLY_TOTAL);
            add(out, Attributes.ATTACK_DAMAGE, 1.5, AttributeModifier.Operation.ADDITION);
            add(out, Attributes.MAX_HEALTH, 2.5, AttributeModifier.Operation.ADDITION);
            // Optional lookup: absent Malum never becomes a class-loading dependency.
            var scythe = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("malum", "scythe_proficiency"));
            if (scythe != null) add(out, scythe, .1, AttributeModifier.Operation.MULTIPLY_BASE);
        }
        return out.build();
    }
    private void add(ImmutableMultimap.Builder<Attribute, AttributeModifier> out, Attribute attribute, double value, AttributeModifier.Operation operation) {
        var key = "hazen_armor/" + shortId() + "/" + ForgeRegistries.ATTRIBUTES.getKey(attribute);
        out.put(attribute, new AttributeModifier(UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)), key, value, operation));
    }
    @Override public ItemStack getDefaultInstance() {
        var stack = super.getDefaultInstance(); initializeSpellContainer(stack); return stack;
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
        if (!enabled()) lines.add(Component.translatable("ui.alex_caves_spellsbook.hazen_armor_disabled").withStyle(ChatFormatting.RED));
        if (spec.melee()) {
            lines.add(Component.translatable("ui.alex_caves_spellsbook.hazen_armor_affinity").withStyle(ChatFormatting.AQUA));
            lines.add(Component.translatable("ui.alex_caves_spellsbook.hazen_armor_set").withStyle(ChatFormatting.GOLD));
        }
    }
    @Override @OnlyIn(Dist.CLIENT) public GeoArmorRenderer<?> supplyRenderer() {
        return new cn.wanyiwige.alexcavesspellsbook.hazen.client.HazenArmorRenderer();
    }
    private static int baseDurability(Type type) {
        return switch (type) { case HELMET -> 11; case CHESTPLATE -> 16; case LEGGINGS -> 15; case BOOTS -> 13; };
    }
    private record Material(HazenArmor.SetSpec spec) implements IronsExtendedArmorMaterial {
        public int getDurabilityForType(Type type) { return baseDurability(type) * spec.durability_multiplier(); }
        public int getDefenseForType(Type type) {
            int[] values = !spec.melee() ? new int[]{3,8,6,3} : spec.id().equals("frostbite_knight") ? new int[]{5,10,8,5} : new int[]{4,9,6,4};
            return values[switch(type) {case HELMET -> 0;case CHESTPLATE -> 1;case LEGGINGS -> 2;case BOOTS -> 3;}];
        }
        public int getEnchantmentValue() { return spec.id().equals("dreadsteel_knight") ? 10 : 20; }
        public SoundEvent getEquipSound() { return spec.id().equals("frostbite_knight") ? HazenSounds.get("cryogenic_ruler_equip") : spec.melee() ? SoundEvents.ARMOR_EQUIP_NETHERITE : SoundEvents.ARMOR_EQUIP_LEATHER; }
        public Ingredient getRepairIngredient() {
            var key = new ResourceLocation(spec.id().equals("dreadsteel_knight") ? "alex_caves_spellsbook:hazen_dreadsteel_ingot" : !spec.melee() ? "irons_spellbooks:magic_cloth" : "irons_spellbooks:mithril_weave");
            return ForgeRegistries.ITEMS.containsKey(key) ? Ingredient.of(ForgeRegistries.ITEMS.getValue(key)) : Ingredient.EMPTY;
        }
        public String getName() { return "alex_caves_spellsbook:hazen_" + spec.id(); }
        public float getToughness() { return !spec.melee() ? 0 : spec.id().equals("frostbite_knight") ? 4 : 2; }
        public float getKnockbackResistance() { return !spec.melee() ? 0 : spec.id().equals("frostbite_knight") ? .15f : .05f; }
        public Map<Attribute, AttributeModifier> getAdditionalAttributes() { return Map.of(); }
    }
}
