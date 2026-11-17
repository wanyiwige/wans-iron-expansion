package cn.wanyiwige.alexcavesspellsbook.foolish;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;
import java.util.*;

public final class FoolishSpells {
    public static final DeferredRegister<AbstractSpell> SPELLS=DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY,AlexCavesSpellsbook.MOD_ID);
    public static final Map<BossSpellSpec,RegistryObject<AbstractSpell>> ENTRIES=new EnumMap<>(BossSpellSpec.class);
    static {for(BossSpellSpec spec:BossSpellSpec.values())ENTRIES.put(spec,SPELLS.register(spec.id,()->new BossSpell(spec)));}
    public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,AlexCavesSpellsbook.MOD_ID);
    public static final RegistryObject<EntityType<BossEffect>> EFFECT=ENTITIES.register("foolish_boss_effect",()->EntityType.Builder
            .<BossEffect>of(BossEffect::new,MobCategory.MISC).sized(0.4F,0.4F).clientTrackingRange(16).updateInterval(1).noSave().build("foolish_boss_effect"));
    public static final RegistryObject<EntityType<BossCompanion>> COMPANION=ENTITIES.register("foolish_boss_companion",()->EntityType.Builder
            .of(BossCompanion::new,MobCategory.CREATURE).sized(0.7F,1F).clientTrackingRange(12).updateInterval(1).noSave().build("foolish_boss_companion"));
    public static void register(IEventBus bus){
        SPELLS.register(bus);ENTITIES.register(bus);
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event)->
                event.put(COMPANION.get(),BossCompanion.createAttributes().build()));
    }
    private FoolishSpells(){}
}
