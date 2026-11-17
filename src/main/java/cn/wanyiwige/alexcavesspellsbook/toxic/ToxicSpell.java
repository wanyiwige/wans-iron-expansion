package cn.wanyiwige.alexcavesspellsbook.toxic;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import java.util.Optional;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;

/** Registry/config plumbing; each attack has its own server entity and timeline. */
public final class ToxicSpell extends AbstractSpell {
    private final ToxicSpec spec;
    private final DefaultConfig config;
    public ToxicSpell(ToxicSpec spec) {
        this.spec=spec;
        config=new DefaultConfig().setSchoolResource(ToxicRegistry.SCHOOL_ID).setMaxLevel(spec.maxLevel)
                .setMinRarity(spec.rarity).setCooldownSeconds(spec.cooldown).setAllowCrafting(true).build();
        baseManaCost=spec.mana;manaCostPerLevel=spec.manaGrowth;baseSpellPower=spec.base;spellPowerPerLevel=spec.growth;castTime=spec.castTicks;
    }
    @Override public ResourceLocation getSpellResource(){return ToxicRegistry.id(spec.id);}
    @Override public DefaultConfig getDefaultConfig(){return config;}
    @Override public int getMaxLevel(){return spec.maxLevel;}
    @Override public int getMinRarity(){return spec.rarity.getValue();}
    @Override public int getMaxRarity(){return spec.rarityAt(spec.maxLevel).getValue();}
    @Override public SpellRarity getRarity(int rank){return spec.rarityAt(rank);}
    @Override public int getMinLevelForRarity(SpellRarity rarity){return spec.firstLevel(rarity);}
    @Override public float getSpellPower(int rank,Entity caster){return super.getSpellPower(spec.clampLevel(rank),caster);}
    @Override public int getManaCost(int rank){return super.getManaCost(spec.clampLevel(rank));}
    @Override public CastType getCastType(){return CastType.LONG;}
    @Override public boolean allowCrafting(){return true;}
    @Override public boolean allowLooting(){return true;}
    @Override public Optional<SoundEvent> getCastStartSound(){return Optional.of(SoundEvents.BEACON_POWER_SELECT);}
    @Override public Optional<SoundEvent> getCastFinishSound(){return Optional.of(switch(spec){
        case DRUM->SoundEvents.TRIDENT_THROW;case PURIFICATION->SoundEvents.AMETHYST_BLOCK_CHIME;
        case FISSION->SoundEvents.RESPAWN_ANCHOR_DEPLETE.value();case WARD->SoundEvents.BEACON_ACTIVATE;
        default->SoundEvents.FIRECHARGE_USE;
    });}
    @Override public void onServerPreCast(Level level,int rank,LivingEntity caster,MagicData data){
        super.onServerPreCast(level,rank,caster,data);
        if(level instanceof ServerLevel server&&(spec==ToxicSpec.ISOTOPE||spec==ToxicSpec.CHAIN||spec==ToxicSpec.FILM)){
            String key="WanRadiationAim_"+spec.id;
            LivingEntity target=ToxicCombat.aimedEnemy(server,caster,24);
            if(target!=null)caster.getPersistentData().putUUID(key,target.getUUID());else caster.getPersistentData().remove(key);
        }
    }
    @Override public void onServerCastComplete(Level level,int rank,LivingEntity caster,MagicData data,boolean cancelled){
        caster.getPersistentData().remove("WanRadiationAim_"+spec.id);
        super.onServerCastComplete(level,rank,caster,data,cancelled);
    }
    @Override public boolean checkPreCastConditions(Level level,int rank,LivingEntity caster,MagicData data) {
        if(!super.checkPreCastConditions(level,rank,caster,data))return false;
        if(!(level instanceof ServerLevel server))return true;
        RadiationNewEffectEntity active=active(server,caster);
        if(twoStage()&&active!=null&&active.canTrigger())return true;
        if(twoStage()&&data.getPlayerRecasts().hasRecastForSpell(this)){
            var stale=data.getPlayerRecasts().getRecastInstance(getSpellId());
            if(stale!=null)data.getPlayerRecasts().removeRecast(stale,RecastResult.COUNTERSPELL);
            return false;
        }
        if((spec==ToxicSpec.MODERATOR||spec==ToxicSpec.CLOUD||spec==ToxicSpec.MATRIX)
                &&RadiationNewEffectEntity.groundAim(server,caster)==null)return fail(caster,"ground");
        if((spec==ToxicSpec.CHERENKOV||spec==ToxicSpec.DECOY)&&(!caster.onGround()||caster.isInWater()))return fail(caster,"ground");
        if(spec==ToxicSpec.ISOTOPE||spec==ToxicSpec.FILM){
            if(ToxicCombat.sphere(server,caster,RadiationNewEffectEntity.areaAim(server,caster),RadiationRules.AREA_RADIUS).isEmpty())return fail(caster,"area_targets");
        }
        if(spec==ToxicSpec.FISSION||spec==ToxicSpec.CHAIN){
            LivingEntity target=ToxicCombat.aimedEnemy(server,caster,spec==ToxicSpec.FISSION?20:24);
            if(target==null)return fail(caster,"target");
            if(spec==ToxicSpec.CHAIN&&ToxicMarks.count(target,caster.getUUID(),server.getGameTime())==0)return fail(caster,"marked_target");
        }
        return true;
    }
    private static boolean fail(LivingEntity caster,String reason){
        if(caster instanceof ServerPlayer player)player.displayClientMessage(Component.translatable("ui.alex_caves_spellsbook.radiation.require_"+reason),true);
        return false;
    }
    private RadiationNewEffectEntity active(ServerLevel server,LivingEntity caster){
        String key="WanToxicCast_"+spec.id;
        Entity entity=caster.getPersistentData().hasUUID(key)?server.getEntity(caster.getPersistentData().getUUID(key)):null;
        return entity instanceof RadiationNewEffectEntity effect&&effect.isAlive()&&effect.spec()==spec
                &&caster.getUUID().equals(effect.ownerUUID())?effect:null;
    }
    @Override public void onRecastFinished(ServerPlayer player,RecastInstance instance,RecastResult result,ICastDataSerializable castData){
        RadiationNewEffectEntity effect=active(player.serverLevel(),player);
        if(effect!=null){
            if(result==RecastResult.TIMEOUT&&spec==ToxicSpec.DECOY)effect.cancel();
            else if(result==RecastResult.TIMEOUT&&effect.canTrigger())effect.trigger();
            else if(result==RecastResult.COUNTERSPELL||result==RecastResult.DEATH||result==RecastResult.USER_CANCEL)effect.cancel();
        }
        super.onRecastFinished(player,instance,result,castData);
    }
    private boolean twoStage(){return spec==ToxicSpec.FILM||spec==ToxicSpec.CLOUD||spec==ToxicSpec.DECOY||spec==ToxicSpec.MATRIX;}
    @Override public List<MutableComponent> getUniqueInfo(int rank,LivingEntity caster) {
        float p=getSpellPower(rank,caster);
        String key="ui.alex_caves_spellsbook.toxic.";
        return switch(spec) {
            case GAMMA->List.of(Component.translatable(key+"gamma",number(.45F*p)),Component.translatable(key+"range",8));
            case DRUM->List.of(Component.translatable(key+"drum.impact",number(.8F*p)),Component.translatable(key+"drum.field",number(.2F*p)));
            case PURIFICATION->List.of(Component.translatable(key+"cleanse"),Component.translatable(key+"shield",number(spec.cleanseProtection(rank)),4));
            case LANCE->List.of(Component.translatable(key+"lance",number(.3F*p)),Component.translatable(key+"range",24));
            case FISSION->List.of(Component.translatable(key+"fission",number(p),number(2.6F*p)),Component.translatable(key+"range",20));
            case WARD->List.of(Component.translatable(key+"shield",number(Math.min(32,.5F*p)),6),Component.translatable(key+"ward",number(.25F*p)));
            case ISOTOPE->List.of(Component.translatable(key+"new."+spec.id),Component.translatable(key+"detail.isotope",number(.2F*p),number((float)RadiationRules.AREA_RADIUS)));
            case NEUTRON->List.of(Component.translatable(key+"new."+spec.id),Component.translatable(key+"detail.neutron",number(p),RadiationRules.maxBounces(rank)));
            case MODERATOR->List.of(Component.translatable(key+"new."+spec.id),Component.translatable(key+"detail.field",number((80+10*(rank-1))/20F),rank>=4?30:15));
            case LEAD->List.of(Component.translatable(key+"new."+spec.id),Component.translatable(key+"detail.lead",RadiationRules.shieldCharges(rank)));
            case CHAIN->List.of(Component.translatable(key+"new."+spec.id),Component.translatable(key+"detail.chain",Math.min(5,rank+1),number(.65F*p)));
            case FILM->List.of(Component.translatable(key+"new."+spec.id),Component.translatable(key+"detail.film",number(.45F*p),number((float)RadiationRules.AREA_RADIUS)));
            case CLOUD->List.of(Component.translatable(key+"new."+spec.id),Component.translatable(key+"detail.cloud",rank>=5?4:3,number(.3F*p),number(.55F*p)));
            case CHERENKOV->List.of(Component.translatable(key+"new."+spec.id),Component.translatable(key+"detail.wake",number(.38F*p)));
            case DECOY->List.of(Component.translatable(key+"new."+spec.id),Component.translatable(key+"detail.decoy",number(.4F*p),RadiationRules.DECOY_RECAST_TICKS/20));
            case MATRIX->List.of(Component.translatable(key+"new."+spec.id),Component.translatable(key+"detail.matrix_window",RadiationRules.MATRIX_RECAST_TICKS/20,number((float)RadiationRules.MATRIX_RADIUS)),Component.translatable(key+"detail.matrix",number(.9F*p),number(.7F*p),number(1.05F*p)));
        };
    }
    private static String number(float n){return String.format(java.util.Locale.ROOT,"%.1f",n);}
    @Override public void onCast(Level level,int rank,LivingEntity caster,CastSource source,MagicData data) {
        if(level instanceof ServerLevel server) {
            List<ToxicEffectEntity> oldEffects=server.getEntitiesOfClass(ToxicEffectEntity.class,caster.getBoundingBox().inflate(64),
                    e->e.ownerUUID()!=null&&e.ownerUUID().equals(caster.getUUID())&&e.spec()==spec);
            RadiationNewEffectEntity active=active(server,caster);
            if(twoStage()&&active!=null&&active.canTrigger()){
                active.trigger();super.onCast(level,rank,caster,source,data);return;
            }
            ToxicEffectEntity effect=switch(spec) {
                case GAMMA->ToxicRegistry.GAMMA.get().create(server);case DRUM->ToxicRegistry.DRUM.get().create(server);
                case PURIFICATION->ToxicRegistry.PURIFICATION.get().create(server);case LANCE->ToxicRegistry.LANCE.get().create(server);
                case FISSION->ToxicRegistry.FISSION.get().create(server);case WARD->ToxicRegistry.WARD.get().create(server);
                default->ToxicRegistry.NEW_EFFECTS.get(spec).get().create(server);
            };
            if(effect!=null) {
                for(ToxicEffectEntity old:oldEffects)old.discard();
                effect.configure(caster,rank,getSpellPower(rank,caster));server.addFreshEntity(effect);
                caster.getPersistentData().remove("WanRadiationReady_"+spec.id);
                if(twoStage()&&caster instanceof ServerPlayer&&(source==CastSource.SPELLBOOK||source==CastSource.SWORD||spec==ToxicSpec.DECOY&&source==CastSource.SCROLL)){
                    int duration=RadiationRules.recastTicks(spec);
                    // ISS counts the initial cast in totalRecasts and initializes remaining to total - 1.
                    data.getPlayerRecasts().addRecast(new RecastInstance(getSpellId(),rank,2,duration,source,null),data);
                }
            }
        }
        super.onCast(level,rank,caster,source,data);
    }
}
