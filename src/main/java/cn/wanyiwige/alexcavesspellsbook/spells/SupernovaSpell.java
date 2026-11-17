package cn.wanyiwige.alexcavesspellsbook.spells;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.alexcavesspellsbook.compat.FantasyEndingCompat;
import cn.wanyiwige.alexcavesspellsbook.compat.SupernovaReactorSupport;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/** Exo spell that launches original Brutality reactors from a ring behind the caster. */
public final class SupernovaSpell extends AbstractSpell {
    public static final int MAX_LEVEL = 5;
    private static final double RING_RADIUS = 1.6D;
    private static final double RING_DISTANCE_BEHIND = 2.0D;
    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(AlexCavesSpellsbook.MOD_ID, "supernova");
    private static final ResourceLocation EXO_SCHOOL =
            ResourceLocation.fromNamespaceAndPath("roacw", "exo");
    private static final ResourceLocation REACTOR_ENTITY =
            ResourceLocation.fromNamespaceAndPath("brutality", "biomech_reactor");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(EXO_SCHOOL)
            .setMaxLevel(MAX_LEVEL)
            .setCooldownSeconds(40)
            .setAllowCrafting(false)
            .build();

    public SupernovaSpell() {
        baseManaCost = 150;
        manaCostPerLevel = 30;
        castTime = 40;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return ID;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public boolean requiresLearning() {
        return false;
    }

    @Override
    public boolean allowLooting() {
        return false;
    }

    @Override
    public boolean allowCrafting() {
        return false;
    }

    @Override
    public SpellRarity getRarity(int spellLevel) {
        return spellLevel >= 4 ? FantasyEndingCompat.originRarity() : SpellRarity.LEGENDARY;
    }

    @Override
    public int getMaxRarity() {
        return FantasyEndingCompat.originRarity().getValue();
    }

    @Override
    public int getMinLevelForRarity(SpellRarity rarity) {
        if (rarity == SpellRarity.LEGENDARY) {
            return 1;
        }
        if (rarity == FantasyEndingCompat.originRarity()) {
            return 4;
        }
        return 0;
    }

    public static int getReactorCount(int spellLevel) {
        // Do not exceed five even when an outside mod boosts the cast above the level cap.
        return Mth.clamp(spellLevel, 1, MAX_LEVEL);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("ui.alex_caves_spellsbook.supernova.reactor_count",
                getReactorCount(spellLevel)),
                Component.translatable("ui.alex_caves_spellsbook.supernova.damage_multiplier", 100),
                Component.translatable("ui.alex_caves_spellsbook.supernova.area_damage_ratio", 80));
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData magicData) {
        return ForgeRegistries.ENTITY_TYPES.containsKey(REACTOR_ENTITY)
                && super.checkPreCastConditions(level, spellLevel, caster, magicData);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource castSource, MagicData magicData) {
        if (level instanceof ServerLevel serverLevel) {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(REACTOR_ENTITY);
            if (type != null) {
                launchReactors(serverLevel, type, caster, getReactorCount(spellLevel));
            }
        }
        super.onCast(level, spellLevel, caster, castSource, magicData);
    }

    private static void launchReactors(ServerLevel level, EntityType<?> type, LivingEntity caster, int count) {
        Vec3 forward = caster.getLookAngle().normalize();
        // Keep the ring vertical behind the body even when aiming up/down.
        float yaw = caster.getYRot() * Mth.DEG_TO_RAD;
        Vec3 bodyForward = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
        Vec3 center = caster.position().add(0, caster.getBbHeight() * 0.5D + RING_RADIUS, 0)
                .subtract(bodyForward.scale(RING_DISTANCE_BEHIND));
        for (int index = 0; index < count; index++) {
            Entity entity = type.create(level);
            if (!(entity instanceof Projectile reactor)) {
                if (entity != null) {
                    entity.discard();
                }
                throw new IllegalStateException("brutality:biomech_reactor must be a projectile");
            }
            double angle = Math.PI * 0.5D + Math.PI * 2.0D * index / count;
            Vec3 position = count == 1 ? center : center
                    .add(right.scale(Math.cos(angle) * RING_RADIUS))
                    .add(0, Math.sin(angle) * RING_RADIUS, 0);
            reactor.setOwner(caster);
            SupernovaReactorSupport.mark(reactor);
            reactor.setPos(position.x, position.y, position.z);
            reactor.shoot(forward.x, forward.y, forward.z, 0.75F, 0.0F);
            if (reactor instanceof AbstractArrow arrow) {
                arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
            }
            // The original onAddedToWorld initializes direct-hit damage from this owner.
            // Damage hooks multiply only marked reactors; original AI, lifespan and VFX remain.
            level.addFreshEntity(reactor);
        }
    }
}
