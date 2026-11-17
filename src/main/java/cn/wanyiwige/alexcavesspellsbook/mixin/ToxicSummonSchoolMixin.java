package cn.wanyiwige.alexcavesspellsbook.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Only the two explicitly migrated summon species, including already saved instances. */
@Pseudo
@Mixin(targets={"com.inolia_zaicek.spirit_mediumship_spell.entity.alexcaves.SummonedBrainiac",
        "com.inolia_zaicek.spirit_mediumship_spell.entity.alexcaves.SummonedTremorzilla"},remap=false)
public abstract class ToxicSummonSchoolMixin {
    @Inject(method={"getSummonSchoolId","getDamageSchool"},at=@At("HEAD"),cancellable=true,remap=false)
    private void wan$toxicSummonSchool(CallbackInfoReturnable<String> callback){callback.setReturnValue("toxic");}
}
