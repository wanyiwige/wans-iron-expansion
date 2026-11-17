package cn.wanyiwige.alexcavesspellsbook.spells;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.alexcavesspellsbook.entities.PureDarknessRiteEntity;
import cn.wanyiwige.alexcavesspellsbook.registry.ModSchools;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
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

public final class PureDarknessRiteSpell extends PureDarknessSpell {
    public enum Kind {
        THORN_GIBBET("thorn_gibbet", 30, 5, 10, 12, 8, 1, 28, 0, 21, 6, 5, UNCOMMON),
        ZEALOT_CHORUS("zealot_chorus", 60, 12, 22, 8, 12, 2, 40, 0, 81, 27, 3, RARE),
        MOTHWING_GUARD("mothwing_guard", 24, 5, 11, 6, 0, 0, 120, 15, 0, 0, 5, COMMON),
        BURROWING_PRAYER("burrowing_prayer", 25, 4, 7, 4, 9, 2, PureDarknessRiteEntity.BURROW_TRAVEL_TICKS + 1, 0, 0, 0, 5, UNCOMMON);

        public final String path;
        private final int mana, manaStep, cooldown, castTicks, duration, durationStep;
        private final double range, rangeStep;
        private final float damage, damageStep;
        private final int maxLevel;
        private final SpellRarity minRarity;

        Kind(String slug, int mana, int manaStep, int cooldown, int castTicks, double range, double rangeStep,
             int duration, int durationStep, float damage, float damageStep, int maxLevel, SpellRarity minRarity) {
            this.path = "pure_darkness_" + slug;
            this.mana = mana;
            this.manaStep = manaStep;
            this.cooldown = cooldown;
            this.castTicks = castTicks;
            this.range = range;
            this.rangeStep = rangeStep;
            this.duration = duration;
            this.durationStep = durationStep;
            this.damage = damage;
            this.damageStep = damageStep;
            this.maxLevel = maxLevel;
            this.minRarity = minRarity;
        }

        private int index(int rank) { return Mth.clamp(rank, 1, maxLevel) - 1; }
        public double range(int rank) { return range + index(rank) * rangeStep; }
        public int duration(int rank) { return duration + index(rank) * durationStep; }
        public float damage(int rank) { return damage + index(rank) * damageStep; }
        public int charges(int rank) { return 2 + index(rank) / 2; }
        public float chanterHealth(int rank) { return 10 + index(rank) * 3; }
        public double chorusRadius(int rank) { return 5 + index(rank) * .5; }
    }

    private final Kind kind;
    private final ResourceLocation id;
    private final DefaultConfig config;

    public PureDarknessRiteSpell(Kind kind) {
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
        return switch (kind) {
            case MOTHWING_GUARD -> List.of(info("blocks", kind.charges(rank)),
                    info("duration_seconds", Utils.stringTruncation(kind.duration(rank) / 20F, 2)));
            case BURROWING_PRAYER -> List.of(info("range", Utils.stringTruncation((float) kind.range(rank), 2)));
            case ZEALOT_CHORUS -> List.of(damageInfo(rank, caster), info("integrity", (int) kind.chanterHealth(rank)),
                    info("range", Utils.stringTruncation((float) kind.chorusRadius(rank), 2)));
            case THORN_GIBBET -> List.of(damageInfo(rank, caster));
        };
    }

    private MutableComponent damageInfo(int rank, LivingEntity caster) {
        return info("damage", Utils.stringTruncation(kind.damage(rank) * getSpellPower(rank, caster), 2));
    }

    private static MutableComponent info(String key, Object value) {
        return Component.translatable("ui.alex_caves_spellsbook.pure_darkness." + key, value);
    }

    @Override public boolean checkPreCastConditions(Level level, int rank, LivingEntity caster, MagicData data) {
        if (!super.checkPreCastConditions(level, rank, caster, data)) return false;
        return !(level instanceof ServerLevel server) || placement(server, caster, rank) != null;
    }

    @Override public void onCast(Level level, int rank, LivingEntity caster, CastSource source, MagicData data) {
        if (level instanceof ServerLevel server) {
            var placement = placement(server, caster, rank);
            if (placement == null || !PureDarknessRiteEntity.spawn(server, caster, kind, rank,
                    kind.damage(rank) * getSpellPower(rank, caster), placement)) return;
        }
        super.onCast(level, rank, caster, source, data);
    }

    private PureDarknessRiteEntity.Placement placement(ServerLevel level, LivingEntity caster, int rank) {
        if (kind == Kind.ZEALOT_CHORUS && PureDarknessRiteEntity.hasRitual(caster)) {
            message(caster, "ritual_busy");
            return null;
        }
        var placement = PureDarknessRiteEntity.plan(level, caster, kind, rank);
        if (placement == null) message(caster, "ground_required");
        return placement;
    }

    private static void message(LivingEntity caster, String key) {
        if (caster instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("ui.alex_caves_spellsbook.pure_darkness." + key), true);
        }
    }
}

