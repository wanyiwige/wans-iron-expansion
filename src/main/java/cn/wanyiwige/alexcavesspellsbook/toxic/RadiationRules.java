package cn.wanyiwige.alexcavesspellsbook.toxic;

/** Shared gameplay rules kept independent of rendering and Minecraft initialization. */
public final class RadiationRules {
    public static final double AREA_RADIUS = 5;
    public static final double MATRIX_RADIUS = 8;
    public static final int DECOY_RECAST_TICKS = 240;
    public static final int MATRIX_RECAST_TICKS = 30 * 20;
    public static final int AREA_TARGET_LIMIT = 32;
    public static int recastTicks(ToxicSpec spec) {
        return switch(spec){case FILM -> 61;case CLOUD -> 80;case DECOY -> DECOY_RECAST_TICKS;case MATRIX -> MATRIX_RECAST_TICKS;default -> 0;};
    }
    public static boolean cloudPathStep(int elapsed,int nodes){return elapsed>=1&&elapsed%2==1&&(elapsed-1)/2<nodes;}
    public static final float LEAD_SCALE = 1.25F;
    public static final float LEAD_MODEL_SIZE = 2.4F * LEAD_SCALE;
    public static final double LEAD_CENTER_HEIGHT = LEAD_MODEL_SIZE / 2.0;
    public static final double LEAD_HALF_WIDTH = 1.35 * LEAD_SCALE;
    public static final double LEAD_HALF_HEIGHT = 1.1 * LEAD_SCALE;
    public static boolean isotopePulse(int age){return age>=1&&age<=101&&(age-1)%20==0;}
    public static boolean filmCapture(int age){return age>=1&&age<=46&&(age-1)%15==0;}
    public static int maxBounces(int rank) { return rank >= 6 ? 2 : rank >= 3 ? 1 : 0; }
    public static int shieldCharges(int rank) { return Math.min(5, 2 + Math.max(0, rank - 1) * 3 / 5); }
    public static float filmDamage(int shot) { return (float)(.45 * Math.pow(.7, Math.max(0, shot))); }
    public static double shieldCrossing(double before, double after) {
        return before > 0 && after <= 0 && after < before ? before / (before - after) : Double.NaN;
    }
    public static int matrixCell(double dx, double dy, double dz) {
        double horizontal = Math.hypot(dx, dz);
        if (horizontal < .75 || Math.abs(dy) > horizontal * 1.5) return 4;
        int x = dx / horizontal > .38 ? 2 : dx / horizontal < -.38 ? 0 : 1;
        int z = dz / horizontal > .38 ? 2 : dz / horizontal < -.38 ? 0 : 1;
        return z * 3 + x;
    }
    public static int matrixPattern(int cells) {
        int[] lines = {7, 56, 448, 73, 146, 292, 273, 84};
        for (int i = 0; i < lines.length; i++) if ((cells & lines[i]) == lines[i]) return i + 1;
        return 0;
    }
    public static int patternCells(int pattern) {
        int[] lines = {0, 7, 56, 448, 73, 146, 292, 273, 84};
        return pattern > 0 && pattern < lines.length ? lines[pattern] : 0;
    }
    private RadiationRules() {}
}
