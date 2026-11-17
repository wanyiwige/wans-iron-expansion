package cn.wanyiwige.twilight;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

final class TwilightVisuals {
    static void burst(ServerLevel level,Vec3 point,ParticleOptions particle,int count,double spread) {
        level.sendParticles(particle,point.x,point.y,point.z,count,spread,spread,spread,.05);
    }
    static void line(ServerLevel level,Vec3 start,Vec3 end,ParticleOptions particle,double step) {
        Vec3 difference=end.subtract(start);int count=Math.min(512,Math.max(1,(int)(difference.length()/step)));
        for(int i=0;i<=count;i++) { Vec3 p=start.add(difference.scale(i/(double)count));level.sendParticles(particle,p.x,p.y,p.z,1,0,0,0,0); }
    }
    static void ring(ServerLevel level,Vec3 center,float radius,ParticleOptions particle) {
        int count=Math.min(256,Math.max(24,(int)(radius*16)));
        for(int i=0;i<count;i++) { double angle=Math.PI*2*i/count;level.sendParticles(particle,center.x+Math.cos(angle)*radius,center.y,center.z+Math.sin(angle)*radius,1,0,0,0,.03); }
    }
    private TwilightVisuals() {}
}
