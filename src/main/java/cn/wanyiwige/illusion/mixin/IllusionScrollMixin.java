package cn.wanyiwige.illusion.mixin;
import cn.wanyiwige.illusion.IllusionContent;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.render.ScrollModel;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=ScrollModel.class,remap=false)
public abstract class IllusionScrollMixin {
    @Inject(method="getScrollModelLocation",at=@At("HEAD"),cancellable=true,remap=false)
    private static void wan$illusion(SchoolType school,CallbackInfoReturnable<ResourceLocation> cir){if(school.getId().equals(IllusionContent.id("illusion")))cir.setReturnValue(IllusionContent.id("item/scroll_illusion"));}
}
