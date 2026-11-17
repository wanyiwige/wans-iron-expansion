package cn.wanyiwige.alexcavesspellsbook.spells;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.alexcavesspellsbook.compat.FantasyEndingCompat;
import cn.wanyiwige.alexcavesspellsbook.registry.ModSpells;
import cn.wanyiwige.astralcompat.entity.AstralAnchorEntity;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

/** Channeled Exo spell inspired by Dark Spark's inverted prism sequence. */
public final class DarkSparkSpell extends AbstractSpell {
    public static final String SPELL_ID = AlexCavesSpellsbook.MOD_ID + ":dark_spark";
    public static final int CHARGE_TICKS = 200;
    public static final int SPLIT_TICKS = 20;
    public static final int MAX_CHANNEL_TICKS = 1200;
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
            AlexCavesSpellsbook.MOD_ID, "dark_spark");
    private static final ResourceLocation EXO_SCHOOL = ResourceLocation.fromNamespaceAndPath("roacw", "exo");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(EXO_SCHOOL)
            .setMaxLevel(3)
            .setCooldownSeconds(20)
            .setAllowCrafting(false)
            .build();

    public DarkSparkSpell() {
        baseManaCost = 4;
        manaCostPerLevel = 2;
        baseSpellPower = 60;
        spellPowerPerLevel = 20;
        castTime = MAX_CHANNEL_TICKS;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return ID;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.CONTINUOUS;
    }

    @Override
    public SpellRarity getRarity(int spellLevel) {
        return spellLevel >= 2 ? FantasyEndingCompat.originRarity() : SpellRarity.LEGENDARY;
    }

    @Override
    public int getMaxRarity() {
        return FantasyEndingCompat.originRarity().getValue();
    }

    @Override
    public int getMinLevelForRarity(SpellRarity rarity) {
        if (rarity == SpellRarity.LEGENDARY) return 1;
        if (rarity == FantasyEndingCompat.originRarity()) return 2;
        return 0;
    }

    @Override
    public boolean requiresLearning() {
        return false;
    }

    @Override
    public boolean allowLooting() {
        return false;
    }

    @Override
    public boolean allowCrafting() {
        return false;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.alex_caves_spellsbook.dark_spark.damage", getSpellPower(spellLevel, caster)),
                Component.translatable("ui.alex_caves_spellsbook.dark_spark.full_charge", 350),
                Component.translatable("ui.alex_caves_spellsbook.dark_spark.beams", 6));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource castSource, MagicData magicData) {
        if (!level.isClientSide && magicData.getCastDurationRemaining() > 0) {
            float damage = getSpellPower(spellLevel, caster);
            AstralAnchorEntity existing = findAnchor(caster);
            if (existing != null) {
                existing.refresh(damage, 40);
            } else {
                AstralAnchorEntity anchor = new AstralAnchorEntity(level, caster,
                        AstralAnchorEntity.DARK_SPARK, spellLevel, damage, 40);
                anchor.setPos(AstralAnchorEntity.darkSparkStart(caster).subtract(0, 0.35, 0));
                level.addFreshEntity(anchor);
            }
        }
        super.onCast(level, spellLevel, caster, castSource, magicData);
    }

    public static AstralAnchorEntity findAnchor(LivingEntity caster) {
        return caster.level().getEntitiesOfClass(AstralAnchorEntity.class,
                caster.getBoundingBox().inflate(8.0),
                anchor -> anchor.getMode() == AstralAnchorEntity.DARK_SPARK
                        && anchor.getSyncedOwner() == caster).stream().findFirst().orElse(null);
    }

    public static int manaCost(int level, int chargeTicks) {
        int base = ModSpells.DARK_SPARK.get().getManaCost(Math.max(1, level));
        return chargeTicks >= SPLIT_TICKS ? base * 4 : base;
    }
}
