package cn.wanyiwige.astralcompat.spells;

import cn.wanyiwige.astralcompat.registry.AstralSchools;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

import java.util.Optional;

/** Native registry definition; gameplay remains in AstralSpellEvents. */
public final class AstralSpell extends AbstractSpell {
    public static final String NAMESPACE = "asterismarcanum";
    private final ResourceLocation spellId;
    private final DefaultConfig defaultConfig;
    private final CastType castType;

    public AstralSpell(String path, SpellRarity rarity, int maxLevel, double cooldownSeconds,
                       int baseMana, int manaPerLevel, int basePower, int powerPerLevel,
                       int castTime, CastType castType) {
        this.spellId = ResourceLocation.fromNamespaceAndPath(NAMESPACE, path);
        this.defaultConfig = new DefaultConfig()
                .setMinRarity(rarity)
                .setSchoolResource(AstralSchools.ASTRAL_RESOURCE)
                .setMaxLevel(maxLevel)
                .setCooldownSeconds(cooldownSeconds)
                .build();
        this.baseManaCost = baseMana;
        this.manaCostPerLevel = manaPerLevel;
        this.baseSpellPower = basePower;
        this.spellPowerPerLevel = powerPerLevel;
        this.castTime = castTime;
        this.castType = castType;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return castType;
    }

    @Override
    public Optional<net.minecraft.sounds.SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.AMETHYST_BLOCK_CHIME);
    }

    @Override
    public boolean allowLooting() {
        return true;
    }

    @Override
    public boolean requiresLearning() {
        return false;
    }
}
