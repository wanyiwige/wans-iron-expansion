package cn.wanyiwige.alexcavesspellsbook.gametest;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.alexcavesspellsbook.entities.DesolateBloodbladeEntity;
import cn.wanyiwige.alexcavesspellsbook.entities.PureDarknessFieldEntity;
import cn.wanyiwige.alexcavesspellsbook.entities.PureDarknessRiteEntity;
import cn.wanyiwige.alexcavesspellsbook.entities.PureDarknessWardEntity;
import cn.wanyiwige.alexcavesspellsbook.registry.ModItems;
import cn.wanyiwige.alexcavesspellsbook.registry.ModSchools;
import cn.wanyiwige.alexcavesspellsbook.registry.ModSpells;
import cn.wanyiwige.alexcavesspellsbook.spells.DesolateBloodbladeSpell;
import cn.wanyiwige.alexcavesspellsbook.spells.PureDarknessFieldSpell;
import cn.wanyiwige.alexcavesspellsbook.spells.PureDarknessRiteSpell;
import cn.wanyiwige.alexcavesspellsbook.spells.PureDarknessWardSpell;
import com.github.alexmodguy.alexscaves.server.item.ACItemRegistry;
import com.mojang.logging.LogUtils;
import io.redspace.ironsspellbooks.api.config.SpellConfigManager;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.block.scroll_forge.ScrollForgeTile;
import io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeMenu;
import io.redspace.ironsspellbooks.item.InkItem;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** Explicitly opt-in and intended for disposable production-mapped Forge test worlds. */
@Mod.EventBusSubscriber(modid = AlexCavesSpellsbook.MOD_ID)
public final class PureDarknessServerAudit {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean ENABLED = Boolean.getBoolean("wans.pure_darkness.audit");
    private static final Vec3 FORWARD = new Vec3(1, 0, 0);
    private static Arena arena;

    private PureDarknessServerAudit() {}

    @SubscribeEvent
    public static void start(ServerStartedEvent event) {
        if (!ENABLED) return;
        ServerLevel level = event.getServer().overworld();
        try {
            // Dedicated offline tests have no login to trigger Iron's first config sync.
            SpellConfigManager.onDatapackSync(new OnDatapackSyncEvent(event.getServer().getPlayerList(), null));
            auditScrollsAndCrafting(level);
            arena = prepareCombat(level);
        } catch (RuntimeException error) {
            LOGGER.error("PURE_DARKNESS_AUDIT_FAILED", error);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END || arena == null) return;
        Arena current = arena;
        long age = current.level.getGameTime() - current.start;
        try {
            if (age == 4) expectHealth(current.volley, 73, "fast daggers reach the target with one direct hit");
            if (age == 10) expectHealth(current.eye, 100, "eye before closing");
            if (age == 16) {
                expectHealth(current.eye, 76, "eye on closing");
                expectEffect(current.mist, MobEffects.MOVEMENT_SLOWDOWN, 524, 540);
            }
            if (age == 23) expectHealth(current.maw, 43, "maw on closure");
            if (age == PureDarknessRiteEntity.CHORUS_BURST - 1) {
                for (Cow target : current.chorusTargets) expectHealth(target, 100, "chorus before burst");
            }
            if (age == PureDarknessRiteEntity.CHORUS_BURST) {
                for (Cow target : current.chorusTargets) expectHealth(target, 19, "chorus hits center and separate outer targets");
                expectHealth(current.chorusOutside, 100, "outside every chorus node");
            }
            if (age < 91) return;
            for (Cow target : current.chorusTargets) expectHealth(target, 19, "chorus only strikes once");
            expectHealth(current.volley, 73, "five daggers direct hit deduplication and no delayed damage");
            expectHealth(current.wall, 100, "daggers behind wall");
            expectHealth(current.eye, 76, "eye only strikes once");
            expectHealth(current.maw, 43, "maw only strikes once");
            expectHealth(current.chorus, 100, "chorus interrupted with only one chanter");
            expectEffect(current.mist, MobEffects.MOVEMENT_SLOWDOWN, 449, 465);
            LOGGER.info("PURE_DARKNESS_DEBUFF_PASS mist-slow=540 persist-after-field");
            LOGGER.info("PURE_DARKNESS_COMBAT_PASS bloodblade-single-hit bloodblade-speed wall-occlusion eye-timing maw-timing chorus-area chorus-cancel");
            LOGGER.info("PURE_DARKNESS_AUDIT_PASS");
            arena = null;
        } catch (RuntimeException error) {
            arena = null;
            LOGGER.error("PURE_DARKNESS_AUDIT_FAILED", error);
        }
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) {
        arena = null;
    }

