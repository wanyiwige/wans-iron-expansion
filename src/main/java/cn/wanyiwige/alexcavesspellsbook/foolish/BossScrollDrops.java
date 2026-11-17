package cn.wanyiwige.alexcavesspellsbook.foolish;
import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.alexcavesspellsbook.registry.ModSpells;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Each real Boss kill drops one level-I scroll for each of its selected spells. */
@Mod.EventBusSubscriber(modid=AlexCavesSpellsbook.MOD_ID)
public final class BossScrollDrops {
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void drops(LivingDropsEvent event){
        if(event.getEntity() instanceof BossCompanion){event.getDrops().clear();return;}
        if(event.getEntity().level().isClientSide)return;
        var id=ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType());
        if(id==null||!id.getNamespace().equals("foolish"))return;
        for(BossSpellSpec spec:BossSpellSpec.values())if(spec.boss.equals(id.getPath()))add(event,FoolishSpells.ENTRIES.get(spec).get());
        if(id.getPath().equals("sacilic_abomination")){add(event,ModSpells.SACILITE_RAIN.get());add(event,ModSpells.SEEKING_SACILITE_SWARM.get());}
    }
    private static void add(LivingDropsEvent event,AbstractSpell spell){
        if(!spell.isEnabled())return;
        ItemStack scroll=new ItemStack(ItemRegistry.SCROLL.get());ISpellContainer.createScrollContainer(spell,1,scroll);
        var boss=event.getEntity();ItemEntity drop=new ItemEntity(boss.level(),boss.getX(),boss.getY()+0.5,boss.getZ(),scroll);
        drop.setDefaultPickUpDelay();event.getDrops().add(drop);
    }
    @SubscribeEvent public static void xp(LivingExperienceDropEvent event){if(event.getEntity() instanceof BossCompanion)event.setDroppedExperience(0);}
    private BossScrollDrops(){}
}
