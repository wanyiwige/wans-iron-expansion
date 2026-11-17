package cn.wanyiwige.twilight;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;

public final class TwilightRegistration {
    public static void attributes(EntityAttributeCreationEvent event){
        event.put(TwilightEntities.WINTER_WOLF.get(),twilightforest.entity.monster.WinterWolf.registerAttributes().build());
        event.put(TwilightEntities.MINOTAUR.get(),twilightforest.entity.monster.Minotaur.registerAttributes().build());
        event.put(TwilightEntities.CARMINITE_GOLEM.get(),twilightforest.entity.monster.CarminiteGolem.registerAttributes().build());
        event.put(TwilightEntities.DEATH_TOME.get(),twilightforest.entity.monster.DeathTome.registerAttributes().build());
        event.put(TwilightEntities.LOYAL_ZOMBIE.get(),net.minecraft.world.entity.monster.Zombie.createAttributes().add(Attributes.MAX_HEALTH,40).add(Attributes.ATTACK_DAMAGE,3).add(Attributes.ARMOR,3).add(Attributes.ATTACK_KNOCKBACK,2).add(Attributes.MOVEMENT_SPEED,.3).build());
        event.put(TwilightEntities.OMINOUS_LICH.get(),TwilightCaster.attributes(true).build());
        event.put(TwilightEntities.ADVANCED_DRUID.get(),TwilightCaster.attributes(false).build());
    }
    public static void extraAttributes(EntityAttributeModificationEvent event){
        for(var type:event.getTypes())for(var attribute:TwilightAttributes.ATTRIBUTES.getEntries())if(!event.has(type,attribute.get()))event.add(type,attribute.get());
    }
    private TwilightRegistration(){}
}
