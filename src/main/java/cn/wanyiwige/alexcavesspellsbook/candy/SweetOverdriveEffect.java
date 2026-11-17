package cn.wanyiwige.alexcavesspellsbook.candy;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.ai.attributes.*;

/** Iron cast-time and vanilla movement modifiers with stable UUIDs; no Citadel time change. */
public final class SweetOverdriveEffect extends MobEffect {
    public SweetOverdriveEffect(){super(MobEffectCategory.BENEFICIAL,0xFFB6D9);
        addAttributeModifier(Attributes.MOVEMENT_SPEED,"3d0db36a-f729-40a3-8c59-07df468b42c1",0.20,AttributeModifier.Operation.MULTIPLY_TOTAL);
        addAttributeModifier(AttributeRegistry.CAST_TIME_REDUCTION.get(),"76fc7a4c-810b-4685-a8c9-418631d67f98",0.10,AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
    @Override public double getAttributeModifierValue(int amplifier,AttributeModifier modifier){
        return modifier.getAmount()+(modifier.getId().toString().equals("3d0db36a-f729-40a3-8c59-07df468b42c1")?0.05:0.025)*Math.max(0,Math.min(4,amplifier));
    }
}
