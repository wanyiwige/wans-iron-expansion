package cn.wanyiwige.alexcavesspellsbook.toxic.client;

import cn.wanyiwige.alexcavesspellsbook.toxic.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import java.util.*;

/**
 * VanillaDI-reference/pixelate.fsh uses BLOCKDIM=16 and voxel-centre sampling.
 * This compatible local surface pass follows that grid and quantizes point-light falloff.
 * It does not replace Minecraft core shaders or claim VanillaDI's ray-traced world lighting.
 */
final class VanillaDIPixelLight {
    private record Patch(Vec3 center,Vec3 normal,double distance){}
    private record Cache(long tick,List<Patch> patches){}
    private static final Map<ToxicEffectEntity,Cache> CACHE=new WeakHashMap<>();
    static void render(ToxicEffectEntity effect,Vec3 origin,PoseStack pose,MultiBufferSource buffers,int alpha) {
        if(alpha<=0)return;
        Vec3 direction=origin.subtract(effect.position()).normalize();origin=origin.subtract(direction.scale(.06));
        long tick=effect.level().getGameTime()/4;
        Cache old=CACHE.get(effect);
        if(old==null||old.tick!=tick) {
            List<Patch> patches=new ArrayList<>();
            for(Direction face:Direction.values()) {
                Vec3 ray=Vec3.atLowerCornerOf(face.getNormal());
                BlockHitResult hit=effect.level().clip(new ClipContext(origin,origin.add(ray.scale(2.2)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,effect));
                if(hit.getType()==HitResult.Type.BLOCK)patches.add(new Patch(hit.getLocation(),Vec3.atLowerCornerOf(hit.getDirection().getNormal()),origin.distanceTo(hit.getLocation())));
            }
            old=new Cache(tick,List.copyOf(patches));CACHE.put(effect,old);
        }
        var vertex=buffers.getBuffer(RenderType.entityTranslucentEmissive(ToxicRegistry.id("textures/entity/toxic/spark.png")));
        for(Patch patch:old.patches) {
            Vec3 n=patch.normal,u=Math.abs(n.y)>.5?new Vec3(1,0,0):n.cross(new Vec3(0,1,0)).normalize();
            Vec3 v=n.cross(u).normalize(),snapped=snap(patch.center);
            // Quantize only the two tangent axes; snapping the normal axis hides negative-facing patches inside blocks.
            Vec3 center=snapped.subtract(n.scale(snapped.subtract(patch.center).dot(n))).add(n.scale(.004)).subtract(effect.position());
            for(int x=-2;x<=2;x++)for(int y=-2;y<=2;y++) {
                double intensity=1/(1+patch.distance*patch.distance+(x*x+y*y)*.24);
                int opacity=(int)(Math.floor(intensity*4)/4*alpha);if(opacity<8)continue;
                Vec3 at=center.add(u.scale(x/16.0)).add(v.scale(y/16.0));
                Vec3 du=u.scale(1/32.0),dv=v.scale(1/32.0);
                ToxicEffectRenderer.quad(pose,vertex,at.subtract(du).subtract(dv),at.add(du).subtract(dv),at.add(du).add(dv),at.subtract(du).add(dv),0xD5F34B,opacity,true);
            }
        }
    }
    private static Vec3 snap(Vec3 p){return new Vec3((Math.floor(p.x*16)+.5)/16,(Math.floor(p.y*16)+.5)/16,(Math.floor(p.z*16)+.5)/16);}
    private VanillaDIPixelLight(){}
}
