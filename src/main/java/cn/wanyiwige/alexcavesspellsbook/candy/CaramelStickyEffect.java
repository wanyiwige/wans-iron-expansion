package cn.wanyiwige.alexcavesspellsbook.candy;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Separate two-tick zone constraint: leaving caramel never removes somebody else's Slowness. */
public final class CaramelStickyEffect extends MobEffect {
    public CaramelStickyEffect() {
        super(MobEffectCategory.HARMFUL,0xBD795C);
        addAttributeModifier(Attributes.MOVEMENT_SPEED,"f7f5622a-7b08-4e68-adeb-f1584d645240",-0.45,AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
