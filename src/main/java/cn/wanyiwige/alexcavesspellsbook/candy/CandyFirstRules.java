package cn.wanyiwige.alexcavesspellsbook.candy;

/** Pure timing/trajectory limits shared by the first thirteen spells and deterministic checks. */
public final class CandyFirstRules {
    private CandyFirstRules() { }
    /** Wall and entity rebounds have independent budgets; hit-ledger limits stay unchanged. */
    public static int wallBounces(String id) {
        return switch(id) {
            case "rainbow_ricochet" -> 8;
            case "sugar_hunt", "crystal_split", "popping_cluster" -> 6;
            default -> 0;
        };
    }
    public static int entityBounces(String id) {
        return switch(id) {
            case "rainbow_ricochet" -> 8;
            case "sugar_hunt" -> 5; // Initial hit plus five redirects: six different targets.
            default -> 0;
        };
    }
    public static boolean implemented(String id) {
        return switch (id) {
            case "rainbow_ricochet", "sugar_hunt", "crystal_split", "popping_cluster", "peppermint_guard",
                 "peppermint_hunt", "peppermint_wheels", "licorice_hex_wave", "sugar_seal_bloom",
                 "frostmint_rain", "soda_meteor", "soda_frostburst", "caramel_bog" -> true;
            default -> false;
        };
    }
    public static int launchCount(String id) {
        return switch (id) {
            case "rainbow_ricochet", "peppermint_wheels" -> 3;
            case "popping_cluster" -> 4;
            case "peppermint_guard", "peppermint_hunt" -> 5;
            case "frostmint_rain" -> 6;
            default -> 1;
        };
    }
    public static int launchTick(String id, int index) {
        return switch (id) {
            case "rainbow_ricochet" -> index*4;
            case "frostmint_rain" -> index*20;
            default -> 0;
        };
    }
    public static double growingRadius(double radius, double tick) { return radius*Math.min(1, Math.max(0, (tick+1)/20.0)); }
    public static double caramelRadius(double maximum, double entityAge) {
        return growingRadius(maximum,Math.max(0,entityAge-4));
    }
    public static double caramelOpacity(double entityAge) {
        return Math.max(0,Math.min(1,(entityAge-4)/6.0));
    }
    public static boolean hexPulse(int tick) { return tick>=0 && tick<100 && tick%20==0; }
    public static float hexScale(int tick) { return tick==80 ? 2 : 0.75F; }
    public static int bloomTick(int ring) { return 20+ring*4; }
    public static int fuseTick(int index) { return 36+Math.max(0,Math.min(3,index))*6; }
    public static boolean bloomBand(double radius,double distance,int ring) {
        if(radius<=0||distance<0||ring<0||ring>2)return false;
        return distance<=radius*(ring+1)/3.0&&(ring==0||distance>radius*ring/3.0);
    }
    public static double cappedLength(double length, double maximum) { return Double.isFinite(length) ? Math.max(0,Math.min(maximum,length)) : 0; }
}
