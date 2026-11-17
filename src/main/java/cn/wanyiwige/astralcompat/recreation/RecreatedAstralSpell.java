package cn.wanyiwige.astralcompat.recreation;

import cn.wanyiwige.astralcompat.registry.AstralSchools;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.spells.ender.TeleportSpell;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.Optional;

/** Behavior recreation from the supplied 0.1.0 JAR, independent of Wan's old spells. */
public final class RecreatedAstralSpell extends AbstractSpell {
    public static final List<String> IDS = List.of("arcanum_starfire", "arcanum_brightburst",
            "arcanum_star_swarm", "arcanum_luminous_beam", "arcanum_piercing_light",
            "arcanum_starcutter", "silvery_barbs", "celestial_tether", "astral_echo");
    private final ResourceLocation id;
    private final String behavior;
    private final DefaultConfig config;

    public RecreatedAstralSpell(String path, String behavior) {
        this.id = new ResourceLocation("asterismarcanum", path);
        this.behavior = behavior;
        int max = 10;
        double cooldown = 12;
        SpellRarity rarity = SpellRarity.COMMON;
        baseManaCost = 3; manaCostPerLevel = 1;
        baseSpellPower = 1; spellPowerPerLevel = 1;
        castTime = 0;
        switch (behavior) {
            case "starfire" -> { baseManaCost = 6; manaCostPerLevel = 2; baseSpellPower = 7; spellPowerPerLevel = 2; cooldown = .3; }
            case "brightburst" -> { baseManaCost = 25; manaCostPerLevel = 5; max = 6; cooldown = 20; rarity = SpellRarity.RARE; }
            case "star_swarm", "luminous_beam" -> castTime = 100;
            case "piercing_light" -> { baseManaCost = 25; manaCostPerLevel = 5; spellPowerPerLevel = 0; cooldown = 20; rarity = SpellRarity.RARE; }
            case "starcutter" -> { baseManaCost = 20; manaCostPerLevel = 5; max = 8; cooldown = 13; rarity = SpellRarity.UNCOMMON; }
            case "silvery_barbs" -> { baseManaCost = 25; manaCostPerLevel = 5; baseSpellPower = 2; max = 5; cooldown = 1; rarity = SpellRarity.RARE; }
            case "celestial_tether" -> { baseManaCost = 30; manaCostPerLevel = 15; baseSpellPower = 5; max = 8; cooldown = 30; rarity = SpellRarity.UNCOMMON; }
            case "astral_echo" -> { baseManaCost = 15; manaCostPerLevel = 5; baseSpellPower = 4; max = 8; rarity = SpellRarity.RARE; }
            default -> throw new IllegalArgumentException(behavior);
        }
        config = new DefaultConfig().setSchoolResource(AstralSchools.ASTRAL_RESOURCE)
                .setMinRarity(rarity).setMaxLevel(max).setCooldownSeconds(cooldown).build();
    }

    @Override public ResourceLocation getSpellResource() { return id; }
    @Override public DefaultConfig getDefaultConfig() { return config; }
    @Override public CastType getCastType() { return isChannel() ? CastType.CONTINUOUS : CastType.INSTANT; }
    public boolean isChannel() { return behavior.equals("star_swarm") || behavior.equals("luminous_beam"); }
    @Override public boolean requiresLearning() { return false; }
    @Override public boolean allowLooting() { return true; }
    @Override public Optional<SoundEvent> getCastFinishSound() {
        return isChannel() ? Optional.empty() : Optional.of(SoundEvents.AMETHYST_BLOCK_CHIME);
    }
    @Override public List<MutableComponent> getUniqueInfo(int level, LivingEntity caster) {
        float power = getSpellPower(level, caster);
        return switch (behavior) {
            case "starfire" -> List.of(info("damage", power * .3F));
            case "brightburst" -> List.of(info("damage", 1 + power / 2), info("radius", 10 + level * 2));
            case "star_swarm", "luminous_beam" -> List.of(info("damage", 1 + power * .75F));
            case "piercing_light" -> List.of(info("damage", 1 + power / 2), info("projectile_count", needleCount(level, power) * 3));
            case "starcutter" -> List.of(info("damage", power * 2), info("radius", 10));
            case "silvery_barbs" -> List.of(info("radius", level + 3), info("duration", 1));
            case "celestial_tether" -> List.of(info("hits_dodged", level + 1), info("duration", (80 + power * 10) * 4 / 20));
            case "astral_echo" -> List.of(info("distance", echoDistance(level, caster)));
            default -> List.of();
        };
    }
    private MutableComponent info(String key, float value) {
        return Component.translatable("ui.irons_spellbooks." + key, Utils.stringTruncation(value, 1));
    }
    private static int needleCount(int level, float power) { return Math.min(128, Math.max(1, (int)((4 + level) * power))); }
    private float echoDistance(int level, LivingEntity caster) {
        return (float)(9 + Utils.softCapFormula(getEntityPowerMultiplier(caster)) * level * 3.5);
    }

