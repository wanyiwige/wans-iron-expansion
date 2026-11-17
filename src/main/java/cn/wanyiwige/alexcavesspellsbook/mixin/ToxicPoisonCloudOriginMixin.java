package cn.wanyiwige.alexcavesspellsbook.mixin;
import io.redspace.ironsspellbooks.entity.spells.poison_arrow.PoisonArrow;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Record the real originating spell before the cloud is spawned, without guessing from the owner. */
@Mixin(targets={"io.redspace.ironsspellbooks.entity.spells.poison_arrow.PoisonArrow",
        "io.redspace.ironsspellbooks.entity.spells.poison_cloud.PoisonSplash"},remap=false)
public abstract class ToxicPoisonCloudOriginMixin {
    @Redirect(method="createPoisonCloud",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;m_7967_(Lnet/minecraft/world/entity/Entity;)Z",remap=false),remap=false,require=1,allow=1)
    private boolean wan$rememberCloudSpell(Level level,Entity child) {
        child.getPersistentData().putString("WanToxicOrigin",(Object)this instanceof PoisonArrow?"poison_arrow":"poison_splash");
        return level.addFreshEntity(child);
    }
}
