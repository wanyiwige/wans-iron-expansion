package cn.wanyiwige.alexcavesspellsbook.gametest;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.alexcavesspellsbook.entities.DesolateBloodbladeEntity;
import cn.wanyiwige.alexcavesspellsbook.spells.DesolateBloodbladeSpell;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AlexCavesSpellsbook.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DesolateBloodbladeGameTests {
    private static Cow cow(GameTestHelper helper, double x) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new Vec3(x, 1, 4.5));
        cow.setNoGravity(true);
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        cow.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        cow.setHealth(100);
        return cow;
    }

    @GameTest(template = "magnetic_arena", timeoutTicks = 50)
    public static void volley_hits_once_and_does_not_leave_daggers(GameTestHelper helper) {
        Cow caster = cow(helper, 2.5);
        Cow target = cow(helper, 5.5);
        int count = DesolateBloodbladeSpell.bladeCount(6);
        helper.assertTrue(count == 5 && DesolateBloodbladeSpell.bladeCount(1) == 3, "3/5 rank count");
        DesolateBloodbladeEntity.launchVolley(helper.getLevel(), caster, new Vec3(1, 0, 0), count, 18, 4);
        int active = helper.getLevel().getEntitiesOfClass(DesolateBloodbladeEntity.class,
                new AABB(caster.position(), caster.position()).inflate(4), e -> e.getOwner() == caster).size();
        helper.assertTrue(active == 5, "five separate daggers spawned");
        helper.runAtTickTime(7, () -> helper.assertTrue(Math.abs(target.getHealth() - 96) < .01F,
                "direct volley must deal exactly one hit: " + target.getHealth()));
        helper.runAtTickTime(24, () -> {
            helper.assertTrue(Math.abs(target.getHealth() - 96) < .01F,
                    "no damage after the direct hit: " + target.getHealth());
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(DesolateBloodbladeEntity.class,
                    new AABB(target.position(), target.position()).inflate(4), e -> e.getOwner() == caster).isEmpty(),
                    "daggers must leave no entity at the impact point");
            helper.succeed();
        });
    }

    @GameTest(template = "magnetic_arena", timeoutTicks = 50)
    public static void wall_blocks_daggers(GameTestHelper helper) {
        Cow caster = cow(helper, 2.5);
        Cow target = cow(helper, 6.5);
        for (int y = 1; y <= 4; y++) {
            for (int z = 2; z <= 6; z++) helper.setBlock(4, y, z, Blocks.STONE);
        }
        DesolateBloodbladeEntity.launchVolley(helper.getLevel(), caster, new Vec3(1, 0, 0), 5, 18, 4);
        helper.runAtTickTime(24, () -> {
            helper.assertTrue(target.getHealth() == 100, "wall must block the entire volley");
            helper.succeed();
        });
    }
}
