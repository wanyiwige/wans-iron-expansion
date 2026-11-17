package cn.wanyiwige.astralcompat.recreation;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=AlexCavesSpellsbook.MOD_ID)
public final class ArcanumEvents {
    public static final String WARD_UNTIL="WanArcanumSilverWardUntil";
    public static boolean hostile(LivingEntity caster,Entity target) {
        if(target==caster || caster.isAlliedTo(target)) return false;
        if(target instanceof TamableAnimal pet && pet.isOwnedBy(caster)) return false;
        return !(target instanceof IMagicSummon summon && summon.getSummoner()==caster);
    }
    @SubscribeEvent public static void attack(LivingAttackEvent event) {
        LivingEntity target=event.getEntity();
        if(target.level().isClientSide || event.getAmount()<=0) return;
        long until=target.getPersistentData().getLong(WARD_UNTIL);
        if(until>target.level().getGameTime()) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING,10,1)); event.setCanceled(true); return;
        }
        if(until!=0) target.getPersistentData().remove(WARD_UNTIL);
        if(target.getVehicle() instanceof ArcanumEffect tether && tether.absorb(event.getSource())) event.setCanceled(true);
    }
    private ArcanumEvents() {}
}
