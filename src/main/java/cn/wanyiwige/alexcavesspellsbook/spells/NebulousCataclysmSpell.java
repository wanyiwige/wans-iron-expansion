package cn.wanyiwige.alexcavesspellsbook.spells;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.alexcavesspellsbook.compat.FantasyEndingCompat;
import cn.wanyiwige.alexcavesspellsbook.entities.NebulousCataclysmEntity;
import cn.wanyiwige.alexcavesspellsbook.util.SaciliteSpellSupport;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/** First sprite-based version: homing cloud, two pierces and 6-8 half-damage novas. */
public final class NebulousCataclysmSpell extends AbstractSpell {
    public static final int MAX_LEVEL = 5;
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
            AlexCavesSpellsbook.MOD_ID, "nebulous_cataclysm");
    private final DefaultConfig config = new DefaultConfig()
            .setSchoolResource(ResourceLocation.fromNamespaceAndPath("roacw", "exo"))
            .setMinRarity(SpellRarity.LEGENDARY).setMaxLevel(MAX_LEVEL)
            .setCooldownSeconds(8).setAllowCrafting(false).build();

    public NebulousCataclysmSpell() {
        baseManaCost = 90;
        manaCostPerLevel = 15;
        // Ten percent above Dark Spark's uncharged per-hit scale; novas deal half.
        baseSpellPower = 66;
        spellPowerPerLevel = 22;
        castTime = 60;
    }
    @Override public ResourceLocation getSpellResource() { return ID; }
    @Override public DefaultConfig getDefaultConfig() { return config; }
    @Override public CastType getCastType() { return CastType.LONG; }
    @Override public boolean requiresLearning() { return false; }
    @Override public boolean allowLooting() { return false; }
    @Override public boolean allowCrafting() { return false; }
    @Override public int getMaxLevel() { return MAX_LEVEL; }
    @Override public int getMinRarity() { return SpellRarity.LEGENDARY.getValue(); }
    @Override public int getMaxRarity() { return FantasyEndingCompat.originRarity().getValue(); }
    @Override public SpellRarity getRarity(int level) {
        return level >= 3 ? FantasyEndingCompat.originRarity() : SpellRarity.LEGENDARY;
    }
    @Override public int getMinLevelForRarity(SpellRarity rarity) {
        if (rarity == SpellRarity.LEGENDARY) return 1;
        if (rarity == FantasyEndingCompat.originRarity()) return 3;
        return 0;
    }
    @Override public float getSpellPower(int level, Entity caster) {
        return super.getSpellPower(Mth.clamp(level, 1, MAX_LEVEL), caster);
    }
    @Override public List<MutableComponent> getUniqueInfo(int level, LivingEntity caster) {
        float power = getSpellPower(level, caster);
        return List.of(Component.translatable("ui.alex_caves_spellsbook.nebulous_cataclysm.damage", power),
                Component.translatable("ui.alex_caves_spellsbook.nebulous_cataclysm.nova", power * 0.5F),
                Component.translatable("ui.alex_caves_spellsbook.nebulous_cataclysm.homing"));
    }
    @Override public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
        if (!level.isClientSide) {
            Vec3 eye = caster.getEyePosition(), forward = caster.getLookAngle().normalize();
            Vec3 start = eye.add(forward.scale(0.8));
            var wall = level.clip(new ClipContext(eye, start, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, caster));
            if (wall.getType() != HitResult.Type.MISS) start = wall.getLocation().subtract(forward.scale(0.05));
            var cloud = new NebulousCataclysmEntity(level, caster, getSpellPower(spellLevel, caster),
                    SaciliteSpellSupport.selectTarget(level, caster, 32));
            cloud.setPos(start);
            cloud.setDeltaMovement(forward.scale(NebulousCataclysmEntity.SPEED));
            level.addFreshEntity(cloud);
            level.playSound(null, caster.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.PLAYERS, 0.9F, 0.65F);
        }
        super.onCast(level, spellLevel, caster, source, data);
    }
}