    @Override public boolean checkPreCastConditions(Level world, int level, LivingEntity caster, MagicData data) {
        return !behavior.equals("celestial_tether") || !caster.isPassenger();
    }

    @Override public void onCast(Level world, int level, LivingEntity caster, CastSource source, MagicData data) {
        if (world.isClientSide) return;
        float power = getSpellPower(level, caster);
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle().normalize();
        switch (behavior) {
            case "starfire" -> ArcanumProjectile.spawn(world, caster, getSpellId(), eye.add(look.scale(.2)), look.scale(2.5), power * .3F, true, .35F);
            case "brightburst" -> {
                float radius = 10 + level * 2;
                for (LivingEntity target : world.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(radius), t -> ArcanumEvents.hostile(caster, t))) {
                    double distance = caster.distanceTo(target);
                    if (distance > radius || caster.isPassengerOfSameVehicle(target)) continue;
                    io.redspace.ironsspellbooks.damage.DamageSources.applyDamage(target, 1 + power / 2, getDamageSource(caster));
                    double force = power * 2 / (distance + .01);
                    force -= Math.min(force, target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) * 5);
                    target.setDeltaMovement(target.getDeltaMovement().add(target.position().subtract(caster.position()).normalize().scale(force)));
                    target.hurtMarked = true;
                }
                ArcanumEffect.create(caster, ArcanumEffect.BURST, getSpellId(), 0, radius, 12);
            }
            case "star_swarm", "luminous_beam" -> {
                // One anchor per cast, refreshed by each paid continuous pulse.
                if (data.getCastDurationRemaining() > 0) {
                    ArcanumEffect.channel(caster, behavior.equals("star_swarm") ? ArcanumEffect.SWARM : ArcanumEffect.BEAM, getSpellId(), 1 + power * .75F);
                }
            }
            case "piercing_light" -> {
                int count = needleCount(level, power);
                Vec3 center = caster.position().add(0, caster.getEyeHeight() / 3, 0);
                for (int i = 0; i < count; i++) {
                    double angle = Math.PI * 2 * i / count;
                    Vec3 direction = new Vec3(Math.sin(angle), caster.getRandom().nextDouble(), Math.cos(angle)).normalize();
                    Vec3 pos = center.add(direction.scale(caster.getBbWidth() + 1.5));
                    for (double speed : new double[]{.375, .125, .625}) {
                        ArcanumProjectile.spawn(world, caster, getSpellId(), pos, direction.scale(speed), 1 + power / 2, false, .2F);
                    }
                }
            }
            case "starcutter" -> {
                Vec3 target = Utils.raycastForEntity(world, caster, (level + 4 + power * 1.5F) * 1.5F, true).getLocation();
                // The upstream entity clamps radius to 1; its burst radius is 10.
                ArcanumEffect orb = ArcanumEffect.create(caster, ArcanumEffect.CUTTER, getSpellId(), power * 2, 1, 30);
                orb.setPos(target);
            }
            case "silvery_barbs" -> {
                Vec3 center = Utils.raycastForEntity(world, caster, 25, true).getLocation();
                for (LivingEntity target : world.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(center, center).inflate(level + 3), t -> t != caster && !ArcanumEvents.hostile(caster, t))) {
                    target.getPersistentData().putLong(ArcanumEvents.WARD_UNTIL, world.getGameTime() + 20);
                    MagicManager.spawnParticles(world, ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 15, .5, .5, .5, 0, false);
                }
            }
            case "celestial_tether" -> {
                ArcanumEffect tether = ArcanumEffect.create(caster, ArcanumEffect.TETHER, getSpellId(), 0, 1, (int)((80 + power * 10) * 4));
                tether.setHits(level + 1);
                if (!caster.startRiding(tether, true)) tether.discard();
            }
            case "astral_echo" -> {
                Vec3 destination = TeleportSpell.findTeleportLocation(world, caster, echoDistance(level, caster));
                ArcanumEffect.create(caster, ArcanumEffect.ECHO, getSpellId(), 0, 1, 30);
                if (caster.isPassenger()) caster.stopRiding();
                Utils.handleSpellTeleport(this, caster, destination);
                caster.resetFallDistance();
            }
        }
        super.onCast(world, level, caster, source, data);
    }
}
