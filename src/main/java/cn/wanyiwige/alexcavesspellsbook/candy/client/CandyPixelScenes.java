package cn.wanyiwige.alexcavesspellsbook.candy.client;

import cn.wanyiwige.alexcavesspellsbook.candy.CandyEffectEntity;
import cn.wanyiwige.alexcavesspellsbook.candy.CandyFirstRules;
import cn.wanyiwige.alexcavesspellsbook.candy.CandyRegistry;
import cn.wanyiwige.alexcavesspellsbook.candy.CandySpellEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Pixel scenes for all 26 spells. Events come from the server. */
public final class CandyPixelScenes {
    public static final List<ResourceLocation> MODELS = List.of(
        model("rainbow_hard_candy"), model("sugar_hunt_filled_candy"), model("crystal_split_candy"),
        model("popping_cluster_candy"), model("peppermint_guard"));
    public static final List<ResourceLocation> RAINBOW_MODELS = List.of(
        model("rainbow_hard_candy_rose"),model("rainbow_hard_candy_orange"),model("rainbow_hard_candy_lemon"),
        model("rainbow_hard_candy_green"),model("rainbow_hard_candy_blue"),model("rainbow_hard_candy_purple"));
    public static final List<ResourceLocation> MINT_MODELS = List.of(
        model("peppermint_guard_rose"),model("peppermint_guard_orange"),model("peppermint_guard_lemon"),
        model("peppermint_guard_green"),model("peppermint_guard_blue"),model("peppermint_guard_purple"));
    public static final List<ResourceLocation> ICE_MODELS=List.of(
        model("icecream_scoop_vanilla"),model("icecream_scoop_strawberry"),model("icecream_scoop_chocolate"),
        model("icecream_scoop_mint"),model("icecream_scoop_blueberry"),model("icecream_scoop_grape"));
    private static int mintColor(CandyEffectEntity e) {
        if(e.visual().contains("PixelPalette"))return Math.floorMod(e.visual().getInt("PixelPalette"),MINT_MODELS.size());
        int cast=e.visual().hasUUID("Parent")?e.visual().getUUID("Parent").hashCode():0;
        return Math.floorMod(Math.floorMod(cast,MINT_MODELS.size())+e.visual().getInt("Index"),MINT_MODELS.size());
    }
    private static int candyColor(CandyEffectEntity e) {
        if(e.visual().contains("PixelPalette"))return Math.floorMod(e.visual().getInt("PixelPalette"),RAINBOW_MODELS.size());
        int cast=e.visual().hasUUID("Parent")?e.visual().getUUID("Parent").hashCode():0;
        return Math.floorMod(Math.floorMod(cast,RAINBOW_MODELS.size())+e.visual().getInt("Index")*2,RAINBOW_MODELS.size());
    }
    private static final int[] RAINBOW = {0xFF729B,0xFFB253,0xFFF1A1,0x8CDFAB,0x72C9F2,0xAE8BE4};
    private static final Map<CandyEffectEntity, History> HISTORY = new WeakHashMap<>();
    private static final Map<String,ResourceLocation> TEXTURES = new java.util.HashMap<>();
    private static final class History {
        final ArrayDeque<Vec3> points = new ArrayDeque<>();
        final ArrayDeque<Hit> hits = new ArrayDeque<>();
        int tick, sequence;
        Vec3 last;
    }
    private record Hit(Vec3 point, Vec3 normal, int tick, int kind, int sequence) { }
    private CandyPixelScenes() { }
    private static ResourceLocation model(String name) { return CandyRegistry.id("candy/"+name); }
    private static ResourceLocation texture(String name) {
        return TEXTURES.computeIfAbsent(name,n->CandyRegistry.id("textures/entity/candy/pixel/"+n+".png"));
    }
    public static boolean handles(String id) {
        return switch(id) {
            case "rainbow_ricochet","sugar_hunt","crystal_split","popping_cluster","peppermint_guard",
                 "peppermint_hunt","peppermint_wheels","licorice_hex_wave","sugar_seal_bloom","frostmint_rain",
                 "soda_meteor","soda_frostburst","caramel_bog","sugar_glass_stars","rainbow_leap",
                 "sweet_overdrive","rainbow_snare","candicorn_charge","gummy_guardian","gingerbread_march",
                 "candy_cane_whirl","gumball_sentry","gum_worm_breach","candy_banquet","ice_cream_storm","licorice_grand_feast" -> true;
            default -> false;
        };
    }
    private static int index(String id) {
        return switch(id) {case "sugar_hunt"->1;case "crystal_split"->2;case "popping_cluster"->3;case "peppermint_guard","peppermint_hunt","peppermint_wheels"->4;default->0;};
    }
    private static int color(int index) {
        return switch(index) {case 1->0xFFC36B;case 2->0x9FE9EC;case 3->0xFF8D79;case 4->0xFF8EAD;default->0xFF98BC;};
    }
    /** Tick sampling is independent of FPS and the generic particle budget. */
    public static void tick(CandyEffectEntity e) {
        if(!handles(e.visual().getString("Spell")))return;
        String spell=e.visual().getString("Spell");
        if(spell.equals("gum_worm_breach")){CandyNativeWormScene.tick(e);return;}
        if((spell.equals("candy_cane_whirl")||spell.equals("gumball_sentry")||spell.equals("candy_banquet")||spell.equals("ice_cream_storm")||spell.equals("licorice_grand_feast"))&&e.visual().getInt("LastRole")==0)return;
        if(spell.equals("sweet_overdrive")||spell.equals("rainbow_snare")||spell.equals("gummy_guardian")||spell.equals("gingerbread_march"))return;
        if(CandyFieldPixelScenes.handles(e.visual().getString("Spell")))return;
        if(e.visual().getString("Spell").equals("caramel_bog")||e.visual().getString("Spell").equals("rainbow_leap"))return;
        var mc=Minecraft.getInstance();
        if(mc.gameRenderer.getMainCamera().getPosition().distanceToSqr(e.position())>64*64){HISTORY.remove(e);return;}
        History h=HISTORY.computeIfAbsent(e,key->new History());h.tick++;
        if(h.last!=null && h.last.distanceToSqr(e.position())>16*16)h.points.clear();
        h.points.addFirst(e.position());while(h.points.size()>9)h.points.removeLast();h.last=e.position();
        int sequence=e.visual().getInt("PixelHitSequence");
        if(sequence!=h.sequence && sequence>0){
            h.sequence=sequence;
            var s=e.visual();
            h.hits.addFirst(new Hit(new Vec3(s.getDouble("PixelHitX"),s.getDouble("PixelHitY"),s.getDouble("PixelHitZ")),
                new Vec3(s.getFloat("PixelNormalX"),s.getFloat("PixelNormalY"),s.getFloat("PixelNormalZ")),h.tick,s.getInt("PixelHitKind"),sequence));
            while(h.hits.size()>3)h.hits.removeLast();
        }
        while(!h.hits.isEmpty() && h.tick-h.hits.peekLast().tick()>10)h.hits.removeLast();
    }
    public static boolean render(CandyEffectEntity e,float age,float partial,PoseStack p,MultiBufferSource buffers,int light) {
        String id=e.visual().getString("Spell");if(!handles(id))return false;
        if(CandyFinalPixelScenes.render(e,age,partial,p,buffers,light))return true;
        if(CandySupportPixelScenes.render(e,age,partial,p,buffers,light))return true;
        if(CandyDessertPixelScenes.render(e,age,partial,p,buffers,light))return true;
        if(CandyFieldPixelScenes.render(e,age,p,buffers,light))return true;
        int index=index(id),role=e.visual().getInt("Role");
        boolean near=CandyVisuals.detail(e);
        float end=e.stage()==CandyEffectEntity.Stage.END?Mth.clamp((age-e.visual().getInt("StageAge"))/8F,0,1):0;
        int alpha=(int)(255*(1-end));
        History h=HISTORY.get(e);
        Vec3 renderPosition=new Vec3(Mth.lerp(partial,e.xo,e.getX()),Mth.lerp(partial,e.yo,e.getY()),Mth.lerp(partial,e.zo,e.getZ()));
        if(role==CandySpellEntity.CONTROL){
            // This is the release cue after the cast, not an invented long charge animation.
            if(age<9){float t=age/9F;sprite(p,buffers,"spark",Vec3.ZERO,.12+t*.14,color(index),(int)(200*(1-t)),age*.12F);}
            return true;
        }
        boolean exploded=index==3&&e.visual().getInt("PixelDetonated")==1;
        boolean fractured=index==2&&e.visual().getInt("PixelFractured")==1;
        if(!exploded&&!fractured&&end<1){
            p.pushPose();
            if(id.equals("peppermint_wheels")){
                Vec3 velocity=e.getDeltaMovement();
                p.mulPose(Axis.YP.rotation((float)Math.atan2(velocity.x,velocity.z)+(float)Math.PI/2));
                p.mulPose(Axis.ZP.rotation(-age*.65F));
            }else if(id.equals("peppermint_hunt")){
                Vec3 velocity=e.getDeltaMovement();
                p.mulPose(Axis.YP.rotation(e.visual().getInt("Orbiting")==1?age*.18F:(float)Math.atan2(velocity.x,velocity.z)));
                p.mulPose(Axis.ZP.rotation(age*.65F));
            }else if(index==4){
                p.mulPose(Axis.YP.rotation(age*.24F+e.getId()));p.mulPose(Axis.XP.rotationDegrees(22));p.mulPose(Axis.ZP.rotation(age*.13F));
            }
            else {p.mulPose(Axis.YP.rotation(age*.085F+e.getId()));p.mulPose(Axis.XP.rotation(age*.055F));p.mulPose(Axis.ZP.rotationDegrees(12));}
            float scale=index==4?1F:e.visual().getInt("PixelShard")==1?.63F:.92F;
            if(index==3){int fuse=e.visual().getInt("Fuse");scale*=1+.045F*Mth.sin(fuse*(.3F+fuse*.012F));}
            scale*=1-end;p.scale(scale,scale,scale);p.translate(-.5,-.5,-.5);
            var mc=Minecraft.getInstance();
            var baked=mc.getModelManager().getModel(index==0?RAINBOW_MODELS.get(candyColor(e)):index==4?MINT_MODELS.get(mintColor(e)):MODELS.get(index));
            mc.getBlockRenderer().getModelRenderer().renderModel(p.last(),buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS)),null,baked,1,1,1,light,OverlayTexture.NO_OVERLAY);
            p.popPose();
            if(index==3){
                int fuse=e.visual().getInt("Fuse"),limit=CandyFirstRules.fuseTick(e.visual().getInt("Index"));
                float urgency=Mth.clamp((float)fuse/limit,0,1);
                if((fuse/(urgency>.75?2:5))%2==0)sprite(p,buffers,"spark",new Vec3(.06,.21,0),.07+urgency*.07,0xFFF0B2,alpha,age*.1F);
            }
        }
        if(h!=null){
            if(near&&!exploded&&!fractured)trail(e,h,renderPosition,p,buffers,index,alpha);
            for(Hit hit:h.hits)hit(p,buffers,hit,renderPosition,h.tick+partial,index,near);
        }
        if(near&&index==4&&alpha>0){
            if(id.equals("peppermint_hunt")&&e.visual().getInt("Orbiting")==0)
                sprite(p,buffers,"mint_slash",Vec3.ZERO,.38,RAINBOW[mintColor(e)],alpha/2,age*.3F);
            if(id.equals("peppermint_wheels")){
                float yaw=(float)Math.atan2(e.getDeltaMovement().x,e.getDeltaMovement().z);
                p.pushPose();p.mulPose(Axis.YP.rotation(yaw));
                plane(p,buffers,"wheel_track",new Vec3(0,-.30,0),new Vec3(1,0,0),new Vec3(0,0,1),.18,RAINBOW[mintColor(e)],alpha/2);
                p.popPose();
            }
        }
        if(e.stage()==CandyEffectEntity.Stage.END&&!exploded&&near){
            for(int i=0;i<3;i++){
                double a=i*2.094+e.getId();Vec3 at=new Vec3(Math.cos(a)*end*.35,end*.25,Math.sin(a)*end*.35);
                sprite(p,buffers,"crumb",at,.035*(1-end),color(index),alpha,age*.15F+i);
            }
        }
        return true;
    }
    private static void trail(CandyEffectEntity e,History h,Vec3 origin,PoseStack p,MultiBufferSource b,int index,int alpha) {
        List<Vec3> points=new ArrayList<>(h.points);
        if(points.size()<2)return;
        Vec3 previous=Vec3.ZERO;
        for(int i=1;i<points.size();i++){
            Vec3 next=points.get(i).subtract(origin);
            float taper=1-i/(float)points.size();int a=(int)(alpha*taper*.68F);
            int c=index==0?RAINBOW[(i-1)%6]:index==4?RAINBOW[mintColor(e)]:color(index);
            ribbon(p,b,previous,next,(index==4?.035:.045)*taper,c,a);
            if(i%2==0)sprite(p,b,"crumb",next,.025*taper,c,a,i*.7F);
            previous=next;
        }
    }
    private static void hit(PoseStack p,MultiBufferSource b,Hit hit,Vec3 origin,float now,int index,boolean near) {
        float t=now-hit.tick();if(t<0||t>=10)return;
        float progress=t/10F,fade=1-progress;Vec3 centre=hit.point().subtract(origin);
        boolean explosion=hit.kind()==2;
        int frame=Math.min(3,(int)(t/(explosion?2F:1.5F)));
        double size=explosion?.25+progress*1.6:.10+progress*.45;
        sprite(p,b,"burst_"+frame,centre,size,explosion?0xFFD38F:color(index),(int)(255*fade),hit.sequence()*.37F);
        Vec3 n=hit.normal();
        if(n.lengthSqr()>.1 && !explosion){
            Vec3 side=n.cross(new Vec3(0,1,0));if(side.lengthSqr()<.01)side=n.cross(new Vec3(1,0,0));side=side.normalize();
            Vec3 up=n.cross(side).normalize();
            plane(p,b,"burst_"+frame,centre.add(n.scale(.025)),side,up,size,color(index),(int)(190*fade));
        }
        int pieces=near?(explosion?10:5):2;
        for(int i=0;i<pieces;i++){
            double angle=i*2.399963+hit.sequence();
            Vec3 dir=new Vec3(Math.cos(angle),.4+(i%3)*.3,Math.sin(angle));
            if(n.lengthSqr()>.1 && dir.dot(n)<0)dir=dir.subtract(n.scale(2*dir.dot(n)));
            Vec3 at=centre.add(dir.scale(progress*(explosion?1.7:.55))).add(0,-progress*progress*.4,0);
            sprite(p,b,"crumb",at,(explosion?.065:.04)*fade,color(index),(int)(230*fade),i+t*.2F);
        }
        if(explosion&&near)for(int i=0;i<3;i++){
            Vec3 at=centre.add(Math.cos(i*2.094)*progress*.7,progress*.8,Math.sin(i*2.094)*progress*.7);
            sprite(p,b,"smoke",at,.18+progress*.4,0xC38799,(int)(120*fade),i*.8F);
        }
        if(index==1&&t<4)sprite(p,b,"turn",centre.add(0,.12,0),.16,0xFFF0B6,(int)(220*fade),hit.sequence());
    }
    static void sprite(PoseStack p,MultiBufferSource b,String tex,Vec3 at,double size,int color,int alpha,float rotation) {
        if(alpha<=0||size<=0)return;
        p.pushPose();p.translate(at.x,at.y,at.z);p.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());p.mulPose(Axis.ZP.rotation(rotation));
        quad(p,b,tex,new Vec3(-size,-size,0),new Vec3(size,-size,0),new Vec3(size,size,0),new Vec3(-size,size,0),color,alpha);p.popPose();
    }
    static void plane(PoseStack p,MultiBufferSource b,String tex,Vec3 centre,Vec3 side,Vec3 up,double size,int color,int alpha) {
        side=side.scale(size);up=up.scale(size);
        quad(p,b,tex,centre.subtract(side).subtract(up),centre.add(side).subtract(up),centre.add(side).add(up),centre.subtract(side).add(up),color,alpha);
    }
    static void pathTrail(CandyEffectEntity e,float partial,PoseStack p,MultiBufferSource b,int color,int alpha){
        History h=HISTORY.get(e);if(h==null)return;
        Vec3 origin=new Vec3(Mth.lerp(partial,e.xo,e.getX()),Mth.lerp(partial,e.yo,e.getY()),Mth.lerp(partial,e.zo,e.getZ()));
        Vec3 last=Vec3.ZERO;int i=0;
        for(Vec3 point:h.points){
            if(i++==0)continue;
            float fade=1-i/10F;Vec3 next=point.subtract(origin);
            ribbon(p,b,last,next,.05*fade,color,(int)(alpha*fade));last=next;
        }
    }
    static List<Vec3> trailPoints(CandyEffectEntity e,float partial){
        History h=HISTORY.get(e);if(h==null)return List.of();
        Vec3 origin=new Vec3(Mth.lerp(partial,e.xo,e.getX()),Mth.lerp(partial,e.yo,e.getY()),Mth.lerp(partial,e.zo,e.getZ()));
        List<Vec3> points=new ArrayList<>();
        for(Vec3 point:h.points)points.add(point.subtract(origin));
        return points;
    }
    static void ribbon(PoseStack p,MultiBufferSource b,Vec3 from,Vec3 to,double width,int color,int alpha) {
        Vec3 delta=to.subtract(from);if(delta.lengthSqr()<.0001||delta.lengthSqr()>64)return;
        Vec3 side=delta.normalize().cross(new Vec3(0,1,0));if(side.lengthSqr()<.001)side=delta.normalize().cross(new Vec3(1,0,0));
        side=side.normalize().scale(width);Vec3 up=delta.normalize().cross(side).normalize().scale(width);
        quad(p,b,"trail",from.add(side),from.subtract(side),to.subtract(side),to.add(side),color,alpha);
        quad(p,b,"trail",from.add(up),from.subtract(up),to.subtract(up),to.add(up),color,alpha);
    }
    private static void quad(PoseStack p,MultiBufferSource b,String tex,Vec3 a,Vec3 c,Vec3 d,Vec3 f,int color,int alpha) {
        VertexConsumer v=b.getBuffer(RenderType.entityTranslucentEmissive(texture(tex)));
        vertex(p,v,a,color,alpha,0,1);vertex(p,v,c,color,alpha,1,1);vertex(p,v,d,color,alpha,1,0);vertex(p,v,f,color,alpha,0,0);
    }
    private static void vertex(PoseStack p,VertexConsumer v,Vec3 at,int color,int alpha,float u,float w) {
        v.vertex(p.last().pose(),(float)at.x,(float)at.y,(float)at.z).color(color>>16&255,color>>8&255,color&255,Mth.clamp(alpha,0,255)).uv(u,w)
            .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(p.last().normal(),0,1,0).endVertex();
    }
}
