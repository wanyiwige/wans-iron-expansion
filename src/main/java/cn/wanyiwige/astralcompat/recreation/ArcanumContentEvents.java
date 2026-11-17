package cn.wanyiwige.astralcompat.recreation;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.core.Position;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;

@Mod.EventBusSubscriber(modid=AlexCavesSpellsbook.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class ArcanumContentEvents {
    @SubscribeEvent public static void attributes(EntityAttributeCreationEvent event) {
        event.put(ArcanumContent.DRAGONFLY.get(),ArcanumInsect.attributes(true).build());
        event.put(ArcanumContent.LUNAR_MOTH.get(),ArcanumInsect.attributes(false).build());
        event.put(ArcanumContent.SUMMONED_MOTH.get(),ArcanumInsect.attributes(false).build());
        event.put(ArcanumContent.ASTROMANCER.get(),Astromancer.attributes().build());
    }
    @SubscribeEvent public static void placements(SpawnPlacementRegisterEvent event) {
        event.register(ArcanumContent.LUNAR_MOTH.get(),SpawnPlacements.Type.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type,level,reason,pos,random)->level.getBlockState(pos.below()).isValidSpawn(level,pos.below(),type),SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
    @SubscribeEvent public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(()->DispenserBlock.registerBehavior(ArcanumContent.MOTHSPAWN.get(),new AbstractProjectileDispenseBehavior() {
            @Override protected Projectile getProjectile(Level level,Position pos,ItemStack stack) {
                MothspawnProjectile egg=new MothspawnProjectile(level,pos.x(),pos.y(),pos.z());egg.setItem(stack);return egg;
            }
        }));
    }
    @Mod.EventBusSubscriber(modid=AlexCavesSpellsbook.MOD_ID)
    public static final class Loot {
        @SubscribeEvent public static void table(net.minecraftforge.event.LootTableLoadEvent event) {
            if(event.getName().equals(new net.minecraft.resources.ResourceLocation("minecraft","gameplay/sniffer_digging"))) {
                event.getTable().addPool(net.minecraft.world.level.storage.loot.LootPool.lootPool().name("wan_astral_mothspawn")
                        .setRolls(net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(1))
                        .add(net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(ArcanumContent.MOTHSPAWN.get())
                                .when(net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition.randomChance(.5F))).build());
            }
        }
    }
    private ArcanumContentEvents() {}
}
