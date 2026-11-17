package cn.wanyiwige.alexcavesspellsbook.entities;

import cn.wanyiwige.alexcavesspellsbook.candy.CandyCombat;
import cn.wanyiwige.alexcavesspellsbook.registry.ModEntities;
import cn.wanyiwige.alexcavesspellsbook.registry.ModSpells;
import cn.wanyiwige.alexcavesspellsbook.registry.PureDarknessParticles;
import cn.wanyiwige.alexcavesspellsbook.spells.PureDarknessFieldSpell.Kind;
import com.github.alexmodguy.alexscaves.server.misc.ACSoundRegistry;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PureDarknessFieldEntity extends Projectile {
    public static final int EYE_HIT = 14, FISSURE_LEAD = 6, BAT_WINDUP = 8, BAT_DIVE = 12;
    public static final double BAT_TRIGGER_DEPTH = 9;
    public static final double EYE_HEIGHT = 2.5;
    public static final double EYE_RISE = .82;
    private static final double MIST_SLOPE = .40;
    private static final Kind[] KINDS = Kind.values();
    private static final EntityDataAccessor<CompoundTag> LAYOUT =
            SynchedEntityData.defineId(PureDarknessFieldEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<CompoundTag> DIVE =
            SynchedEntityData.defineId(PureDarknessFieldEntity.class, EntityDataSerializers.COMPOUND_TAG);

    public record Perch(Vec3 anchor, BlockPos support) {}
    public record Placement(Vec3 origin, Vec3 direction, List<Vec3> route, List<Perch> perches) {}

    private static final class Flock {
        final Set<UUID> hits = new HashSet<>();
        final Map<UUID, PureDarknessFieldEntity> assigned = new HashMap<>();

        boolean canTarget(UUID target) {
            PureDarknessFieldEntity bat = assigned.get(target);
            return !hits.contains(target) && (bat == null || bat.isRemoved());
        }
    }

    private Set<UUID> hit = new HashSet<>();
    private float damage;
    private Flock flock;
    private CompoundTag cachedLayout;
    private List<Vec3> route = List.of();

    public PureDarknessFieldEntity(EntityType<? extends PureDarknessFieldEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    @Override protected void defineSynchedData() {
        entityData.define(LAYOUT, new CompoundTag());
        entityData.define(DIVE, new CompoundTag());
    }

    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public boolean ready() { return entityData.get(LAYOUT).contains("Start"); }
    public Kind kind() { return KINDS[entityData.get(LAYOUT).getInt("Kind")]; }
    public int rank() { return entityData.get(LAYOUT).getInt("Rank"); }
    public Vec3 heading() { return vector(entityData.get(LAYOUT).getCompound("Heading")); }
    public float age(float partial) { return level().getGameTime() - entityData.get(LAYOUT).getLong("Start") + partial; }
    public CompoundTag dive() { return entityData.get(DIVE); }

    public static Placement plan(ServerLevel level, LivingEntity caster, Kind kind, int rank) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        Vec3 forward = Vec3.directionFromRotation(0, caster.getYRot());
        double range = kind.range(rank);
        return switch (kind) {
            case MOTHSCALE_MIST -> new Placement(eye, look, List.of(), List.of());
            case CLOSING_EYE -> planEye(level, caster, eye, look, forward, range);
            case CORRODENT_BURROW -> planBurrow(level, caster, forward, range);
            case VESPER_PERCH -> planPerches(level, caster, eye, look, forward, range, kind.batCount(rank));
        };
    }

    private static Placement planEye(ServerLevel level, LivingEntity caster, Vec3 eye, Vec3 look,
                                     Vec3 forward, double range) {
        Vec3 end = eye.add(look.scale(range));
        BlockHitResult block = clip(level, eye, end, ClipContext.Block.COLLIDER, caster);
        double distance = eye.distanceTo(block.getLocation());
        Vec3 anchor = eye.add(look.scale(Math.max(0, distance - .48)));
        double nearest = distance;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(eye, block.getLocation()).inflate(1), target -> CandyCombat.enemy(caster, target))) {
            var crossing = target.getBoundingBox().inflate(.18).clip(eye, block.getLocation());
            if (crossing.isPresent() && eye.distanceTo(crossing.get()) < nearest) {
                nearest = eye.distanceTo(crossing.get());
                anchor = center(target);
            }
        }
        Vec3 cube = anchor.add(0, EYE_RISE, 0);
        if (distance < .8 || !level.hasChunkAt(BlockPos.containing(cube))
                || level.getBlockCollisions(caster, new AABB(cube, cube).inflate(.53, .375, .53)).iterator().hasNext()) {
            return null;
        }
        return new Placement(anchor, forward, List.of(), List.of());
    }

    private static Placement planBurrow(ServerLevel level, LivingEntity caster, Vec3 forward, double range) {
        List<Vec3> route = traceBurrow(level, caster, caster.position(), forward, range);
        return route.size() < 3 ? null : new Placement(route.get(0), forward, route, List.of());
    }

    private static List<Vec3> traceBurrow(ServerLevel level, LivingEntity caster, Vec3 origin,
                                         Vec3 forward, double range) {
        List<Vec3> route = new ArrayList<>();
        double y = origin.y;
        for (int i = 0; i <= (int) (range * 2); i++) {
            Vec3 column = origin.add(forward.scale(i * .5));
            BlockPos columnPos = BlockPos.containing(column.x, y, column.z);
            if (!level.hasChunkAt(columnPos) || !level.getWorldBorder().isWithinBounds(columnPos)) break;
            BlockHitResult surface = clip(level, new Vec3(column.x, y + .8, column.z),
                    new Vec3(column.x, y - 1.25, column.z), ClipContext.Block.COLLIDER, caster);
            if (surface.getType() == HitResult.Type.MISS || surface.getDirection() != Direction.UP) break;
            Vec3 point = surface.getLocation();
            Vec3 above = point.add(0, .42, 0);
            if (level.getBlockCollisions(caster, new AABB(above, above).inflate(.23)).iterator().hasNext()) break;
            if (route.isEmpty() && clip(level, caster.position().add(0, .42, 0),
                    above, ClipContext.Block.COLLIDER, caster).getType() != HitResult.Type.MISS) break;
            if (!route.isEmpty() && clip(level, route.get(route.size() - 1).add(0, .42, 0),
                    above, ClipContext.Block.COLLIDER, caster).getType() != HitResult.Type.MISS) break;
            route.add(point);
            y = point.y;
        }
        return route;
    }

    private static Placement planPerches(ServerLevel level, LivingEntity caster, Vec3 eye, Vec3 look,
                                         Vec3 forward, double range, int count) {
        BlockHitResult hit = clip(level, eye, eye.add(look.scale(range)), ClipContext.Block.OUTLINE, caster);
        Perch first = legalPerch(level, caster, eye, hit);
        if (first == null) return null;

        BlockPos center = first.support();
        List<BlockPos> nearby = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-3, -2, -3), center.offset(3, 2, 3))) {
            if (pos.equals(center) || pos.distSqr(center) > 15) continue;
            nearby.add(pos.immutable());
        }
        nearby.sort(Comparator.comparingDouble(pos -> Vec3.atCenterOf(pos).distanceToSqr(first.anchor())));

        List<Perch> perches = new ArrayList<>(count);
        perches.add(first);
        for (BlockPos pos : nearby) {
            if (!validSupport(level, pos)) continue;
            Vec3 middle = Vec3.atBottomCenterOf(pos);
            BlockHitResult bottom = clip(level, middle.subtract(0, .1, 0), middle.add(0, .999, 0),
                    ClipContext.Block.OUTLINE, caster);
            if (!bottom.getBlockPos().equals(pos)) continue;
            Perch perch = legalPerch(level, caster, eye, bottom);
            if (perch != null && perches.stream().noneMatch(other ->
                    other.anchor().subtract(perch.anchor()).horizontalDistanceSqr() < 1.1 * 1.1)) {
                perches.add(perch);
                if (perches.size() == count) break;
            }
        }
        return perches.size() >= 4 ? new Placement(first.anchor(), forward, List.of(), List.copyOf(perches)) : null;
    }

    private static Perch legalPerch(ServerLevel level, LivingEntity caster, Vec3 eye, BlockHitResult hit) {
        if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.DOWN
                || !validSupport(level, hit.getBlockPos())) return null;
        Vec3 anchor = hit.getLocation().subtract(0, .05, 0);
        AABB clearance = new AABB(anchor.add(-.36, -1.1, -.36), anchor.add(.36, 0, .36));
        if (level.getBlockCollisions(caster, clearance).iterator().hasNext()
                || level.containsAnyLiquid(clearance)
                || clip(level, eye, anchor, ClipContext.Block.COLLIDER, caster).getType() != HitResult.Type.MISS) {
            return null;
        }
        return new Perch(anchor, hit.getBlockPos());
    }

    private static boolean validSupport(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        var state = level.getBlockState(pos);
        return state.isFaceSturdy(level, pos, Direction.DOWN)
                || "alexscaves:thornwood_branch".equals(String.valueOf(ForgeRegistries.BLOCKS.getKey(state.getBlock())));
    }

    public static void spawn(ServerLevel level, LivingEntity caster, Kind kind, int rank, float damage,
                             Placement placement) {
        if (kind == Kind.VESPER_PERCH) {
            Flock flock = new Flock();
            boolean spawned = false;
            for (Perch perch : placement.perches()) {
                var bat = createField(level, caster, kind, rank, damage, perch.anchor(),
                        placement.direction(), List.of(), perch.support());
                bat.flock = flock;
                spawned |= level.addFreshEntity(bat);
            }
            if (!spawned) return;
        } else if (kind == Kind.CORRODENT_BURROW) {
            if (!spawnCorrodents(level, caster, kind, rank, damage, placement)) return;
        } else if (!level.addFreshEntity(createField(level, caster, kind, rank, damage, placement.origin(),
                placement.direction(), placement.route(), null))) {
            return;
        }
        SoundEvent cue = switch (kind) {
            case CLOSING_EYE -> ACSoundRegistry.WATCHER_IDLE.get();
            case MOTHSCALE_MIST -> SoundEvents.SOUL_ESCAPE;
            case CORRODENT_BURROW -> ACSoundRegistry.CORRODENT_DIG_STOP.get();
            case VESPER_PERCH -> ACSoundRegistry.VESPER_QUIET_IDLE.get();
        };
        sound(level, placement.origin, cue, .65F, .85F);
    }

    private static boolean spawnCorrodents(ServerLevel level, LivingEntity caster, Kind kind, int rank,
                                           float damage, Placement placement) {
        Set<UUID> packHits = new HashSet<>();
        Vec3 forward = placement.direction();
        Vec3 right = new Vec3(forward.z, 0, -forward.x);
        List<Vec3> center = placement.route();
        boolean spawned = false;
        int centerFollowers = 0;
        for (int i = 0; i < kind.corrodentCount(rank); i++) {
            int lane = ((i + 1) / 2) * (i % 2 == 0 ? -1 : 1);
            List<Vec3> route = center;
            int delay = i * 2;
            if (lane != 0) {
                Vec3 flank = placement.origin().add(right.scale(lane * 1.2));
                List<Vec3> side = traceBurrow(level, caster, flank, forward, kind.range(rank));
                if (side.size() >= 3) {
                    route = side;
                } else {
                    delay = ++centerFollowers * 4;
                }
            }
            var runner = createField(level, caster, kind, rank, damage, route.get(0), forward, route, null);
            runner.hit = packHits;
            if (delay > 0) {
                CompoundTag layout = runner.entityData.get(LAYOUT).copy();
                layout.putLong("Start", layout.getLong("Start") + delay);
                runner.entityData.set(LAYOUT, layout);
            }
            spawned |= level.addFreshEntity(runner);
        }
        return spawned;
    }

    private static PureDarknessFieldEntity createField(ServerLevel level, LivingEntity caster, Kind kind,
                                                       int rank, float damage, Vec3 origin, Vec3 direction,
                                                       List<Vec3> route, BlockPos support) {
        var field = new PureDarknessFieldEntity(ModEntities.PURE_DARKNESS_FIELD.get(), level);
        field.setOwner(caster);
        field.setPos(origin);
        field.damage = damage;
        CompoundTag layout = new CompoundTag();
        layout.putInt("Kind", kind.ordinal());
        layout.putInt("Rank", rank);
        layout.putLong("Start", level.getGameTime());
        layout.put("Heading", vector(direction));
        if (support != null) layout.putLong("Support", support.asLong());
        ListTag points = new ListTag();
        route.forEach(point -> points.add(vector(point)));
        layout.put("Route", points);
        field.entityData.set(LAYOUT, layout);
        return field;
    }

    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        if (!ready() || !(getOwner() instanceof LivingEntity caster) || !caster.isAlive()
                || caster.level() != server) {
            discard();
            return;
        }
        int age = (int) age(0);
        if (age < 0) return;
        if (age >= kind().duration(rank()) && (kind() != Kind.VESPER_PERCH || dive().isEmpty())) {
            discard();
            return;
        }
        switch (kind()) {
            case CLOSING_EYE -> tickEye(server, caster, age);
            case MOTHSCALE_MIST -> tickMist(server, caster, age);
            case CORRODENT_BURROW -> tickBurrow(server, caster, age);
            case VESPER_PERCH -> tickPerch(server, caster, age);
        }
    }

    private void tickEye(ServerLevel server, LivingEntity caster, int age) {
        if (age == 4) {
            sound(server, position(), ACSoundRegistry.WATCHER_SCARE.get(), .55F, .8F);
            particles(server, PureDarknessParticles.SCARLET_FILAMENT.get(), position(), 5, .4, .6, .4);
        }
        if (age != EYE_HIT) return;
        Vec3 normal = heading();
        Vec3 right = new Vec3(normal.z, 0, -normal.x);
        double halfWidth = kind().eyeWidth(rank()) * .5;
        for (LivingEntity target : enemies(server, caster, getBoundingBox().inflate(halfWidth + .5, 1.5, halfWidth + .5))) {
            Vec3 offset = center(target).subtract(position());
            double radius = target.getBbWidth() * .5 * (Math.abs(normal.x) + Math.abs(normal.z));
            AABB bounds = target.getBoundingBox();
            if (Math.abs(offset.dot(normal)) <= .19 + radius && Math.abs(offset.dot(right)) <= halfWidth + radius
                    && bounds.maxY >= getY() - EYE_HEIGHT / 2 && bounds.minY <= getY() + EYE_HEIGHT / 2
                    && CandyCombat.visible(server, position(), target)) {
                strike(caster, target);
            }
        }
        particles(server, PureDarknessParticles.IRIS_FRAGMENT.get(), position(), 9, halfWidth * .46, .63, halfWidth * .46);
        sound(server, position(), ACSoundRegistry.WATCHER_ATTACK.get(), .85F, 1F);
    }

    private void tickMist(ServerLevel server, LivingEntity caster, int age) {
        if ((age & 1) != 0) return;
        Vec3 axis = heading();
        double reach = kind().range(rank()) * Math.min(1, (age + 2) / 8.0);
        Vec3 origin = position();
        for (LivingEntity target : enemies(server, caster, new AABB(origin, origin.add(axis.scale(reach))).inflate(3))) {
            Vec3 sample = closest(target.getBoundingBox(), origin.add(axis.scale(Mth.clamp(
                    center(target).subtract(origin).dot(axis), 0, reach))));
            Vec3 offset = sample.subtract(origin);
            double along = offset.dot(axis);
            if (along >= 0 && along <= reach && offset.subtract(axis.scale(along)).length() <= .35 + along * MIST_SLOPE
                    && clip(server, origin, sample, ClipContext.Block.COLLIDER, this).getType() == HitResult.Type.MISS
                    && hit.add(target.getUUID())) {
                hurt(caster, target);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, kind().slowTicks(rank()), 0), caster);
            }
        }
        if (age % 4 != 0) return;
        Vec3 right = axis.cross(new Vec3(0, 1, 0)).normalize();
        if (right.lengthSqr() < .01) right = new Vec3(1, 0, 0);
        Vec3 up = right.cross(axis);
        for (int i = 0; i < 9; i++) {
            double distance = reach * (.2 + random.nextDouble() * .8);
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = Math.sqrt(random.nextDouble()) * (.23 + distance * MIST_SLOPE);
            Vec3 point = origin.add(axis.scale(distance)).add(right.scale(radius * Math.cos(angle)))
                    .add(up.scale(radius * Math.sin(angle)));
            if (clip(server, origin, point, ClipContext.Block.COLLIDER, this).getType() != HitResult.Type.MISS) continue;
            var particle = i < 4 ? PureDarknessParticles.BLOOD_MIST : i < 7 ? PureDarknessParticles.MOTH_SCALE
                    : PureDarknessParticles.GLOOM_MOTH;
            particles(server, particle.get(), point, 1, 0, 0, 0);
        }
    }

    private void tickBurrow(ServerLevel server, LivingEntity caster, int age) {
        Vec3 crack = burrowPoint(age + FISSURE_LEAD);
        if (age % 4 == 0 && age < 30) {
            particles(server, PureDarknessParticles.SOIL_CLAST.get(), crack.add(0, .18, 0), 4, .22, .12, .22);
        }
        if (age < FISSURE_LEAD) return;
        Vec3 from = burrowPoint(age - 1).add(0, .55, 0);
        Vec3 ground = burrowPoint(age);
        Vec3 to = ground.add(0, .55, 0);
        if (!server.hasChunkAt(BlockPos.containing(to))
                || clip(server, ground.add(0, .52, 0), ground.subtract(0, .62, 0),
                        ClipContext.Block.COLLIDER, this).getType() != HitResult.Type.BLOCK
                || clip(server, from, to, ClipContext.Block.COLLIDER, this).getType() != HitResult.Type.MISS
                || server.getBlockCollisions(this, new AABB(to, to).inflate(.19)).iterator().hasNext()) {
            discard();
            return;
        }
        if (age == FISSURE_LEAD) sound(server, to, ACSoundRegistry.CORRODENT_TEETH.get(), .75F, .9F);
        for (LivingEntity target : enemies(server, caster, new AABB(from, to).inflate(.8, .7, .8))) {
            AABB contact = target.getBoundingBox().inflate(.55, .48, .55);
            if ((contact.contains(from) || contact.clip(from, to).isPresent())
                    && CandyCombat.visible(server, to, target) && strike(caster, target)) {
                particles(server, PureDarknessParticles.BLOOD_CUT.get(), center(target), 3, .2, .18, .2);
                sound(server, to, ACSoundRegistry.CORRODENT_ATTACK.get(), .72F, 1F);
            }
        }
    }

    private void tickPerch(ServerLevel server, LivingEntity caster, int age) {
        CompoundTag phase = dive();
        if (phase.isEmpty()) {
            if (flock == null || !validSupport(server, BlockPos.of(entityData.get(LAYOUT).getLong("Support")))) {
                discard();
                return;
            }
            if ((age & 1) != 0) return;
            double radius = kind().triggerRadius(rank());
            LivingEntity nearest = null;
            double best = Double.MAX_VALUE;
            Vec3 anchor = position().add(0, -.65, 0);
            for (LivingEntity target : enemies(server, caster, new AABB(anchor.add(-radius, -BAT_TRIGGER_DEPTH, -radius),
                    anchor.add(radius, 0, radius)))) {
                Vec3 offset = center(target).subtract(anchor);
                double distance = offset.lengthSqr();
                if (offset.y < -.1 && offset.y >= -BAT_TRIGGER_DEPTH && offset.horizontalDistanceSqr() <= radius * radius
                        && distance < best && flock.canTarget(target.getUUID())
                        && CandyCombat.visible(server, anchor, target)) {
                    nearest = target;
                    best = distance;
                }
            }
            if (nearest != null) {
                flock.assigned.put(nearest.getUUID(), this);
                CompoundTag start = new CompoundTag();
                start.putInt("Lock", age);
                start.put("Target", vector(center(nearest)));
                entityData.set(DIVE, start);
                sound(server, anchor, ACSoundRegistry.VESPER_SCREAM.get(), .72F, 1.1F);
                particles(server, PureDarknessParticles.SHADOW_SHARD.get(), anchor, 7, .42, .35, .42);
            }
            return;
        }
        if (phase.contains("Impact")) {
            if (age >= phase.getInt("Impact") + 7) discard();
            return;
        }
        int flying = age - phase.getInt("Lock") - BAT_WINDUP;
        if (flying < 0) return;
        Vec3 from = batPoint(age - 1);
        Vec3 to = batPoint(age);
        if (flying == 0) sound(server, from, ACSoundRegistry.VESPER_FLAP.get(), .8F, .85F);
        BlockHitResult wall = clip(server, from, to, ClipContext.Block.COLLIDER, this);
        Vec3 end = wall.getLocation();
        if (!server.hasChunkAt(BlockPos.containing(end))) {
            discard();
            return;
        }
        LivingEntity first = null;
        Vec3 collision = end;
        double nearest = from.distanceToSqr(end);
        for (LivingEntity target : enemies(server, caster, new AABB(from, end).inflate(.55))) {
            if (flock.hits.contains(target.getUUID())) continue;
            AABB contact = target.getBoundingBox().inflate(.32);
            Vec3 impact = contact.contains(from) ? from : contact.clip(from, end).orElse(null);
            if (impact != null && from.distanceToSqr(impact) <= nearest && CandyCombat.visible(server, from, target)) {
                nearest = from.distanceToSqr(impact);
                first = target;
                collision = impact;
            }
        }
        if (first != null) {
            Vec3 incoming = from.subtract(to);
            Vec3 facing = first.getLookAngle();
            if (first.isBlocking() && facing.x * incoming.x + facing.z * incoming.z > 0) {
                CompoundTag fall = phase.copy();
                fall.putInt("Impact", age);
                fall.put("Contact", vector(collision));
                entityData.set(DIVE, fall);
                sound(server, collision, SoundEvents.SHIELD_BLOCK, .8F, .9F);
                particles(server, PureDarknessParticles.SHADOW_SHARD.get(), collision, 5, .22, .2, .22);
                return;
            }
            if (flock.hits.add(first.getUUID())) hurt(caster, first);
            particles(server, PureDarknessParticles.BLOOD_CUT.get(), collision, 6, .25, .22, .25);
            discard();
        } else if (wall.getType() != HitResult.Type.MISS || flying >= BAT_DIVE) {
            particles(server, PureDarknessParticles.SHADOW_SHARD.get(), end, 5, .23, .23, .23);
            discard();
        } else if (flying % 3 == 0) {
            particles(server, PureDarknessParticles.SCARLET_FILAMENT.get(), to, 2, .12, .12, .12);
        }
    }

    public Vec3 burrowPoint(float age) {
        CompoundTag layout = entityData.get(LAYOUT);
        if (cachedLayout != layout) {
            route = new ArrayList<>();
            for (Tag entry : layout.getList("Route", Tag.TAG_COMPOUND)) route.add(vector((CompoundTag) entry));
            cachedLayout = layout;
        }
        if (route.isEmpty()) return position();
        float progress = Mth.clamp((age - FISSURE_LEAD) / 29F, 0, 1) * (route.size() - 1);
        int index = (int) progress;
        return route.get(index).lerp(route.get(Math.min(index + 1, route.size() - 1)), progress - index);
    }

    public Vec3 batPoint(float age) {
        CompoundTag phase = dive();
        Vec3 start = position().add(0, -.65, 0);
        if (phase.isEmpty()) return start;
        if (phase.contains("Impact")) {
            float ticks = Mth.clamp(age - phase.getInt("Impact"), 0, 7);
            return vector(phase.getCompound("Contact")).subtract(0, ticks * ticks * .023, 0);
        }
        float t = Mth.clamp((age - phase.getInt("Lock") - BAT_WINDUP) / BAT_DIVE, 0, 1);
        return start.lerp(vector(phase.getCompound("Target")), t).add(0, Math.sin(t * Math.PI) * .32, 0);
    }

    private List<LivingEntity> enemies(ServerLevel server, LivingEntity caster, AABB bounds) {
        return server.getEntitiesOfClass(LivingEntity.class, bounds, target -> CandyCombat.enemy(caster, target));
    }

    private boolean strike(LivingEntity caster, LivingEntity target) {
        if (!hit.add(target.getUUID())) return false;
        hurt(caster, target);
        return true;
    }

    private void hurt(LivingEntity caster, LivingEntity target) {
        DamageSources.applyDamage(target, damage, ModSpells.pureDarknessField(kind()).getDamageSource(this, caster));
    }

    private static Vec3 center(LivingEntity target) { return target.getBoundingBox().getCenter(); }

    private static Vec3 closest(AABB box, Vec3 point) {
        return new Vec3(Mth.clamp(point.x, box.minX, box.maxX), Mth.clamp(point.y, box.minY, box.maxY),
                Mth.clamp(point.z, box.minZ, box.maxZ));
    }

    private static BlockHitResult clip(Level level, Vec3 from, Vec3 to, ClipContext.Block shape,
                                       net.minecraft.world.entity.Entity entity) {
        return level.clip(new ClipContext(from, to, shape, ClipContext.Fluid.NONE, entity));
    }

    private static void particles(ServerLevel server, SimpleParticleType particle, Vec3 point, int count,
                                  double x, double y, double z) {
        server.sendParticles(particle, point.x, point.y, point.z, count, x, y, z, .038);
    }

    private static void sound(ServerLevel level, Vec3 point, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, point.x, point.y, point.z, sound, SoundSource.HOSTILE, volume, pitch);
    }

    private static CompoundTag vector(Vec3 value) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("X", value.x);
        tag.putDouble("Y", value.y);
        tag.putDouble("Z", value.z);
        return tag;
    }

    private static Vec3 vector(CompoundTag tag) {
        return new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"));
    }
}

