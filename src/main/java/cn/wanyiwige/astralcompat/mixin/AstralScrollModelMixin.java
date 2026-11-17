package cn.wanyiwige.astralcompat.mixin;

import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.render.ScrollModel;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A unique model keeps old KubeJS resource overrides from restoring the retired art. */
@Mixin(value=ScrollModel.class,remap=false)
public abstract class AstralScrollModelMixin {
    @Inject(method="getScrollModelLocation",at=@At("HEAD"),cancellable=true,remap=false)
    private static void wan$astralScroll(SchoolType school,CallbackInfoReturnable<ResourceLocation> callback) {
        if(school.getId().equals(new ResourceLocation("kubejs","astral"))) {
            callback.setReturnValue(new ResourceLocation("asterismarcanum","item/arcanum_scroll_astral"));
        }
    }
}
