package cn.wanyiwige.alexcavesspellsbook.spells;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.alexcavesspellsbook.entities.SaciliteSpellProjectile;
import cn.wanyiwige.alexcavesspellsbook.util.SaciliteSpellSupport;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/** Foolish visuals with player-owned projectiles and Geomancy Plus damage scaling. */
public final class SaciliteSpell extends AbstractSpell {
    public static final ResourceLocation GEO_SCHOOL = ResourceLocation.fromNamespaceAndPath("gtbcs_geomancy_plus", "geo");
    public static final double RANGE = 32;
    private final boolean rain;
    private final ResourceLocation id;
    private final DefaultConfig config;

    public SaciliteSpell(boolean rain) {
        this.rain = rain;
        id = ResourceLocation.fromNamespaceAndPath(AlexCavesSpellsbook.MOD_ID, rain ? "sacilite_rain" : "seeking_sacilite_swarm");
        config = new DefaultConfig().setMinRarity(SpellRarity.LEGENDARY).setSchoolResource(GEO_SCHOOL)
                .setMaxLevel(3).setCooldownSeconds(rain ? 18 : 12).setAllowCrafting(false).build();
        baseManaCost = rain ? 70 : 45;
        manaCostPerLevel = rain ? 15 : 10;
        baseSpellPower = rain ? 14 : 8;
        spellPowerPerLevel = rain ? 4 : 2;
        castTime = rain ? 30 : 20;
    }

    public int crystalCount(int level) { return (rain ? 6 : 4) + Mth.clamp(level, 1, 3) - 1; }
    @Override public ResourceLocation getSpellResource() { return id; }
    @Override public DefaultConfig getDefaultConfig() { return config; }
    @Override public CastType getCastType() { return CastType.LONG; }
    @Override public boolean requiresLearning() { return false; }
    @Override public int getMaxLevel() { return 3; }
    @Override public float getSpellPower(int level, net.minecraft.world.entity.Entity caster) {
        return super.getSpellPower(Mth.clamp(level, 1, 3), caster);
    }
    @Override public int getMinRarity() { return SpellRarity.LEGENDARY.getValue(); }
    @Override public boolean allowLooting() { return false; }
    @Override public boolean allowCrafting() { return false; }
    @Override public SpellRarity getRarity(int level) {
        return SpellRarity.LEGENDARY;
    }
    @Override public int getMaxRarity() { return SpellRarity.LEGENDARY.getValue(); }
    @Override public int getMinLevelForRarity(SpellRarity rarity) {
        return rarity == SpellRarity.LEGENDARY ? 1 : 0;
    }
    @Override public List<MutableComponent> getUniqueInfo(int level, LivingEntity caster) {
        return List.of(Component.translatable("ui.alex_caves_spellsbook.sacilite.damage", getSpellPower(level, caster)),
                Component.translatable("ui.alex_caves_spellsbook.sacilite.count", crystalCount(level)),
                Component.translatable("ui.alex_caves_spellsbook.sacilite.range", (int) RANGE),
                Component.translatable(rain ? "ui.alex_caves_spellsbook.sacilite.blast" : "ui.alex_caves_spellsbook.sacilite.homing"));
    }

    @Override public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
        if (!level.isClientSide) {
            LivingEntity target = SaciliteSpellSupport.selectTarget(level, caster, RANGE);
            Vec3 center = target == null ? SaciliteSpellSupport.aimPoint(level, caster, RANGE) : target.position();
            int count = crystalCount(spellLevel);
            SaciliteSpellSupport.limitProjectiles(level, caster, count);
            SaciliteSpellSupport.sound(level, caster.position(), "bassdrop", 0.7F, 1);
            for (int i = 0; i < count; i++) {
                Vec3 start;
                if (rain) {
                    // The first crystal anchors the aim; the others cover a stratified disk.
                    double angle = i * 2.399963 + level.random.nextDouble() * 0.3;
                    double radius = i == 0 ? 0 : 4.5 * Math.sqrt((double) i / (count - 1));
                    Vec3 floor = center.add(Math.cos(angle) * radius, 0.25, Math.sin(angle) * radius);
                    start = SaciliteSpellSupport.rainOrigin(level, caster, floor, 6 + level.random.nextDouble() * 2);
                    if (start == null) continue;
                } else {
                    double angle = i * Mth.TWO_PI / count;
                    // Rotate the fan with the caster rather than leaving it on a world axis.
                    double yaw = Math.toRadians(caster.getYRot());
                    Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
                    start = caster.getEyePosition().add(right.scale(Math.cos(angle) * 1.05))
                            .add(0, Math.sin(angle) * 0.65, 0).add(caster.getLookAngle().scale(0.7));
                    var cover = level.clip(new net.minecraft.world.level.ClipContext(caster.getEyePosition(), start,
                            net.minecraft.world.level.ClipContext.Block.COLLIDER,
                            net.minecraft.world.level.ClipContext.Fluid.NONE, caster));
                    if (cover.getType() != net.minecraft.world.phys.HitResult.Type.MISS)
                        start = cover.getLocation().add(caster.getEyePosition().subtract(start).normalize().scale(0.1));
                }
                SaciliteSpellProjectile crystal = new SaciliteSpellProjectile(level, caster, target,
                        rain, i * (rain ? 3 : 2), getSpellPower(spellLevel, caster), caster.getLookAngle());
                crystal.setPos(start);
                if (!rain) crystal.aimAt(target == null ? caster.getLookAngle()
                        : target.getBoundingBox().getCenter().subtract(start));
                level.addFreshEntity(crystal);
            }
        }
        super.onCast(level, spellLevel, caster, source, data);
    }
}
