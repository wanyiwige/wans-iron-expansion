package cn.wanyiwige.alexcavesspellsbook.traveller;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainerMutable;
import io.redspace.ironsspellbooks.item.SpellBook;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Preserves the eight-slot KubeJS codex and its original preloaded spells. */
public final class TravellerCodexItem extends SpellBook {
    public TravellerCodexItem(Item.Properties properties) {
        super(8, properties);
    }

    @Override
    public void initializeSpellContainer(ItemStack stack) {
        if (stack == null || ISpellContainer.isSpellContainer(stack)) {
            return;
        }
        ISpellContainerMutable container = ISpellContainer.create(getMaxSpellSlots(), true, true).mutableCopy();
        add(container, "apprenticecodex:auto_magnet", 10);
        add(container, "apprenticecodex:manifestation_grimoire", 1);
        add(container, "irons_spellbooks:pocket_dimension", 1);
        add(container, "irons_spellbooks:recall", 1);
        add(container, "irons_spellbooks:teleport", 5);
        add(container, "irons_spellbooks:portal", 3);
        ISpellContainer.set(stack, container.toImmutable());
    }

    private static void add(ISpellContainerMutable container, String spellId, int level) {
        ResourceLocation id = new ResourceLocation(spellId);
        AbstractSpell spell = SpellRegistry.getSpell(id);
        if (spell != null && spell.getSpellResource().equals(id)) {
            container.addSpell(spell, level, true);
        }
    }
}
