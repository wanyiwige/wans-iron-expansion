package cn.wanyiwige.alexcavesspellsbook.candy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;
import java.util.UUID;

/** Common staged effect host. Specific spell timelines are installed in steps 7-32. */
public class CandyEffectEntity extends Entity {
    public enum Mode { BALL, MINT, GLYPH, GROUND, ROCKET, PHANTOM, SUMMON, ANCHOR }
    public enum Stage { START, ACTIVE, IMPACT, END }
    private static final EntityDataAccessor<CompoundTag> STATE = SynchedEntityData.defineId(CandyEffectEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private CandySession session;
    private UUID parent;
    private CandySpecs.Spec spec = CandySpecs.ALL.get(0);
    private int age, activeTicks = 60, startupTicks = 4, remainingBounces = 3;
    private float power = 1;
    private int spellLevel = 1;
    private int impactAge = -1, endAge = -1;
    private boolean pendingEnd;
    public CandyEffectEntity(EntityType<?> type, Level level) { super(type, level); setNoGravity(true); }
    public void configure(CandySpecs.Spec spec, int spellLevel, float power, Mode mode, Vec3 anchor, Vec3 second) {
        this.spec = spec; this.spellLevel = spec.clampLevel(spellLevel);
        this.power = Float.isFinite(power) ? Math.max(0, Math.min(100, power)) : 1;
        activeTicks = spec.activeTicks();
        CompoundTag s = new CompoundTag(); s.putString("Spell", spec.id()); s.putInt("Mode", mode.ordinal());
        s.putInt("Stage", Stage.START.ordinal()); s.putInt("Age", 0); s.putInt("Color", 0);
        s.putFloat("Radius", (float)spec.radius()); s.putFloat("AngularSpeed", 0.18F); s.putFloat("Phase", 0);
        if(spec.id().equals("caramel_bog")) {
            s.putFloat("CaramelMaxRadius",(float)spec.radius());
            s.putFloat("Radius",(float)CandyFirstRules.caramelRadius(spec.radius(),0));
        }
        s.putInt("ActiveTicks", activeTicks); s.putInt("Level", this.spellLevel);
        putVector(s, "Anchor", anchor); putVector(s, "Second", second); entityData.set(STATE, s);
    }
    public void orbit(float radius, float speed, float phase) {
        if (!Float.isFinite(radius) || !Float.isFinite(speed) || !Float.isFinite(phase)) return;
        CompoundTag s = visual().copy(); s.putFloat("Radius", Math.max(0.2F, Math.min(8, radius)));
        s.putFloat("AngularSpeed", Math.max(-0.5F, Math.min(0.5F, speed))); s.putFloat("Phase", phase); entityData.set(STATE, s);
    }
    public void color(int index) { CompoundTag s = visual().copy(); s.putInt("Color", Math.floorMod(index, 12)); entityData.set(STATE, s); }
    public CompoundTag visual() { return entityData.get(STATE); }
    public Mode mode() { return Mode.values()[Math.max(0, Math.min(7, visual().getInt("Mode")))]; }
    public Stage stage() { return Stage.values()[Math.max(0, Math.min(3, visual().getInt("Stage")))]; }
    public float renderAge(float partial) { return visual().getInt("Age") + partial; }
    public CandySpecs.Spec spec() { return spec; }
    public float power() { return power; }
    public int spellLevel() { return spellLevel; }
    public CandySession session() { return session; }
    public CandyHitLedger ledger() { if (session == null) throw new IllegalStateException("Effect has no cast session"); return session.ledger(); }
    public LivingEntity owner() {
        if (session == null || !(level() instanceof ServerLevel server)) return null;
        Entity entity = server.getEntity(session.ownerId());
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }
    void attach(CandySession session, CandyEffectEntity parent) {
        this.session = session; this.parent = parent == null ? null : parent.getUUID();
        CompoundTag s = visual().copy(); s.putUUID("Owner", session.ownerId());
        if (this.parent != null) s.putUUID("Parent", this.parent);
        entityData.set(STATE, s);
    }
    /** Children inherit immutable damage/ownership limits; the same ledger spans the full family. */
    public CandyEffectEntity child(Mode mode, Vec3 position, Vec3 velocity) {
        if (!(level() instanceof ServerLevel) || session == null || session.ended()) return null;
        CandyEffectEntity child = CandyRegistry.EFFECT.get().create(level());
        if (child == null) return null;
        child.configure(spec, spellLevel, power, mode, anchor("Anchor"), anchor("Second"));
        child.setPos(position); child.setDeltaMovement(velocity); child.color(visual().getInt("Color"));
        return session.spawn(child, this) ? child : null;
    }
    public void impact() { if (level().isClientSide || endAge >= 0 || impactAge==age) return; impactAge = age; setStage(Stage.IMPACT); }
    public void finish() {
        if (level().isClientSide || endAge >= 0) return;
        if(stage()==Stage.IMPACT&&age-impactAge<2){pendingEnd=true;setDeltaMovement(Vec3.ZERO);return;}
        pendingEnd=false;endAge=age;setStage(Stage.END);
    }
    private void setStage(Stage stage) {
        CompoundTag s = visual().copy(); s.putInt("Stage", stage.ordinal()); s.putInt("StageAge", age); entityData.set(STATE, s);
        if (level() instanceof ServerLevel server) CandySounds.stage(server, this, stage);
    }
    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        LivingEntity owner = owner();
        if (owner == null || session.ended() || (parent != null && server.getEntity(parent) == null)) { discard(); return; }
        if (parent == null) session.prune();
        if (isRemoved() || session.ended()) return;
        age++; CompoundTag s = visual().copy(); s.putInt("Age", age); entityData.set(STATE, s);
        if (age == 1) CandySounds.stage(server, this, Stage.START);
        if (endAge >= 0) { if (age-endAge >= 8) discard(); return; }
        if(pendingEnd){if(age-impactAge>=2)finish();return;}
        if (age >= startupTicks + activeTicks) { finish(); return; }
        if (age < startupTicks) return;
        if (stage() == Stage.START || (stage() == Stage.IMPACT && age-impactAge >= 2)) setStage(Stage.ACTIVE);
        motion(server, owner);
        if(endAge>=0||isRemoved())return;
        serverTimeline(server, owner, age-startupTicks);
    }
    /** Override in dedicated hosts or call shared motion/hit APIs from subsequent spell implementations. */
    protected void serverTimeline(ServerLevel level, LivingEntity owner, int activeAge) { }
    protected void motion(ServerLevel level, LivingEntity owner) {
        if (mode() == Mode.MINT) {
            float r = visual().getFloat("Radius"), angular = visual().getFloat("AngularSpeed");
            double phase = visual().getFloat("Phase") + age * angular;
            Vec3 center = owner.position().add(0, owner.getBbHeight()*0.6, 0);
            Vec3 next = center.add(Math.cos(phase)*r, 0, Math.sin(phase)*r);
            BlockHitResult clip = level.clip(new ClipContext(center, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            setPos(clip.getLocation());
        } else if (mode() == Mode.BALL || mode() == Mode.ROCKET || mode() == Mode.PHANTOM || mode() == Mode.GLYPH) {
            Vec3 from = position(), velocity = getDeltaMovement(), to = from.add(velocity);
            BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (hit.getType() != HitResult.Type.MISS) {
                setPos(hit.getLocation().subtract(velocity.normalize().scale(0.02)));
                if (mode() == Mode.BALL && remainingBounces-- > 0) {
                    Vec3 n = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
                    setDeltaMovement(velocity.subtract(n.scale(2*velocity.dot(n))));
                    CandySounds.bounce(level, position()); impact();
                } else { setDeltaMovement(Vec3.ZERO); impact(); finish(); }
            } else setPos(to);
        }
    }
    public Vec3 anchor(String name) { CompoundTag s=visual(); return new Vec3(s.getDouble(name+"X"), s.getDouble(name+"Y"), s.getDouble(name+"Z")); }
    public void visualValue(String key,int value) { CompoundTag s=visual().copy();s.putInt(key,value);entityData.set(STATE,s); }
    public void visualValue(String key,float value) { CompoundTag s=visual().copy();s.putFloat(key,value);entityData.set(STATE,s); }
    protected void visualSnapshot(CompoundTag value) { entityData.set(STATE,value); }
    private static void putVector(CompoundTag s, String name, Vec3 v) { s.putDouble(name+"X",v.x); s.putDouble(name+"Y",v.y); s.putDouble(name+"Z",v.z); }
    @Override protected void defineSynchedData() { entityData.define(STATE, new CompoundTag()); }
    @Override public void remove(RemovalReason reason) { super.remove(reason); if (session != null && !level().isClientSide) session.removed(getUUID()); }
    // Short-lived controllers are registered noSave; refuse recovery without an active server session.
    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
