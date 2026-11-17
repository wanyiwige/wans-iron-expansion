package cn.wanyiwige.alexcavesspellsbook.hazen.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.RenderUtils;
import java.util.List;
import java.util.function.Function;

/** GeckoLib armor with the source models' three auxiliary torso/leg attachments. */
public class AddedArmorAttachmentRenderer<T extends Item & GeoItem> extends GeoArmorRenderer<T> {
    private final List<Attachment> attachments=List.of(
            new Attachment("armorLeggingTorsoLayer",EquipmentSlot.LEGS,m->m.body,Vec3.ZERO),
            new Attachment("armorTorsoExtensionRightLeg",EquipmentSlot.CHEST,m->m.rightLeg,new Vec3(2,12,0)),
            new Attachment("armorTorsoExtensionLeftLeg",EquipmentSlot.CHEST,m->m.leftLeg,new Vec3(-2,12,0)));
    public AddedArmorAttachmentRenderer(GeoModel<T> model){super(model);}
    @Override protected void grabRelevantBones(BakedGeoModel baked){
        if(lastModel!=baked)for(var a:attachments)a.bone=model.getBone(a.name).orElse(null);
        super.grabRelevantBones(baked);
    }
    @Override protected void applyBoneVisibilityBySlot(EquipmentSlot slot){
        super.applyBoneVisibilityBySlot(slot);for(var a:attachments)setBoneVisible(a.bone,slot==a.slot);
    }
    @Override public void applyBoneVisibilityByPart(EquipmentSlot slot,ModelPart part,HumanoidModel<?> model){
        super.applyBoneVisibilityByPart(slot,part,model);for(var a:attachments)setBoneVisible(a.bone,part==a.follow.apply(model));
    }
    @Override protected void applyBaseTransformations(HumanoidModel<?> base){
        super.applyBaseTransformations(base);
        for(var a:attachments)if(a.bone!=null){var part=a.follow.apply(base);RenderUtils.matchModelPartRot(part,a.bone);a.bone.updatePosition((float)a.offset.x+part.x,(float)a.offset.y-part.y,(float)a.offset.z+part.z);}
    }
    @Override public void setAllVisible(boolean visible){super.setAllVisible(visible);if(attachments!=null)for(var a:attachments)setBoneVisible(a.bone,visible);}
    private static final class Attachment {
        final String name;final EquipmentSlot slot;final Function<HumanoidModel<?>,ModelPart> follow;final Vec3 offset;GeoBone bone;
        Attachment(String name,EquipmentSlot slot,Function<HumanoidModel<?>,ModelPart> follow,Vec3 offset){this.name=name;this.slot=slot;this.follow=follow;this.offset=offset;}
    }
}
