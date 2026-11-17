package cn.wanyiwige.alexcavesspellsbook.entities;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.alexcavesspellsbook.candy.CandyCombat;
import cn.wanyiwige.alexcavesspellsbook.registry.ModEntities;
import cn.wanyiwige.alexcavesspellsbook.registry.ModSpells;
import cn.wanyiwige.alexcavesspellsbook.registry.PureDarknessParticles;
import cn.wanyiwige.alexcavesspellsbook.spells.PureDarknessRiteSpell.Kind;
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
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = AlexCavesSpellsbook.MOD_ID)
public final class PureDarknessRiteEntity extends Entity {
    public static final int CHORUS_BURST = 24;
    public static final double CHORUS_HEIGHT = 2.75;

    public static int chorusBeat(int slot) { return 6 + slot * 6; }
    public static final int GIBBET_HOLD = 12;
    public static final int GIBBET_HIT = 20;
    public static final int BURROW_TRAVEL_TICKS = 18;
    private static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("forge", "bosses"));
    private static final Kind[] KINDS = Kind.values();
    private static final Map<LivingEntity, WeakReference<PureDarknessRiteEntity>> RITUALS = new WeakHashMap<>();
    private static final Map<LivingEntity, WeakReference<PureDarknessRiteEntity>> GUARDS = new WeakHashMap<>();
    private static final Map<LivingEntity, WeakReference<PureDarknessRiteEntity>> BURROWS = new WeakHashMap<>();
    private static final Map<LivingEntity, Long> CONTROL_UNTIL = new WeakHashMap<>();
    private static final EntityDataAccessor<CompoundTag> LAYOUT =
            SynchedEntityData.defineId(PureDarknessRiteEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Integer> STATUS =
            SynchedEntityData.defineId(PureDarknessRiteEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> END =
            SynchedEntityData.defineId(PureDarknessRiteEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> IMPACT =
            SynchedEntityData.defineId(PureDarknessRiteEntity.class, EntityDataSerializers.INT);

    // Null altar lane endpoints are obstructed and remain inactive.
    public record Placement(Vec3 origin, Vec3 forward, List<Vec3> points) {}

    private LivingEntity caster;
    private float damage;
    private float integrity;
    private Vec3 lastBurrow;
    private Placement placement;
    private PureDarknessRiteEntity conductor;
    private final List<PureDarknessRiteEntity> chanters = new ArrayList<>(3);
    private final List<LivingEntity> held = new ArrayList<>();

    public PureDarknessRiteEntity(EntityType<? extends PureDarknessRiteEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override protected void defineSynchedData() {
        entityData.define(LAYOUT, new CompoundTag());
        entityData.define(STATUS, 0);
        entityData.define(END, -1);
        entityData.define(IMPACT, 0);
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public boolean ready() { return entityData.get(LAYOUT).contains("Start"); }
    public Kind kind() { return KINDS[entityData.get(LAYOUT).getInt("Kind")]; }
    public int rank() { return entityData.get(LAYOUT).getInt("Rank"); }
    public int slot() { return entityData.get(LAYOUT).getInt("Slot"); }
    public float age(float partial) { return level().getGameTime() - entityData.get(LAYOUT).getLong("Start") + partial; }
    public int status() { return entityData.get(STATUS); }
    public int ending() { return entityData.get(END); }
    public int guardImpact() { return entityData.get(IMPACT); }
    public Vec3 forward() { return vector(entityData.get(LAYOUT).getCompound("Forward")); }
    public Vec3 origin() { return vector(entityData.get(LAYOUT).getCompound("Origin")); }

    public Vec3 point(int index) {
        ListTag points = entityData.get(LAYOUT).getList("Points", Tag.TAG_COMPOUND);
        CompoundTag point = points.getCompound(index);
        return point.isEmpty() ? null : vector(point);
    }

    public LivingEntity owner() {
        if (caster != null) return caster;
        return level().getEntity(entityData.get(LAYOUT).getInt("Owner")) instanceof LivingEntity living ? living : null;
    }

    public static boolean hasRitual(LivingEntity owner) {
        return active(RITUALS, owner) != null;
    }

    private static PureDarknessRiteEntity active(Map<LivingEntity, WeakReference<PureDarknessRiteEntity>> map,
                                                LivingEntity owner) {
        var ref = map.get(owner);
        var entity = ref == null ? null : ref.get();
        return entity != null && entity.isAlive() && entity.ending() < 0 && entity.level() == owner.level() ? entity : null;
    }

    public static Placement plan(ServerLevel level, LivingEntity caster, Kind kind, int rank) {
        Vec3 forward = Vec3.directionFromRotation(0, caster.getYRot());
        double range = kind.range(rank);
        if (kind == Kind.MOTHWING_GUARD) return new Placement(caster.position(), forward, List.of());
        if (kind == Kind.BURROWING_PRAYER) return planBurrow(level, caster, forward, range);
        Vec3 from = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        Vec3 aimed = ray(level, caster, from, from.add(look.scale(range))).getLocation().subtract(look.scale(.035));
        Vec3 origin = surface(level, caster, aimed, .35, 3.0);
        if (origin == null || origin.distanceToSqr(from) > (range + .1) * (range + .1)
                || !clear(level, caster, from, origin.add(0, .15, 0))) return null;
        if (kind == Kind.THORN_GIBBET) {
            Vec3 right = new Vec3(forward.z, 0, -forward.x);
            for (int side : new int[]{-1, 1}) {
                Vec3 hook = origin.add(right.scale(side * .81));
                if (surface(level, caster, hook, .08, .16) == null || !space(level, caster, hook, .19, 1.55)) return null;
            }
            return new Placement(origin, forward, List.of());
        }
        List<Vec3> nodes = new ArrayList<>(3);
        for (int i = 0; i < 3; i++) {
            double angle = Math.PI * 2 * i / 3;
            Vec3 anchor = origin.add(forward.yRot((float) angle).scale(1.65));
            Vec3 node = surface(level, caster, anchor, .42, .68);
            if (node == null || !space(level, caster, node, .34, 1.1)
                    || !clear(level, caster, origin.add(0, .35, 0), node.add(0, .35, 0))) return null;
            nodes.add(node);
        }
        return new Placement(origin, forward, nodes);
    }

    private static Placement planBurrow(ServerLevel level, LivingEntity caster, Vec3 forward, double range) {
        if (!caster.onGround() || caster.isPassenger() || caster.isInWaterOrBubble()
                || active(BURROWS, caster) != null) return null;
        List<Vec3> path = new ArrayList<>();
        Vec3 start = caster.position();
        path.add(start);
        for (int step = 1; step <= range * 4; step++) {
            Vec3 previous = path.get(path.size() - 1);
            Vec3 offset = start.add(forward.scale(step * .25));
            Vec3 point = surface(level, caster, new Vec3(offset.x, previous.y, offset.z), .26, .37);
            if (point == null || !canMove(level, caster, previous, point, start)) break;
            path.add(point);
        }
        return path.size() >= 5 ? new Placement(start, forward, path) : null;
    }

    public static boolean spawn(ServerLevel level, LivingEntity caster, Kind kind, int rank, float damage,
                                Placement placement) {
        PureDarknessRiteEntity entity = create(level, caster, kind, rank, -1, placement.origin(), placement);
        entity.damage = damage;
        if (kind == Kind.MOTHWING_GUARD) entity.entityData.set(STATUS, kind.charges(rank));
        if (kind == Kind.ZEALOT_CHORUS) entity.entityData.set(STATUS, 7);
        if (kind == Kind.BURROWING_PRAYER) entity.lastBurrow = placement.origin();
        if (!level.addFreshEntity(entity)) return false;
        if (kind == Kind.ZEALOT_CHORUS) {
            for (int i = 0; i < 3; i++) {
                var child = create(level, caster, kind, rank, i, placement.points().get(i), placement);
                child.conductor = entity;
                child.integrity = kind.chanterHealth(rank);
                if (!level.addFreshEntity(child)) {
                    entity.chanters.forEach(Entity::discard);
                    entity.discard();
                    return false;
                }
                entity.chanters.add(child);
            }
        }
        if (kind == Kind.ZEALOT_CHORUS) RITUALS.put(caster, new WeakReference<>(entity));
        if (kind == Kind.MOTHWING_GUARD) {
            var previous = active(GUARDS, caster);
            if (previous != null) previous.beginEnd();
            GUARDS.put(caster, new WeakReference<>(entity));
        }
        if (kind == Kind.BURROWING_PRAYER) BURROWS.put(caster, new WeakReference<>(entity));
        switch (kind) {
            case THORN_GIBBET -> sound(level, placement.origin(), SoundEvents.ROOTS_BREAK, .7F, .65F);
            case ZEALOT_CHORUS -> sound(level, placement.origin(), ACSoundRegistry.UNDERZEALOT_CHANT.get(), .7F, .8F);
            case MOTHWING_GUARD -> sound(level, placement.origin(), ACSoundRegistry.GLOOMOTH_FLAP.get(), .8F, .75F);
            case BURROWING_PRAYER -> sound(level, placement.origin(), ACSoundRegistry.UNDERZEALOT_DIG.get(), .8F, .82F);
        }
        return true;
    }

    private static PureDarknessRiteEntity create(ServerLevel level, LivingEntity caster, Kind kind, int rank,
                                                int slot, Vec3 at, Placement placement) {
        var entity = new PureDarknessRiteEntity(ModEntities.PURE_DARKNESS_RITE.get(), level);
        entity.caster = caster;
        entity.placement = placement;
        entity.setPos(at);
        CompoundTag tag = new CompoundTag();
        tag.putInt("Owner", caster.getId());
        tag.putInt("Kind", kind.ordinal());
        tag.putInt("Rank", rank);
        tag.putInt("Slot", slot);
        tag.putLong("Start", level.getGameTime());
        tag.put("Forward", vector(placement.forward()));
        tag.put("Origin", vector(placement.origin()));
        ListTag points = new ListTag();
        if (kind != Kind.BURROWING_PRAYER) {
            for (Vec3 point : placement.points()) points.add(point == null ? new CompoundTag() : vector(point));
        }
        tag.put("Points", points);
        entity.entityData.set(LAYOUT, tag);
        return entity;
    }

    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        if (caster == null || !caster.isAlive() || caster.level() != server) {
            discardRite();
            return;
        }
        int age = (int) age(0);
        if (slot() >= 0) {
            if (conductor == null || conductor.isRemoved()) discard();
            return;
        }
        if (kind() == Kind.MOTHWING_GUARD) setPos(caster.position());
        if (ending() >= 0) {
            if (age - ending() >= 8) discardRite();
            return;
        }
        if (age >= kind().duration(rank())) {
            if (kind() == Kind.MOTHWING_GUARD) beginEnd();
            else discardRite();
            return;
        }
        switch (kind()) {
            case THORN_GIBBET -> tickGibbet(server, age);
            case ZEALOT_CHORUS -> tickChorus(server, age);
            case MOTHWING_GUARD -> { }
            case BURROWING_PRAYER -> tickBurrow(server, age);
        }
    }

    private void tickGibbet(ServerLevel level, int age) {
        if (age == 8) {
            emit(level, PureDarknessParticles.THORN_SPLINTER.get(), position().add(0, .4, 0), 10, .8);
            sound(level, position(), SoundEvents.WOOD_BREAK, .85F, .6F);
        }
        if (age == GIBBET_HOLD) {
            for (LivingEntity target : enemies(level, position(), 1.05, 1.35)) {
                if (largeTarget(target) || target instanceof Player
                        || !tryShortControl(level, target)) continue;
                held.add(target);
            }
        }
        if (age >= GIBBET_HOLD && age < GIBBET_HIT) {
            for (LivingEntity target : held) {
                if (!target.isAlive() || target.level() != level || target.distanceToSqr(position()) > 3) continue;
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 2, 6, false, false), caster);
                Vec3 motion = target.getDeltaMovement();
                target.setDeltaMovement(motion.x * .08, Math.min(motion.y, .025), motion.z * .08);
                if (target instanceof Mob mob) mob.getNavigation().stop();
            }
        }
        if (age == GIBBET_HIT) {
            for (LivingEntity target : enemies(level, position(), 1.04, 1.4)) {
                strike(target);
                if (largeTarget(target) || target instanceof Player) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 24, 0), caster);
                }
            }
            held.clear();
            emit(level, PureDarknessParticles.BLOOD_CUT.get(), position().add(0, .65, 0), 8, .43);
            sound(level, position(), SoundEvents.WOOD_HIT, .95F, .58F);
        }
    }

    private void tickChorus(ServerLevel level, int age) {
        int mask = 0;
        for (PureDarknessRiteEntity chanter : chanters) {
            if (!chanter.isAlive()) continue;
            if (surface(level, this, chanter.position(), .09, .12) == null) {
                chanter.breakRite(level);
                continue;
            }
            mask |= 1 << chanter.slot();
        }
        entityData.set(STATUS, mask);
        if (age <= CHORUS_BURST && Integer.bitCount(mask) < 2) {
            breakRite(level);
            return;
        }
        if (age >= chorusBeat(0) && age <= chorusBeat(2) && (age - chorusBeat(0)) % 6 == 0) {
            int index = (age - chorusBeat(0)) / 6;
            PureDarknessRiteEntity voice = chanters.get(index);
            if (!voice.isAlive()) {
                voice = chanters.stream().filter(Entity::isAlive).findFirst().orElse(null);
            }
            if (voice != null) {
                sound(level, voice.position(), ACSoundRegistry.UNDERZEALOT_CHANT.get(), .88F, .82F + index * .1F);
                emit(level, PureDarknessParticles.SCARLET_FILAMENT.get(), voice.position().add(0, .65, 0), 8, .27);
            }
        }
        if (age == CHORUS_BURST) {
            double radius = kind().chorusRadius(rank());
            Set<LivingEntity> targets = new LinkedHashSet<>();
            for (PureDarknessRiteEntity chanter : chanters) {
                if ((mask & (1 << chanter.slot())) == 0) continue;
                Vec3 center = chanter.position();
                targets.addAll(enemies(level, center, radius, CHORUS_HEIGHT));
                emit(level, PureDarknessParticles.BLOOD_MIST.get(), center.add(0, .35, 0), 9, .95);
                emit(level, PureDarknessParticles.SHADOW_SHARD.get(), center.add(0, .45, 0), 6, .68);
            }
            targets.forEach(this::strike);
            sound(level, position(), ACSoundRegistry.UNDERZEALOT_TRANSFORMATION.get(), 1, .8F);
        }
    }

    private void tickBurrow(ServerLevel level, int age) {
        if (age <= 0) return;
        if (caster.isPassenger() || caster.position().distanceToSqr(lastBurrow) > .8 * .8
                || caster.hurtTime > 0 && caster.getDeltaMovement().horizontalDistanceSqr() > .10) {
            finishBurrow(level);
            return;
        }
        List<Vec3> route = placement.points();
        int sample = Math.min(route.size() - 1, (int) Math.ceil((double) age * (route.size() - 1) / BURROW_TRAVEL_TICKS));
        Vec3 next = route.get(sample);
        Vec3 from = caster.position();
        if (!canMove(level, caster, from, next, from)) {
            finishBurrow(level);
            return;
        }
        if (caster instanceof ServerPlayer player) {
            player.connection.teleport(next.x, next.y, next.z, player.getYRot(), player.getXRot());
        } else {
            caster.move(MoverType.SELF, next.subtract(from));
        }
        lastBurrow = caster.position();
        setPos(lastBurrow);
        if (age % 3 == 0) {
            emit(level, PureDarknessParticles.SHADOW_SHARD.get(), lastBurrow.add(0, .11, 0), 4, .28);
            emit(level, PureDarknessParticles.SCARLET_FILAMENT.get(), lastBurrow.add(0, .12, 0), 3, .22);
        }
        if (age >= BURROW_TRAVEL_TICKS) finishBurrow(level);
    }

    private void finishBurrow(ServerLevel level) {
        emit(level, PureDarknessParticles.SOIL_CLAST.get(), caster.position().add(0, .13, 0), 8, .34);
        sound(level, caster.position(), ACSoundRegistry.CORRODENT_DIG_STOP.get(), .8F, .95F);
        discard();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void blockProjectile(ProjectileImpactEvent event) {
        if (event.getImpactResult() != ProjectileImpactEvent.ImpactResult.DEFAULT
                || !(event.getRayTraceResult() instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof LivingEntity target)
                || !(target.level() instanceof ServerLevel server)) return;
        Projectile shot = event.getProjectile();
        if (shot.isRemoved() || !(shot instanceof AbstractArrow || shot instanceof Snowball
                || shot instanceof ThrownEgg || shot instanceof LlamaSpit)) return;
        var guard = active(GUARDS, target);
        if (guard == null || guard.status() <= 0 || guard.age(0) < 4 || guard.age(0) >= guard.kind().duration(guard.rank())) return;
        Entity attacker = shot.getOwner();
        if (attacker == target || attacker instanceof LivingEntity living && !CandyCombat.enemy(target, living)) return;
        Vec3 motion = shot.getDeltaMovement();
        Vec3 front = Vec3.directionFromRotation(0, target.getYRot());
        if (motion.horizontalDistanceSqr() < .0025
                || new Vec3(-motion.x, 0, -motion.z).normalize().dot(front) < .5) return;
        event.setImpactResult(ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT_NO_DAMAGE);
        if (shot instanceof ThrownTrident) {
            // Preserve a thrown item so the opposing player can recover it.
            shot.setDeltaMovement(motion.reverse().scale(.12).add(0, .1, 0));
            event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
        } else {
            shot.discard();
        }
        Vec3 contact = target.getBoundingBox().inflate(.08)
                .clip(shot.position(), shot.position().add(motion)).orElse(shot.position());
        Vec3 right = new Vec3(front.z, 0, -front.x);
        int sign = contact.subtract(target.position()).dot(right) >= 0 ? 1 : -1;
        guard.entityData.set(IMPACT, sign * (1 + (int) guard.age(0)));
        guard.entityData.set(STATUS, guard.status() - 1);
        emit(server, PureDarknessParticles.MOTH_SCALE.get(), contact, 7, .29);
        sound(server, contact, ACSoundRegistry.GLOOMOTH_FLAP.get(), .8F, 1.24F);
        if (guard.status() == 0) guard.beginEnd();
    }

    @Override public boolean isPickable() {
        return ready() && !isRemoved() && ending() < 0 && slot() >= 0;
    }

    @Override public boolean isAttackable() { return isPickable(); }

    @Override public boolean hurt(DamageSource source, float amount) {
        if (!(level() instanceof ServerLevel server) || !isPickable() || !Float.isFinite(amount)
                || amount <= 0 || isInvulnerableTo(source)) return false;
        if (source.getEntity() instanceof LivingEntity attacker && caster != null
                && attacker != caster && !CandyCombat.enemy(attacker, caster)) return false;
        integrity -= amount;
        markHurt();
        if (integrity <= 0) breakRite(server);
        else {
            emit(server, PureDarknessParticles.SHADOW_SHARD.get(), position().add(0, .42, 0), 3, .22);
            sound(server, position(), SoundEvents.DEEPSLATE_HIT, .72F, .82F);
        }
        return true;
    }

    private void breakRite(ServerLevel level) {
        if (ending() >= 0) return;
        emit(level, PureDarknessParticles.SHADOW_SHARD.get(), position().add(0, .4, 0), 12, .42);
        sound(level, position(), ACSoundRegistry.UNDERZEALOT_HURT.get(), .92F, .73F);
        if (slot() >= 0) {
            discard();
        } else {
            beginEnd();
            chanters.stream().filter(Entity::isAlive).forEach(PureDarknessRiteEntity::beginEnd);
        }
    }

    private void beginEnd() { entityData.set(END, Math.max(0, (int) age(0))); }

    private void discardRite() {
        chanters.forEach(Entity::discard);
        discard();
    }

    static boolean tryShortControl(ServerLevel level, LivingEntity target) {
        long now = level.getGameTime();
        if (CONTROL_UNTIL.getOrDefault(target, Long.MIN_VALUE) > now) return false;
        CONTROL_UNTIL.put(target, now + 36);
        return true;
    }

    static boolean largeTarget(LivingEntity target) {
        return target.getType().is(BOSSES) || target.getType() == EntityType.WITHER
                || target.getType() == EntityType.ENDER_DRAGON || target.getBbWidth() > 1.5F;
    }

    private List<LivingEntity> enemies(ServerLevel level, Vec3 center, double radius, double height) {
        var bounds = new AABB(center.add(-radius, -.25, -radius), center.add(radius, height, radius));
        return level.getEntitiesOfClass(LivingEntity.class, bounds, target -> {
            Vec3 nearest = target.getBoundingBox().getCenter().subtract(center);
            double reach = radius + Math.min(.55, target.getBbWidth() * .5);
            return nearest.horizontalDistanceSqr() <= reach * reach && CandyCombat.enemy(caster, target)
                    && CandyCombat.visible(level, center.add(0, .55, 0), target);
        });
    }

    private void strike(LivingEntity target) {
        DamageSources.applyDamage(target, damage, ModSpells.pureDarknessRite(kind()).getDamageSource(this, caster));
    }

    private static Vec3 surface(ServerLevel level, Entity shape, Vec3 point, double up, double down) {
        if (!level.hasChunkAt(BlockPos.containing(point)) || !level.getWorldBorder().isWithinBounds(BlockPos.containing(point))) return null;
        BlockHitResult hit = ray(level, shape, point.add(0, up, 0), point.subtract(0, down, 0));
        return hit.getType() == HitResult.Type.BLOCK && hit.getDirection() == Direction.UP ? hit.getLocation() : null;
    }

    private static boolean canMove(ServerLevel level, LivingEntity caster, Vec3 from, Vec3 to, Vec3 base) {
        int steps = Math.max(1, (int) Math.ceil(from.distanceTo(to) * 8));
        AABB original = caster.getBoundingBox();
        for (int i = 1; i <= steps; i++) {
            Vec3 next = from.lerp(to, (double) i / steps);
            if (surface(level, caster, next, .18, .31) == null) return false;
            AABB moved = original.move(next.subtract(base)).deflate(.005);
            if (!level.hasChunksAt(BlockPos.containing(moved.minX, moved.minY, moved.minZ),
                    BlockPos.containing(moved.maxX, moved.maxY, moved.maxZ))
                    || !level.noCollision(caster, moved) || level.containsAnyLiquid(moved)) return false;
        }
        return true;
    }

    private static boolean space(ServerLevel level, Entity entity, Vec3 base, double radius, double height) {
        AABB bounds = new AABB(base.add(-radius, .025, -radius), base.add(radius, height, radius));
        return !level.getBlockCollisions(entity, bounds).iterator().hasNext() && !level.containsAnyLiquid(bounds);
    }

    private static boolean clear(Level level, Entity entity, Vec3 from, Vec3 to) {
        return ray(level, entity, from, to).getType() == HitResult.Type.MISS;
    }

    private static BlockHitResult ray(Level level, Entity entity, Vec3 from, Vec3 to) {
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
    }

    private static void emit(ServerLevel level, SimpleParticleType particle, Vec3 at, int count, double spread) {
        level.sendParticles(particle, at.x, at.y, at.z, count, spread, spread * .52, spread, .033);
    }

    private static void sound(ServerLevel level, Vec3 at, SoundEvent event, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, event, SoundSource.HOSTILE, volume, pitch);
    }

    private static CompoundTag vector(Vec3 vec) {
        var tag = new CompoundTag();
        tag.putDouble("X", vec.x);
        tag.putDouble("Y", vec.y);
        tag.putDouble("Z", vec.z);
        return tag;
    }

    private static Vec3 vector(CompoundTag tag) {
        return new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"));
    }
}

