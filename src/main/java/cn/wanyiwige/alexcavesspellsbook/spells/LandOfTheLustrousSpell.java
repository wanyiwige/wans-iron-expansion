package cn.wanyiwige.alexcavesspellsbook.spells;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.alexcavesspellsbook.entities.LustrousGemProjectile;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Three waves of three solid gems; all retain the one target selected at release. */
public final class LandOfTheLustrousSpell extends AbstractSpell {
    public static final int GEM_COUNT = 9;
    public static final double LOCK_RANGE = 32.0;
    private static final ResourceLocation ID = new ResourceLocation(AlexCavesSpellsbook.MOD_ID, "land_of_the_lustrous");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.NATURE_RESOURCE)
            .setMaxLevel(3).setCooldownSeconds(12).build();

    public LandOfTheLustrousSpell() {
        baseManaCost = 100;
        manaCostPerLevel = 40;
        baseSpellPower = 40;
        spellPowerPerLevel = 20;
        castTime = 20;
    }

    @Override public ResourceLocation getSpellResource() { return ID; }
    @Override public DefaultConfig getDefaultConfig() { return defaultConfig; }
    @Override public CastType getCastType() { return CastType.LONG; }
    @Override public SpellRarity getRarity(int level) { return SpellRarity.LEGENDARY; }
    @Override public int getMaxRarity() { return SpellRarity.LEGENDARY.getValue(); }
    @Override public int getMinLevelForRarity(SpellRarity rarity) {
        return rarity == SpellRarity.LEGENDARY ? 1 : 0;
    }
    @Override public boolean requiresLearning() { return false; }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("ui.alex_caves_spellsbook.lustrous.damage", getSpellPower(spellLevel, caster)),
                Component.translatable("ui.alex_caves_spellsbook.lustrous.gems", GEM_COUNT),
                Component.translatable("ui.alex_caves_spellsbook.lustrous.lock_range", (int) LOCK_RANGE),
                Component.translatable("ui.alex_caves_spellsbook.lustrous.piercing"));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
        if (!level.isClientSide) {
            LivingEntity target = selectTarget(level, caster);
            Vec3 look = caster.getLookAngle().normalize();
            List<Integer> kinds = new ArrayList<>();
            for (int i = 0; i < GEM_COUNT; i++) kinds.add(i);
            // Every cast contains all nine models; randomize their wave order.
            for (int i = kinds.size() - 1; i > 0; i--) {
                int j = level.random.nextInt(i + 1);
                int value = kinds.get(i); kinds.set(i, kinds.get(j)); kinds.set(j, value);
            }
            for (int i = 0; i < GEM_COUNT; i++) {
                LustrousGemProjectile gem = new LustrousGemProjectile(level, caster, target, look,
                        kinds.get(i), (i / 3) * 6, getSpellPower(spellLevel, caster), i * Mth.TWO_PI / GEM_COUNT);
                gem.setPos(LustrousGemProjectile.casterOrigin(caster));
                level.addFreshEntity(gem);
            }
        }
        super.onCast(level, spellLevel, caster, source, data);
    }

    private static LivingEntity selectTarget(Level level, LivingEntity caster) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle().normalize();
        Vec3 end = eye.add(look.scale(LOCK_RANGE));
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(eye, eye).inflate(LOCK_RANGE), target -> LustrousGemProjectile.canDamage(caster, target)
                        && eye.distanceToSqr(target.getBoundingBox().getCenter()) <= LOCK_RANGE * LOCK_RANGE
                        && caster.hasLineOfSight(target));
        // Prefer the actual crosshair ray over the broader aim cone.
        LivingEntity aimed = candidates.stream()
                .filter(target -> target.getBoundingBox().inflate(0.35).clip(eye, end).isPresent())
                .min(Comparator.comparingDouble(target -> eye.distanceToSqr(
                        target.getBoundingBox().inflate(0.35).clip(eye, end).orElse(eye)))).orElse(null);
        if (aimed != null) return aimed;
        return candidates.stream().filter(target -> {
            Vec3 direction = target.getBoundingBox().getCenter().subtract(eye).normalize();
            return direction.dot(look) >= 0.985; // Approximately ten degrees around the crosshair.
        }).min(Comparator.comparingDouble(target -> eye.distanceToSqr(target.getBoundingBox().getCenter()))).orElse(null);
    }
}
