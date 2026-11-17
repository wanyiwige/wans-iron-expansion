package cn.wanyiwige.alexcavesspellsbook.candy;

import com.github.alexmodguy.alexscaves.server.misc.ACSoundRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import java.util.*;

/** The 13 remaining spells use dedicated timelines and the same cast ledger as the first half. */
public final class CandyLastSpell extends AbstractSpell {
    private final CandySpecs.Spec spec;
    private final DefaultConfig config;
    private static final Map<UUID,CandyLastSpellEntity> CHANNELS=new HashMap<>();
    public CandyLastSpell(CandySpecs.Spec spec){
        if(!CandyLastRules.implemented(spec.id()))throw new IllegalArgumentException(spec.id());
        this.spec=spec;
        config=new DefaultConfig().setSchoolResource(CandyRegistry.SCHOOL_ID).setMinRarity(spec.rarity())
                .setMaxLevel(spec.maxLevel()).setCooldownSeconds(spec.cooldown()).setAllowCrafting(true).build();
        baseManaCost=spec.mana();manaCostPerLevel=spec.manaGrowth();baseSpellPower=1;spellPowerPerLevel=0;castTime=spec.castTicks();
    }
    @Override public ResourceLocation getSpellResource(){return CandyRegistry.id(spec.id());}
    @Override public DefaultConfig getDefaultConfig(){return config;}
    @Override public CastType getCastType(){return spec.castType();}
    @Override public boolean allowCrafting(){return true;}
    @Override public boolean allowLooting(){return true;}
    @Override public boolean checkPreCastConditions(Level level,int spellLevel,LivingEntity caster,MagicData data){
        return super.checkPreCastConditions(level,spellLevel,caster,data)
                &&(!(level instanceof ServerLevel server)||CandyLastCasting.canCast(server,caster,spec));
    }
    @Override public Optional<SoundEvent> getCastStartSound(){return Optional.of(ACSoundRegistry.SUGAR_STAFF_CAST_HEX.get());}
    @Override public Optional<SoundEvent> getCastFinishSound(){return Optional.of(ACSoundRegistry.GUMBALL_LAUNCH.get());}
    @Override public void onCast(Level level,int spellLevel,LivingEntity caster,CastSource source,MagicData data){
        if(!(level instanceof ServerLevel server))return;
        if(spec.id().equals("candy_cane_whirl")){
            CandyLastSpellEntity old=CHANNELS.get(caster.getUUID());
            if(old!=null&&!old.isRemoved()&&old.owner()==caster){super.onCast(level,spellLevel,caster,source,data);return;}
        }
        CandyLastSpellEntity entity=CandyLastCasting.cast(server,caster,spec,spellLevel,getSpellPower(spellLevel,caster));
        if(entity!=null){if(spec.id().equals("candy_cane_whirl"))CHANNELS.put(caster.getUUID(),entity);
            super.onCast(level,spellLevel,caster,source,data);}
    }
    @Override public void onServerCastComplete(Level level,int spellLevel,LivingEntity caster,MagicData data,boolean cancelled){
        if(spec.id().equals("candy_cane_whirl")){
            // The cast callback can run as soon as the book releases the spell.
            // The orbiting arms must outlive it and finish on their own 100-tick timeline.
            CHANNELS.remove(caster.getUUID());
        }
        super.onServerCastComplete(level,spellLevel,caster,data,cancelled);
    }
    @Override public List<MutableComponent> getUniqueInfo(int level,LivingEntity caster){
        if(spec.id().equals("rainbow_leap"))return List.of(Component.translatable("ui.alex_caves_spellsbook.candy.range",spec.range()));
        if(spec.id().equals("sweet_overdrive"))return List.of(
                Component.translatable("ui.alex_caves_spellsbook.candy.speed",20+5*(spec.clampLevel(level)-1)),
                Component.translatable("ui.alex_caves_spellsbook.candy.cast_speed",10+2.5*(spec.clampLevel(level)-1)),
                Component.translatable("ui.alex_caves_spellsbook.candy.duration",spec.activeTicks()/20.0));
        return List.of(Component.translatable("ui.alex_caves_spellsbook.candy.damage",spec.hitDamage(level)*getSpellPower(level,caster)),
                Component.translatable("ui.alex_caves_spellsbook.candy.limit",spec.maxHits()),
                Component.translatable("ui.alex_caves_spellsbook.candy.duration",spec.activeTicks()/20.0));
    }
    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook.MOD_ID)
    public static final class ChannelCleanup {
        @net.minecraftforge.eventbus.api.SubscribeEvent public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event){CHANNELS.remove(event.getEntity().getUUID());}
        @net.minecraftforge.eventbus.api.SubscribeEvent public static void stop(net.minecraftforge.event.server.ServerStoppedEvent event){CHANNELS.clear();}
    }
}
