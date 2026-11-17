package cn.wanyiwige.alexcavesspellsbook.candy;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Server-only cast state, one ledger and bounded family per cast. */
@Mod.EventBusSubscriber(modid = AlexCavesSpellsbook.MOD_ID)
public final class CandySession {
    private static final Map<ServerLevel, Set<CandySession>> LIVE = new HashMap<>();
    private final ServerLevel level;
    private final UUID owner;
    private final CandySpecs.Spec spec;
    private final CandyHitLedger ledger = new CandyHitLedger();
    private final Map<UUID, UUID> family = new HashMap<>();
    private final Map<UUID, Entity> members = new HashMap<>();
    private boolean ended;
    public CandySession(ServerLevel level, LivingEntity owner, CandySpecs.Spec spec) {
        this.level = level; this.owner = owner.getUUID(); this.spec = spec;
    }
    public CandyHitLedger ledger() { return ledger; }
    public UUID ownerId() { return owner; }
    public boolean ended() { return ended; }
    public static boolean canStart(ServerLevel level,LivingEntity owner,int required) {
        Set<CandySession> sessions=LIVE.getOrDefault(level,Set.of());
        int total=sessions.stream().mapToInt(s->s.family.size()).sum();
        int owned=sessions.stream().filter(s->s.owner.equals(owner.getUUID())).mapToInt(s->s.family.size()).sum();
        return total+required<=CandyConfig.DIMENSION_BUDGET.get()&&owned+required<=CandyConfig.OWNER_BUDGET.get();
    }
    /** Reserve before addFreshEntity. Null parent is the single root. */
    public boolean reserve(Entity entity, UUID parent) {
        if (ended || entity.level() != level || family.containsKey(entity.getUUID())
                || (parent != null && !family.containsKey(parent)) || (parent == null && !family.isEmpty())) return false;
        Set<CandySession> sessions = LIVE.computeIfAbsent(level, k -> new HashSet<>());
        int total = sessions.stream().mapToInt(s -> s.family.size()).sum();
        int owned = sessions.stream().filter(s -> s.owner.equals(owner)).mapToInt(s -> s.family.size()).sum();
        if (family.size() >= Math.min(12, spec.entityBudget()) || owned >= CandyConfig.OWNER_BUDGET.get()
                || total >= CandyConfig.DIMENSION_BUDGET.get()) return false;
        family.put(entity.getUUID(), parent); members.put(entity.getUUID(), entity); sessions.add(this); return true;
    }
    public boolean spawn(CandyEffectEntity entity, CandyEffectEntity parent) {
        if (!entity.spec().id().equals(spec.id())) return false;
        if (parent != null && parent.session() != this) return false;
        if (!reserve(entity, parent == null ? null : parent.getUUID())) return false;
        entity.attach(this, parent);
        if (!level.addFreshEntity(entity)) { removed(entity.getUUID()); return false; }
        return true;
    }
    /** Only dedicated summon implementations are accepted; ordinary cave mobs have no owner AI. */
    public boolean summon(LivingEntity summon, CandyEffectEntity parent) {
        Entity ownerEntity = level.getEntity(owner);
        if (!(summon instanceof IMagicSummon) || summon.getType().canSerialize() || !(ownerEntity instanceof LivingEntity)
                || parent.session() != this || !reserve(summon, parent.getUUID())) return false;
        SummonManager.setOwner(summon, ownerEntity);
        SummonManager.setDuration(summon, spec.activeTicks());
        if (!level.addFreshEntity(summon)) { SummonManager.removeSummon(summon); SummonManager.stopTrackingExpiration(summon); removed(summon.getUUID()); return false; }
        return true;
    }
    public void removed(UUID id) {
        if (!family.containsKey(id)) return;
        List<UUID> children = family.entrySet().stream().filter(e -> id.equals(e.getValue())).map(Map.Entry::getKey).toList();
        boolean root = family.get(id) == null;
        family.remove(id);
        Entity ownMember = members.remove(id);
        if (ownMember instanceof IMagicSummon) { SummonManager.removeSummon(ownMember); SummonManager.stopTrackingExpiration(ownMember); }
        for (UUID child : children) {
            Entity entity = members.get(child);
            if (entity != null) {
                if (entity instanceof IMagicSummon) { SummonManager.removeSummon(entity); SummonManager.stopTrackingExpiration(entity); }
                entity.discard();
            }
            removed(child);
        }
        if (root || family.isEmpty()) { ended = true; Set<CandySession> sessions = LIVE.get(level);
            if (sessions != null) { sessions.remove(this); if (sessions.isEmpty()) LIVE.remove(level); } }
    }
    public void cancel() {
        ended = true;
        for (UUID id : List.copyOf(family.keySet())) {
            Entity entity = members.get(id);
            if (entity != null) { if (entity instanceof IMagicSummon) { SummonManager.removeSummon(entity); SummonManager.stopTrackingExpiration(entity); } entity.discard(); }
        }
        family.clear(); members.clear(); Set<CandySession> sessions = LIVE.get(level);
        if (sessions != null) { sessions.remove(this); if (sessions.isEmpty()) LIVE.remove(level); }
    }
    /** Called from the owning root every server tick; cleans removed/unloaded summoned children. */
    public void prune() {
        for (UUID id : List.copyOf(family.keySet())) {
            Entity e = level.getEntity(id);
            if (e == null || e.isRemoved() || !e.isAlive()) removed(id);
        }
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel server) {
            Set<CandySession> sessions = LIVE.get(server);
            if (sessions != null) for (CandySession s : List.copyOf(sessions)) s.cancel();
            LIVE.remove(server);
        }
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { LIVE.clear(); }
}
