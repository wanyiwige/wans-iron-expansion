package cn.wanyiwige.alexcavesspellsbook.mixin;
import cn.wanyiwige.alexcavesspellsbook.toxic.ToxicRegistry;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="com.inolia_zaicek.spirit_mediumship_spell.util.SummonScaling",remap=false)
public abstract class ToxicSummonPowerMixin {
    @Inject(method="getSchoolPower",at=@At("HEAD"),cancellable=true,remap=false)
    private static void wan$resolveToxicSchool(LivingEntity owner,String school,CallbackInfoReturnable<Double> callback) {
        if("toxic".equals(school)&&ToxicRegistry.POWER.isPresent())callback.setReturnValue(owner==null?1D:Math.max(0,owner.getAttributeValue(ToxicRegistry.POWER.get())));
    }
    @Inject(method="getSchoolPowerValue",at=@At("HEAD"),cancellable=true,remap=false)
    private static void wan$resolveToxicAttribute(LivingEntity owner,String school,CallbackInfoReturnable<Double> callback) {
        if("toxic".equals(school)&&ToxicRegistry.POWER.isPresent())callback.setReturnValue(owner==null?1D:Math.max(0,owner.getAttributeValue(ToxicRegistry.POWER.get())));
    }
}
