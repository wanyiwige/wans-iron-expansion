package cn.wanyiwige.twilight;

import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import twilightforest.entity.EnforcedHomePoint;

public final class TwilightWorldEvents {
    @SubscribeEvent public static void spawned(MobSpawnEvent.FinalizeSpawn event){if(event.getSpawnType()==MobSpawnType.SPAWN_EGG)event.getEntity().getPersistentData().putBoolean("TwilightFromEgg",true);}
    @SubscribeEvent public static void death(LivingDeathEvent event){
        LivingEntity target=event.getEntity();if(!(target.level() instanceof ServerLevel server)||!(event.getSource().getEntity() instanceof Player)||target.getPersistentData().getBoolean("TwilightFromEgg"))return;
        ResourceLocation id=ForgeRegistries.ENTITY_TYPES.getKey(target.getType());if(!id.getNamespace().equals("twilightforest"))return;
        String soul=switch(id.getPath()){case "lich"->"lich_soul";case "snow_queen"->"snow_queen_soul";case "ur_ghast"->"urghast_soul";case "hydra"->"hydra_soul";default->null;};if(soul==null)return;
        GlobalPos home=target instanceof EnforcedHomePoint point?point.getRestrictionPoint():null;
        if(home!=null)TwilightSoul.spawn(server,soul,target.getBoundingBox().getCenter(),net.minecraft.world.phys.Vec3.atCenterOf(home.pos()));
    }
    @SubscribeEvent public static void transform(PlayerInteractEvent.EntityInteract event){
        if(!(event.getLevel() instanceof ServerLevel server))return;
        Entity target=event.getTarget();ResourceLocation id=ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        var powder=ForgeRegistries.ITEMS.getValue(new ResourceLocation("twilightforest","transformation_powder"));
        if(!event.getItemStack().is(powder)||!id.equals(new ResourceLocation("irons_spellbooks","necromancer")))return;
        var druid=TwilightEntities.ADVANCED_DRUID.get().create(server);druid.moveTo(target.getX(),target.getY(),target.getZ(),target.getYRot(),target.getXRot());druid.finalizeSpawn(server,server.getCurrentDifficultyAt(druid.blockPosition()),MobSpawnType.CONVERSION,null,null);
        if(server.addFreshEntity(druid)){target.discard();if(!event.getEntity().isCreative())event.getItemStack().shrink(1);event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);event.setCanceled(true);}
    }
    private TwilightWorldEvents(){}
}
