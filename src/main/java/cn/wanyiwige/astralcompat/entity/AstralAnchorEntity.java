package cn.wanyiwige.astralcompat.entity;

import cn.wanyiwige.astralcompat.events.AstralSpellEvents;
import cn.wanyiwige.alexcavesspellsbook.spells.DarkSparkSpell;
import cn.wanyiwige.astralcompat.registry.ModEntities;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.util.RaycastBuilder;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;

public class AstralAnchorEntity extends Projectile implements AntiMagicSusceptible {
    public static final int LUMINOUS_BEAM = 0;
    public static final int STAR_SWARM = 1;
    public static final int STARCUTTER = 2;
    public static final int ASTRAL_ECHO = 3;
    public static final int CELESTIAL_TETHER = 4;
    public static final int BRIGHTBURST = 5;
    public static final int PIERCING_LIGHT_CIRCLE = 6;
    public static final int DARK_SPARK = 7;

    private static final double LUMINOUS_BEAM_RANGE = 150.0;
    private static final double LUMINOUS_BEAM_RADIUS = 1.625;
    private static final double LUMINOUS_BEAM_START_OFFSET = 3.0;
    private static final double DARK_SPARK_RANGE = 120.0;
    private static final double DARK_SPARK_HIT_RADIUS = 0.48;
    private static final double DARK_SPARK_START_OFFSET = 1.25;
    private static final double DARK_SPARK_FOLLOW_RATE = 0.20;
    private static final ResourceLocation LAST_PRISM_PARTICLE =
            new ResourceLocation("brutality", "last_prism_ray_particle");
    private static final ResourceLocation LAST_PRISM_SOUND =
            new ResourceLocation("brutality", "last_prism_use");

