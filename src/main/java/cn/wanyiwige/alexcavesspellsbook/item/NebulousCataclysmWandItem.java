package cn.wanyiwige.alexcavesspellsbook.item;

import cn.wanyiwige.alexcavesspellsbook.registry.ModSpells;
import cn.wanyiwige.astralcompat.registry.AstralAttributes;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.item.ISpellbook;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainerMutable;
import io.redspace.ironsspellbooks.item.weapons.StaffItem;
import net.kamaarion.roacw.registeries.ROACWAttributeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Native Iron's staff with a usable handheld twelve-slot spell container. */
public final class NebulousCataclysmWandItem extends StaffItem implements IPresetSpellContainer, ISpellbook {
    public static final int SPELL_SLOTS = 12;
    public static final int BUILT_IN_LEVEL = 1;
    private Multimap<Attribute, AttributeModifier> mainHandModifiers;

    public NebulousCataclysmWandItem(Item.Properties properties) {
        super(properties, 0, 0, Map.of());
    }

    @Override public void initializeSpellContainer(ItemStack stack) {
        if (stack == null || ISpellContainer.isSpellContainer(stack)) return;
        // mustEquip=false: the spell wheel can use the staff held in either hand.
        ISpellContainerMutable spells = ISpellContainer.create(SPELL_SLOTS, true, false).mutableCopy();
        if (!spells.addSpell(ModSpells.NEBULOUS_CATACLYSM.get(), BUILT_IN_LEVEL, true))
            throw new IllegalStateException("Could not initialize Nebulous Cataclysm in its staff");
        ISpellContainer.set(stack, spells.toImmutable());
    }

    @Override public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        initializeSpellContainer(stack);
        return stack;
    }

    @Override public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        initializeSpellContainer(stack);
        super.inventoryTick(stack, level, holder, slot, selected);
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        initializeSpellContainer(player.getItemInHand(hand));
        return super.use(level, player, hand);
    }

    @Override public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot != EquipmentSlot.MAINHAND) return super.getDefaultAttributeModifiers(slot);
        if (mainHandModifiers == null) {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
            builder.putAll(super.getDefaultAttributeModifiers(slot));
            add(builder, AttributeRegistry.ENDER_SPELL_POWER.get(), "ender", 0.20);
            add(builder, ROACWAttributeRegistry.EXO_MAGIC_POWER.get(), "exo", 0.20);
            add(builder, AstralAttributes.SPELL_POWER.get(), "astral", 0.20);
            // Iron's 3.16.3 stores reduction attributes around 1.0; positive bonuses reduce time.
            add(builder, AttributeRegistry.COOLDOWN_REDUCTION.get(), "cooldown", 0.20);
            add(builder, AttributeRegistry.CAST_TIME_REDUCTION.get(), "cast_time", 0.50);
            mainHandModifiers = builder.build();
        }
        return mainHandModifiers;
    }

    private static void add(ImmutableMultimap.Builder<Attribute, AttributeModifier> builder,
                            Attribute attribute, String key, double amount) {
        UUID id = UUID.nameUUIDFromBytes(("alex_caves_spellsbook:nebulous_cataclysm_wand/" + key)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        builder.put(attribute, new AttributeModifier(id, "Nebulous Cataclysm " + key,
                amount, AttributeModifier.Operation.ADDITION));
    }

    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
        lines.add(Component.translatable("item.alex_caves_spellsbook.nebulous_cataclysm_wand.slots", SPELL_SLOTS)
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("item.alex_caves_spellsbook.nebulous_cataclysm_wand.preset")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
