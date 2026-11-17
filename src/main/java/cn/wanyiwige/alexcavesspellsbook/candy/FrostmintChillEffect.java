package cn.wanyiwige.alexcavesspellsbook.candy;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.ai.attributes.*;
/** Bounded cold/slow marker; does not remove or overwrite external Slowness. */
public final class FrostmintChillEffect extends MobEffect {
    public FrostmintChillEffect(){super(MobEffectCategory.HARMFUL,0x6EDBC0);
        addAttributeModifier(Attributes.MOVEMENT_SPEED,"d5a66fda-37d7-4c81-90a6-4d8ab3f55dc1",-0.25,AttributeModifier.Operation.MULTIPLY_TOTAL);}
}
