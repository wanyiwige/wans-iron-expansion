package cn.wanyiwige.alexcavesspellsbook.spells;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.alexcavesspellsbook.entities.PureDarknessFinaleEntity;
import cn.wanyiwige.alexcavesspellsbook.registry.ModSchools;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

import static io.redspace.ironsspellbooks.api.spells.SpellRarity.*;

public final class PureDarknessFinaleSpell extends PureDarknessSpell {
    public enum Kind {
        HOLLOWS_SCREECH("hollows_screech", 42, 5, 11, 20, 12, 1, 24, 30, 6, 10, RARE),
        DREADBOW_GALLOWS("dreadbow_gallows", 38, 5, 12, 8, 16, 1, 48, 54, 6, 8, RARE),
        DARKBORN_RECOVERY("darkborn_recovery", 30, 5, 26, 8, 0, 0, 80, 0, 0, 5, UNCOMMON);

        public final String path;
        private final int mana, manaStep, cooldown, castTicks, duration;
        private final double range, rangeStep;
        private final float damage, damageStep;
        private final int maxLevel;
        private final SpellRarity minRarity;

        Kind(String slug, int mana, int manaStep, int cooldown, int castTicks, double range, double rangeStep,
             int duration, float damage, float damageStep, int maxLevel, SpellRarity minRarity) {
            path = "pure_darkness_" + slug;
            this.mana = mana;
            this.manaStep = manaStep;
            this.cooldown = cooldown;
            this.castTicks = castTicks;
            this.range = range;
            this.rangeStep = rangeStep;
            this.duration = duration;
            this.damage = damage;
            this.damageStep = damageStep;
            this.maxLevel = maxLevel;
            this.minRarity = minRarity;
        }

        private int index(int rank) { return Mth.clamp(rank, 1, maxLevel) - 1; }
        public double range(int rank) { return range + index(rank) * rangeStep; }
        public int duration() { return duration; }
        public float damage(int rank) { return damage + index(rank) * damageStep; }
        public int bowCount(int rank) { return 3 + index(rank) / 3; }
        public float healing(int rank) { return 1 + .25F * index(rank); }
        public boolean held() {
            return this == DREADBOW_GALLOWS || this == DARKBORN_RECOVERY;
        }
    }

    private final Kind kind;
    private final ResourceLocation id;
    private final DefaultConfig config;

    public PureDarknessFinaleSpell(Kind kind) {
        this.kind = kind;
        id = ResourceLocation.fromNamespaceAndPath(AlexCavesSpellsbook.MOD_ID, kind.path);
        baseManaCost = kind.mana;
        manaCostPerLevel = kind.manaStep;
        baseSpellPower = 1;
        spellPowerPerLevel = 0;
        castTime = kind.castTicks;
        config = new DefaultConfig().setSchoolResource(ModSchools.PURE_DARKNESS_RESOURCE)
                .setMaxLevel(kind.maxLevel).setMinRarity(kind.minRarity)
                .setCooldownSeconds(kind.cooldown).setAllowCrafting(true).build();
    }

    @Override public ResourceLocation getSpellResource() { return id; }
    @Override public DefaultConfig getDefaultConfig() { return config; }
    @Override public CastType getCastType() { return CastType.LONG; }
    @Override public List<MutableComponent> getUniqueInfo(int rank, LivingEntity caster) {
        float damage = kind.damage(rank) * getSpellPower(rank, caster);
        return switch (kind) {
            case DARKBORN_RECOVERY -> List.of(info("healing_pulse", Utils.stringTruncation(kind.healing(rank), 2)));
            case DREADBOW_GALLOWS -> List.of(info("damage", Utils.stringTruncation(damage, 2)),
                    info("bow_count", kind.bowCount(rank)), info("range", Utils.stringTruncation((float) kind.range(rank), 2)));
            case HOLLOWS_SCREECH -> List.of(info("damage", Utils.stringTruncation(damage, 2)));
        };
    }

    @Override public boolean checkPreCastConditions(Level level, int rank, LivingEntity caster, MagicData data) {
        if (!super.checkPreCastConditions(level, rank, caster, data)) return false;
        if (!(level instanceof ServerLevel server)) return true;
        return placement(server, caster, rank) != null;
    }

    @Override public void onServerPreCast(Level level, int rank, LivingEntity caster, MagicData data) {
        super.onServerPreCast(level, rank, caster, data);
        if (!(level instanceof ServerLevel server)) return;
        var plan = placement(server, caster, rank);
        if (plan != null) {
            PureDarknessFinaleEntity.prepare(server, caster, kind, rank, getSpellPower(rank, caster), data.getCastSource(), plan);
        }
    }

    @Override public void onCast(Level level, int rank, LivingEntity caster, CastSource source, MagicData data) {
        if (level instanceof ServerLevel server) {
            var ready = PureDarknessFinaleEntity.pending(caster, kind);
            // Direct command casts can lack an Iron's pre-cast callback.
            if (ready == null && !kind.held() && source == CastSource.COMMAND) {
                var plan = placement(server, caster, rank);
                if (plan != null) ready = PureDarknessFinaleEntity.prepare(server, caster, kind, rank,
                        getSpellPower(rank, caster), source, plan);
            }
            if (ready == null || !ready.canOpen()) return;
            ready.open();
        }
        super.onCast(level, rank, caster, source, data);
    }

    @Override public void onServerCastComplete(Level level, int rank, LivingEntity caster, MagicData data, boolean cancelled) {
        if (cancelled && level instanceof ServerLevel) {
            var pending = PureDarknessFinaleEntity.pending(caster, kind);
            if (pending != null) pending.interrupt();
        }
        super.onServerCastComplete(level, rank, caster, data, cancelled);
    }

    private PureDarknessFinaleEntity.Placement placement(ServerLevel level, LivingEntity caster, int rank) {
        if (kind.held() && !(caster instanceof ServerPlayer)) return null;
        var plan = PureDarknessFinaleEntity.plan(level, caster, kind, rank);
        if (plan == null) message(caster, kind == Kind.DARKBORN_RECOVERY ? "dark_required" : "ground_required");
        return plan;
    }

    private static MutableComponent info(String key, Object... args) {
        return Component.translatable("ui.alex_caves_spellsbook.pure_darkness." + key, args);
    }

    private static void message(LivingEntity caster, String key) {
        if (caster instanceof ServerPlayer player) player.displayClientMessage(info(key), true);
    }
}

