package cn.wanyiwige.alexcavesspellsbook.candy.client;

import cn.wanyiwige.alexcavesspellsbook.candy.*;
import com.github.alexmodguy.alexscaves.client.particle.ACParticleRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;

/** Client-only budgeted native particles. Rainbow geometry has its own renderer. */
public final class CandyVisuals {
    public static final int CREAM = 0xFFD0D8, BERRY = 0xDB397D, MINT = 0x6EDBC0;
    public static final int[] COLORS = {0xFF658B,0xFFB65C,0xFFE78A,0xA7E881,0x6EDBC0,0x69C8EA,
            0x7193EA,0xAB84E8,0xDA8FDA,0xFFB6D9,0xFFF1E3,0xBD795C};
    private CandyVisuals() { }
    public static boolean detail(CandyEffectEntity entity) {
        return Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(entity.position()) < 32*32
                && Minecraft.getInstance().options.particles().get() != ParticleStatus.MINIMAL;
    }
    public static int layers(CandyEffectEntity entity) { return detail(entity) ? CandyConfig.TRANSPARENT_LAYERS.get() : 1; }
    public static int particles(CandyEffectEntity entity, int remaining) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level != entity.level() || minecraft.gameRenderer.getMainCamera().getPosition().distanceToSqr(entity.position()) > 64*64) return 0;
        if(entity.visual().getInt("Program")==1&&entity.visual().getInt("Role")==CandySpellEntity.CONTROL&&entity.stage()==CandyEffectEntity.Stage.ACTIVE)return 0;
        int count = Math.min(Math.max(0,remaining),Math.min(CandyConfig.PARTICLES_PER_TICK.get(), detail(entity) ? 4 : 1));
        ParticleOptions particle = switch (entity.mode()) {
            case ROCKET -> ACParticleRegistry.PURPLE_SODA_BUBBLE.get();
            case GLYPH -> ACParticleRegistry.PURPLE_WITCH_MAGIC.get();
            case GROUND -> ACParticleRegistry.CARAMEL_DROP.get();
            case SUMMON -> ACParticleRegistry.WITCH_COOKIE.get();
            default -> ACParticleRegistry.SUGAR_FLAKE.get();
        };
        if (entity.visual().getString("Spell").equals("ice_cream_storm")) particle = ACParticleRegistry.ICE_CREAM_DRIP.get();
        if(entity.visual().getInt("LastProgram")==1){
            String spell=entity.visual().getString("Spell");
            if(spell.equals("licorice_grand_feast")||spell.equals("gum_worm_breach"))particle=ACParticleRegistry.PURPLE_WITCH_MAGIC.get();
            else if(spell.equals("ice_cream_storm"))particle=ACParticleRegistry.ICE_CREAM_DRIP.get();
            else if(spell.equals("candy_banquet")||spell.equals("rainbow_snare"))particle=ACParticleRegistry.WITCH_COOKIE.get();
            else particle=ACParticleRegistry.SUGAR_FLAKE.get();
        }
        if(entity.visual().getInt("Role")==CandySpellEntity.SPEAR)particle=net.minecraft.core.particles.ParticleTypes.SNOWFLAKE;
        if (entity.stage() == CandyEffectEntity.Stage.END) particle = ACParticleRegistry.SUGAR_FLAKE.get();
        Vec3 pos = entity.position();
        for (int i=0;i<count;i++) {
            double angle = (entity.tickCount+i*3)*1.618;
            entity.level().addParticle(particle, pos.x+Math.cos(angle)*0.2, pos.y+0.1, pos.z+Math.sin(angle)*0.2, 0, 0.015, 0);
        }
        // CANDICORN_CHARGE and PLAYER_RAINBOW require concrete native owner types; never feed this entity's ID.
        return count;
    }
}
