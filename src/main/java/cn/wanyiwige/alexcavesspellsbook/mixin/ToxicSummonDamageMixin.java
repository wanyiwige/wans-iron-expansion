package cn.wanyiwige.alexcavesspellsbook.mixin;
import cn.wanyiwige.alexcavesspellsbook.toxic.ToxicRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

@Pseudo
@Mixin(targets="com.inolia_zaicek.spirit_mediumship_spell.event.SummonDamageEventHandler",remap=false)
public abstract class ToxicSummonDamageMixin {
    @Inject(method="resolveDamageType",at=@At("HEAD"),cancellable=true,remap=false)
    private static void wan$resolveToxicDamage(String school,CallbackInfoReturnable<ResourceKey<DamageType>> callback) {
        if("toxic".equals(school))callback.setReturnValue(ToxicRegistry.DAMAGE);
    }
    @Inject(method="onLivingHurt",at=@At("HEAD"),cancellable=true,remap=false)
    private static void wan$avoidSecondSummonConversion(LivingHurtEvent event,CallbackInfo callback) {
        if(event.getSource() instanceof SpellDamageSource source) {
            String id=source.spell().getSpellResource().toString();
            if(id.equals("spirit_mediumship_spell:mediumship_alexcaves_brainiac")
                    ||id.equals("spirit_mediumship_spell:mediumship_alexcaves_tremorzilla"))callback.cancel();
        }
    }
    @Redirect(method="onLivingHurt",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z",remap=false),remap=false,require=1,allow=1)
    private static boolean wan$attributeToxicSummon(LivingEntity target,DamageSource source,float amount) {
        if(source.is(ToxicRegistry.DAMAGE)&&source.getEntity() instanceof LivingEntity owner&&source.getDirectEntity()!=null) {
            String path=net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(source.getDirectEntity().getType()).getPath();
            String id=path.contains("tremorzilla")?"mediumship_alexcaves_tremorzilla":"mediumship_alexcaves_brainiac";
            var spell=SpellRegistry.getSpell(ResourceLocation.fromNamespaceAndPath("spirit_mediumship_spell",id));
            return DamageSources.applyDamage(target,amount,spell.getDamageSource(source.getDirectEntity(),owner));
        }
        return target.hurt(source,amount);
    }
}
