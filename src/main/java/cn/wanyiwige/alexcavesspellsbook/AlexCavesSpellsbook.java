package cn.wanyiwige.alexcavesspellsbook;

import cn.wanyiwige.astralcompat.AstralSpellCompat;
import cn.wanyiwige.alexcavesspellsbook.registry.ModCreativeTabs;
import cn.wanyiwige.alexcavesspellsbook.registry.ModAttributes;
import cn.wanyiwige.alexcavesspellsbook.registry.ModEffects;
import cn.wanyiwige.alexcavesspellsbook.registry.ModEntities;
import cn.wanyiwige.alexcavesspellsbook.registry.ModItems;
import cn.wanyiwige.alexcavesspellsbook.registry.ModSchools;
import cn.wanyiwige.alexcavesspellsbook.registry.ModSpells;
import cn.wanyiwige.alexcavesspellsbook.traveller.TravellerAttributes;
import cn.wanyiwige.alexcavesspellsbook.traveller.TravellerItems;
import cn.wanyiwige.alexcavesspellsbook.traveller.TravellerSchools;
import cn.wanyiwige.alexcavesspellsbook.traveller.TravellerSpells;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(AlexCavesSpellsbook.MOD_ID)
public final class AlexCavesSpellsbook {
    public static final String MOD_ID = "alex_caves_spellsbook";

    public AlexCavesSpellsbook(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        ModItems.ITEMS.register(modEventBus);
        ModAttributes.ATTRIBUTES.register(modEventBus);
        cn.wanyiwige.alexcavesspellsbook.registry.PureDarknessParticles.TYPES.register(modEventBus);
        ModEffects.EFFECTS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModSchools.SCHOOLS.register(modEventBus);
        ModSpells.SPELLS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        modEventBus.addListener(ModCreativeTabs::addRoacwEquipment);
        AstralSpellCompat.register(modEventBus);
        TravellerItems.ITEMS.register(modEventBus);
        TravellerAttributes.ATTRIBUTES.register(modEventBus);
        TravellerSchools.SCHOOLS.register(modEventBus);
        TravellerSpells.SPELLS.register(modEventBus);
        modEventBus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) ->
                event.enqueueWork(cn.wanyiwige.alexcavesspellsbook.traveller.MapTravelNetwork::register));
        modEventBus.<net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent>addListener(event ->
                event.enqueueWork(cn.wanyiwige.alexcavesspellsbook.network.PureDarknessInput::register));
        cn.wanyiwige.alexcavesspellsbook.candy.CandyRegistry.register(modEventBus);
        cn.wanyiwige.alexcavesspellsbook.foolish.FoolishSpells.register(modEventBus);
        cn.wanyiwige.alexcavesspellsbook.toxic.ToxicRegistry.register(modEventBus);
        cn.wanyiwige.alexcavesspellsbook.hazen.HazenEquipment.register(context, modEventBus);
        cn.wanyiwige.alexcavesspellsbook.easyarcane.EasyArcaneContent.register(modEventBus);
        cn.wanyiwige.twilight.TwilightFoundation.register(modEventBus);
        cn.wanyiwige.illusion.IllusionContent.register(modEventBus);
    }
}
