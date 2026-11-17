package cn.wanyiwige.alexcavesspellsbook.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

/** Age-sampled four-frame spell particles authored on the native pixel grid. */
final class PureDarknessParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    PureDarknessParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz,
                        SpriteSet sprites, boolean cut) {
        this(level, x, y, z, dx, dy, dz, sprites, cut ? .15F : .065F, cut ? 0 : .18F, cut ? 7 : 12);
    }

    PureDarknessParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz,
                        SpriteSet sprites, float size, float gravity, int duration) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
        lifetime = duration;
        this.gravity = gravity;
        friction = .88F;
        quadSize = size;
        setSpriteFromAge(sprites);
    }

    @Override public void tick() {
        super.tick();
        if (isAlive()) setSpriteFromAge(sprites);
    }

    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
}
