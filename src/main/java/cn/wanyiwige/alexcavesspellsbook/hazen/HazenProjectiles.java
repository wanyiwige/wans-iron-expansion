package cn.wanyiwige.alexcavesspellsbook.hazen;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class HazenProjectiles {
    public static HazenProjectile create(String kind, LivingEntity caster, float damage) {
        var bolt = HazenEntities.BOLTS.get(kind).get().create(caster.level());
        if (bolt == null) throw new IllegalStateException("Could not create " + kind);
        bolt.setOwner(caster); bolt.setDamage(damage);
        return bolt;
    }
    public static void throwKnives(Player player, HazenWeaponItem item) {
        float damage = (float)(2 + 2*(player.getAttributeValue(AttributeRegistry.BLOOD_SPELL_POWER.get())
                + player.getAttributeValue(AttributeRegistry.SPELL_POWER.get())));
        Vec3 look = player.getLookAngle();
        for (int i = 0; i < 5; i++) {
            var knife = create("vampire_knife", player, damage);
            knife.setPos(player.getEyePosition().subtract(0, .1, 0));
            knife.shoot(look.x + player.getRandom().nextGaussian()*.2,
                    look.y + player.getRandom().nextGaussian()*.2, look.z + player.getRandom().nextGaussian()*.2, 1.5f, 0);
            player.level().addFreshEntity(knife);
        }
        HazenSounds.play(player.level(), player.position(), "vampire_knife_throw", 1, 1);
        player.getCooldowns().addCooldown(item, 40);
    }
    private HazenProjectiles() {}
}
