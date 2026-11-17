package cn.wanyiwige.astralcompat.entity;

import cn.wanyiwige.astralcompat.registry.ModEntities;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.function.Supplier;

public class AstralBoltEntity extends AbstractMagicProjectile {
    public static final int STARFIRE = 0;
    public static final int PIERCING_LIGHT = 1;
    public static final int STAR_SWARM = 2;

    private static final DustParticleOptions STAR_BLUE =
            new DustParticleOptions(new Vector3f(0.35F, 0.78F, 1.0F), 0.9F);
    private static final DustParticleOptions STAR_GOLD =
            new DustParticleOptions(new Vector3f(1.0F, 0.72F, 0.22F), 0.8F);
    private static final DustParticleOptions STAR_WHITE =
            new DustParticleOptions(new Vector3f(0.95F, 0.95F, 1.0F), 1.1F);

    private static final EntityDataAccessor<Integer> DATA_KIND =
            SynchedEntityData.defineId(AstralBoltEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SCALE =
            SynchedEntityData.defineId(AstralBoltEntity.class, EntityDataSerializers.FLOAT);

    public AstralBoltEntity(EntityType<? extends AstralBoltEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public AstralBoltEntity(Level level, LivingEntity owner, int kind) {
        this(ModEntities.ASTRAL_BOLT.get(), level);
        setOwner(owner);
        setKind(kind);
        if (kind == STARFIRE) {
            setPierceLevel(2);
            setCanRicochet(true);
            setScale(0.35F);
        } else if (kind == STAR_SWARM) {
            setScale(0.42F);
        } else {
            setScale(0.2F);
        }
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_KIND, STARFIRE);
        entityData.define(DATA_SCALE, 0.35F);
        super.defineSynchedData();
    }

    public int getKind() {
        return entityData.get(DATA_KIND);
    }

    public void setKind(int kind) {
        entityData.set(DATA_KIND, kind);
    }

    public float getScale() {
        return entityData.get(DATA_SCALE);
    }

    public void setScale(float scale) {
        entityData.set(DATA_SCALE, scale);
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        Entity target = hitResult.getEntity();
        Entity owner = getOwner();
        if (owner instanceof LivingEntity livingOwner && livingOwner.isAlliedTo(target)) {
            return;
        }

        super.onHitEntity(hitResult);
        if (level().isClientSide) {
            return;
        }

        String spellId = switch (getKind()) {
            case STARFIRE -> "asterismarcanum:starfire";
            case STAR_SWARM -> "asterismarcanum:star_swarm";
            default -> "asterismarcanum:piercing_light";
        };
        DamageSources.applyDamage(target, getDamage(), SpellRegistry.getSpell(spellId).getDamageSource(this, owner));

        if (getKind() == STARFIRE) {
            consumeEntityImpact(hitResult, true);
        } else {
            discard();
        }
    }

    @Override
    public void trailParticles() {
        if (getKind() == STARFIRE) {
            Vec3 forward = getDeltaMovement().normalize();
            Vec3 right = forward.cross(new Vec3(0, 1, 0));
            if (right.lengthSqr() < 1.0E-4) {
                right = new Vec3(1, 0, 0);
            } else {
                right = right.normalize();
            }
            Vec3 up = right.cross(forward).normalize();
            double phase = tickCount * 0.82;

            for (int strand = 0; strand < 3; strand++) {
                double angle = phase + strand * Math.PI * 2.0 / 3.0;
                Vec3 spiral = right.scale(Math.cos(angle) * 0.13)
                        .add(up.scale(Math.sin(angle) * 0.13));
                Vec3 trailPos = position().subtract(forward.scale(0.12 + strand * 0.08)).add(spiral);
                level().addParticle(strand == 1 ? STAR_GOLD : STAR_BLUE,
                        trailPos.x, trailPos.y, trailPos.z,
                        -forward.x * 0.035, -forward.y * 0.035, -forward.z * 0.035);
            }

            level().addParticle(STAR_WHITE, getX(), getY(), getZ(), 0, 0, 0);
            if ((tickCount & 1) == 0) {
                level().addParticle(ParticleTypes.END_ROD, getX(), getY(), getZ(),
                        -forward.x * 0.025, -forward.y * 0.025, -forward.z * 0.025);
            }
        } else if (getKind() == STAR_SWARM) {
            Vec3 forward = getDeltaMovement().normalize();
            double phase = tickCount * 0.72 + getId() * 0.37;
            double radius = 0.055;
            level().addParticle((tickCount & 1) == 0 ? STAR_BLUE : STAR_GOLD,
                    getX() + Math.cos(phase) * radius,
                    getY() + Math.sin(phase * 1.7) * radius,
                    getZ() + Math.sin(phase) * radius,
                    -forward.x * 0.025, -forward.y * 0.025, -forward.z * 0.025);
            level().addParticle(STAR_WHITE, getX(), getY(), getZ(), 0, 0, 0);
        } else {
            double speed = 0.05;
            double dx = random.nextDouble() * 2 * speed - speed;
            double dy = random.nextDouble() * 2 * speed - speed;
            double dz = random.nextDouble() * 2 * speed - speed;
            level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX() + dx, getY() + dy, getZ() + dz, dx, dy, dz);
        }
    }

    @Override
    public void impactParticles(double x, double y, double z) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (getKind() == STAR_SWARM) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z,
                    10, 0.18, 0.18, 0.18, 0.1);
            serverLevel.sendParticles(ParticleTypes.END_ROD, x, y, z,
                    7, 0.14, 0.14, 0.14, 0.07);
            serverLevel.sendParticles(STAR_BLUE, x, y, z,
                    6, 0.12, 0.12, 0.12, 0.06);
            return;
        }
        if (getKind() != STARFIRE) {
            return;
        }

        serverLevel.sendParticles(ParticleTypes.FLASH, x, y, z,
                1, 0, 0, 0, 0);
        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z,
                28, 0.45, 0.45, 0.45, 0.16);
        serverLevel.sendParticles(ParticleTypes.END_ROD, x, y, z,
                18, 0.32, 0.32, 0.32, 0.11);
        serverLevel.sendParticles(ParticleTypes.GLOW, x, y, z,
                10, 0.42, 0.42, 0.42, 0.08);
        serverLevel.sendParticles(ParticleTypes.FIREWORK, x, y, z,
                8, 0.28, 0.28, 0.28, 0.10);

        for (int i = 0; i < 12; i++) {
            double angle = Math.PI * 2.0 * i / 12.0;
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            serverLevel.sendParticles((i & 1) == 0 ? STAR_BLUE : STAR_GOLD,
                    x, y, z, 0, dx, 0.15, dz, 0.24);
            serverLevel.sendParticles(STAR_WHITE,
                    x, y, z, 0, dx * 0.55, Math.sin(angle * 2.0) * 0.65, dz * 0.55, 0.18);
        }
    }

    @Override
    public float getSpeed() {
        return 2.5F;
    }

    @Override
    public Optional<Supplier<SoundEvent>> getImpactSound() {
        return Optional.of(() -> SoundEvents.AMETHYST_BLOCK_CHIME);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Kind", getKind());
        tag.putFloat("Scale", getScale());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setKind(tag.getInt("Kind"));
        if (tag.contains("Scale")) {
            setScale(tag.getFloat("Scale"));
        }
    }
}
