package cn.wanyiwige.illusion.mixin;
import cn.wanyiwige.illusion.IllusionContent;
import io.redspace.ironsspellbooks.spells.ender.CounterspellSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=CounterspellSpell.class,remap=false)
public abstract class TrapCounterspellMixin {
    @Inject(method="onCast",at=@At("HEAD"),remap=false)
    private void wan$dispel(Level level,int rank,LivingEntity caster,CastSource source,MagicData data,CallbackInfo ci){
        if(level.isClientSide)return;Vec3 eye=caster.getEyePosition(),look=caster.getLookAngle();
        for(int i=0;i<160;i++){BlockPos pos=BlockPos.containing(eye.add(look.scale(i*.5)));var state=level.getBlockState(pos);if(state.is(IllusionContent.TRAP.get())){level.removeBlock(pos,false);level.playSound(null,pos,net.minecraft.sounds.SoundEvents.AMETHYST_CLUSTER_BREAK,net.minecraft.sounds.SoundSource.BLOCKS,1,.8F);break;}if(state.isSolidRender(level,pos))break;}
    }
}
