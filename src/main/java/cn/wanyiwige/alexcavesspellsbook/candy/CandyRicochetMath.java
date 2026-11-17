package cn.wanyiwige.alexcavesspellsbook.candy;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Surface reflection for a swept entity hit, preserving projectile speed. */
public final class CandyRicochetMath {
    private CandyRicochetMath() { }
    public static Vec3 normal(AABB box,Vec3 contact,Vec3 incoming){
        Vec3[] normals={new Vec3(-1,0,0),new Vec3(1,0,0),new Vec3(0,-1,0),new Vec3(0,1,0),new Vec3(0,0,-1),new Vec3(0,0,1)};
        double[] distances={Math.abs(contact.x-box.minX),Math.abs(contact.x-box.maxX),Math.abs(contact.y-box.minY),Math.abs(contact.y-box.maxY),Math.abs(contact.z-box.minZ),Math.abs(contact.z-box.maxZ)};
        double nearest=Double.POSITIVE_INFINITY;Vec3 result=null;
        for(int i=0;i<normals.length;i++)if(incoming.dot(normals[i])<-1e-9&&distances[i]<nearest){
            nearest=distances[i];result=normals[i];
        }
        return result!=null?result:incoming.lengthSqr()>1e-12?incoming.normalize().scale(-1):new Vec3(0,1,0);
    }
    public static Vec3 reflect(Vec3 incoming,Vec3 normal){
        return incoming.subtract(normal.scale(2*incoming.dot(normal)));
    }
}