    private static final EntityDataAccessor<Integer> DATA_MODE =
            SynchedEntityData.defineId(AstralAnchorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_OWNER_ID =
            SynchedEntityData.defineId(AstralAnchorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LEVEL =
            SynchedEntityData.defineId(AstralAnchorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DURATION =
            SynchedEntityData.defineId(AstralAnchorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_DAMAGE =
            SynchedEntityData.defineId(AstralAnchorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_BEAM_LENGTH =
            SynchedEntityData.defineId(AstralAnchorEntity.class, EntityDataSerializers.FLOAT);

    private boolean darkSparkAimInitialized;
    private float darkSparkYaw;
    private float darkSparkPitch;
    private float previousDarkSparkYaw;
    private float previousDarkSparkPitch;

    public AstralAnchorEntity(EntityType<? extends AstralAnchorEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public AstralAnchorEntity(Level level, LivingEntity owner, int mode, int spellLevel, float damage, int duration) {
        this(ModEntities.ASTRAL_ANCHOR.get(), level);
        setOwner(owner);
        entityData.set(DATA_OWNER_ID, owner.getId());
        entityData.set(DATA_MODE, mode);
        entityData.set(DATA_LEVEL, spellLevel);
        entityData.set(DATA_DAMAGE, damage);
        entityData.set(DATA_DURATION, duration);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_MODE, LUMINOUS_BEAM);
        entityData.define(DATA_OWNER_ID, -1);
        entityData.define(DATA_LEVEL, 1);
        entityData.define(DATA_DURATION, 100);
        entityData.define(DATA_DAMAGE, 1.0F);
        entityData.define(DATA_BEAM_LENGTH, 0.0F);
    }

    public int getMode() {
        return entityData.get(DATA_MODE);
    }

    public int getOwnerId() {
        return entityData.get(DATA_OWNER_ID);
    }

    public int getSpellLevel() {
        return entityData.get(DATA_LEVEL);
    }

    public float getDamage() {
        return entityData.get(DATA_DAMAGE);
    }

    public int getDuration() {
        return entityData.get(DATA_DURATION);
    }

    public float getBeamLength() {
        return entityData.get(DATA_BEAM_LENGTH);
    }

    public void refresh(float damage, int duration) {
        entityData.set(DATA_DAMAGE, damage);
        entityData.set(DATA_DURATION, Math.max(getDuration(), tickCount + duration));
    }

    public Entity getSyncedOwner() {
        Entity owner = getOwner();
        if (owner != null) {
            return owner;
        }
        return getOwnerId() < 0 ? null : level().getEntity(getOwnerId());
    }

    @Override
    public void tick() {
        super.tick();
        Entity ownerEntity = getSyncedOwner();

        if (getMode() == LUMINOUS_BEAM || getMode() == STAR_SWARM || getMode() == DARK_SPARK) {
            if (!(ownerEntity instanceof LivingEntity owner) || !owner.isAlive()) {
                discard();
                return;
            }
            if (!level().isClientSide) {
                MagicData magicData = MagicData.getPlayerMagicData(owner);
                String expectedSpell = switch (getMode()) {
                    case LUMINOUS_BEAM -> "asterismarcanum:luminous_beam";
                    case DARK_SPARK -> DarkSparkSpell.SPELL_ID;
                    default -> "asterismarcanum:star_swarm";
                };
                if (!magicData.isCasting() || !expectedSpell.equals(magicData.getCastingSpellId())) {
                    discard();
                    return;
                }
            }
            Vec3 anchor;
            if (getMode() == LUMINOUS_BEAM) {
                Vec3 look = owner.getLookAngle().normalize();
                anchor = getLuminousBeamTracingPoint(owner)
                        .add(look.scale(LUMINOUS_BEAM_START_OFFSET))
                        .subtract(0, 0.35, 0);
            } else if (getMode() == DARK_SPARK) {
                if (!darkSparkAimInitialized) {
                    darkSparkYaw = previousDarkSparkYaw = owner.getYRot();
                    darkSparkPitch = previousDarkSparkPitch = owner.getXRot();
                    darkSparkAimInitialized = true;
                } else {
                    previousDarkSparkYaw = darkSparkYaw;
                    previousDarkSparkPitch = darkSparkPitch;
                    darkSparkYaw = Mth.rotLerp((float) DARK_SPARK_FOLLOW_RATE, darkSparkYaw, owner.getYRot());
                    darkSparkPitch = Mth.lerp((float) DARK_SPARK_FOLLOW_RATE, darkSparkPitch, owner.getXRot());
                }
                Vec3 aim = Vec3.directionFromRotation(darkSparkPitch, darkSparkYaw);
                Vec3 target = owner.getEyePosition().add(aim.scale(DARK_SPARK_START_OFFSET)).subtract(0, 0.35, 0);
                anchor = position().lerp(target, DARK_SPARK_FOLLOW_RATE);
            } else {
                anchor = owner.getEyePosition().subtract(0, 0.35, 0);
            }
            setPos(anchor);
            setYRot(getMode() == DARK_SPARK ? darkSparkYaw : owner.getYRot());
            setXRot(getMode() == DARK_SPARK ? darkSparkPitch : owner.getXRot());
        }

        if (!level().isClientSide) {
            switch (getMode()) {
                case LUMINOUS_BEAM -> tickBeam(ownerEntity);
                case DARK_SPARK -> tickDarkSpark(ownerEntity);
                case STAR_SWARM -> tickSwarm(ownerEntity);
                case STARCUTTER -> tickStarcutter(ownerEntity);
                case ASTRAL_ECHO -> tickEcho();
                case CELESTIAL_TETHER -> tickTether();
                case BRIGHTBURST, PIERCING_LIGHT_CIRCLE -> {
                    // Damage is applied once at cast time; this short-lived anchor is visual only.
                }
                default -> discard();
            }
        }

        if (tickCount > getDuration()) {
            discard();
        }
    }

    private void tickBeam(Entity ownerEntity) {
        if (!(ownerEntity instanceof LivingEntity owner)) {
            return;
        }
        Vec3 traceStart = getLuminousBeamTracingPoint(owner);
        Vec3 look = owner.getLookAngle().normalize();
        Vec3 beamStart = traceStart.add(look.scale(LUMINOUS_BEAM_START_OFFSET));
        Vec3 intendedEnd = traceStart.add(look.scale(LUMINOUS_BEAM_RANGE));
        HitResult blockHit = level().clip(new ClipContext(traceStart, intendedEnd,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        Vec3 hitEnd = blockHit.getType() == HitResult.Type.MISS ? intendedEnd : blockHit.getLocation();
        double tracedDistance = traceStart.distanceTo(hitEnd);
        double beamLength = Math.max(0.0, tracedDistance - LUMINOUS_BEAM_START_OFFSET);
        Vec3 end = beamStart.add(look.scale(beamLength));
        entityData.set(DATA_BEAM_LENGTH, (float) beamLength);

        ParticleOptions prismParticle = getLastPrismParticle();
        AABB search = new AABB(beamStart, end).inflate(LUMINOUS_BEAM_RADIUS);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, search,
                target -> target != owner && target.isAlive() && !owner.isAlliedTo(target))) {
            if (distanceToSegmentSqr(target.getBoundingBox().getCenter(), beamStart, end)
                    <= LUMINOUS_BEAM_RADIUS * LUMINOUS_BEAM_RADIUS) {
                // Brutality 的 last_prism 使用 bypasses_cooldown 伤害类型；这里保留
                // Iron's Spells 的法术伤害源，只对本次射线命中清除受伤冷却。
                target.invulnerableTime = 0;
                DamageSources.applyDamage(target, getDamage(),
                        SpellRegistry.getSpell("asterismarcanum:luminous_beam").getDamageSource(this, owner));
                spawnPrismParticles(prismParticle,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 1);
            }
        }

        spawnPrismParticles(prismParticle, hitEnd.x, hitEnd.y, hitEnd.z, 5);

        if (tickCount % 6 == 0) {
            SoundEvent prismSound = ForgeRegistries.SOUND_EVENTS.getValue(LAST_PRISM_SOUND);
            if (prismSound != null) {
                level().playSound(null, blockPosition(), prismSound, SoundSource.PLAYERS, 1.4F, 1.0F);
            } else {
                level().playSound(null, blockPosition(), SoundEvents.BEACON_AMBIENT,
                        SoundSource.PLAYERS, 0.8F, 1.7F);
            }
        }
    }

    /** Each of six rays has its own six-per-second hit schedule. */
    private void tickDarkSpark(Entity ownerEntity) {
        if (!(ownerEntity instanceof LivingEntity owner)) return;
        int rayCount = tickCount < DarkSparkSpell.SPLIT_TICKS ? 1 : 6;
        float charge = Math.min(1.0F, tickCount / (float) DarkSparkSpell.CHARGE_TICKS);
        float hitDamage = getDamage() * (1.0F + 2.5F * charge);
        ParticleOptions particle = tickCount < 120 ? ParticleTypes.REVERSE_PORTAL : getLastPrismParticle();

        for (int ray = 0; ray < rayCount; ray++) {
            Vec3 rayStart = getDarkSparkRayStart(ray, tickCount, 1.0F);
            Vec3 intendedEnd = getDarkSparkIntendedEnd(ray, tickCount, 1.0F);
            HitResult blockHit = level().clip(new ClipContext(rayStart, intendedEnd,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
            Vec3 end = blockHit.getType() == HitResult.Type.MISS ? intendedEnd : blockHit.getLocation();
            if (ray == 0) entityData.set(DATA_BEAM_LENGTH, (float) rayStart.distanceTo(end));
            if (tickCount % 4 == ray % 4) {
                spawnPrismParticles(particle, end.x, end.y, end.z, 1);
            }
            // Terraria's ten frames per hit is six hits per second. At 20 TPS,
            // (age * 6) mod 20 produces exactly six opportunities per 20 ticks.
            if (((tickCount + ray * 3) * 6) % 20 >= 6) continue;
            AABB search = new AABB(rayStart, end).inflate(DARK_SPARK_HIT_RADIUS + 1.0);
            for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, search,
                    candidate -> candidate != owner && candidate.isAlive() && !owner.isAlliedTo(candidate))) {
                if (target.getBoundingBox().inflate(DARK_SPARK_HIT_RADIUS).clip(rayStart, end).isEmpty()) continue;
                target.invulnerableTime = 0;
                DamageSources.applyDamage(target, hitDamage,
                        SpellRegistry.getSpell(DarkSparkSpell.SPELL_ID).getDamageSource(this, owner));
            }
        }

        if (tickCount % 6 == 0) {
            SoundEvent prismSound = ForgeRegistries.SOUND_EVENTS.getValue(LAST_PRISM_SOUND);
            level().playSound(null, blockPosition(), prismSound == null ? SoundEvents.BEACON_AMBIENT : prismSound,
                    SoundSource.PLAYERS, 0.8F, 0.85F + charge * 0.3F);
        }
    }

    public static Vec3 darkSparkStart(LivingEntity owner) {
        Vec3 look = owner.getLookAngle().normalize();
        return owner.getEyePosition().add(look.scale(DARK_SPARK_START_OFFSET));
    }

    public Vec3 getDarkSparkStart(float partialTick) {
        return new Vec3(xOld, yOld, zOld).lerp(position(), partialTick).add(0, 0.35, 0);
    }

    public float getDarkSparkYaw(float partialTick) {
        return Mth.rotLerp(partialTick, previousDarkSparkYaw, darkSparkYaw);
    }

    public float getDarkSparkPitch(float partialTick) {
        return Mth.lerp(partialTick, previousDarkSparkPitch, darkSparkPitch);
    }

    public Vec3 getDarkSparkLook(float partialTick) {
        return Vec3.directionFromRotation(getDarkSparkPitch(partialTick), getDarkSparkYaw(partialTick));
    }

    public Vec3 getDarkSparkRayStart(int ray, double age, float partialTick) {
        Vec3 start = getDarkSparkStart(partialTick);
        if (age < DarkSparkSpell.SPLIT_TICKS) return start;
        Vec3 look = getDarkSparkLook(partialTick);
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 0.001 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 up = right.cross(look).normalize();
        double split = Mth.clamp((age - DarkSparkSpell.SPLIT_TICKS) / 24.0, 0.0, 1.0);
        double angle = darkSparkRotation(age) + ray * Math.PI * 2.0 / 6.0;
        return start.add(right.scale(Math.cos(angle) * 0.22 * split))
                .add(up.scale(Math.sin(angle) * 0.22 * split));
    }

    public Vec3 getDarkSparkIntendedEnd(int ray, double age, float partialTick) {
        Vec3 start = getDarkSparkRayStart(ray, age, partialTick);
        Vec3 look = getDarkSparkLook(partialTick);
        if (age < DarkSparkSpell.SPLIT_TICKS) return start.add(look.scale(DARK_SPARK_RANGE));
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 0.001 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 up = right.cross(look).normalize();
        double split = Mth.clamp((age - DarkSparkSpell.SPLIT_TICKS) / 24.0, 0.0, 1.0);
        double progress = Math.min(1.0, age / DarkSparkSpell.CHARGE_TICKS);
        double fan = (0.42 - 0.22 * progress) * split;
        double rotation = darkSparkRotation(age);
        double angle = rotation + ray * Math.PI * 2.0 / 6.0;
        Vec3 direction = look.add(right.scale(Math.cos(angle) * fan))
                .add(up.scale(Math.sin(angle) * fan)).normalize();
        return start.add(direction.scale(DARK_SPARK_RANGE));
    }

    private static double darkSparkRotation(double age) {
        // The angular speed decays smoothly from about 0.30 to 0.04 radians per tick.
        return 0.04 * age + 15.6 * (1.0 - Math.exp(-age / 60.0));
    }

    private static Vec3 getLuminousBeamTracingPoint(LivingEntity owner) {
        // Mirrors Brutality's OrientedBoundingBox#getInitialTracingPoint.
        return owner.getEyePosition().add(0, owner.getBbHeight() * 0.15 * owner.getScale(), 0);
    }

    private ParticleOptions getLastPrismParticle() {
        if (ForgeRegistries.PARTICLE_TYPES.getValue(LAST_PRISM_PARTICLE) instanceof SimpleParticleType particle) {
            return particle;
        }
        return ParticleTypes.END_ROD;
    }

    private void spawnPrismParticles(ParticleOptions particle, double x, double y, double z, int count) {
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(particle, x, y, z, count, 1.0, 1.0, 1.0, 0.0);
        }
    }

    private void tickSwarm(Entity ownerEntity) {
        if (!(ownerEntity instanceof LivingEntity owner)) {
            return;
        }
        if (tickCount % 8 != 0) {
            return;
        }

        // Blood Needles' useful behavior is not merely the circular spawn layout:
        // every projectile independently aims from its own origin at the same
        // crosshair raycast point. This produces a real converging barrage.
        Vec3 target = RaycastBuilder.begin(level(), owner)
                .range(40.0F)
                .checkForBlocks(true)
                .build()
                .getLocation();
        Vec3 look = owner.getLookAngle().normalize();
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 0.001) {
            right = new Vec3(1, 0, 0);
        } else {
            right = right.normalize();
        }
        Vec3 up = right.cross(look).normalize();
        Vec3 center = owner.getEyePosition().add(look.scale(0.45));
        int count = Math.min(11, 7 + getSpellLevel() / 2);
        double phase = tickCount * 0.31;

        for (int i = 0; i < count; i++) {
            double angle = phase + Math.PI * 2.0 * i / count;
            double radius = (i & 1) == 0 ? 2.15 : 1.65;
            Vec3 origin = center
                    .add(right.scale(Math.cos(angle) * radius))
                    .add(up.scale(Math.sin(angle) * radius));
            // A tiny focus disk prevents every projectile from occupying the
            // exact same pixel while keeping the entire volley inside the crosshair.
            Vec3 focus = target
                    .add(right.scale(Math.cos(angle * 2.0) * 0.12))
                    .add(up.scale(Math.sin(angle * 2.0) * 0.12));
            Vec3 direction = focus.subtract(origin).normalize();

            AstralBoltEntity bolt = new AstralBoltEntity(level(), owner, AstralBoltEntity.STAR_SWARM);
            bolt.setPos(origin);
            bolt.setDamage(getDamage());
            bolt.setScale(0.40F + (i % 3) * 0.035F);
            bolt.shoot(direction);
            level().addFreshEntity(bolt);

            MagicManager.spawnParticles(level(), ParticleTypes.END_ROD,
                    origin.x, origin.y, origin.z, 2, 0.06, 0.06, 0.06, 0.01, false);
        }

        if (tickCount % 16 == 0) {
            level().playSound(null, owner.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK,
                    SoundSource.PLAYERS, 0.65F, 1.55F + random.nextFloat() * 0.2F);
        }
    }

    private void tickStarcutter(Entity ownerEntity) {
        // Starcutter's front-arc hit is resolved at cast time, matching Flaming Strike.
        // This anchor only provides the brief crescent and constellation visual.
    }

    private void explodeStarcutter(Entity ownerEntity) {
        double radius = 10.0;
        Vec3 center = position();
        MagicManager.spawnParticles(level(), ParticleTypes.FLASH, center.x, center.y, center.z, 1, 0, 0, 0, 0, false);
        MagicManager.spawnParticles(level(), ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z,
                80, 2.5, 2.5, 2.5, 0.16, true);
        level().playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2F, 1.5F);

        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius), LivingEntity::isAlive)) {
            if (target == ownerEntity || (ownerEntity instanceof LivingEntity owner && owner.isAlliedTo(target))) {
                continue;
            }
            double distance = target.getBoundingBox().getCenter().distanceTo(center);
            if (distance > radius) {
                continue;
            }
            Vec3 targetCenter = target.getBoundingBox().getCenter();
            HitResult los = level().clip(new ClipContext(center, targetCenter, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (los.getType() != HitResult.Type.MISS && los.getLocation().distanceToSqr(targetCenter) > 0.4) {
                continue;
            }
            float scaledDamage = (float) (getDamage() * (1.0 - distance / radius));
            DamageSources.applyDamage(target, scaledDamage,
                    SpellRegistry.getSpell("asterismarcanum:starcutter").getDamageSource(this, ownerEntity));
        }
    }

    private void tickEcho() {
        if (tickCount % 5 == 0) {
            MagicManager.spawnParticles(level(), ParticleTypes.REVERSE_PORTAL, getX(), getY() + 0.8, getZ(),
                    8, 0.35, 0.7, 0.35, 0.03, false);
        }
    }

    private void tickTether() {
        if (tickCount % 4 == 0) {
            MagicManager.spawnParticles(level(), ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.9, getZ(),
                    Math.max(8, getSpellLevel() * 3), 1.25, 1.15, 1.25, 0.025, false);
            MagicManager.spawnParticles(level(), ParticleTypes.END_ROD, getX(), getY() + 1.0, getZ(),
                    Math.max(4, getSpellLevel() + 3), 1.0, 1.0, 1.0, 0.015, false);
        }
        if (tickCount % 10 == 0) {
            MagicManager.spawnParticles(level(), ParticleTypes.REVERSE_PORTAL, getX(), getY() + 0.85, getZ(),
                    12, 0.95, 0.9, 0.95, 0.025, false);
        }
        if (tickCount % 40 == 0) {
            level().playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.PLAYERS, 0.45F, 1.65F);
        }
    }

