package cn.wanyiwige.alexcavesspellsbook.entities;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.alexcavesspellsbook.candy.CandyCombat;
import cn.wanyiwige.alexcavesspellsbook.registry.ModEntities;
import cn.wanyiwige.alexcavesspellsbook.registry.ModSpells;
import cn.wanyiwige.alexcavesspellsbook.registry.PureDarknessParticles;
import cn.wanyiwige.alexcavesspellsbook.spells.PureDarknessFinaleSpell.Kind;
import com.github.alexmodguy.alexscaves.server.misc.ACSoundRegistry;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.util.Utils;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;

import java.lang.ref.WeakReference;
import java.util.*;

@Mod.EventBusSubscriber(modid = AlexCavesSpellsbook.MOD_ID)
public final class PureDarknessFinaleEntity extends Entity {
    public static final int BOW_MINIMUM = 8, BOW_FULL = 32, BOW_TIMEOUT = 48;
    public static final int HOLD = 0, RELEASE = 1, CANCEL = 2;
    private static final double WAVE_SPEED = 1.2;
    private static final int INPUT_TIMEOUT = 18;
    private static final Kind[] KINDS = Kind.values();
    private static final CastSource[] SOURCES = CastSource.values();
    private static final Map<LivingEntity, WeakReference<PureDarknessFinaleEntity>> PENDING = new WeakHashMap<>();
    private static final Map<LivingEntity, WeakReference<PureDarknessFinaleEntity>> CHANNELS = new WeakHashMap<>();
    private static final EntityDataAccessor<CompoundTag> LAYOUT =
            SynchedEntityData.defineId(PureDarknessFinaleEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Long> OPEN =
            SynchedEntityData.defineId(PureDarknessFinaleEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> END =
            SynchedEntityData.defineId(PureDarknessFinaleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ACTION =
            SynchedEntityData.defineId(PureDarknessFinaleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> TRAVEL =
            SynchedEntityData.defineId(PureDarknessFinaleEntity.class, EntityDataSerializers.FLOAT);

    public record BowShot(Vec3 origin, Vec3 target) {
        public Vec3 aim() { return target.subtract(origin).normalize(); }
    }

    public record Placement(Vec3 origin, Vec3 direction, List<BowShot> bows) {
        public Placement(Vec3 origin, Vec3 direction) { this(origin, direction, List.of()); }
    }
    private record Contact(LivingEntity target, Vec3 point, double distance) {}

    private LivingEntity caster;
    private float power;
    private float shotDamage;
    private float shotSpeed;
    private double shotRange;
    private boolean receivedHold;
    private long lastInput;
    private int releaseAge = -1;
    private final Set<UUID> waveHits = new HashSet<>();

    public PureDarknessFinaleEntity(EntityType<? extends PureDarknessFinaleEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override protected void defineSynchedData() {
        entityData.define(LAYOUT, new CompoundTag());
        entityData.define(OPEN, -1L);
        entityData.define(END, -1);
        entityData.define(ACTION, 0);
        entityData.define(TRAVEL, 0F);
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public boolean ready() { return entityData.get(LAYOUT).contains("Start"); }
    public Kind kind() { return KINDS[entityData.get(LAYOUT).getInt("Kind")]; }
    public CastSource source() { return SOURCES[entityData.get(LAYOUT).getInt("Source")]; }
    public int rank() { return entityData.get(LAYOUT).getInt("Rank"); }
    public boolean arrow() { return entityData.get(LAYOUT).getBoolean("Arrow"); }
    public boolean opened() { return entityData.get(OPEN) >= 0; }
    public int ending() { return entityData.get(END); }
    public int action() { return entityData.get(ACTION); }
    public float travelled() { return entityData.get(TRAVEL); }

    public List<BowShot> bows() {
        ListTag layout = entityData.get(LAYOUT).getList("Bows", Tag.TAG_COMPOUND);
        List<BowShot> shots = new ArrayList<>(layout.size());
        for (int i = 0; i < layout.size(); i++) {
            CompoundTag bow = layout.getCompound(i);
            shots.add(new BowShot(new Vec3(bow.getDouble("X"), bow.getDouble("Y"), bow.getDouble("Z")),
                    new Vec3(bow.getDouble("Tx"), bow.getDouble("Ty"), bow.getDouble("Tz"))));
        }
        return shots;
    }
    public Vec3 aim() {
        CompoundTag tag = entityData.get(LAYOUT);
        return new Vec3(tag.getDouble("Dx"), tag.getDouble("Dy"), tag.getDouble("Dz"));
    }
    public float age(float partial) {
        return level().getGameTime() - entityData.get(LAYOUT).getLong("Start") + partial;
    }
    public float phaseAge(float partial) {
        return opened() ? level().getGameTime() - entityData.get(OPEN) + partial : 0;
    }
    public LivingEntity owner() {
        if (caster != null) return caster;
        return level().getEntity(entityData.get(LAYOUT).getInt("Owner")) instanceof LivingEntity living ? living : null;
    }

    public static Placement plan(ServerLevel level, LivingEntity caster, Kind kind, int rank) {
        Vec3 eyes = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        Vec3 flat = Vec3.directionFromRotation(0, caster.getYRot());
        if (kind == Kind.DARKBORN_RECOVERY) return dark(level, caster) ? new Placement(caster.position(), flat) : null;
        if (kind == Kind.HOLLOWS_SCREECH) {
            Vec3 mouth = eyes.add(flat.scale(.65));
            if (!clear(level, caster, eyes, mouth)) mouth = eyes;
            return new Placement(mouth, look);
        }
        return planBows(level, caster, kind, rank, eyes, look, flat);
    }

    private static Placement planBows(ServerLevel level, LivingEntity caster, Kind kind, int rank,
                                      Vec3 eyes, Vec3 look, Vec3 forward) {
        double range = kind.range(rank);
        BlockHitResult sight = ray(level, caster, eyes, eyes.add(look.scale(range)));
        Vec3 center = sight.getType() == HitResult.Type.BLOCK && sight.getDirection() == Direction.UP
                ? sight.getLocation() : surface(level, caster, sight.getLocation(), .1, 4);
        if (center == null) return null;
        Vec3 target = center.add(0, .04, 0);
        Vec3 toward = new Vec3(center.x - eyes.x, 0, center.z - eyes.z).normalize();
        if (toward.horizontalDistanceSqr() < .01) toward = forward;
        Vec3 right = new Vec3(toward.z, 0, -toward.x);
        int count = kind.bowCount(rank);
        List<BowShot> shots = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double lane = i == 0 ? 0 : ((i + 1) / 2) * (i % 2 == 0 ? -1 : 1);
            double angle = lane * 1.05;
            Vec3 at = eyes.add(toward.scale(Math.cos(angle) * 1.25))
                    .add(right.scale(Math.sin(angle) * 2.0))
                    .add(0, Mth.clamp(center.y - eyes.y + 1.3, .75, 1.35) + .16 * Math.abs(lane), 0);
            Vec3 offset = target.add(right.scale(-lane * .85));
            Vec3 impact = lane == 0 ? center : surface(level, caster, offset, 1.1, 2.2);
            if (impact == null) continue;
            Vec3 end = impact.add(0, .04, 0);
            if (at.y - end.y < .6 || !space(level, caster, at, .26, .32)
                    || !clear(level, caster, eyes, at) || !clear(level, caster, at, end)) continue;
            shots.add(new BowShot(at, end));
        }
        return shots.size() >= 2 ? new Placement(caster.position(), toward, shots) : null;
    }

    public static PureDarknessFinaleEntity prepare(ServerLevel level, LivingEntity caster, Kind kind, int rank,
                                                   float power, CastSource source, Placement placement) {
        var active = find(CHANNELS, caster);
        if (active != null) active.interrupt();
        var previous = find(PENDING, caster);
        if (previous != null) previous.finish();
        var field = create(level, caster, kind, rank, source, placement, false);
        field.power = power;
        if (!level.addFreshEntity(field)) return null;
        PENDING.put(caster, new WeakReference<>(field));
        return field;
    }

    private static PureDarknessFinaleEntity create(ServerLevel level, LivingEntity caster, Kind kind, int rank,
                                                    CastSource source, Placement placement, boolean arrow) {
        var field = new PureDarknessFinaleEntity(ModEntities.PURE_DARKNESS_FINALE.get(), level);
        field.caster = caster;
        field.lastInput = level.getGameTime();
        field.setPos(placement.origin());
        CompoundTag tag = new CompoundTag();
        tag.putLong("Start", level.getGameTime());
        tag.putInt("Owner", caster.getId());
        tag.putInt("Kind", kind.ordinal());
        tag.putInt("Rank", rank);
        tag.putInt("Source", source.ordinal());
        tag.putDouble("Dx", placement.direction().x);
        tag.putDouble("Dy", placement.direction().y);
        tag.putDouble("Dz", placement.direction().z);
        tag.putBoolean("Arrow", arrow);
        if (!placement.bows().isEmpty()) {
            ListTag bows = new ListTag();
            for (BowShot shot : placement.bows()) {
                CompoundTag bow = new CompoundTag();
                bow.putDouble("X", shot.origin().x);
                bow.putDouble("Y", shot.origin().y);
                bow.putDouble("Z", shot.origin().z);
                bow.putDouble("Tx", shot.target().x);
                bow.putDouble("Ty", shot.target().y);
                bow.putDouble("Tz", shot.target().z);
                bows.add(bow);
            }
            tag.put("Bows", bows);
        }
        field.entityData.set(LAYOUT, tag);
        return field;
    }

    public static PureDarknessFinaleEntity pending(LivingEntity caster, Kind kind) {
        var field = find(PENDING, caster);
        return field != null && field.kind() == kind && !field.opened() ? field : null;
    }

    private static PureDarknessFinaleEntity find(Map<LivingEntity, WeakReference<PureDarknessFinaleEntity>> map,
                                                   LivingEntity caster) {
        var reference = map.get(caster);
        var field = reference == null ? null : reference.get();
        return field != null && field.isAlive() && field.ending() < 0 && field.level() == caster.level() ? field : null;
    }

    public boolean canOpen() {
        if (!(level() instanceof ServerLevel server) || ending() >= 0) return false;
        if (!validChannel(server)) {
            finish();
            return false;
        }
        return true;
    }

    public void open() {
        if (!(level() instanceof ServerLevel server)) return;
        if (kind() == Kind.HOLLOWS_SCREECH) {
            Placement launch = plan(server, caster, kind(), rank());
            setPos(launch.origin());
            CompoundTag layout = entityData.get(LAYOUT).copy();
            layout.putDouble("Dx", launch.direction().x);
            layout.putDouble("Dy", launch.direction().y);
            layout.putDouble("Dz", launch.direction().z);
            entityData.set(LAYOUT, layout);
        }
        entityData.set(OPEN, server.getGameTime());
        PENDING.remove(caster);
        if (kind().held()) CHANNELS.put(caster, new WeakReference<>(this));
        if (kind() == Kind.HOLLOWS_SCREECH) {
            sound(server, position(), ACSoundRegistry.FORSAKEN_SCREECH.get(), .9F, .8F);
        }
    }

    public static void input(ServerPlayer player, int entityId, UUID uuid, int signal) {
        if (!(player.level().getEntity(entityId) instanceof PureDarknessFinaleEntity field)
                || !uuid.equals(field.getUUID()) || field.caster != player || !field.kind().held()
                || field.arrow() || field.ending() >= 0) return;
        if (signal == HOLD) {
            if (field.releaseAge >= 0) return;
            field.receivedHold = true;
            field.lastInput = field.level().getGameTime();
        } else if (signal == CANCEL || field.kind() != Kind.DREADBOW_GALLOWS) {
            field.interrupt();
        } else if (signal == RELEASE) {
            int age = (int) field.age(0);
            if (!field.receivedHold || age < BOW_MINIMUM || age >= BOW_TIMEOUT) field.interrupt();
            else if (field.releaseAge < 0) field.releaseAge = age;
        }
    }

    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        if (caster == null || !caster.isAlive() || caster.level() != server
                || caster instanceof ServerPlayer player && player.hasDisconnected()) {
            discard();
            return;
        }
        if (arrow()) {
            tickArrow(server);
            return;
        }
        if (kind() == Kind.DARKBORN_RECOVERY) setPos(caster.position());
        int age = (int) age(0);
        if (ending() >= 0) {
            if (age - ending() >= 8) discard();
            return;
        }
        if (kind().held() && (!validChannel(server)
                || releaseAge < 0 && server.getGameTime() - lastInput > INPUT_TIMEOUT)) {
            interrupt();
            return;
        }
        if (kind() == Kind.DREADBOW_GALLOWS && age >= BOW_TIMEOUT) {
            interrupt();
            return;
        }
        if (!opened()) {
            if (age > 100) finish();
            return;
        }
        int phase = (int) phaseAge(0);
        if (phase > kind().duration()) {
            finish();
            return;
        }
        switch (kind()) {
            case HOLLOWS_SCREECH -> tickScreech(server, phase);
            case DREADBOW_GALLOWS -> {
                if (releaseAge >= BOW_MINIMUM) releaseBow(server);
            }
            case DARKBORN_RECOVERY -> tickRecovery(server, phase);
        }
    }

    private boolean validChannel(ServerLevel server) {
        if (kind().held() && caster.isSpectator()) return false;
        if (opened() && kind().held() && caster instanceof ServerPlayer player) {
            MagicData magic = MagicData.getPlayerMagicData(player);
            if (magic.isCasting() && !ModSpells.pureDarknessFinale(kind()).getSpellId().equals(magic.getCastingSpellId())) return false;
        }
        return switch (kind()) {
            case DARKBORN_RECOVERY -> dark(server, caster);
            case DREADBOW_GALLOWS -> caster.position().distanceToSqr(position()) < 9 * 9
                    && bows().stream().filter(bow -> bowAvailable(server, bow)).limit(2).count() >= 2;
            default -> true;
        };
    }

    private void tickScreech(ServerLevel server, int age) {
        double travelled = travelled();
        double reach = kind().range(rank());
        if (age < 1 || travelled >= reach || action() != 0) return;
        Vec3 from = position().add(aim().scale(travelled));
        Vec3 end = position().add(aim().scale(Math.min(reach, travelled + WAVE_SPEED)));
        BlockHitResult wall = ray(server, this, from, end);
        Vec3 to = wall.getLocation();
        for (Contact hit : swept(server, from, to, .77)) {
            if (!waveHits.add(hit.target().getUUID())) continue;
            float damage = (float) (kind().damage(rank()) * power * Math.pow(.75, waveHits.size() - 1));
            strike(hit.target(), damage);
            emit(server, PureDarknessParticles.SONIC_WAVEFRONT.get(), hit.point(), 3, .24);
            if (waveHits.size() >= 4) {
                entityData.set(ACTION, 1);
                return;
            }
        }
        if (age % 2 == 0) emit(server, PureDarknessParticles.SONIC_WAVEFRONT.get(), to, 3, .25);
        if (wall.getType() != HitResult.Type.MISS) {
            entityData.set(TRAVEL, (float) position().distanceTo(to));
            emit(server, PureDarknessParticles.SHADOW_SHARD.get(), to, 6, .27);
            entityData.set(ACTION, 1);
        } else {
            entityData.set(TRAVEL, (float) position().distanceTo(to));
        }
    }

    private boolean bowAvailable(ServerLevel server, BowShot bow) {
        return space(server, this, bow.origin(), .26, .32)
                && clear(server, caster, caster.getEyePosition(), bow.origin())
                && clear(server, this, bow.origin(), bow.target());
    }

    private void releaseBow(ServerLevel server) {
        float draw = Mth.clamp((releaseAge - BOW_MINIMUM) / (float) (BOW_FULL - BOW_MINIMUM), 0, 1);
        for (BowShot bow : bows()) {
            if (!bowAvailable(server, bow)) continue;
            var shot = create(server, caster, kind(), rank(), source(), new Placement(bow.origin(), bow.aim()), true);
            shot.shotDamage = kind().damage(rank()) * power * Mth.lerp(draw, .55F, 1F);
            shot.shotSpeed = Mth.lerp(draw, 1.7F, 2.25F);
            shot.shotRange = bow.origin().distanceTo(bow.target()) + .4;
            if (server.addFreshEntity(shot)) {
                entityData.set(ACTION, 1);
                sound(server, bow.origin(), ACSoundRegistry.DREADBOW_RELEASE.get(), .65F, .84F);
                emit(server, PureDarknessParticles.SCARLET_FILAMENT.get(), bow.origin(), 5, .27);
            }
        }
        finish();
    }

    private void tickArrow(ServerLevel server) {
        double remaining = shotRange - travelled();
        if (remaining <= 1.0E-4) {
            discard();
            return;
        }
        Vec3 from = position();
        Vec3 end = from.add(aim().scale(Math.min(remaining, shotSpeed)));
        BlockHitResult wall = ray(server, this, from, end);
        Vec3 to = wall.getLocation();
        List<Contact> hits = swept(server, from, to, .2);
        if (!hits.isEmpty()) {
            Contact hit = hits.get(0);
            strike(hit.target(), shotDamage);
            burstArrow(server, hit.point());
            return;
        }
        if (wall.getType() != HitResult.Type.MISS) {
            burstArrow(server, to);
            return;
        }
        setPos(to);
        entityData.set(TRAVEL, travelled() + (float) from.distanceTo(to));
        emit(server, PureDarknessParticles.DREAD_ARROW_TAIL.get(), from.lerp(to, .55), 3, .073);
    }

    private void burstArrow(ServerLevel server, Vec3 at) {
        emit(server, PureDarknessParticles.SHADOW_SHARD.get(), at, 8, .29);
        emit(server, PureDarknessParticles.BLOOD_CUT.get(), at, 4, .2);
        sound(server, at, SoundEvents.ARROW_HIT, .86F, .69F);
        discard();
    }

    private void tickRecovery(ServerLevel server, int age) {
        if (age <= 0 || age % 20 != 0 || !receivedHold) return;
        caster.heal(kind().healing(rank()));
        emit(server, PureDarknessParticles.BLOOD_MIST.get(), position().add(0, .91, 0), 5, .29);
        emit(server, PureDarknessParticles.SCARLET_FILAMENT.get(), position().add(0, 1.1, 0), 6, .24);
        sound(server, position(), SoundEvents.SOUL_ESCAPE, .65F, .9F + .003F * age);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel) || event.getAmount() <= 0) return;
        var preparation = find(PENDING, victim);
        if (preparation != null) preparation.interrupt();
        var channel = find(CHANNELS, victim);
        if (channel != null) channel.interrupt();
    }

    public void interrupt() {
        if (ending() >= 0 || !(level() instanceof ServerLevel server)) return;
        finish();
        emit(server, PureDarknessParticles.SHADOW_SHARD.get(), position().add(0, .28, 0), 6, .32);
        if (!opened() && caster instanceof ServerPlayer player) {
            MagicData data = MagicData.getPlayerMagicData(player);
            if (data.isCasting() && ModSpells.pureDarknessFinale(kind()).getSpellId().equals(data.getCastingSpellId())) {
                Utils.serverSideCancelCast(player);
            }
        }
    }

    private void finish() { if (ending() < 0) entityData.set(END, Math.max(0, (int) age(0))); }

    private List<Contact> swept(ServerLevel server, Vec3 from, Vec3 to, double width) {
        if (from.distanceToSqr(to) < 1E-8) return List.of();
        List<Contact> hits = new ArrayList<>();
        for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(width),
                living -> CandyCombat.enemy(caster, living))) {
            AABB box = target.getBoundingBox().inflate(width);
            Vec3 point = box.contains(from) ? from : box.clip(from, to).orElse(null);
            if (point != null && CandyCombat.visible(server, point, target)) {
                hits.add(new Contact(target, point, from.distanceToSqr(point)));
            }
        }
        hits.sort(Comparator.comparingDouble(Contact::distance).thenComparing(hit -> hit.target().getUUID()));
        return hits;
    }

