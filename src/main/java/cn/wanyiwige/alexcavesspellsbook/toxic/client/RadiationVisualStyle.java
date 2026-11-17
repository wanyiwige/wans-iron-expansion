package cn.wanyiwige.alexcavesspellsbook.toxic.client;

/** Shared by legacy and new world effects; never changes item icons or sprite pixel grids. */
public final class RadiationVisualStyle {
    public static int opacity(int alpha){return Math.max(0,Math.min(255,Math.round(alpha*1.65F)));}
    public static int tint(String texture){
        return switch(texture){
            case "wake" -> 0x65DFFF;
            case "film","shell" -> 0x9EFF55;
            case "impact","burst","pulse","matrix" -> 0xD4FF45;
            default -> 0xB8FF45;
        };
    }
    private RadiationVisualStyle(){}
}
