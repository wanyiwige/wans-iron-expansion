package cn.wanyiwige.alexcavesspellsbook.candy;

import com.github.alexmodguy.alexscaves.server.misc.ACSoundRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;
import java.util.*;

/** Thirteen implemented spells share registry/config plumbing, each with an explicit server timeline. */
public final class CandyFirstSpell extends AbstractSpell {
    private final CandySpecs.Spec spec;
    private final DefaultConfig config;
    public CandyFirstSpell(CandySpecs.Spec spec) {
        if(!CandyFirstRules.implemented(spec.id()))throw new IllegalArgumentException(spec.id());
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
                && (!(level instanceof ServerLevel server)||CandyFirstCasting.canCast(server,caster,spec));
    }
    @Override public Optional<SoundEvent> getCastStartSound(){return Optional.of(spec.id().startsWith("peppermint")
            ?ACSoundRegistry.SUGAR_STAFF_CAST_PEPPERMINT.get():spec.id().equals("frostmint_rain")
            ?ACSoundRegistry.FROSTMINT_SPEAR_THROW.get():spec.id().startsWith("soda_")
            ?ACSoundRegistry.PURPLE_SODA_SUBMERGE.get():spec.id().equals("caramel_bog")
            ?ACSoundRegistry.CARAMEL_CUBE_SMALL.get():ACSoundRegistry.SUGAR_STAFF_CAST_HEX.get());}
    @Override public Optional<SoundEvent> getCastFinishSound(){return Optional.of(spec.id().startsWith("peppermint")
            ? ACSoundRegistry.LICOWITCH_CAST_PEPPERMINT.get():ACSoundRegistry.GUMBALL_LAUNCH.get());}
    @Override public void onServerCastTick(Level level,int spellLevel,LivingEntity caster,MagicData data){
        if(level instanceof ServerLevel server && caster.tickCount%5==0){
            var hand=caster.getEyePosition().add(caster.getLookAngle().scale(0.45));
            server.sendParticles(new DustParticleOptions(new Vector3f(1,0.65F,0.78F),0.65F),hand.x,hand.y-0.3,hand.z,3,0.08,0.08,0.08,0);
        }
    }
    @Override public void onCast(Level level,int spellLevel,LivingEntity caster,CastSource source,MagicData data){
        if(level instanceof ServerLevel server && CandyFirstCasting.cast(server,caster,spec,spellLevel,getSpellPower(spellLevel,caster)))
            super.onCast(level,spellLevel,caster,source,data);
    }
    @Override public List<MutableComponent> getUniqueInfo(int level,LivingEntity caster){
        return List.of(Component.translatable("ui.alex_caves_spellsbook.candy.damage",spec.hitDamage(level)*getSpellPower(level,caster)),
                Component.translatable("ui.alex_caves_spellsbook.candy.limit",spec.maxHits()),
                Component.translatable("ui.alex_caves_spellsbook.candy.duration",spec.activeTicks()/20.0));
    }
}