    private void strike(LivingEntity target, float damage) {
        DamageSources.applyDamage(target, damage, ModSpells.pureDarknessFinale(kind()).getDamageSource(this, caster));
    }

    private static boolean dark(ServerLevel level, LivingEntity caster) {
        BlockPos feet = caster.blockPosition(), eyes = BlockPos.containing(caster.getEyePosition());
        return !level.canSeeSky(eyes) && level.getBrightness(LightLayer.BLOCK, feet) <= 3
                && level.getBrightness(LightLayer.BLOCK, eyes) <= 3;
    }

    private static Vec3 surface(ServerLevel level, Entity entity, Vec3 at, double up, double down) {
        BlockPos block = BlockPos.containing(at);
        if (!level.hasChunkAt(block) || !level.getWorldBorder().isWithinBounds(block)) return null;
        BlockHitResult ground = ray(level, entity, at.add(0, up, 0), at.subtract(0, down, 0));
        return ground.getType() == HitResult.Type.BLOCK && ground.getDirection() == Direction.UP ? ground.getLocation() : null;
    }

    private static boolean space(ServerLevel level, Entity entity, Vec3 center, double radius, double height) {
        if (!level.hasChunkAt(BlockPos.containing(center))) return false;
        AABB bounds = new AABB(center, center).inflate(radius, height, radius);
        return !level.getBlockCollisions(entity, bounds).iterator().hasNext() && !level.containsAnyLiquid(bounds);
    }

    private static boolean clear(Level level, Entity entity, Vec3 from, Vec3 to) {
        return ray(level, entity, from, to).getType() == HitResult.Type.MISS;
    }

    private static BlockHitResult ray(Level level, Entity entity, Vec3 from, Vec3 to) {
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
    }

    private static void emit(ServerLevel level, SimpleParticleType particle, Vec3 at, int count, double spread) {
        level.sendParticles(particle, at.x, at.y, at.z, count, spread, spread * .55, spread, .032);
    }

    private static void sound(ServerLevel level, Vec3 at, SoundEvent event, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, event, SoundSource.HOSTILE, volume, pitch);
    }
}

