package cn.wanyiwige.illusion;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import net.minecraft.core.*;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import java.util.*;

public final class IllusionSpell extends AbstractSpell {
    private final String kind;private final DefaultConfig config;
    public IllusionSpell(String kind){
        this.kind=kind;int max;double cooldown;SpellRarity rarity;
        switch(kind){
            case "illusion_wall"->{baseManaCost=35;manaCostPerLevel=5;baseSpellPower=5;spellPowerPerLevel=10;castTime=0;max=4;cooldown=10;rarity=SpellRarity.COMMON;}
            case "fear_bolt"->{baseManaCost=20;manaCostPerLevel=5;baseSpellPower=6;spellPowerPerLevel=1;castTime=0;max=11;cooldown=12;rarity=SpellRarity.COMMON;}
            case "disguise","squish"->{baseManaCost=70;manaCostPerLevel=15;baseSpellPower=8;spellPowerPerLevel=8;castTime=40;max=8;cooldown=70;rarity=SpellRarity.EPIC;}
            case "paranoia"->{baseManaCost=50;manaCostPerLevel=10;baseSpellPower=20;spellPowerPerLevel=4;castTime=30;max=4;cooldown=80;rarity=SpellRarity.EPIC;}
            default->{baseManaCost=60;manaCostPerLevel=20;baseSpellPower=1;spellPowerPerLevel=0;castTime=50;max=1;cooldown=45;rarity=SpellRarity.RARE;}
        }
        config=new DefaultConfig().setSchoolResource(IllusionContent.id("illusion")).setMinRarity(rarity).setMaxLevel(max).setCooldownSeconds(cooldown).build();
    }
    @Override public ResourceLocation getSpellResource(){return IllusionContent.id(kind);}
    @Override public DefaultConfig getDefaultConfig(){return config;}
    @Override public CastType getCastType(){return castTime==0?CastType.INSTANT:CastType.LONG;}
    @Override public Optional<SoundEvent> getCastStartSound(){return Optional.empty();}
    @Override public Optional<SoundEvent> getCastFinishSound(){return Optional.of(kind.equals("fear_bolt")?SoundEvents.PHANTOM_AMBIENT:SoundEvents.ILLUSIONER_CAST_SPELL);}
    public int duration(int rank,LivingEntity caster){return Math.max(1,(int)(getSpellPower(rank,caster)*20*(kind.equals("emptiness")||kind.equals("depth_trick")?30:1)));}
    private int height(int rank,LivingEntity caster){return Math.min(32,1+rank+(int)(getSpellPower(rank,caster)*.2F));}
    @Override public List<MutableComponent> getUniqueInfo(int rank,LivingEntity caster){
        if(kind.equals("illusion_wall"))return List.of(Component.translatable("ui.illusionist_grimoire.wall_size",(2*(height(rank,caster)/2)+1)+"x"+height(rank,caster)));
        if(kind.equals("fear_bolt"))return List.of(Component.translatable("ui.irons_spellbooks.damage",Utils.stringTruncation(getSpellPower(rank,caster)*.5F,1)));
        return List.of(Component.translatable("ui.irons_spellbooks.effect_length",Utils.timeFromTicks(duration(rank,caster),1)));
    }
    @Override public boolean checkPreCastConditions(Level level,int rank,LivingEntity caster,MagicData data){
        if(kind.equals("illusion_wall")||kind.equals("fear_bolt"))return true;
        boolean close=kind.equals("disguise")||kind.equals("squish");
        boolean hit=Utils.preCastTargetHelper(level,caster,data,this,close?10:32,close?.15F:.35F);
        if(!hit){data.setAdditionalCastData(null);return kind.equals("squish");}
        if(data.getAdditionalCastData() instanceof TargetEntityCastData target && level instanceof ServerLevel server){
            LivingEntity victim=target.getTarget(server);
            if(victim==null)return false;
            if(close&&IllusionState.tagged(victim,kind.equals("disguise")?"disguise_blacklist":"squish_blacklist"))return false;
        }
        return true;
    }
    @Override public void onCast(Level level,int rank,LivingEntity caster,CastSource source,MagicData data){
        if(level instanceof ServerLevel server){
            LivingEntity target=data.getAdditionalCastData() instanceof TargetEntityCastData t?t.getTarget(server):null;
            switch(kind){
                case "illusion_wall"->{
                    Direction face=caster.getDirection(),side=face.getClockWise();BlockPos start=caster.blockPosition().relative(face,2);int h=height(rank,caster);
                    for(int y=0;y<h;y++)for(int x=-h/2;x<=h/2;x++){
                        BlockPos p=start.relative(side,x).above(y);if(!server.hasChunkAt(p)||!server.getBlockState(p).isAir())continue;
                        server.setBlock(p,IllusionContent.WALL.get().defaultBlockState(),3);
                        if(server.getBlockEntity(p) instanceof IllusionBlockEntity b){b.owner=caster.getUUID();b.ownerPower=IllusionState.attribute(caster,io.redspace.ironsspellbooks.api.registry.AttributeRegistry.EVOCATION_SPELL_POWER.get());b.expiry=server.getGameTime()+600;b.setChanged();server.sendBlockUpdated(p,b.getBlockState(),b.getBlockState(),3);}
                    }
                    caster.addEffect(new MobEffectInstance(io.redspace.ironsspellbooks.registries.MobEffectRegistry.TRUE_INVISIBILITY.get(),20,0,false,false));
                }
                case "fear_bolt"->{FearBolt bolt=new FearBolt(IllusionContent.FEAR_BOLT.get(),level);bolt.setOwner(caster);bolt.setPos(caster.getEyePosition());bolt.shoot(caster.getLookAngle());bolt.setDamage(getSpellPower(rank,caster)*.5F);server.addFreshEntity(bolt);}
                case "disguise"->{if(target!=null&&!IllusionState.tagged(target,"disguise_blacklist"))IllusionState.disguise(caster,IllusionState.capture(target),duration(rank,caster));}
                case "squish"->{if(target==null)target=caster;if(!IllusionState.tagged(target,"squish_blacklist")){target.addEffect(new MobEffectInstance(IllusionContent.effect(kind),duration(rank,caster),0,false,false));IllusionState.sync(target);}}
                default->{if(target!=null&&!IllusionState.immune(target,caster)){target.addEffect(new MobEffectInstance(IllusionContent.effect(kind),duration(rank,caster),0,false,false));IllusionState.sync(target);}}
            }
        }
        super.onCast(level,rank,caster,source,data);
    }
}
