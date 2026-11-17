package cn.wanyiwige.alexcavesspellsbook.hazen.client;

import cn.wanyiwige.alexcavesspellsbook.hazen.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mod.EventBusSubscriber(modid="alex_caves_spellsbook",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class HazenAddedArmorKeys {
    private static final net.minecraft.client.KeyMapping ABILITY=new net.minecraft.client.KeyMapping("key.alex_caves_spellsbook.hazen_added_ability",72,"key.categories.hazen_added");
    @SubscribeEvent public static void keys(net.minecraftforge.client.event.RegisterKeyMappingsEvent e){e.register(ABILITY);}
    @Mod.EventBusSubscriber(modid="alex_caves_spellsbook",value=Dist.CLIENT)
    public static class Events {
        private static final java.util.Map<net.minecraft.client.model.PlayerModel<?>,java.util.Deque<boolean[]>> VISIBILITY=new java.util.IdentityHashMap<>();
        @SubscribeEvent public static void before(net.minecraftforge.client.event.RenderLivingEvent.Pre<?,?> e){
            if(e.getRenderer().getModel() instanceof net.minecraft.client.model.PlayerModel<?> model){
                VISIBILITY.computeIfAbsent(model,k->new java.util.ArrayDeque<>()).push(new boolean[]{model.hat.visible,model.jacket.visible,model.leftSleeve.visible,model.rightSleeve.visible});
                if(e.getEntity().getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).getItem() instanceof HazenAddedArmor.Armor a&&a.spec.hide_hat())model.hat.visible=false;
                if(e.getEntity().getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).getItem() instanceof HazenAddedArmor.Armor a&&a.spec.hide_jacket()){model.jacket.visible=false;model.leftSleeve.visible=false;model.rightSleeve.visible=false;}
            }
        }
        @SubscribeEvent public static void after(net.minecraftforge.client.event.RenderLivingEvent.Post<?,?> e){
            if(e.getRenderer().getModel() instanceof net.minecraft.client.model.PlayerModel<?> model){
                var stack=VISIBILITY.get(model);if(stack!=null&&!stack.isEmpty()){var old=stack.pop();model.hat.visible=old[0];model.jacket.visible=old[1];model.leftSleeve.visible=old[2];model.rightSleeve.visible=old[3];if(stack.isEmpty())VISIBILITY.remove(model);}
            }
        }
        @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent e){
            if(e.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;
            var p=net.minecraft.client.Minecraft.getInstance().player;
            while(ABILITY.consumeClick())if(p!=null&&HazenAddedArmorEffects.family(p,"garments_of_the_first_flamebearer"))HazenAddedArmorAbility.CHANNEL.sendToServer(new HazenAddedArmorAbility.Activate());
        }
    }
}
