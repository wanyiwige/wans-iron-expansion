package cn.wanyiwige.alexcavesspellsbook.spells;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.alexcavesspellsbook.entities.PureDarknessFieldEntity;
import cn.wanyiwige.alexcavesspellsbook.registry.ModSchools;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import cn.wanyiwige.alexcavesspellsbook.registry.PureDarknessParticles;
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

public final class PureDarknessFieldSpell extends PureDarknessSpell {
    public enum Kind {
        CLOSING_EYE("closing_eye", 28, 4, 6, 10, 9, .5, 24, 4.5F, 20, 0, 10, UNCOMMON),
        MOTHSCALE_MIST("mothscale_mist", 22, 3, 8, 8, 4, .375, 6, 1.5F, 28, 2, 8, COMMON),
        CORRODENT_BURROW("corrodent_burrow", 32, 4, 8, 10, 9, 1, 24, 4.5F, 36, 0, 8, UNCOMMON),
        VESPER_PERCH("vesper_perch", 30, 4, 11, 10, 10, 1, 24, 3.75F, 100, 10, 8, UNCOMMON);

        public final String path;
        private final int mana, manaStep, cooldown, castTicks, duration, durationStep;
        private final double range, rangeStep;
        private final float damage, damageStep;
        private final int maxLevel;
        private final SpellRarity minRarity;

        Kind(String name, int mana, int manaStep, int cooldown, int castTicks, double range,
             double rangeStep, float damage, float damageStep, int duration, int durationStep,
             int maxLevel, SpellRarity minRarity) {
            this.path = "pure_darkness_" + name;
            this.mana = mana;
            this.manaStep = manaStep;
            this.cooldown = cooldown;
            this.castTicks = castTicks;
            this.range = range;
            this.rangeStep = rangeStep;
            this.damage = damage;
            this.damageStep = damageStep;
            this.duration = duration;
            this.durationStep = durationStep;
            this.maxLevel = maxLevel;
            this.minRarity = minRarity;
        }

        private int index(int level) { return Mth.clamp(level - 1, 0, maxLevel - 1); }
        public double range(int level) { return range + index(level) * rangeStep; }
        public float damage(int level) { return damage + index(level) * damageStep; }
        public int duration(int level) { return duration + index(level) * durationStep; }
        public double eyeWidth(int level) { return 1.5 + index(level) * .125; }
        public int slowTicks(int level) { return 540 + index(level) * 20; }
        public double triggerRadius(int level) { return 5 + index(level) * .5; }
        public int corrodentCount(int level) { return 3 + index(level) / 3; }
        public int batCount(int level) { return index(level) < 4 ? 4 : 5; }
    }

    private final Kind kind;
    private final ResourceLocation id;
    private final DefaultConfig config;

    public PureDarknessFieldSpell(Kind kind) {
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
    @Override
    public List<MutableComponent> getUniqueInfo(int level, LivingEntity caster) {
        MutableComponent damage = Component.translatable("ui.alex_caves_spellsbook.pure_darkness.damage",
                Utils.stringTruncation(kind.damage(level) * getSpellPower(level, caster), 2));
        MutableComponent range = Component.translatable("ui.alex_caves_spellsbook.pure_darkness.range",
                Utils.stringTruncation((float) kind.range(level), 2));
        if (kind == Kind.MOTHSCALE_MIST) {
            return List.of(damage, range, Component.translatable("ui.alex_caves_spellsbook.pure_darkness.debuff_seconds",
                    kind.slowTicks(level) / 20));
        }
        if (kind == Kind.CORRODENT_BURROW) {
            return List.of(damage, range, Component.translatable(
                    "ui.alex_caves_spellsbook.pure_darkness.corrodent_count", kind.corrodentCount(level)));
        }
        if (kind == Kind.VESPER_PERCH) {
            return List.of(damage, range,
                    Component.translatable("ui.alex_caves_spellsbook.pure_darkness.bat_count", kind.batCount(level)),
                    Component.translatable("ui.alex_caves_spellsbook.pure_darkness.bat_trigger_radius",
                            Utils.stringTruncation((float) kind.triggerRadius(level), 2)));
        }
        return List.of(damage, range);
    }

    @Override
    public boolean checkPreCastConditions(Level level, int rank, LivingEntity caster, MagicData data) {
        if (!super.checkPreCastConditions(level, rank, caster, data)) return false;
        if (!(level instanceof ServerLevel server) || PureDarknessFieldEntity.plan(server, caster, kind, rank) != null) {
            return true;
        }
        showPlacementError(caster);
        return false;
    }

    @Override
    public void onServerPreCast(Level level, int rank, LivingEntity caster, MagicData data) {
        super.onServerPreCast(level, rank, caster, data);
        if (kind == Kind.MOTHSCALE_MIST && level instanceof ServerLevel server) {
            var point = caster.getEyePosition().add(caster.getLookAngle().scale(.45)).subtract(0, .28, 0);
            server.sendParticles(PureDarknessParticles.MOTH_SCALE.get(), point.x, point.y, point.z,
                    7, .18, .14, .18, .026);
        }
    }

    @Override
    public void onCast(Level level, int rank, LivingEntity caster, CastSource source, MagicData data) {
        if (level instanceof ServerLevel server) {
            var placement = PureDarknessFieldEntity.plan(server, caster, kind, rank);
            if (placement == null) {
                showPlacementError(caster);
                return;
            }
            PureDarknessFieldEntity.spawn(server, caster, kind, rank,
                    kind.damage(rank) * getSpellPower(rank, caster), placement);
        }
        super.onCast(level, rank, caster, source, data);
    }

    private void showPlacementError(LivingEntity caster) {
        if (caster instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("ui.alex_caves_spellsbook." + kind.path + ".placement"), true);
        }
    }
}

