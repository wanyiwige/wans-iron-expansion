package cn.wanyiwige.twilight.mixin;
import cn.wanyiwige.twilight.TwilightFoundation;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.render.ScrollModel;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=ScrollModel.class,remap=false)
public abstract class TwilightScrollModelMixin {
    @Inject(method="getScrollModelLocation",at=@At("HEAD"),cancellable=true,remap=false)
    private static void twilight$schoolScroll(SchoolType school,CallbackInfoReturnable<ResourceLocation> callback){if(school.getId().equals(TwilightFoundation.id("exanimated")))callback.setReturnValue(TwilightFoundation.id("item/scroll_exanimated"));}
}
