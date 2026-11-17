package cn.wanyiwige.astralcompat;

import cn.wanyiwige.astralcompat.registry.AstralAttributes;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.fml.ModList;
import java.util.List;
import java.util.function.BiFunction;
import net.minecraft.world.item.ItemStack;
import cn.wanyiwige.astralcompat.recreation.ArcanumContent;

/** Shared hooks for the audited Tempest Almanac school compatibility patch. */
public final class TempestAstralIntegration {
    public static final List<String> SPELLS = List.of("meteor_strike", "comet_dash", "star_ward",
            "starfall", "gravity_well", "cosmic_burn");

    public static ItemStack focusIcon() {
        return ArcanumContent.WINGS.get().getDefaultInstance();
    }

    public static void addScrolls(CreativeModeTab.Output output, BiFunction<String, Integer, ItemStack> scroll) {
        if (!ModList.get().isLoaded("tempest_almanac")) return;
        for (String path : SPELLS) {
            ResourceLocation id = new ResourceLocation("tempest_almanac", path);
            var spell = SpellRegistry.getSpell(id);
            if (!id.equals(spell.getSpellResource()) || !spell.isEnabled()) continue;
            for (int level = spell.getMinLevel(); level <= spell.getMaxLevel(); level++) {
                output.accept(scroll.apply(spell.getSpellId(), level));
            }
        }
    }

    /** Native Astral stores a bonus around zero; Tempest's old attribute had base one. */
    public static AttributeModifier.Operation boonOperation(Attribute attribute) {
        return attribute == AstralAttributes.SPELL_POWER.get()
                ? AttributeModifier.Operation.ADDITION : AttributeModifier.Operation.MULTIPLY_BASE;
    }
    private TempestAstralIntegration() {}
}
