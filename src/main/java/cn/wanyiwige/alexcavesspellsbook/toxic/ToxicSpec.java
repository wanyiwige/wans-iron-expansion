package cn.wanyiwige.alexcavesspellsbook.toxic;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import static io.redspace.ironsspellbooks.api.spells.SpellRarity.*;

public enum ToxicSpec {
    GAMMA("gamma_jet",45,5,8,8,24,10,2,
            COMMON,COMMON,COMMON,UNCOMMON,UNCOMMON,UNCOMMON,RARE,RARE,EPIC,EPIC),
    DRUM("waste_drum_cast",60,7,14,12,160,12,3,
            UNCOMMON,UNCOMMON,RARE,RARE,RARE,EPIC,EPIC,LEGENDARY),
    PURIFICATION("raycat_purification",40,6,16,16,24,8,2,
            COMMON,COMMON,UNCOMMON,UNCOMMON,RARE),
    LANCE("proton_lance",70,8,12,14,24,24,4,
            RARE,RARE,EPIC,EPIC,EPIC,LEGENDARY,LEGENDARY),
    FISSION("critical_fission",90,15,18,24,16,28,7,
            EPIC,EPIC,LEGENDARY,LEGENDARY),
    WARD("reactor_ward",80,9,25,12,120,24,4,
            UNCOMMON,RARE,RARE,EPIC,EPIC,LEGENDARY),
    ISOTOPE("isotope_brand",30,4,7,8,120,10,2,COMMON,COMMON,UNCOMMON,UNCOMMON,UNCOMMON,RARE,EPIC,LEGENDARY),
    NEUTRON("neutron_rebound",40,5,6,8,48,15,3,UNCOMMON,UNCOMMON,RARE,RARE,EPIC,EPIC,LEGENDARY),
    MODERATOR("moderation_field",55,7,15,12,120,12,2,RARE,RARE,EPIC,EPIC,LEGENDARY),
    LEAD("lead_curtain",65,7,20,10,100,8,1,UNCOMMON,RARE,RARE,EPIC,EPIC,LEGENDARY),
    CHAIN("chain_reaction",100,12,18,14,22,22,5,EPIC,EPIC,LEGENDARY,LEGENDARY),
    FILM("radiation_film",70,8,14,10,100,16,3,RARE,RARE,EPIC,EPIC,LEGENDARY,LEGENDARY),
    CLOUD("cloud_chamber_trail",65,7,16,10,120,18,3,UNCOMMON,UNCOMMON,RARE,RARE,EPIC,EPIC,LEGENDARY,LEGENDARY),
    CHERENKOV("cherenkov_wake",75,9,14,8,34,18,3,UNCOMMON,UNCOMMON,RARE,RARE,EPIC,EPIC,LEGENDARY),
    DECOY("decay_transposition",75,8,20,8,247,18,3,RARE,RARE,EPIC,EPIC,LEGENDARY),
    MATRIX("critical_matrix",105,14,22,12,RadiationRules.MATRIX_RECAST_TICKS+10,24,6,EPIC,EPIC,LEGENDARY,LEGENDARY);
    public final String id;
    public final int mana,manaGrowth,cooldown,castTicks,life,base,growth,maxLevel;
    public final SpellRarity rarity;
    private final SpellRarity[] progression;
    ToxicSpec(String id,int mana,int manaGrowth,int cooldown,int castTicks,int life,int base,int growth,SpellRarity... progression) {
        if(progression.length==0)throw new IllegalArgumentException("Missing radiation progression: "+id);
        for(int i=1;i<progression.length;i++)if(progression[i].getValue()<progression[i-1].getValue())
            throw new IllegalArgumentException("Descending radiation rarity: "+id);
        this.id=id;this.mana=mana;this.manaGrowth=manaGrowth;this.cooldown=cooldown;this.castTicks=castTicks;
        this.life=life;this.base=base;this.growth=growth;this.progression=progression.clone();this.maxLevel=progression.length;this.rarity=progression[0];
    }
    public int clampLevel(int level){return Math.max(1,Math.min(maxLevel,level));}
    public SpellRarity rarityAt(int level){return progression[clampLevel(level)-1];}
    public int firstLevel(SpellRarity rarity){for(int i=0;i<progression.length;i++)if(progression[i]==rarity)return i+1;return 0;}
    public int cleanseProtection(int level){return 4+2*clampLevel(level);}
}
