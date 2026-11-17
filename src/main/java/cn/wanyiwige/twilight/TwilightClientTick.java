package cn.wanyiwige.twilight;
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="alex_caves_spellsbook",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class TwilightClientTick {
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event){if(event.phase==net.minecraftforge.event.TickEvent.Phase.END)TwilightBossClient.tick();}
    private TwilightClientTick(){}
}