    private static double distanceToSegmentSqr(Vec3 point, Vec3 start, Vec3 end) {
        Vec3 segment = end.subtract(start);
        double lengthSqr = segment.lengthSqr();
        if (lengthSqr < 1.0E-7) {
            return point.distanceToSqr(start);
        }
        double t = Math.max(0.0, Math.min(1.0, point.subtract(start).dot(segment) / lengthSqr));
        return point.distanceToSqr(start.add(segment.scale(t)));
    }

    @Override
    public void onAntiMagic(MagicData magicData) {
        Entity owner = getSyncedOwner();
        if (getMode() == ASTRAL_ECHO && owner != null) {
            owner.teleportTo(getX(), getY(), getZ());
            owner.resetFallDistance();
        } else if (getMode() == STARCUTTER && !level().isClientSide) {
            explodeStarcutter(owner);
        } else if (getMode() == CELESTIAL_TETHER && owner instanceof Player player) {
            AstralSpellEvents.cancelTether(player);
        }
        discard();
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Mode", getMode());
        tag.putInt("OwnerId", getOwnerId());
        tag.putInt("SpellLevel", getSpellLevel());
        tag.putInt("Duration", getDuration());
        tag.putFloat("Damage", getDamage());
        tag.putFloat("BeamLength", getBeamLength());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(DATA_MODE, tag.getInt("Mode"));
        entityData.set(DATA_OWNER_ID, tag.getInt("OwnerId"));
        entityData.set(DATA_LEVEL, tag.getInt("SpellLevel"));
        entityData.set(DATA_DURATION, tag.getInt("Duration"));
        entityData.set(DATA_DAMAGE, tag.getFloat("Damage"));
        entityData.set(DATA_BEAM_LENGTH, tag.getFloat("BeamLength"));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
