package cn.wanyiwige.alexcavesspellsbook.spells;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.alexcavesspellsbook.entities.DesolateBloodbladeEntity;
import cn.wanyiwige.alexcavesspellsbook.registry.ModSchools;
import com.github.alexmodguy.alexscaves.server.misc.ACSoundRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public final class DesolateBloodbladeSpell extends PureDarknessSpell {
    public static final String PATH = "pure_darkness_desolate_bloodblade";
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(AlexCavesSpellsbook.MOD_ID, PATH);
    private static final int MAX_LEVEL = 10;
    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(ModSchools.PURE_DARKNESS_RESOURCE)
            .setMaxLevel(MAX_LEVEL)
            .setCooldownSeconds(4)
            .setAllowCrafting(true)
            .build();

    public DesolateBloodbladeSpell() {
        baseManaCost = 18;
        manaCostPerLevel = 3;
        baseSpellPower = 1;
        spellPowerPerLevel = 0;
        castTime = 6;
    }

    private static int rank(int level) { return Mth.clamp(level, 1, MAX_LEVEL); }
    public static int bladeCount(int level) { return rank(level) >= 6 ? 5 : 3; }
    public static double range(int level) { return 17.0 + rank(level); }
    public static float directBase(int level) { return 3F * (3 + rank(level)); }

    @Override public ResourceLocation getSpellResource() { return ID; }
    @Override public DefaultConfig getDefaultConfig() { return config; }
    @Override public CastType getCastType() { return CastType.LONG; }
    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(ACSoundRegistry.DESOLATE_DAGGER_SUMMON.get());
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int level, LivingEntity caster) {
        float power = getSpellPower(level, caster);
        return List.of(
                Component.translatable("ui.alex_caves_spellsbook.bloodblade.count", bladeCount(level)),
                Component.translatable("ui.alex_caves_spellsbook.bloodblade.direct", format(directBase(level) * power)));
    }

    private static String format(float value) {
        return io.redspace.ironsspellbooks.api.util.Utils.stringTruncation(value, 2);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
        if (level instanceof ServerLevel server) {
            DesolateBloodbladeEntity.launchVolley(server, caster, caster.getLookAngle(), bladeCount(spellLevel),
                    range(spellLevel), directBase(spellLevel) * getSpellPower(spellLevel, caster));
        }
        super.onCast(level, spellLevel, caster, source, data);
    }
}
