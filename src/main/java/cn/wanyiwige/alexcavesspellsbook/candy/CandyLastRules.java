package cn.wanyiwige.alexcavesspellsbook.candy;

import java.util.Set;

/** Fixed timing gates for the second half of the school. */
public final class CandyLastRules {
    private static final Set<String> IDS=Set.of("sugar_glass_stars","rainbow_leap","sweet_overdrive",
            "rainbow_snare","candicorn_charge","gummy_guardian","gingerbread_march","candy_cane_whirl",
            "gumball_sentry","gum_worm_breach","candy_banquet","ice_cream_storm","licorice_grand_feast");
    public static boolean implemented(String id){return IDS.contains(id);}
    public static boolean groundSpell(String id){return switch(id){
        case "rainbow_snare","gumball_sentry","gum_worm_breach","candy_banquet","ice_cream_storm","licorice_grand_feast"->true;
        default->false;};}
    public static boolean pulse(CandySpecs.Spec spec,int age){return age>=0&&age%Math.max(1,spec.hitInterval())==0;}
    public static boolean within(double distance,double radius){return Double.isFinite(distance)&&distance<=radius;}
    private CandyLastRules(){}
}
