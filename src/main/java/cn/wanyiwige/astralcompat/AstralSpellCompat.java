package cn.wanyiwige.astralcompat;

import cn.wanyiwige.astralcompat.registry.ModEntities;
import cn.wanyiwige.astralcompat.registry.ModItems;
import cn.wanyiwige.astralcompat.registry.ModCreativeTabs;
import cn.wanyiwige.astralcompat.registry.AstralAttributes;
import cn.wanyiwige.astralcompat.registry.AstralSchools;
import cn.wanyiwige.astralcompat.registry.AstralSpells;
import net.minecraftforge.eventbus.api.IEventBus;

public final class AstralSpellCompat {
    public static final String MOD_ID = "astral_spell_compat";

    public static void register(IEventBus modBus) {
        cn.wanyiwige.astralcompat.recreation.ArcanumContent.register(modBus);
        ModEntities.ENTITY_TYPES.register(modBus);
        ModItems.ITEMS.register(modBus);
        AstralAttributes.ATTRIBUTES.register(modBus);
        AstralSchools.SCHOOLS.register(modBus);
        AstralSpells.SPELLS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
    }

    private AstralSpellCompat() {
    }
}
