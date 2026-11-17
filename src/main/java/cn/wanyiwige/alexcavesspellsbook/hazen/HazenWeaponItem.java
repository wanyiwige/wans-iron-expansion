package cn.wanyiwige.alexcavesspellsbook.hazen;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.item.ISpellbook;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.item.weapons.StaffTier;
import io.redspace.ironsspellbooks.item.weapons.IronsWeaponTier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Native sword combat, original attributes, Iron's NBT containers and GeckoLib rendering. */
public final class HazenWeaponItem extends SwordItem implements GeoItem, IPresetSpellContainer, ISpellbook {
    public final HazenWeaponSpec spec;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private Multimap<Attribute, AttributeModifier> modifiers;

    public HazenWeaponItem(HazenWeaponSpec spec) {
        super(new WeaponTier(spec), 0, (float)spec.speed, properties(spec));
        this.spec = spec;
    }
    private static Properties properties(HazenWeaponSpec spec) {
        var props = new Properties().stacksTo(1).rarity(HazenRarities.get(spec.rarity));
        if (spec.fire_resistant) props.fireResistant();
        if (spec.durability > 0) props.durability(spec.durability);
        return props;
    }
    private record WeaponTier(HazenWeaponSpec spec) implements Tier {
        public int getUses() { return spec.durability; }
        public float getSpeed() { return 1; }
        public float getAttackDamageBonus() { return (float)spec.damage; }
        public int getLevel() { return 4; }
        public int getEnchantmentValue() { return spec.enchantment; }
        public Ingredient getRepairIngredient() { return HazenMaterials.repairIngredient(); }
    }
    @Override public int getMaxDamage(ItemStack stack) { return spec.durability; }
    @Override public boolean isDamageable(ItemStack stack) { return spec.durability > 0; }
    @Override public int getEnchantmentValue() { return spec.enchantment; }
    public int getMaxSpellSlots() { return spec.presets.size(); }
    @Override public void initializeSpellContainer(ItemStack stack) {
        if (stack == null || spec.presets.isEmpty() || ISpellContainer.isSpellContainer(stack)) return;
        var container = ISpellContainer.create(spec.presets.size(), true, false).mutableCopy();
        for (var preset : spec.presets) {
            var spell = HazenSpells.resolve(preset.source_field());
            if (!container.addSpell(spell, preset.level(), true))
                throw new IllegalStateException("Cannot install " + preset.source_field() + " in " + spec.id);
        }
        ISpellContainer.set(stack, container.toImmutable());
    }
    @Override public ItemStack getDefaultInstance() {
        var stack = super.getDefaultInstance();
        initializeSpellContainer(stack);
        return stack;
    }
    @Override public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        if (!level.isClientSide) initializeSpellContainer(stack);
        super.inventoryTick(stack, level, holder, slot, selected);
    }
    @Override public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot != EquipmentSlot.MAINHAND || !HazenConfig.itemEnabled(spec.id)) return ImmutableMultimap.of();
        if (modifiers == null) {
            var b = ImmutableMultimap.<Attribute, AttributeModifier>builder();
            boolean artificer = "ARTIFICER".equals(spec.vanilla_staff_tier);
            // The upstream custom tier interface uses vanilla-like names. Its production
            // method mappings are supplied explicitly in gradle/iss-weapon-tier.tsrg.
            IronsWeaponTier staffTier = StaffTier.ARTIFICER;
            b.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier",
                    artificer ? staffTier.getAttackDamageBonus() : spec.damage, AttributeModifier.Operation.ADDITION));
            b.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier",
                    artificer ? staffTier.getSpeed() : spec.speed, AttributeModifier.Operation.ADDITION));
            if (artificer) for (var extra : StaffTier.ARTIFICER.getAdditionalAttributes())
                b.put(extra.attribute().get(), extra.createModifier("mainhand"));
            for (var extra : spec.attributes) {
                var attribute = HazenAttributes.resolve(extra.source_attribute());
                var operation = switch (extra.operation()) {
                    case "ADD_VALUE" -> AttributeModifier.Operation.ADDITION;
                    case "ADD_MULTIPLIED_BASE" -> AttributeModifier.Operation.MULTIPLY_BASE;
                    case "ADD_MULTIPLIED_TOTAL" -> AttributeModifier.Operation.MULTIPLY_TOTAL;
                    default -> throw new IllegalStateException(extra.operation());
                };
                // Original libraries gave these bonus attributes a zero base. Multiplying zero
                // loses the advertised bonus; recreated zero-base bonuses use addition.
                if (attribute.getDefaultValue() == 0 && operation != AttributeModifier.Operation.ADDITION)
                    operation = AttributeModifier.Operation.ADDITION;
                UUID id = UUID.nameUUIDFromBytes(("hazen-recreated/" + extra.source_attribute()).getBytes(StandardCharsets.UTF_8));
                b.put(attribute, new AttributeModifier(id, "Hazen " + extra.source_attribute(), extra.amount(), operation));
            }
            modifiers = b.build();
        }
        return modifiers;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!HazenConfig.itemEnabled(spec.id)) return InteractionResultHolder.fail(stack);
        if (spec.id.equals("vampire_knives")) {
            if (!player.getCooldowns().isOnCooldown(this) && !level.isClientSide) HazenProjectiles.throwKnives(player, this);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        initializeSpellContainer(stack);
        if (spec.presets.isEmpty() && !spec.kind.equals("staff")) return InteractionResultHolder.pass(stack);
        var selection = new SpellSelectionManager(player).getSelection();
        if (selection == null || selection.spellData.equals(SpellData.EMPTY)) return InteractionResultHolder.pass(stack);
        if (level.isClientSide) return InteractionResultHolder.consume(stack);
        var data = selection.spellData;
        String slot = hand == InteractionHand.MAIN_HAND ? SpellSelectionManager.MAINHAND : SpellSelectionManager.OFFHAND;
        return data.getSpell().attemptInitiateCast(stack, data.getSpell().getLevelFor(data.getLevel(), player),
                level, player, selection.getCastSource(), true, slot)
                ? InteractionResultHolder.consume(stack) : InteractionResultHolder.fail(stack);
    }
    @Override public int getUseDuration(ItemStack stack) { return 7200; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }
    @Override public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int ticks) {
        user.stopUsingItem();
        Utils.releaseUsingHelper(user, stack, ticks);
    }
    @Override public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!HazenConfig.itemEnabled(spec.id)) return false;
        if (!attacker.level().isClientSide && spec.id.matches("ionic_splitter_t[123]"))
            HazenSounds.play(attacker.level(), target.position(), "electric_impact", 1, 1);
        if (spec.durability == 0) return true;
        return super.hurtEnemy(stack, target, attacker);
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
        for (var key : spec.descriptions) lines.add(Component.translatable("hazen_recreated." + key).withStyle(ChatFormatting.GRAY));
        if (!HazenConfig.itemEnabled(spec.id)) lines.add(Component.translatable("tooltip.alex_caves_spellsbook.hazen_disabled").withStyle(ChatFormatting.RED));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        if (spec.animated && spec.animation_name != null && !spec.animation_name.isBlank())
            controllers.add(new AnimationController<>(this, "hazen", 0, event -> {
                event.getController().setAnimation(RawAnimation.begin().thenLoop(spec.animation_name));
                return PlayState.CONTINUE;
            }));
    }
    @Override public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        cn.wanyiwige.alexcavesspellsbook.hazen.client.HazenWeaponRenderer.register(this, consumer);
    }
}
