package cn.wanyiwige.astralcompat.events;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.astralcompat.entity.AstralAnchorEntity;
import cn.wanyiwige.astralcompat.entity.AstralBoltEntity;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.util.CameraShakeData;
import io.redspace.ironsspellbooks.api.util.CameraShakeManager;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import io.redspace.ironsspellbooks.spells.ender.TeleportSpell;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AlexCavesSpellsbook.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AstralSpellEvents {
    private static final String PREFIX = "asterismarcanum:";
    private static final String CELESTIAL_TETHER_SPELL = PREFIX + "celestial_tether";
    private static final String TETHER_UNTIL = "AstralCompatTetherUntil";
    private static final String TETHER_X = "AstralCompatTetherX";
    private static final String TETHER_Y = "AstralCompatTetherY";
    private static final String TETHER_Z = "AstralCompatTetherZ";

    @SubscribeEvent
    public static void onSpellPreCast(SpellPreCastEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        Player caster = event.getEntity();
        if (!isLegacySpell(event.getSpellId())) {
            return;
        }

        if (manaIsFreeFor(caster)) {
            return;
        }

        AbstractSpell spell = SpellRegistry.getSpell(event.getSpellId());
        int manaCost = getAstralManaCost(event.getSpellId(), Math.max(1, event.getSpellLevel()));
        if (MagicData.getPlayerMagicData(event.getEntity()).getMana() < manaCost) {
            event.setCanceled(true);
            event.getEntity().displayClientMessage(
                    Component.translatable("ui.irons_spellbooks.cast_error_mana", spell.getDisplayName(event.getEntity()))
                            .withStyle(ChatFormatting.RED), true);
        }
    }

    @SubscribeEvent
    public static void onSpellCast(SpellOnCastEvent event) {
        if (event.getEntity().level().isClientSide || !isLegacySpell(event.getSpellId())) {
            return;
        }
        Player caster = event.getEntity();
        int spellLevel = Math.max(1, event.getSpellLevel());
        // KubeJS registers the displayed costs, but its dynamic spell bridge can
        // deliver a zero-cost SpellOnCastEvent.  The native Iron's payment code
        // uses this event value, so set it authoritatively before either native
        // (spellbook) or compatibility (scroll/special source) payment runs.
        event.setManaCost(getAstralManaCost(event.getSpellId(), spellLevel));
        if (!consumeManaForNormallyFreeSource(event, caster)) {
            return;
        }

        switch (event.getSpellId()) {
            case PREFIX + "starfire" -> castStarfire(caster, spellLevel);
            case PREFIX + "brightburst" -> castBrightburst(caster, spellLevel);
            case PREFIX + "star_swarm" -> castContinuousAnchor(caster, AstralAnchorEntity.STAR_SWARM,
                    spellLevel, 12.0F + spellLevel * 4.0F, 100);
            case PREFIX + "luminous_beam" -> castContinuousAnchor(caster, AstralAnchorEntity.LUMINOUS_BEAM,
                    spellLevel, 24.0F + Math.max(0, spellLevel - 1) * 7.0F, 20);
            case PREFIX + "piercing_light" -> castPiercingLight(caster, spellLevel);
            default -> {
            }
        }
    }

    private static float astralDamage(Player caster, float baseDamage) {
        return (float) (baseDamage * cn.wanyiwige.astralcompat.registry.AstralSchools.ASTRAL.get().getPowerFor(caster));
    }

    private static void castStarfire(Player caster, int spellLevel) {
        AstralBoltEntity bolt = new AstralBoltEntity(caster.level(), caster, AstralBoltEntity.STARFIRE);
        Vec3 look = caster.getLookAngle().normalize();
        bolt.setPos(caster.getEyePosition().add(look.scale(0.2)));
        bolt.setDamage(astralDamage(caster, 14.0F + spellLevel * 6.0F));
        bolt.shoot(look);
        caster.level().addFreshEntity(bolt);
    }

    private static void castBrightburst(Player caster, int spellLevel) {
        double radius = 10.0 + spellLevel * 2.0;
        float spellPower = 1.0F + spellLevel;
        float damage = astralDamage(caster, 42.0F + spellLevel * 12.0F);
        AABB area = caster.getBoundingBox().inflate(radius);

        for (LivingEntity target : caster.level().getEntitiesOfClass(LivingEntity.class, area,
                target -> target != caster && target.isAlive() && !caster.isAlliedTo(target)
                        && !caster.isPassengerOfSameVehicle(target))) {
            double distance = caster.distanceTo(target);
            if (distance > radius) {
                continue;
            }
            DamageSources.applyDamage(target, damage,
                    SpellRegistry.getSpell(PREFIX + "brightburst").getDamageSource(caster));
            double baseScale = spellPower * 2.0 / (distance + 0.01);
            double resistance = target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) * 5.0;
            double scale = baseScale - Mth.clamp(resistance, 0.0, baseScale);
            target.setDeltaMovement(target.getDeltaMovement().add(
                    target.position().subtract(caster.position()).normalize().scale(scale)));
            target.hurtMarked = true;
        }

        AstralAnchorEntity wave = new AstralAnchorEntity(caster.level(), caster,
                AstralAnchorEntity.BRIGHTBURST, spellLevel, damage, 18);
        wave.setPos(caster.getX(), caster.getY() + 0.08, caster.getZ());
        caster.level().addFreshEntity(wave);

        MagicManager.spawnParticles(caster.level(), ParticleTypes.FLASH,
                caster.getX(), caster.getY() + 0.2, caster.getZ(), 1, 0, 0, 0, 0, false);
        MagicManager.spawnParticles(caster.level(), ParticleTypes.END_ROD,
                caster.getX(), caster.getY() + 0.2, caster.getZ(), 32,
                radius * 0.12, 0.18, radius * 0.12, 0.08, true);
        CameraShakeManager.addCameraShake(new CameraShakeData(caster.level(), 18,
                caster.position(), (float) radius * 1.5F));
        caster.level().playSound(null, caster.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST,
                SoundSource.PLAYERS, 1.35F, 1.35F);
        caster.level().playSound(null, caster.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 1.1F, 0.8F);
    }

    private static void castContinuousAnchor(Player caster, int mode, int spellLevel, float damage,
                                             int persistenceTicks) {
        damage = astralDamage(caster, damage);
        MagicData magicData = MagicData.getPlayerMagicData(caster);
        if (magicData.getCastDurationRemaining() <= 0) {
            removeOwnedAnchor(caster, mode);
            return;
        }
        AstralAnchorEntity existing = findOwnedAnchor(caster, mode);
        if (existing != null) {
            existing.refresh(damage, persistenceTicks);
            return;
        }
        AstralAnchorEntity anchor = new AstralAnchorEntity(caster.level(), caster, mode,
                spellLevel, damage, persistenceTicks);
        anchor.setPos(caster.getEyePosition().subtract(0, 0.35, 0));
        caster.level().addFreshEntity(anchor);
    }

    private static void castPiercingLight(Player caster, int spellLevel) {
        Vec3 center = caster.position();
        AstralAnchorEntity circle = new AstralAnchorEntity(caster.level(), caster,
                AstralAnchorEntity.PIERCING_LIGHT_CIRCLE, spellLevel, 0, 40);
        circle.setPos(center.add(0, 0.04, 0));
        caster.level().addFreshEntity(circle);
        for (int i = 0; i < 7; i++) {
            var star = new cn.wanyiwige.astralcompat.entity.PiercingStarEntity(
                    caster.level(), caster, caster.getLookAngle(), 8 + i * 2,
                    astralDamage(caster, 18.0F + spellLevel * 6.0F));
            star.setPos(center.add(0, 0.8, 0));
            for (int attempt = 0; attempt < 32; attempt++) {
                double radius = Math.sqrt(caster.getRandom().nextDouble()) * 2.7;
                double angle = caster.getRandom().nextDouble() * Math.PI * 2;
                Vec3 spawn = center.add(Math.cos(angle) * radius,
                        0.30 + caster.getRandom().nextDouble() * 1.38, Math.sin(angle) * radius);
                star.setPos(spawn);
                if (caster.level().noCollision(star, star.getBoundingBox())) break;
                star.setPos(center.add(0, 0.8, 0));
            }
            caster.level().addFreshEntity(star);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        Player player = event.player;
        maintainContinuousAstralCasting(player);
        CompoundTag data = player.getPersistentData();
        // Remove any persisted state from versions where 天穹缚 was available.
        // This runs independently of the recast UI so a player can never remain locked.
        if (data.getLong(TETHER_UNTIL) > 0 || hasActiveTetherRecast(player)) {
            finishTether(player, RecastResult.USER_CANCEL, false);
            return;
        }
    }

    private static boolean consumeManaForNormallyFreeSource(SpellOnCastEvent event, Player caster) {
        if (event.getCastSource().consumesMana() || manaIsFreeFor(caster)) {
            return true;
        }

        MagicData magicData = MagicData.getPlayerMagicData(caster);
        int manaCost = Math.max(0, event.getManaCost());
        if (magicData.getMana() < manaCost) {
            return false;
        }

        magicData.setMana(Math.max(0.0F, magicData.getMana() - manaCost));
        if (caster instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new SyncManaPacket(magicData));
        }
        return true;
    }

    private static void maintainContinuousAstralCasting(Player player) {
        MagicData magicData = MagicData.getPlayerMagicData(player);
        String castingSpellId = magicData.isCasting() ? magicData.getCastingSpellId() : "";

        if (!(player instanceof ServerPlayer serverPlayer) || !magicData.isCasting()
                || !isLegacySpell(castingSpellId) || magicData.getCastSource().consumesMana()
                || manaIsFreeFor(player)) {
            return;
        }

        AbstractSpell spell = SpellRegistry.getSpell(castingSpellId);
        if (spell.getCastType() == CastType.CONTINUOUS
                && magicData.getMana() < getAstralManaCost(castingSpellId, magicData.getCastingSpellLevel())) {
            Utils.serverSideCancelCast(serverPlayer);
            removeOwnedAnchor(player, AstralAnchorEntity.LUMINOUS_BEAM);
            removeOwnedAnchor(player, AstralAnchorEntity.STAR_SWARM);
        }
    }

    private static boolean manaIsFreeFor(Player player) {
        return player.isCreative() && !ServerConfigs.CREATIVE_MANA_COST.get();
    }

    /**
     * Authoritative Iron's Spells mana costs for all KubeJS Astral spells.
     * Formula: base + (level - 1) * perLevel.  Keeping this here prevents a
     * third-party dynamic-spell bridge from turning the actual payment event
     * into a free cast while retaining the same values in the KubeJS tooltip.
     */
    private static int getAstralManaCost(String spellId, int spellLevel) {
        int level = Math.max(1, spellLevel);
        int base;
        int perLevel;
        switch (spellId) {
            case PREFIX + "starfire" -> { base = 24; perLevel = 8; }
            case PREFIX + "brightburst" -> { base = 90; perLevel = 25; }
            case PREFIX + "star_swarm" -> { base = 12; perLevel = 4; }
            case PREFIX + "luminous_beam" -> { base = 50; perLevel = 10; }
            case PREFIX + "piercing_light" -> { base = 80; perLevel = 20; }
            default -> { return 0; }
        }
        return base + (level - 1) * perLevel;
    }

    private static boolean isAlly(Player caster, LivingEntity target) {
        if (caster.isAlliedTo(target)) {
            return true;
        }
        return target instanceof TamableAnimal tamable && tamable.isOwnedBy(caster);
    }

    private static AstralAnchorEntity findOwnedAnchor(Player caster, int mode) {
        return caster.level().getEntitiesOfClass(AstralAnchorEntity.class, caster.getBoundingBox().inflate(160),
                        anchor -> anchor.getOwnerId() == caster.getId() && anchor.getMode() == mode)
                .stream().findFirst().orElse(null);
    }

    private static void removeOwnedAnchor(Player caster, int mode) {
        caster.level().getEntitiesOfClass(AstralAnchorEntity.class, caster.getBoundingBox().inflate(160),
                        anchor -> anchor.getOwnerId() == caster.getId() && anchor.getMode() == mode)
                .forEach(Entity::discard);
    }

    private static void clearTether(CompoundTag data) {
        data.remove(TETHER_UNTIL);
        data.remove(TETHER_X);
        data.remove(TETHER_Y);
        data.remove(TETHER_Z);
    }

    private static boolean hasActiveTetherRecast(Player player) {
        return MagicData.getPlayerMagicData(player).getPlayerRecasts()
                .hasRecastForSpell(CELESTIAL_TETHER_SPELL);
    }

    private static void finishTether(Player player, RecastResult result, boolean manual) {
        clearTether(player.getPersistentData());
        removeOwnedAnchor(player, AstralAnchorEntity.CELESTIAL_TETHER);

        var recasts = MagicData.getPlayerMagicData(player).getPlayerRecasts();
        RecastInstance instance = recasts.getRecastInstance(CELESTIAL_TETHER_SPELL);
        if (instance != null) {
            recasts.removeRecast(instance, result);
        }

        if (manual) {
            MagicManager.spawnParticles(player.level(), ParticleTypes.REVERSE_PORTAL,
                    player.getX(), player.getY() + 1.0, player.getZ(), 34,
                    0.85, 0.9, 0.85, 0.08, false);
            player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.PLAYERS, 0.9F, 1.5F);
        }
    }

    public static void cancelTether(Player player) {
        finishTether(player, RecastResult.COUNTERSPELL, false);
    }

    private static boolean isLegacySpell(String id) {
        return id.equals(PREFIX + "starfire") || id.equals(PREFIX + "brightburst")
                || id.equals(PREFIX + "star_swarm") || id.equals(PREFIX + "luminous_beam")
                || id.equals(PREFIX + "piercing_light");
    }

    private AstralSpellEvents() {
    }
}
