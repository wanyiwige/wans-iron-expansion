package cn.wanyiwige.alexcavesspellsbook.spells;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.alexcavesspellsbook.entities.PureDarknessWardEntity;
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

public final class PureDarknessWardSpell extends PureDarknessSpell {
    public enum Kind {
        WATCHER_DREAD("watcher_dread", 34, 5, 13, 12, 4, .5, 14, 0, 0, 0, 5, RARE),
        MARTYR_VESTMENT("martyr_vestment", 38, 6, 21, 12, 0, 0, 640, 40, 0, 0, 5, UNCOMMON),
        FORSAKEN_MAW("forsaken_maw", 54, 10, 15, 16, 10, 2, 32, 0, 57, 15, 3, EPIC);

        public final String path;
        private final int mana, manaStep, cooldown, castTicks, duration, durationStep;
        private final double range, rangeStep;
        private final float damage, damageStep;
        private final int maxLevel;
        private final SpellRarity minRarity;

        Kind(String slug, int mana, int manaStep, int cooldown, int castTicks, double range, double rangeStep,
             int duration, int durationStep, float damage, float damageStep, int maxLevel, SpellRarity minRarity) {
            path = "pure_darkness_" + slug;
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
        public float budget(int rank) { return 6 + 2 * index(rank); }
        public int weakness(int rank) { return 32 + 6 * index(rank); }
        public double radius(int rank) { return 2 + .25 * index(rank); }
    }

    public static final int HEALTH_COST = 2;
    private final Kind kind;
    private final ResourceLocation id;
    private final DefaultConfig config;

    public PureDarknessWardSpell(Kind kind) {
        this.kind = kind;
        id = ResourceLocation.fromNamespaceAndPath(AlexCavesSpellsbook.MOD_ID, kind.path);
        baseManaCost = kind.mana;
        manaCostPerLevel = kind.manaStep;
        castTime = kind.castTicks;
        baseSpellPower = 1;
        spellPowerPerLevel = 0;
        config = new DefaultConfig().setSchoolResource(ModSchools.PURE_DARKNESS_RESOURCE)
                .setMaxLevel(kind.maxLevel).setMinRarity(kind.minRarity)
                .setCooldownSeconds(kind.cooldown).setAllowCrafting(true).build();
    }

    @Override public ResourceLocation getSpellResource() { return id; }
    @Override public DefaultConfig getDefaultConfig() { return config; }
    @Override public CastType getCastType() { return CastType.LONG; }
    @Override public List<MutableComponent> getUniqueInfo(int rank, LivingEntity caster) {
        return switch (kind) {
            case WATCHER_DREAD -> List.of(info("range", Utils.stringTruncation((float) kind.range(rank), 2)));
            case MARTYR_VESTMENT -> List.of(info("health_cost", HEALTH_COST), info("ward_budget", (int) kind.budget(rank)),
                    info("duration_seconds", kind.duration(rank) / 20));
            case FORSAKEN_MAW -> List.of(damageInfo(rank, caster), info("ward_radius", Utils.stringTruncation((float) kind.radius(rank), 2)));
        };
    }

    private MutableComponent damageInfo(int rank, LivingEntity caster) {
        return info("damage", Utils.stringTruncation(kind.damage(rank) * getSpellPower(rank, caster), 2));
    }

    private static MutableComponent info(String key, Object value) {
        return Component.translatable("ui.alex_caves_spellsbook.pure_darkness." + key, value);
    }

    @Override public boolean checkPreCastConditions(Level level, int rank, LivingEntity caster, MagicData data) {
        return super.checkPreCastConditions(level, rank, caster, data)
                && (!(level instanceof ServerLevel server) || placement(server, caster, rank) != null);
    }

    @Override public void onCast(Level level, int rank, LivingEntity caster, CastSource source, MagicData data) {
        if (level instanceof ServerLevel server) {
            var placement = placement(server, caster, rank);
            if (placement == null || !PureDarknessWardEntity.spawn(server, caster, kind, rank,
                    kind.damage(rank) * getSpellPower(rank, caster), placement)) return;
            if (kind == Kind.MARTYR_VESTMENT) caster.setHealth(caster.getHealth() - HEALTH_COST);
        }
        super.onCast(level, rank, caster, source, data);
    }

    private PureDarknessWardEntity.Placement placement(ServerLevel level, LivingEntity caster, int rank) {
        if (kind == Kind.MARTYR_VESTMENT && caster.getHealth() < HEALTH_COST + 1) {
            message(caster, "ward_low_health");
            return null;
        }
        var placement = PureDarknessWardEntity.plan(level, caster, kind, rank);
        if (placement == null) message(caster, "ground_required");
        return placement;
    }

    private static void message(LivingEntity caster, String key) {
        if (caster instanceof ServerPlayer player) player.displayClientMessage(info(key, ""), true);
    }
}

