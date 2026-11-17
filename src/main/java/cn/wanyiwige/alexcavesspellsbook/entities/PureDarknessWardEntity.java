package cn.wanyiwige.alexcavesspellsbook.entities;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.alexcavesspellsbook.candy.CandyCombat;
import cn.wanyiwige.alexcavesspellsbook.registry.ModEntities;
import cn.wanyiwige.alexcavesspellsbook.registry.ModSpells;
import cn.wanyiwige.alexcavesspellsbook.registry.PureDarknessParticles;
import cn.wanyiwige.alexcavesspellsbook.spells.PureDarknessWardSpell.Kind;
import com.github.alexmodguy.alexscaves.server.misc.ACSoundRegistry;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;

import java.lang.ref.WeakReference;
import java.util.*;

@Mod.EventBusSubscriber(modid = AlexCavesSpellsbook.MOD_ID)
public final class PureDarknessWardEntity extends Entity {
    public static final int DREAD_WAVE = 8, MAW_BITE = 18;
    private static final int RETREAT_TICKS = 8;
    private static final Kind[] KINDS = Kind.values();
    private static final Map<LivingEntity, WeakReference<PureDarknessWardEntity>> VESTMENTS = new WeakHashMap<>();
    private static final EntityDataAccessor<CompoundTag> LAYOUT =
            SynchedEntityData.defineId(PureDarknessWardEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Float> REMAINING =
            SynchedEntityData.defineId(PureDarknessWardEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> END =
            SynchedEntityData.defineId(PureDarknessWardEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LAST_HIT =
            SynchedEntityData.defineId(PureDarknessWardEntity.class, EntityDataSerializers.INT);

    public record Placement(Vec3 origin, Vec3 heading) {}

    private LivingEntity caster;
    private float damage;
    private final Map<LivingEntity, Vec3> retreating = new HashMap<>();

    public PureDarknessWardEntity(EntityType<? extends PureDarknessWardEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    @Override protected void defineSynchedData() {
        entityData.define(LAYOUT, new CompoundTag());
        entityData.define(REMAINING, 0F);
        entityData.define(END, -1);
        entityData.define(LAST_HIT, -1);
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public boolean ready() { return entityData.get(LAYOUT).contains("Start"); }
    public Kind kind() { return KINDS[entityData.get(LAYOUT).getInt("Kind")]; }
    public int rank() { return entityData.get(LAYOUT).getInt("Rank"); }
    public Vec3 heading() {
        double angle = entityData.get(LAYOUT).getDouble("Yaw");
        return new Vec3(Math.sin(angle), 0, Math.cos(angle));
    }
    public float age(float partial) { return level().getGameTime() - entityData.get(LAYOUT).getLong("Start") + partial; }
    public int ending() { return entityData.get(END); }
    public int lastHit() { return entityData.get(LAST_HIT); }
    public float remaining() { return entityData.get(REMAINING); }
    public boolean shattered() { return remaining() <= 0 && lastHit() >= 0; }
    public LivingEntity owner() {
        if (caster != null) return caster;
        return level().getEntity(entityData.get(LAYOUT).getInt("Owner")) instanceof LivingEntity living ? living : null;
    }


    public static Placement plan(ServerLevel level, LivingEntity caster, Kind kind, int rank) {
        Vec3 direction = Vec3.directionFromRotation(0, caster.getYRot());
        if (kind == Kind.MARTYR_VESTMENT) return new Placement(caster.position(), direction);
        Vec3 eyes = caster.getEyePosition();
        if (kind == Kind.WATCHER_DREAD) {
            Vec3 face = eyes.add(direction.scale(.7));
            return clear(level, caster, eyes, face) ? new Placement(face, direction) : null;
        }
        Vec3 look = caster.getLookAngle();
        double range = kind.range(rank);
        Vec3 aim = ray(level, caster, eyes, eyes.add(look.scale(range))).getLocation().subtract(look.scale(.06));
        Vec3 ground = surface(level, caster, aim, .25, 3.1);
        if (ground == null || ground.distanceToSqr(eyes) > (range + .1) * (range + .1)
                || !clear(level, caster, eyes, ground.add(0, .12, 0))) return null;
        if (kind == Kind.FORSAKEN_MAW && !space(level, caster, ground, .52, .9)) return null;
        return new Placement(ground, direction);
    }

    public static boolean spawn(ServerLevel level, LivingEntity caster, Kind kind, int rank, float damage, Placement plan) {
        var root = create(level, caster, kind, rank, plan.origin(), plan.heading());
        root.damage = damage;
        if (kind == Kind.MARTYR_VESTMENT) root.entityData.set(REMAINING, kind.budget(rank));
        if (!level.addFreshEntity(root)) return false;
        if (kind == Kind.MARTYR_VESTMENT) {
            var old = VESTMENTS.put(caster, new WeakReference<>(root));
            var previous = old == null ? null : old.get();
            if (previous != null) previous.beginEnd();
            emit(level, PureDarknessParticles.SCARLET_FILAMENT.get(), caster.position().add(0, 1.1, 0), 10, .35);
        }
        SoundEvent cue = switch (kind) {
            case WATCHER_DREAD -> ACSoundRegistry.WATCHER_IDLE.get();
            case MARTYR_VESTMENT -> SoundEvents.SOUL_ESCAPE;
            case FORSAKEN_MAW -> ACSoundRegistry.FORSAKEN_SPAWN.get();
        };
        sound(level, plan.origin(), cue, .7F, .76F);
        return true;
    }

    private static PureDarknessWardEntity create(ServerLevel level, LivingEntity caster, Kind kind, int rank,
                                                  Vec3 at, Vec3 heading) {
        var entity = new PureDarknessWardEntity(ModEntities.PURE_DARKNESS_WARD.get(), level);
        entity.caster = caster;
        entity.setPos(at);
        CompoundTag tag = new CompoundTag();
        tag.putInt("Owner", caster.getId());
        tag.putInt("Kind", kind.ordinal());
        tag.putInt("Rank", rank);
        tag.putLong("Start", level.getGameTime());
        tag.putDouble("Yaw", Math.atan2(heading.x, heading.z));
        entity.entityData.set(LAYOUT, tag);
        return entity;
    }

    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        if (caster == null || !caster.isAlive() || caster.level() != server) {
            discard();
            return;
        }
        int age = (int) age(0);
        if (kind() == Kind.MARTYR_VESTMENT) setPos(caster.position());
        if (ending() >= 0) {
            if (age - ending() >= 8) discard();
            return;
        }
        int duration = kind().duration(rank());
        if (kind() == Kind.WATCHER_DREAD) duration = DREAD_WAVE + RETREAT_TICKS + 1;
        if (age >= duration) {
            if (kind() == Kind.MARTYR_VESTMENT) beginEnd();
            else discard();
            return;
        }
        switch (kind()) {
            case WATCHER_DREAD -> tickDread(server, age);
            case MARTYR_VESTMENT -> { }
            case FORSAKEN_MAW -> tickMaw(server, age);
        }
    }

    private void tickDread(ServerLevel level, int age) {
        Vec3 center = position().subtract(heading().scale(.7));
        double range = kind().range(rank());
        if (age == DREAD_WAVE) {
            for (LivingEntity target : enemies(level, new AABB(center, center.add(heading().scale(range))).inflate(range * .75, 2, range * .75))) {
                Vec3 offset = target.getBoundingBox().getCenter().subtract(center);
                Vec3 flat = offset.multiply(1, 0, 1);
                if (flat.lengthSqr() > range * range || flat.normalize().dot(heading()) < .59 || Math.abs(offset.y) > 2
                        || !CandyCombat.visible(level, center, target)) continue;
                int weak = kind().weakness(rank());
                if (!(target instanceof Player) && !PureDarknessRiteEntity.largeTarget(target)
                        && PureDarknessRiteEntity.tryShortControl(level, target)) {
                    retreating.put(target, flat.normalize());
                } else {
                    applyWeakness(target, PureDarknessRiteEntity.largeTarget(target) ? weak / 2 : weak);
                }
            }
            emit(level, PureDarknessParticles.IRIS_FRAGMENT.get(), position().add(heading().scale(.5)), 9, .52);
            emit(level, PureDarknessParticles.SONIC_WAVEFRONT.get(), position().add(heading().scale(1.3)), 5, .6);
            sound(level, position(), ACSoundRegistry.WATCHER_SCARE.get(), 1, .87F);
        }
        if (age >= DREAD_WAVE && age < DREAD_WAVE + RETREAT_TICKS) {
            retreating.forEach((target, direction) -> {
                if (!target.isAlive() || target.level() != level || !target.onGround()) return;
                Vec3 ahead = target.position().add(direction.scale(.34));
                if (surface(level, target, ahead, .14, .38) == null
                        || !level.noCollision(target, target.getBoundingBox().move(direction.scale(.21)))) return;
                if (target instanceof Mob mob) mob.getNavigation().stop();
                target.setDeltaMovement(direction.x * .21, Math.min(target.getDeltaMovement().y, .06), direction.z * .21);
                target.hasImpulse = true;
            });
        }
        if (age == DREAD_WAVE + RETREAT_TICKS) {
            retreating.keySet().forEach(target -> {
                if (target.isAlive() && target.level() == level) applyWeakness(target, kind().weakness(rank()));
            });
            retreating.clear();
        }
    }

    private void tickMaw(ServerLevel level, int age) {
        if (age == 6) {
            emit(level, PureDarknessParticles.SCARLET_FILAMENT.get(), position().add(0, .24, 0), 8, .8);
        }
        if (age != MAW_BITE || surface(level, this, position(), .12, .18) == null) return;
        double radius = kind().radius(rank());
        AABB bounds = new AABB(position().add(-radius, -.22, -radius), position().add(radius, 2.45, radius));
        for (LivingEntity target : enemies(level, bounds)) {
            AABB box = target.getBoundingBox();
            double x = Mth.clamp(getX(), box.minX, box.maxX) - getX();
            double z = Mth.clamp(getZ(), box.minZ, box.maxZ) - getZ();
            if (x * x + z * z <= radius * radius && CandyCombat.visible(level, position().add(0, .55, 0), target)) {
                strike(this, target);
            }
        }
        emit(level, PureDarknessParticles.BLOOD_MIST.get(), position().add(0, .7, 0), 14, radius * .42);
        emit(level, PureDarknessParticles.BLOOD_CUT.get(), position().add(0, .82, 0), 8, radius * .29);
        sound(level, position(), ACSoundRegistry.FORSAKEN_BITE.get(), 1.05F, .74F);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void absorbDamage(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (!(target.level() instanceof ServerLevel server) || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || !Float.isFinite(event.getAmount()) || event.getAmount() <= 0) return;
        var ref = VESTMENTS.get(target);
        var ward = ref == null ? null : ref.get();
        if (ward == null || !ward.isAlive() || ward.level() != server || ward.ending() >= 0
                || ward.age(0) >= ward.kind().duration(ward.rank())) return;
        float absorbed = Math.min(ward.remaining(), event.getAmount());
        if (absorbed <= 0) return;
        float remaining = ward.remaining() - absorbed;
        ward.entityData.set(REMAINING, remaining);
        ward.entityData.set(LAST_HIT, (int) ward.age(0));
        event.setAmount(event.getAmount() - absorbed);
        Vec3 center = target.getBoundingBox().getCenter();
        emit(server, PureDarknessParticles.SCARLET_FILAMENT.get(), center, remaining == 0 ? 10 : 5, .34);
        if (remaining == 0) {
            ward.beginEnd();
            emit(server, PureDarknessParticles.SHADOW_SHARD.get(), center, 8, .37);
            sound(server, center, SoundEvents.WOOL_BREAK, .86F, .72F);
        }
    }



    private void beginEnd() {
        if (ending() < 0) entityData.set(END, Math.max(0, (int) age(0)));
    }

    private List<LivingEntity> enemies(ServerLevel level, AABB bounds) {
        return level.getEntitiesOfClass(LivingEntity.class, bounds, target -> CandyCombat.enemy(caster, target));
    }

    private void strike(Entity direct, LivingEntity target) {
        DamageSources.applyDamage(target, damage, ModSpells.pureDarknessWard(kind()).getDamageSource(direct, caster));
    }

    private void applyWeakness(LivingEntity target, int duration) {
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0), caster);
    }


    private static Vec3 surface(ServerLevel level, Entity entity, Vec3 at, double up, double down) {
        BlockPos pos = BlockPos.containing(at);
        if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)) return null;
        BlockHitResult result = ray(level, entity, at.add(0, up, 0), at.subtract(0, down, 0));
        return result.getType() == HitResult.Type.BLOCK && result.getDirection() == Direction.UP ? result.getLocation() : null;
    }

    private static boolean space(ServerLevel level, Entity entity, Vec3 at, double half, double height) {
        if (!level.hasChunkAt(BlockPos.containing(at))) return false;
        AABB area = new AABB(at.add(-half, .02, -half), at.add(half, height, half));
        return !level.getBlockCollisions(entity, area).iterator().hasNext() && !level.containsAnyLiquid(area);
    }

    private static boolean clear(Level level, Entity entity, Vec3 from, Vec3 to) {
        return ray(level, entity, from, to).getType() == HitResult.Type.MISS;
    }

    private static BlockHitResult ray(Level level, Entity entity, Vec3 from, Vec3 to) {
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
    }

    private static void emit(ServerLevel level, SimpleParticleType particle, Vec3 at, int count, double spread) {
        level.sendParticles(particle, at.x, at.y, at.z, count, spread, spread * .5, spread, .034);
    }

    private static void sound(ServerLevel level, Vec3 at, SoundEvent event, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, event, SoundSource.HOSTILE, volume, pitch);
    }
}

