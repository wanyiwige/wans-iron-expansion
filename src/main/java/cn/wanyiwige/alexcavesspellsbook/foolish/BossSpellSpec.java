package cn.wanyiwige.alexcavesspellsbook.foolish;

import net.minecraft.resources.ResourceLocation;

/** Exactly the user-selected nineteen new attacks. Sacilite spells retain their existing IDs. */
public enum BossSpellSpec {
    INCINERATOR_STREAM("incinerator_stream", "pyre", "irons_spellbooks:fire", 12, 5, 65, 18, 20),
    WHITE_FIRE_RAIN("white_fire_rain", "pyre", "irons_spellbooks:fire", 22, 8, 80, 22, 20),
    TIDAL_SPIN("tidal_spin", "thrasher", "traveloptics:aqua", 26, 10, 60, 15, 20),
    TIDAL_GRIP("tidal_grip", "thrasher", "traveloptics:aqua", 12, 5, 70, 20, 20),
    RADIUM_BOLT("radium_bolt", "varmint_king", "irons_spellbooks:nature", 30, 12, 55, 12, 10),
    CORROSIVE_CLOUD("corrosive_cloud", "varmint_king", "irons_spellbooks:nature", 5, 2, 70, 20, 10),
    STORM_MINEFIELD("storm_minefield", "thunderbird", "irons_spellbooks:lightning", 24, 9, 90, 24, 10),
    PARALYZING_FEATHERS("paralyzing_feathers", "thunderbird", "irons_spellbooks:lightning", 16, 6, 65, 16, 10),
    THUNDER_WINGBEAT("thunder_wingbeat", "thunderbird", "irons_spellbooks:lightning", 30, 12, 60, 15, 20),
    SCARLANT_CAPSULE("scarlant_capsule_spell", "scarlant_queen", "entomology_spell:insect", 10, 4, 80, 30, 12),
    SCARLET_EDICT("scarlet_edict", "scarlant_queen", "irons_spellbooks:blood", 12, 5, 55, 20, 10),
    NOURISHING_LEAVES("nourishing_leaves", "terrorflora", "irons_spellbooks:nature", 2, 1, 75, 30, 10),
    TANGLE_ROOTS("tangle_roots", "terrorflora", "irons_spellbooks:nature", 6, 3, 60, 18, 10),
    BLINDING_POLLEN("blinding_pollen", "terrorflora", "irons_spellbooks:nature", 18, 7, 45, 12, 10),
    EXPLOSIVE_SPORES("explosive_spores", "terrorflora", "irons_spellbooks:nature", 24, 9, 85, 24, 20),
    ASTRAL_CHAIN_SLASH("astral_chain_slash", "astralis", "kubejs:astral", 18, 7, 90, 24, 40),
    METEOR_JUDGMENT("meteor_judgment", "astralis", "kubejs:astral", 30, 12, 110, 30, 40),
    SEEKING_STAR_BLADE("seeking_star_blade", "astralis", "kubejs:astral", 16, 6, 85, 20, 10),
    FALLING_STAR_BOMB("falling_star_bomb", "astralis", "kubejs:astral", 48, 18, 100, 28, 40);

    public final String id, boss, school;
    public final int damage, growth, mana, cooldown, castTicks;
    BossSpellSpec(String id, String boss, String school, int damage, int growth, int mana, int cooldown, int castTicks) {
        this.id=id; this.boss=boss; this.school=school; this.damage=damage; this.growth=growth;
        this.mana=mana; this.cooldown=cooldown; this.castTicks=castTicks;
    }
    public ResourceLocation resource() { return ResourceLocation.fromNamespaceAndPath("alex_caves_spellsbook", id); }
    public ResourceLocation schoolResource() { return ResourceLocation.parse(school); }
    public boolean astral() { return school.equals("kubejs:astral"); }
}
