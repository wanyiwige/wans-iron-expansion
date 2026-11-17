package cn.wanyiwige.alexcavesspellsbook.spells;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.config.ServerConfigs;

import java.util.List;

abstract class PureDarknessSpell extends AbstractSpell {
    private static final SpellRarity[] TIERS = {
            SpellRarity.COMMON, SpellRarity.UNCOMMON, SpellRarity.RARE, SpellRarity.EPIC, SpellRarity.LEGENDARY
    };
    private static final List<Double> DEFAULT_WEIGHTS = List.of(.30, .25, .20, .15, .10);

    @Override public final int getMaxRarity() {
        return SpellRarity.LEGENDARY.getValue();
    }

    @Override public final SpellRarity getRarity(int level) {
        int maxLevel = getMaxLevel();
        int rank = Math.max(1, level);
        if (rank >= maxLevel - 1) return SpellRarity.LEGENDARY;

        // Apply Iron's cumulative weight rule to the five standard tiers only.
        int minimum = Math.max(0, Math.min(getMinRarity(), TIERS.length - 1));
        List<? extends Double> weights = ServerConfigs.RARITY_CONFIG.get();
        if (weights.size() < TIERS.length) weights = DEFAULT_WEIGHTS;
        double total = 0;
        for (int i = minimum; i < TIERS.length; i++) total += weights.get(i);
        if (total <= 0) return TIERS[minimum];

        double position = rank * total / maxLevel;
        double boundary = 0;
        for (int i = minimum; i < TIERS.length - 1; i++) {
            boundary += weights.get(i);
            if (position <= boundary + 1e-12) return TIERS[i];
        }
        return SpellRarity.LEGENDARY;
    }

    @Override public final int getMinLevelForRarity(SpellRarity rarity) {
        int maxLevel = getMaxLevel();
        for (int level = 1; level <= maxLevel; level++) {
            if (getRarity(level) == rarity) return level;
        }
        return 0;
    }
}

