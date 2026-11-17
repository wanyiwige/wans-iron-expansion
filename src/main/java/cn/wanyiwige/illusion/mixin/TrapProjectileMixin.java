package cn.wanyiwige.illusion.mixin;
import cn.wanyiwige.illusion.TrapCaster;
import cn.wanyiwige.illusion.IllusionEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=Projectile.class,remap=false)
public abstract class TrapProjectileMixin {
    @Inject(method={"getOwner","m_19749_"},remap=false,at=@At("RETURN"),cancellable=true)
    private void wan$owner(CallbackInfoReturnable<Entity> cir){
        if(cir.getReturnValue() instanceof TrapCaster caster&&caster.getSummoner()!=null){cir.setReturnValue(caster.getSummoner());return;}
        Entity self=(Entity)(Object)this;
        if((cir.getReturnValue()==null||cir.getReturnValue().isRemoved())&&self.getPersistentData().hasUUID(IllusionEvents.OWNER_KEY)&&self.level() instanceof net.minecraft.server.level.ServerLevel server){var owner=server.getServer().getPlayerList().getPlayer(self.getPersistentData().getUUID(IllusionEvents.OWNER_KEY));if(owner!=null)cir.setReturnValue(owner);}
    }
    @Inject(method={"canHitEntity","m_5603_"},at=@At("HEAD"),cancellable=true,remap=false)
    private void wan$friendly(Entity target,CallbackInfoReturnable<Boolean> cir){if(IllusionEvents.trapAlly((Entity)(Object)this,target))cir.setReturnValue(false);}
}
