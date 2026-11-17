package cn.wanyiwige.alexcavesspellsbook.hazen;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Rarity;
import java.util.LinkedHashMap;
import java.util.Map;

/** Native Forge rarity styles retaining the original school's text colors. */
public final class HazenRarities {
    private static final Map<String,Rarity> VALUES=new LinkedHashMap<>();
    static {
        add("BLOOD",ChatFormatting.DARK_RED.getColor());add("DEUS",0xc4ffee);add("ELDRITCH",1016732);
        add("RADIANCE",0xe4a6ea);add("ENDER",ChatFormatting.LIGHT_PURPLE.getColor());add("SHADOW",0x553a7f);
        add("EVOCATION",ChatFormatting.WHITE.getColor());add("FIRE",ChatFormatting.GOLD.getColor());add("HOLY",16775380);
        add("ICE",13695487);add("LIGHTNING",ChatFormatting.AQUA.getColor());add("NATURE",ChatFormatting.GREEN.getColor());
        add("COSMIC",0x473196);add("HYDRO",0x275ffb);add("FIREBLOSSOM",0xee6646);
    }
    private static void add(String name,int color){VALUES.put(name+"_RARITY",Rarity.create("WANS_HAZEN_"+name,style->style.withColor(color)));}
    public static Rarity get(String source){
        if(VALUES.containsKey(source))return VALUES.get(source);
        return Rarity.valueOf(source);
    }
    private HazenRarities(){}
}
