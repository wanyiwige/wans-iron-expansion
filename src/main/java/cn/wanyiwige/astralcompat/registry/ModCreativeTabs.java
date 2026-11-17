package cn.wanyiwige.astralcompat.registry;

import cn.wanyiwige.astralcompat.AstralSpellCompat;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AstralSpellCompat.MOD_ID);

    public static final RegistryObject<CreativeModeTab> ASTRAL = TABS.register("astral", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.astral_spell_compat.astral"))
            .icon(cn.wanyiwige.astralcompat.TempestAstralIntegration::focusIcon)
            .displayItems((parameters, output) -> {
                output.accept(ModItems.ASTRAL_LANCE.get());
                cn.wanyiwige.astralcompat.recreation.ArcanumContent.ITEMS.getEntries().forEach(item -> output.accept(item.get().getDefaultInstance()));
                for (RegistryObject<AbstractSpell> entry : AstralSpells.SPELLS.getEntries()) {
                    // The gateway is already available through its unique talisman above.
                    if (entry == AstralSpells.ASTRAL_GATEWAY) continue;
                    AbstractSpell spell = entry.get();
                    for (int level = 1; level <= spell.getMaxLevel(); level++) {
                        output.accept(createSpellScroll(spell.getSpellId(), level));
                    }
                }
                cn.wanyiwige.astralcompat.TempestAstralIntegration.addScrolls(output, ModCreativeTabs::createSpellScroll);
            })
            .build());

    private ModCreativeTabs() {
    }

    private static ItemStack createSpellScroll(String spellId, int level) {
        ItemStack scroll = new ItemStack(ItemRegistry.SCROLL.get());
        CompoundTag spellData = new CompoundTag();
        spellData.putString("id", spellId);
        spellData.putInt("index", 0);
        spellData.putInt("level", level);
        spellData.putBoolean("locked", true);

        ListTag spells = new ListTag();
        spells.add(spellData);

        CompoundTag container = new CompoundTag();
        container.put("data", spells);
        container.putInt("maxSpells", 1);
        container.putBoolean("mustEquip", false);
        container.putBoolean("spellWheel", false);
        scroll.getOrCreateTag().put("irons_spellbooks:spell_container", container);
        return scroll;
    }
}
