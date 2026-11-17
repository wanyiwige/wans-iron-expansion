package cn.wanyiwige.alexcavesspellsbook.mixin.client;

import com.github.alexmodguy.alexscaves.client.model.CaniacModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Access only the native arm parts; never substitute approximated geometry. */
@Mixin(value=CaniacModel.class,remap=false)
public interface CaniacModelAccessor {
    @Accessor(value="left_Arm",remap=false) AdvancedModelBox wan$getLeftArm();
    @Accessor(value="right_Arm",remap=false) AdvancedModelBox wan$getRightArm();
}
