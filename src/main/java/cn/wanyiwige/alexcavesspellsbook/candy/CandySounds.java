package cn.wanyiwige.alexcavesspellsbook.candy;

import com.github.alexmodguy.alexscaves.server.misc.ACSoundRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** Use native sound events; no client-side duplicate playback. */
public final class CandySounds {
    private CandySounds() { }
    public static void stage(ServerLevel level,CandyEffectEntity effect,CandyEffectEntity.Stage stage){
        if(effect.visual().getInt("LastProgram")==1){stageLast(level,effect,stage);return;}
        if(effect.visual().getInt("Program")==0){stage(level,effect.position(),effect.mode(),stage);return;}
        String id=effect.visual().getString("Spell");
        boolean hit=stage==CandyEffectEntity.Stage.IMPACT;
        SoundEvent sound;
        if(effect.visual().getInt("Role")==CandySpellEntity.SPEAR)sound=hit?ACSoundRegistry.FROSTMINT_SPEAR_HIT.get():ACSoundRegistry.FROSTMINT_SPEAR_THROW.get();
        else if(id.equals("caramel_bog"))sound=hit?ACSoundRegistry.CARAMEL_CUBE_ATTACK.get():ACSoundRegistry.CARAMEL_CUBE_SMALL.get();
        else if(id.startsWith("soda_"))sound=hit?ACSoundRegistry.PURPLE_SODA_UNSUBMERGE.get():ACSoundRegistry.PURPLE_SODA_SWIM.get();
        else if(id.startsWith("peppermint"))sound=hit?ACSoundRegistry.GUMBALL_HIT.get():ACSoundRegistry.SUGAR_STAFF_CAST_PEPPERMINT.get();
        else if(id.equals("licorice_hex_wave")||id.equals("sugar_seal_bloom"))sound=hit?ACSoundRegistry.LICOWITCH_CELEBRATE.get():ACSoundRegistry.SUGAR_STAFF_CAST_HEX.get();
        else sound=hit?ACSoundRegistry.GUMBALL_HIT.get():ACSoundRegistry.GUMBALL_LAUNCH.get();
        play(level,effect.position(),sound,stage==CandyEffectEntity.Stage.ACTIVE?0.3F:stage==CandyEffectEntity.Stage.END?0.2F:0.7F);
    }
    private static void stageLast(ServerLevel level,CandyEffectEntity effect,CandyEffectEntity.Stage stage){
        String id=effect.visual().getString("Spell");
        int role=effect.visual().getInt("LastRole");
        SoundEvent sound;
        if(role==1||role==2)sound=stage==CandyEffectEntity.Stage.IMPACT?ACSoundRegistry.GUMBALL_HIT.get():ACSoundRegistry.GUMBALL_LAUNCH.get();
        else if(role==3)sound=stage==CandyEffectEntity.Stage.IMPACT?ACSoundRegistry.FROSTMINT_SPEAR_HIT.get():ACSoundRegistry.FROSTMINT_SPEAR_THROW.get();
        else sound=switch(id){
            case "candicorn_charge" -> stage==CandyEffectEntity.Stage.START?ACSoundRegistry.CANDICORN_CHARGE_START.get():ACSoundRegistry.CANDICORN_GALLOP.get();
            case "gum_worm_breach" -> stage==CandyEffectEntity.Stage.IMPACT?ACSoundRegistry.GUM_WORM_ATTACK.get():ACSoundRegistry.GUM_WORM_DIG_STOP.get();
            case "gummy_guardian","gingerbread_march","licorice_grand_feast" -> ACSoundRegistry.LICOWITCH_CAST_SUMMON.get();
            case "candy_cane_whirl" -> ACSoundRegistry.SUGAR_STAFF_CAST_PEPPERMINT.get();
            case "rainbow_leap","rainbow_snare","candy_banquet" -> ACSoundRegistry.LICOWITCH_CAST_HEX.get();
            case "sweet_overdrive" -> ACSoundRegistry.LICOWITCH_CELEBRATE.get();
            case "ice_cream_storm" -> ACSoundRegistry.FROSTMINT_SPEAR_THROW.get();
            default -> ACSoundRegistry.GUMBALL_LAUNCH.get();
        };
        play(level,effect.position(),sound,stage==CandyEffectEntity.Stage.ACTIVE?0.25F:stage==CandyEffectEntity.Stage.END?0.2F:0.65F);
    }
    public static void stage(ServerLevel level, Vec3 pos, CandyEffectEntity.Mode mode, CandyEffectEntity.Stage stage) {
        SoundEvent sound = stage == CandyEffectEntity.Stage.IMPACT ? ACSoundRegistry.GUMBALL_HIT.get()
                : stage == CandyEffectEntity.Stage.END ? ACSoundRegistry.PURPLE_SODA_UNSUBMERGE.get()
                : mode == CandyEffectEntity.Mode.MINT ? ACSoundRegistry.LICOWITCH_CAST_PEPPERMINT.get()
                : mode == CandyEffectEntity.Mode.GLYPH ? ACSoundRegistry.LICOWITCH_CAST_HEX.get()
                : mode == CandyEffectEntity.Mode.SUMMON ? ACSoundRegistry.LICOWITCH_CAST_SUMMON.get()
                : mode == CandyEffectEntity.Mode.ROCKET ? ACSoundRegistry.PURPLE_SODA_SWIM.get()
                : ACSoundRegistry.GUMBALL_LAUNCH.get();
        play(level, pos, sound, stage == CandyEffectEntity.Stage.ACTIVE ? 0.3F : 0.7F);
    }
    public static void bounce(ServerLevel level, Vec3 pos) { play(level, pos, ACSoundRegistry.GUMBALL_BOUNCE.get(), 0.5F); }
    private static void play(ServerLevel level, Vec3 pos, SoundEvent sound, float volume) {
        level.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, volume, 1);
    }
}