    private static void auditScrollsAndCrafting(ServerLevel level) {
        var block = ((BlockItem) ItemRegistry.SCROLL_FORGE_BLOCK.get()).getBlock();
        var tile = new ScrollForgeTile(BlockPos.ZERO, block.defaultBlockState());
        tile.setLevel(level);
        var menu = new ScrollForgeMenu(0, FakePlayerFactory.getMinecraft(level).getInventory(), tile);
        Item focus = ACItemRegistry.PURE_DARKNESS.get();
        List<AbstractSpell> spells = ModSpells.SPELLS.getEntries().stream().map(RegistryObject::get)
                .filter(spell -> ModSchools.PURE_DARKNESS_RESOURCE.equals(spell.getSchoolType().getId())).toList();
        require(spells.size() == 15, "expected fifteen registered spells, got " + spells.size());
        require(spells.stream().noneMatch(spell -> spell.getSpellId().endsWith("blood_carved_altar")
                || spell.getSpellId().endsWith("thorn_barricade")
                || spell.getSpellId().endsWith("forsaken_rite")
                || spell.getSpellId().endsWith("forsaken_grand_rite")
                || spell.getSpellId().endsWith("light_stripping_drape")), "removed spell still registered");
        int valid = 0, rejected = 0, levels = 0;
        for (AbstractSpell spell : spells) {
            require(spell.isEnabled() && spell.allowCrafting(), "spell disabled or uncraftable " + spell.getSpellId());
            List<SpellRarity> ranks = new ArrayList<>();
            for (int rank = 1; rank <= spell.getMaxLevel(); rank++) ranks.add(spell.getRarity(rank));
            require(ranks.size() >= 2 && ranks.get(ranks.size() - 1) == SpellRarity.LEGENDARY
                    && ranks.get(ranks.size() - 2) == SpellRarity.LEGENDARY,
                    "last two ranks must be legendary: " + spell.getSpellId());
            levels += ranks.size();
            for (SpellRarity rarity : SpellRarity.values()) {
                int expected = ranks.indexOf(rarity) + 1;
                int actual = spell.getMinLevelForRarity(rarity);
                require(expected == actual, "invalid rarity boundary " + spell.getSpellId() + " " + rarity);
                InkItem ink = InkItem.getInkForRarity(rarity);
                if (ink.getRarity() != rarity) continue;
                tile.getItemHandler().setStackInSlot(0, new ItemStack(ink));
                tile.getItemHandler().setStackInSlot(1, new ItemStack(Items.PAPER));
                tile.getItemHandler().setStackInSlot(2, new ItemStack(focus));
                menu.setRecipeSpell(spell);
                ItemStack result = tile.getItemHandler().getStackInSlot(3);
                if (expected == 0) {
                    require(result.isEmpty(), "unsupported ink produced a scroll: " + spell.getSpellId() + " " + rarity);
                    rejected++;
                } else {
                    require(result.is(ModItems.PURE_DARKNESS_SPELL_SCROLL.get()), "wrong scroll item: " + spell.getSpellId());
                    var data = ISpellContainer.get(result).getSpellAtIndex(0);
                    require(data.getSpell() == spell && data.getLevel() == expected, "wrong scroll contents: " + spell.getSpellId());
                    valid++;
                }
            }
            LOGGER.info("PURE_DARKNESS_RANKS {} {}", spell.getSpellId(),
                    ranks.stream().map(Enum::name).collect(Collectors.joining(",")));
        }
        require(levels == 98, "expected 98 ranks, got " + levels);
        Item blankRune = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("irons_spellbooks:blank_rune"));
        Item blankOrb = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("irons_spellbooks:upgrade_orb"));
        auditRecipe(level, menu, "pure_darkness_rune", focus, blankRune, ModItems.PURE_DARKNESS_RUNE.get());
        auditRecipe(level, menu, "pure_darkness_upgrade_orb", ModItems.PURE_DARKNESS_RUNE.get(),
                blankOrb, ModItems.PURE_DARKNESS_UPGRADE_ORB.get());
        LOGGER.info("PURE_DARKNESS_ACQUISITION_PASS spells={} ranks={} valid-ink={} rejected-ink={} recipes=2",
                spells.size(), levels, valid, rejected);
    }

    private static void auditRecipe(ServerLevel level, ScrollForgeMenu menu, String path, Item surround, Item center, Item result) {
        var id = ResourceLocation.fromNamespaceAndPath(AlexCavesSpellsbook.MOD_ID, path);
        var container = new TransientCraftingContainer(menu, 3, 3);
        for (int slot : new int[]{1, 3, 5, 7}) container.setItem(slot, new ItemStack(surround));
        container.setItem(4, new ItemStack(center));
        var recipe = level.getRecipeManager().byKey(id).orElseThrow();
        require(recipe instanceof CraftingRecipe, "not a crafting recipe: " + id);
        var craft = (CraftingRecipe) recipe;
        require(craft.matches(container, level) && craft.assemble(container, level.registryAccess()).is(result),
                "wrong recipe output: " + id);
    }

    private static Arena prepareCombat(ServerLevel level) {
        BlockPos spawn = level.getSharedSpawnPos();
        int x = (spawn.getX() & ~15) + 2, z = (spawn.getZ() & ~15) + 2;
        int y = 88;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 87; dz++) {
                BlockPos floor = new BlockPos(x + dx, y - 1, z + dz);
                if (!level.hasChunkAt(floor)) level.getChunkAt(floor);
                level.setChunkForced(floor.getX() >> 4, floor.getZ() >> 4, true);
                level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
            }
        }
        Vec3 base = new Vec3(x + .5, y, z + .5);
        Cow caster = cow(level, base.add(1, 0, 3));
        Cow volley = cow(level, base.add(5, 0, 3));
        DesolateBloodbladeEntity.launchVolley(level, caster, FORWARD, 5, 12, DesolateBloodbladeSpell.directBase(6));
        var knives = level.getEntitiesOfClass(DesolateBloodbladeEntity.class,
                new AABB(caster.position(), caster.position()).inflate(4), knife -> knife.getOwner() == caster);
        require(knives.size() == 5 && knives.stream().allMatch(knife ->
                Math.abs(knife.getDeltaMovement().length() - DesolateBloodbladeEntity.SPEED) < 1e-4),
                "five daggers must launch at the configured speed");

        Cow blockedCaster = cow(level, base.add(1, 0, 10));
        Cow wall = cow(level, base.add(7, 0, 10));
        for (int dy = 0; dy < 4; dy++) {
            for (int dz = 7; dz <= 13; dz++) {
                level.setBlockAndUpdate(BlockPos.containing(base.add(4, dy, dz)), Blocks.STONE.defaultBlockState());
            }
        }
        DesolateBloodbladeEntity.launchVolley(level, blockedCaster, FORWARD, 5, 12, DesolateBloodbladeSpell.directBase(6));

        Cow eye = cow(level, base.add(7, 0, 17));
        require(eye.isAlive(), "eye target failed to spawn");
        PureDarknessFieldEntity.spawn(level, caster, PureDarknessFieldSpell.Kind.CLOSING_EYE, 1,
                PureDarknessFieldSpell.Kind.CLOSING_EYE.damage(1),
                new PureDarknessFieldEntity.Placement(eye.getBoundingBox().getCenter(), FORWARD, List.of(), List.of()));

        Cow maw = cow(level, base.add(7, 0, 23));
        require(PureDarknessWardEntity.spawn(level, caster, PureDarknessWardSpell.Kind.FORSAKEN_MAW, 1,
                PureDarknessWardSpell.Kind.FORSAKEN_MAW.damage(1),
                new PureDarknessWardEntity.Placement(maw.position(), FORWARD)), "maw failed to spawn");

        Vec3 chorusOrigin = base.add(6, 0, 30);
        Cow chorus = cow(level, chorusOrigin);
        List<Vec3> nodes = Arrays.asList(chorusOrigin.add(1.6, 0, 0), chorusOrigin.add(-1, 0, 1.4), chorusOrigin.add(-1, 0, -1.4));
        require(PureDarknessRiteEntity.spawn(level, caster, PureDarknessRiteSpell.Kind.ZEALOT_CHORUS, 1,
                PureDarknessRiteSpell.Kind.ZEALOT_CHORUS.damage(1),
                new PureDarknessRiteEntity.Placement(chorusOrigin, FORWARD, nodes)), "chorus failed to spawn");
        var chanters = level.getEntitiesOfClass(PureDarknessRiteEntity.class,
                new AABB(chorusOrigin, chorusOrigin).inflate(3), e -> e.slot() >= 0);
        require(chanters.size() == 3, "expected three damageable chanters");
        require(chanters.get(0).hurt(level.damageSources().generic(), 40)
                && chanters.get(1).hurt(level.damageSources().generic(), 40), "two chanters failed to die");
        Vec3 mistOrigin = base.add(3, .7, 38);
        Cow mist = cow(level, base.add(5, 0, 38));
        var mistKind = PureDarknessFieldSpell.Kind.MOTHSCALE_MIST;
        PureDarknessFieldEntity.spawn(level, caster, mistKind, 1, mistKind.damage(1),
                new PureDarknessFieldEntity.Placement(mistOrigin, FORWARD, List.of(), List.of()));

        auditPerches(level, base.add(7, 0, 56), 1, false);
        auditPerches(level, base.add(7, 0, 66), 8, true);
        Vec3 activeOrigin = base.add(6, 0, 77);
        Cow conductor = cow(level, base.add(1, 0, 76));
        Cow center = cow(level, activeOrigin);
        Cow east = cow(level, activeOrigin.add(5.7, 0, 0));
        Cow south = cow(level, activeOrigin.add(-1, 1, 5.8));
        Cow outside = cow(level, activeOrigin.add(8, 0, 0));
        List<Vec3> activeNodes = List.of(activeOrigin.add(1.6, 0, 0), activeOrigin.add(-1, 0, 1.4),
                activeOrigin.add(-1, 0, -1.4));
        var chorusKind = PureDarknessRiteSpell.Kind.ZEALOT_CHORUS;
        require(PureDarknessRiteEntity.spawn(level, conductor, chorusKind, 1, chorusKind.damage(1),
                new PureDarknessRiteEntity.Placement(activeOrigin, FORWARD, activeNodes)), "active chorus failed to spawn");
        return new Arena(level, level.getGameTime(), volley, wall, eye, maw, chorus, mist,
                List.of(center, east, south), outside);
    }

    private static void auditPerches(ServerLevel level, Vec3 center, int rank, boolean uneven) {
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                int height = !uneven || x == 0 && z == 0 ? 5 : 6 + Math.floorMod(x + z, 2);
                level.setBlockAndUpdate(BlockPos.containing(center.add(x, height, z)), Blocks.STONE.defaultBlockState());
            }
        }
        Cow caster = cow(level, center);
        caster.setXRot(-90);
        var kind = PureDarknessFieldSpell.Kind.VESPER_PERCH;
        var placement = PureDarknessFieldEntity.plan(level, caster, kind, rank);
        int count = uneven ? 5 : 4;
        require(placement != null && placement.perches().size() == count, "legal perches: expected " + count
                + ", got " + (placement == null ? 0 : placement.perches().size()));
        if (uneven) {
            require(placement.perches().stream().map(p -> p.anchor().y).distinct().count() > 1,
                    "uneven ceiling did not yield multiple elevations");
        }
        PureDarknessFieldEntity.spawn(level, caster, kind, rank, kind.damage(rank), placement);
        int spawned = level.getEntitiesOfClass(PureDarknessFieldEntity.class,
                new AABB(center, center).inflate(5, 9, 5), field -> field.getOwner() == caster).size();
        require(spawned == count, "wrong number of Vespers: " + spawned);
        require(kind.triggerRadius(rank) == (uneven ? 8.5 : 5), "wrong Vesper radius");
        LOGGER.info("PURE_DARKNESS_VESPER_PASS rank={} bats={} uneven={} radius={}",
                rank, count, uneven, kind.triggerRadius(rank));
    }

    private static Cow cow(ServerLevel level, Vec3 at) {
        var cow = EntityType.COW.create(level);
        if (cow == null) throw new IllegalStateException("cow creation failed");
        cow.setPos(at);
        cow.setNoAi(true);
        cow.setNoGravity(true);
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        cow.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        cow.setHealth(100);
        require(level.addFreshEntity(cow), "cow registration failed");
        return cow;
    }

    private static void expectHealth(Cow cow, float expected, String stage) {
        require(Math.abs(cow.getHealth() - expected) < .01F, stage + ": expected " + expected + ", got " + cow.getHealth());
    }

    private static void expectEffect(Cow target, MobEffect effect, int minimum, int maximum) {
        var instance = target.getEffect(effect);
        require(instance != null && instance.getDuration() >= minimum && instance.getDuration() <= maximum,
                effect.getDescriptionId() + " expected " + minimum + "–" + maximum + " ticks, got "
                        + (instance == null ? "no effect" : instance.getDuration()));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private record Arena(ServerLevel level, long start, Cow volley, Cow wall, Cow eye, Cow maw, Cow chorus, Cow mist,
                         List<Cow> chorusTargets, Cow chorusOutside) {}
}

