package cn.wanyiwige.alexcavesspellsbook.mixin;
import cn.wanyiwige.alexcavesspellsbook.toxic.*;
import io.redspace.ironsspellbooks.api.config.*;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=SpellConfigManager.class,remap=false)
public abstract class ToxicSpellSchoolMixin {
    @Inject(method="getSpellConfigValue",at=@At("HEAD"),cancellable=true,remap=false)
    private static void wan$toxicSchool(AbstractSpell spell,SpellConfigParameter<?> parameter,CallbackInfoReturnable<Object> callback) {
        if(parameter==SpellConfigParameter.SCHOOL&&ToxicRegistry.SCHOOL.isPresent()&&spell.getSpellResource()!=null
                &&ToxicMigrations.SPELLS.contains(spell.getSpellResource().toString()))callback.setReturnValue(ToxicRegistry.SCHOOL.get());
    }
}
