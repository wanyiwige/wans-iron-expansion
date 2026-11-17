package cn.wanyiwige.astralcompat.recreation;

import cn.wanyiwige.astralcompat.registry.AstralSchools;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.events.SpellSummonEvent;
import io.redspace.ironsspellbooks.capabilities.magic.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;

public final class ArcanumTravelSpells {
    public static final class LunarMoths extends AbstractSpell {
        private final DefaultConfig config=new DefaultConfig().setSchoolResource(AstralSchools.ASTRAL_RESOURCE).setMinRarity(SpellRarity.RARE).setMaxLevel(1).setCooldownSeconds(150).build();
        public LunarMoths() { baseManaCost=150;manaCostPerLevel=0;baseSpellPower=20;spellPowerPerLevel=10;castTime=30; }
        @Override public ResourceLocation getSpellResource() { return ArcanumContent.id("summon_lunar_moths"); }
        @Override public DefaultConfig getDefaultConfig() { return config; }
        @Override public CastType getCastType() { return CastType.LONG; }
        @Override public boolean requiresLearning() { return false; }
        @Override public int getRecastCount(int level,LivingEntity caster) { return 1; }
        @Override public ICastDataSerializable getEmptyCastData() { return new SummonedEntitiesCastData(); }
        @Override public void onRecastFinished(ServerPlayer player,RecastInstance instance,RecastResult result,ICastDataSerializable data) {
            if(SummonManager.recastFinishedHelper(player,instance,result,data))super.onRecastFinished(player,instance,result,data);
        }
        @Override public void onCast(Level level,int spellLevel,LivingEntity caster,CastSource source,MagicData magic) {
            if(level instanceof ServerLevel server && !magic.getPlayerRecasts().hasRecastForSpell(this)) {
                SummonedMoth moth=ArcanumContent.SUMMONED_MOTH.get().create(level);
                if(moth==null)return;
                moth.setSummoner(caster);moth.moveTo(caster.getX(),caster.getEyeY()+1,caster.getZ(),caster.getYRot(),0);
                moth.finalizeSpawn(server,server.getCurrentDifficultyAt(moth.blockPosition()),MobSpawnType.MOB_SUMMONED,null,null);
                SpellSummonEvent<SummonedMoth> event=new SpellSummonEvent<>(caster,moth,getSpellResource(),spellLevel);MinecraftForge.EVENT_BUS.post(event);
                var creature=event.getCreature();
                if(level.addFreshEntity(creature)) {
                    SummonedEntitiesCastData data=new SummonedEntitiesCastData();SummonManager.initSummon(caster,creature,12000,data);
                    magic.getPlayerRecasts().addRecast(new RecastInstance(getSpellId(),spellLevel,1,12000,source,data),magic);
                }
            }
            super.onCast(level,spellLevel,caster,source,magic);
        }
    }
    public static final class Gateway extends AbstractSpell {
        private final DefaultConfig config=new DefaultConfig().setSchoolResource(AstralSchools.ASTRAL_RESOURCE).setMinRarity(SpellRarity.LEGENDARY).setMaxLevel(1).setCooldownSeconds(60).build();
        public Gateway() { baseManaCost=300;manaCostPerLevel=0;baseSpellPower=0;spellPowerPerLevel=0;castTime=40; }
        @Override public ResourceLocation getSpellResource() { return ArcanumContent.id("astral_gateway"); }
        @Override public DefaultConfig getDefaultConfig() { return config; }
        @Override public CastType getCastType() { return CastType.LONG; }
        @Override public boolean allowLooting() { return false; }
        @Override public boolean requiresLearning() { return false; }
        @Override public boolean checkPreCastConditions(Level level,int rank,LivingEntity caster,MagicData magic) {
            return caster instanceof ServerPlayer player && player.getServer().getLevel(AstralSea.SEA)!=null
                    && (player.getMainHandItem().is(ArcanumContent.TALISMAN.get())||player.getOffhandItem().is(ArcanumContent.TALISMAN.get()));
        }
        @Override public void onCast(Level level,int rank,LivingEntity caster,CastSource source,MagicData magic) {
            if(caster instanceof ServerPlayer player)AstralSea.travelGroup(player);
            super.onCast(level,rank,caster,source,magic);
        }
    }
    private ArcanumTravelSpells() {}
}
