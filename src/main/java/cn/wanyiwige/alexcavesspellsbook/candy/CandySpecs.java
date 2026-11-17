package cn.wanyiwige.alexcavesspellsbook.candy;

import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import java.util.List;

/** Fixed design limits; spell-specific implementations arrive in steps 7-32. */
public final class CandySpecs {
    /** Shared by server hits, summons and tooltip values; apply exactly once to base damage. */
    public static final float DAMAGE_MULTIPLIER = 2.5F;
    public record Spec(String id, String chineseName, CastType castType, SpellRarity rarity,
                       int maxLevel, int mana, int manaGrowth, int cooldown, int castTicks,
                       float damage, float damageGrowth, double range, double radius,
                       int activeTicks, int hitInterval, int maxHits, int entityBudget, int controlTicks) {
        public int clampLevel(int level) { return Math.max(1, Math.min(maxLevel, level)); }
        public float hitDamage(int level) { return (damage + damageGrowth * (clampLevel(level) - 1)) * DAMAGE_MULTIPLIER; }
        public float maximumDamage(int level) { return hitDamage(level) * maxHits; }
    }
    public static final List<Spec> ALL = List.of(
        new Spec("rainbow_ricochet", "七彩跳弹", CastType.LONG, SpellRarity.COMMON, 10, 25, 5, 5, 10, 6F, 2F, 24, 0.3, 60, 10, 3, 4, 0),
        new Spec("sugar_hunt", "糖珠追猎", CastType.LONG, SpellRarity.UNCOMMON, 10, 40, 8, 9, 15, 7F, 2F, 24, 0.3, 80, 10, 4, 4, 0),
        new Spec("crystal_split", "糖晶裂射", CastType.LONG, SpellRarity.RARE, 8, 50, 10, 12, 15, 8F, 2F, 24, 0.3, 60, 10, 4, 5, 0),
        new Spec("popping_cluster", "爆糖星簇", CastType.LONG, SpellRarity.RARE, 8, 65, 12, 16, 20, 10F, 3F, 20, 2.5, 80, 20, 4, 5, 0),
        new Spec("peppermint_guard", "薄荷卫环", CastType.LONG, SpellRarity.UNCOMMON, 5, 45, 8, 18, 15, 4F, 1F, 3, 2.5, 160, 20, 8, 6, 0),
        new Spec("peppermint_hunt", "薄荷追刃", CastType.LONG, SpellRarity.RARE, 8, 60, 10, 14, 20, 9F, 2F, 24, 0.4, 100, 10, 5, 6, 0),
        new Spec("peppermint_wheels", "薄荷滚轮", CastType.LONG, SpellRarity.UNCOMMON, 10, 40, 8, 10, 15, 8F, 2F, 20, 0.6, 60, 10, 3, 4, 0),
        new Spec("licorice_hex_wave", "甘草咒潮", CastType.LONG, SpellRarity.RARE, 8, 70, 12, 18, 20, 4F, 1F, 20, 3, 100, 20, 5, 3, 20),
        new Spec("sugar_seal_bloom", "糖印绽放", CastType.LONG, SpellRarity.EPIC, 5, 85, 20, 22, 25, 18F, 6F, 20, 4, 40, 20, 1, 1, 30),
        new Spec("frostmint_rain", "霜薄荷矛雨", CastType.LONG, SpellRarity.EPIC, 8, 90, 20, 24, 25, 10F, 3F, 24, 4, 120, 20, 6, 7, 40),
        new Spec("soda_meteor", "苏打流星", CastType.LONG, SpellRarity.RARE, 8, 65, 12, 16, 20, 15F, 4F, 28, 3, 80, 20, 1, 1, 0),
        new Spec("soda_frostburst", "苏打霜爆", CastType.LONG, SpellRarity.EPIC, 5, 95, 20, 24, 25, 20F, 6F, 20, 4, 50, 20, 1, 2, 50),
        new Spec("caramel_bog", "焦糖沼域", CastType.LONG, SpellRarity.RARE, 5, 65, 10, 20, 20, 2F, 0.5F, 18, 4, 160, 20, 8, 1, 40),
        new Spec("sugar_glass_stars", "糖玻璃碎星", CastType.LONG, SpellRarity.RARE, 10, 55, 10, 14, 15, 6F, 2F, 24, 0.3, 60, 10, 6, 7, 0),
        new Spec("rainbow_leap", "虹糖跃迁", CastType.INSTANT, SpellRarity.UNCOMMON, 5, 30, 6, 10, 0, 0F, 0F, 12, 0, 20, 20, 0, 1, 0),
        new Spec("sweet_overdrive", "甜蜜超频", CastType.LONG, SpellRarity.RARE, 5, 60, 10, 30, 15, 0F, 0F, 0, 0, 200, 20, 0, 1, 0),
        new Spec("rainbow_snare", "虹桥束缚", CastType.LONG, SpellRarity.RARE, 5, 65, 12, 20, 20, 4F, 1F, 20, 3, 80, 20, 4, 1, 40),
        new Spec("candicorn_charge", "独角兽虹袭", CastType.LONG, SpellRarity.EPIC, 3, 85, 20, 22, 25, 18F, 6F, 24, 1.5, 60, 20, 1, 1, 20),
        new Spec("gummy_guardian", "软糖守卫", CastType.LONG, SpellRarity.RARE, 5, 80, 15, 45, 25, 6F, 2F, 24, 0.8, 600, 20, 30, 2, 0),
        new Spec("gingerbread_march", "姜饼行军", CastType.LONG, SpellRarity.EPIC, 5, 100, 25, 50, 30, 3F, 1F, 24, 0.5, 400, 20, 20, 4, 0),
        new Spec("candy_cane_whirl", "糖杖狂旋", CastType.CONTINUOUS, SpellRarity.RARE, 10, 12, 2, 16, 100, 4F, 1F, 4, 3, 100, 10, 10, 1, 0),
        new Spec("gumball_sentry", "糖球哨塔", CastType.LONG, SpellRarity.EPIC, 5, 100, 25, 45, 30, 5F, 2F, 24, 0.3, 240, 20, 12, 10, 0),
        new Spec("gum_worm_breach", "蠕虫破宴", CastType.LONG, SpellRarity.LEGENDARY, 3, 130, 30, 35, 30, 24F, 8F, 24, 4, 80, 20, 1, 1, 30),
        new Spec("candy_banquet", "糖境盛宴", CastType.LONG, SpellRarity.LEGENDARY, 3, 150, 30, 45, 40, 4F, 1.5F, 24, 6, 200, 20, 10, 12, 30),
        new Spec("ice_cream_storm", "冰淇淋暴雨", CastType.LONG, SpellRarity.EPIC, 8, 110, 25, 30, 30, 6F, 2F, 24, 5, 120, 20, 6, 8, 40),
        new Spec("licorice_grand_feast", "甘草魔宴", CastType.LONG, SpellRarity.LEGENDARY, 3, 160, 35, 50, 40, 8F, 3F, 24, 6, 160, 20, 8, 12, 30));
    private CandySpecs() { }
    public static Spec byId(String id) {
        return ALL.stream().filter(s -> s.id().equals(id)).findFirst().orElseThrow();
    }
}
