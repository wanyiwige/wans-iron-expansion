package cn.wanyiwige.alexcavesspellsbook.foolish;

import io.redspace.ironsspellbooks.api.config.*;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import cn.wanyiwige.astralcompat.registry.AstralAttributes;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import java.util.List;

public final class BossSpell extends AbstractSpell {
    public final BossSpellSpec spec;
    private final DefaultConfig config;
    public BossSpell(BossSpellSpec spec) {
        this.spec=spec;
        config=new DefaultConfig().setMinRarity(SpellRarity.LEGENDARY).setMaxLevel(3)
                .setSchoolResource(spec.schoolResource()).setCooldownSeconds(spec.cooldown).setAllowCrafting(false).build();
        baseManaCost=spec.mana;manaCostPerLevel=20;baseSpellPower=spec.damage;
        spellPowerPerLevel=spec.growth;castTime=spec.castTicks;
    }
    @Override public ResourceLocation getSpellResource(){return spec.resource();}
    @Override public DefaultConfig getDefaultConfig(){return config;}
    @Override public CastType getCastType(){return CastType.LONG;}
    @Override public int getMaxLevel(){return 3;}
    @Override public int getMinRarity(){return SpellRarity.LEGENDARY.getValue();}
    @Override public int getMaxRarity(){return SpellRarity.LEGENDARY.getValue();}
    @Override public SpellRarity getRarity(int level){return SpellRarity.LEGENDARY;}
    @Override public int getMinLevelForRarity(SpellRarity rarity){return rarity==SpellRarity.LEGENDARY?1:0;}
    @Override public boolean requiresLearning(){return false;}
    @Override public boolean allowLooting(){return false;}
    @Override public boolean allowCrafting(){return false;}
    @Override public float getSpellPower(int level, Entity caster) {
        if (!spec.astral()) return super.getSpellPower(Math.max(1,Math.min(3,level)),caster);
        // The retained Astral attributes are percentage points, with a neutral value of zero.
        double power=1,school=1;
        if(caster instanceof LivingEntity living){
            power=living.getAttributeValue(AttributeRegistry.SPELL_POWER.get());
            school=1+living.getAttributeValue(AstralAttributes.SPELL_POWER.get())/100.0;
        }
        double multiplier=((Number)SpellConfigManager.getSpellConfigValue(this,SpellConfigParameter.POWER_MULTIPLIER)).doubleValue();
        return (float)((spec.damage+spec.growth*(Math.max(1,Math.min(3,level))-1))*power*school*multiplier);
    }
    @Override public List<MutableComponent> getUniqueInfo(int level, LivingEntity caster){
        return List.of(Component.translatable("ui.alex_caves_spellsbook.boss_spell.power",getSpellPower(level,caster)),
                Component.translatable("ui.alex_caves_spellsbook.boss_spell.drop",Component.translatable("entity.foolish."+spec.boss)));
    }
    @Override public void onCast(Level level,int rank,LivingEntity caster,CastSource source,MagicData data){
        if(level instanceof ServerLevel server) BossCast.start(server,caster,spec,Math.max(1,Math.min(3,rank)),getSpellPower(rank,caster));
        super.onCast(level,rank,caster,source,data);
    }
}
