package cn.wanyiwige.alexcavesspellsbook.toxic;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** One matrix cell per distinct radiation effect cast, before damage is applied. */
@Mod.EventBusSubscriber(modid=AlexCavesSpellsbook.MOD_ID)
public final class RadiationHitEvents {
    @SubscribeEvent public static void tickMarks(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event){
        LivingEntity target=event.getEntity();
        if(!target.level().isClientSide&&target.tickCount%20==0&&(target.hasEffect(ToxicRegistry.MARK_VISUAL.get())||ToxicMarks.hasStoredMarks(target)))
            ToxicMarks.refreshVisual(target,target.level().getGameTime());
    }
    @SubscribeEvent public static void onPreCast(SpellPreCastEvent event) {
        if(event.getEntity().level().isClientSide||!ToxicMigrations.SPELLS.contains(event.getSpellId()))return;
        event.getEntity().getPersistentData().putUUID("WanRadiationCast_"+event.getSpellId(),UUID.randomUUID());
    }
    @SubscribeEvent public static void onHurt(LivingHurtEvent event) {
        if(!(event.getEntity().level() instanceof ServerLevel server)||event.getAmount()<=0)return;
        Entity direct=event.getSource().getDirectEntity();
        LivingEntity caster;UUID castId;
        if(direct instanceof ToxicEffectEntity effect){
            if(effect.spec()==ToxicSpec.MATRIX)return;
            caster=effect.owner();castId=effect.getUUID();
        }else if(event.getSource() instanceof SpellDamageSource spellSource
                &&ToxicMigrations.SPELLS.contains(spellSource.spell().getSpellResource().toString())
                &&event.getSource().getEntity() instanceof LivingEntity owner){
            caster=owner;
            String spellId=spellSource.spell().getSpellResource().toString();
            String key="WanRadiationCast_"+spellId;
            castId=direct!=null&&direct!=caster?direct.getUUID():caster.getPersistentData().hasUUID(key)
                    ?caster.getPersistentData().getUUID(key)
                    :UUID.nameUUIDFromBytes((caster.getUUID()+":"+spellId).getBytes(StandardCharsets.UTF_8));
        }else return;
        if(caster==null)return;
        Vec3 origin=direct instanceof ToxicEffectEntity effect?effect.damageOrigin():caster.getEyePosition();
        for(RadiationNewEffectEntity matrix:server.getEntitiesOfClass(RadiationNewEffectEntity.class,
                event.getEntity().getBoundingBox().inflate(24),e->e.spec()==ToxicSpec.MATRIX&&caster.getUUID().equals(e.ownerUUID())))
            matrix.registerRadiationHit(server,caster,event.getEntity(),castId,origin);
    }
    private RadiationHitEvents(){}
}
