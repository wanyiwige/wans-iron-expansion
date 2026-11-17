package cn.wanyiwige.alexcavesspellsbook.hazen;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.events.SpellSummonEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.*;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import io.redspace.ironsspellbooks.spells.ender.TeleportSpell;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import java.util.List;
import java.util.Optional;

/** Recreated mechanics using the native 1.20.1 casting and summon lifecycle. */
public final class HazenSpell extends AbstractSpell {
    public final String kind;
    private final ResourceLocation resource;
    private final DefaultConfig defaults;

    public HazenSpell(String kind) {
        this.kind = kind;
        resource = HazenEquipment.id("hazen_" + kind);
        String school;
        int max, cooldown;
        SpellRarity rarity;
        switch (kind) {
            case "thorn_chakram" -> { school = "nature"; max = 10; cooldown = 1; rarity = SpellRarity.COMMON;
                manaCostPerLevel = 4; baseSpellPower = 10; spellPowerPerLevel = 1; castTime = 0; baseManaCost = 20; }
            case "shooting_star" -> { school = "cosmic"; max = 10; cooldown = 2; rarity = SpellRarity.COMMON;
                manaCostPerLevel = 3; baseSpellPower = 12; spellPowerPerLevel = 1; castTime = 0; baseManaCost = 40; }
            case "spectral_axe" -> { school = "evocation"; max = 8; cooldown = 3; rarity = SpellRarity.RARE;
                manaCostPerLevel = 4; baseSpellPower = 20; spellPowerPerLevel = 1; castTime = 0; baseManaCost = 30; }
            case "nights_edge_strike" -> { school = "shadow"; max = 5; cooldown = 15; rarity = SpellRarity.COMMON;
                manaCostPerLevel = 15; baseSpellPower = 5; spellPowerPerLevel = 2; castTime = 10; baseManaCost = 30; }
            case "terraprismic_barrage" -> { school = "radiance"; max = 5; cooldown = 2; rarity = SpellRarity.RARE;
                manaCostPerLevel = 3; baseSpellPower = 12; spellPowerPerLevel = 1; castTime = 30; baseManaCost = 40; }
            case "call_forth_terraprisma" -> { school = "radiance"; max = 5; cooldown = 150; rarity = SpellRarity.EPIC;
                manaCostPerLevel = 15; baseSpellPower = 1; spellPowerPerLevel = 2; castTime = 20; baseManaCost = 80; }
            case "prismatic_shift" -> { school = "radiance"; max = 3; cooldown = 0; rarity = SpellRarity.UNCOMMON;
                manaCostPerLevel = 10; baseSpellPower = 12; spellPowerPerLevel = 4; castTime = 0; baseManaCost = 40; }
            default -> throw new IllegalArgumentException(kind);
        }
        ResourceLocation schoolId = switch (school) {
            case "nature" -> SchoolRegistry.NATURE_RESOURCE;
            case "evocation" -> SchoolRegistry.EVOCATION_RESOURCE;
            default -> HazenEquipment.id("hazen_" + school);
        };
        defaults = new DefaultConfig().setSchoolResource(schoolId).setMaxLevel(max).setMinRarity(rarity)
                .setCooldownSeconds(kind.equals("prismatic_shift") ? 1.5 : cooldown).build();
    }
    @Override public ResourceLocation getSpellResource() { return resource; }
    @Override public DefaultConfig getDefaultConfig() { return defaults; }
    @Override public CastType getCastType() { return castTime == 0 ? CastType.INSTANT : CastType.LONG; }
    @Override public boolean isEnabled() { return HazenConfig.spellEnabled(kind) && super.isEnabled(); }
    @Override public boolean allowLooting() { return !kind.equals("nights_edge_strike"); }
    @Override public boolean canBeCraftedBy(Player player) {
        if (!isEnabled()) return false;
        if (!kind.equals("nights_edge_strike")) return super.canBeCraftedBy(player);
        return player.getInventory().contains(HazenEquipment.WEAPONS.get("nights_edge").get().getDefaultInstance())
                || player.getInventory().contains(HazenEquipment.WEAPONS.get("true_nights_edge").get().getDefaultInstance());
    }
    @Override public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
        return isEnabled();
    }
    @Override public boolean canBeInterrupted(Player player) { return !kind.equals("nights_edge_strike") && super.canBeInterrupted(player); }
    @Override public int getEffectiveCastTime(int level, LivingEntity caster) {
        return kind.equals("nights_edge_strike") ? getCastTime(level) : super.getEffectiveCastTime(level, caster);
    }
    @Override public AnimationHolder getCastStartAnimation() {
        return kind.equals("nights_edge_strike") ? SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION : super.getCastStartAnimation();
    }
    @Override public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(HazenSounds.get(switch (kind) {
            case "thorn_chakram" -> "terraria_swing";
            case "shooting_star" -> "star_cast_1";
            case "spectral_axe", "nights_edge_strike" -> "terrablade_slash";
            case "terraprismic_barrage", "call_forth_terraprisma" -> "terraprisma_summon";
            default -> "terraria_cast";
        }));
    }
    @Override public Optional<SoundEvent> getCastStartSound() {
        if (kind.equals("nights_edge_strike")) return Optional.of(io.redspace.ironsspellbooks.registries.SoundRegistry.FLAMING_STRIKE_UPSWING.get());
        return kind.contains("terraprisma") ? Optional.of(HazenSounds.get("terraprisma_casting")) : Optional.empty();
    }
    public float damage(int level, LivingEntity caster) {
        float power = getSpellPower(level, caster);
        return switch (kind) {
            case "shooting_star" -> power * .4f;
            case "spectral_axe" -> power * .3f;
            case "nights_edge_strike" -> 5 + power * .6f + (caster == null ? 0 : Utils.getWeaponDamage(caster)
                    + EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS, caster.getMainHandItem()));
            default -> power * .5f;
        };
    }
    @Override public List<MutableComponent> getUniqueInfo(int level, LivingEntity caster) {
        if (kind.equals("prismatic_shift")) return List.of(Component.translatable("ui.alex_caves_spellsbook.hazen_distance", distance(level, caster)),
                Component.translatable("tooltip.alex_caves_spellsbook.hazen_chaos"));
        if (kind.equals("call_forth_terraprisma")) return List.of(Component.translatable("ui.alex_caves_spellsbook.hazen_hp", Utils.stringTruncation(25 + (getSpellPower(level, caster)-1)*.1, 1)),
                Component.translatable("ui.alex_caves_spellsbook.hazen_damage", Utils.stringTruncation(2 + (getSpellPower(level, caster)-1)*.05, 1)));
        return List.of(Component.translatable("ui.alex_caves_spellsbook.hazen_damage", Utils.stringTruncation(damage(level, caster), 2)));
    }
    @Override public SpellDamageSource getDamageSource(Entity projectile, Entity attacker) {
        var result = super.getDamageSource(projectile, attacker);
        return kind.equals("shooting_star") || kind.equals("terraprismic_barrage") ? result.setIFrames(0) : result;
    }
    @Override public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
        if (level.isClientSide || !isEnabled()) return;
        switch (kind) {
            case "thorn_chakram", "spectral_axe" -> {
                var bolt = HazenProjectiles.create(kind, caster, damage(spellLevel, caster));
                bolt.setPos(caster.getEyePosition().subtract(0, bolt.getBbHeight() * .5, 0)
                        .add(kind.equals("spectral_axe") ? caster.getForward() : Vec3.ZERO));
                bolt.shoot(caster.getLookAngle());
                level.addFreshEntity(bolt);
            }
            case "shooting_star" -> {
                HitResult hit = Utils.raycastForEntity(level, caster, 45, true);
                Vec3 target = hit instanceof EntityHitResult entityHit ? entityHit.getEntity().position()
                        : Utils.moveToRelativeGroundLevel(level, hit.getLocation(), 5);
                for (int i = 0; i < 2; i++) {
                    Vec3 start = target.add((caster.getRandom().nextDouble()-.5)*5, 8, (caster.getRandom().nextDouble()-.5)*5);
                    var star = HazenProjectiles.create(kind, caster, damage(spellLevel, caster));
                    star.setPos(start); star.shoot(target.subtract(start).normalize()); level.addFreshEntity(star);
                }
            }
            case "nights_edge_strike" -> slash(level, spellLevel, caster);
            case "terraprismic_barrage" -> {
                for (int i = 0; i < 2 + spellLevel; i++) {
                    var bolt = HazenProjectiles.create("terraprisma", caster, damage(spellLevel, caster));
                    bolt.setPos(caster.getEyePosition().subtract(0, .8, 0));
                    bolt.delay = i * 2;
                    bolt.setCursorHoming(true); bolt.shoot(caster.getLookAngle()); level.addFreshEntity(bolt);
                }
            }
            case "call_forth_terraprisma" -> summon(level, spellLevel, caster, source, data);
            case "prismatic_shift" -> shift(level, spellLevel, caster);
        }
        super.onCast(level, spellLevel, caster, source, data);
    }
    private void slash(Level level, int spellLevel, LivingEntity caster) {
        Vec3 forward = caster.getForward();
        Vec3 center = caster.position().add(0, caster.getBbHeight()*.3, 0).add(forward.scale(1.9));
        for (var target : level.getEntities(caster, AABB.ofSize(center, 6.5, 3.25, 6.5))) {
            if (target instanceof LivingEntity living && living.isAlive() && !caster.isAlliedTo(living)
                    && caster.distanceToSqr(target) < 3.25*3.25 && target.position().subtract(caster.getEyePosition()).dot(forward) >= 0
                    && target.getBoundingBox().getCenter().subtract(caster.getEyePosition()).dot(forward) >= 0
                    && Utils.hasLineOfSight(level, caster.getEyePosition(), target.getBoundingBox().getCenter(), true)
                    && DamageSources.applyDamage(target, damage(spellLevel, caster), getDamageSource(caster))) {
                HazenParticles.spawn(level, "nights_edge_particle", target.getBoundingBox().getCenter(), 30, .4, .4, .4, .03);
                EnchantmentHelper.doPostDamageEffects(caster, target);
            }
        }
        HazenParticles.slash(level, center.add(0, .3, 0), forward);
        var bolt = HazenProjectiles.create("nights_edge_after_slash", caster, damage(spellLevel, caster));
        bolt.setPos(caster.getEyePosition().subtract(0, .2, 0)); bolt.shoot(caster.getLookAngle()); level.addFreshEntity(bolt);
    }
    private float distance(int level, LivingEntity caster) {
        return (float)(Utils.softCapFormula(getEntityPowerMultiplier(caster)) * getSpellPower(level, (Entity)null));
    }
    private void shift(Level level, int spellLevel, LivingEntity caster) {
        Vec3 destination = null;
        LivingEntity lookTarget=null;
        HitResult hit=Utils.raycastForEntity(level,caster,distance(spellLevel,caster),true);
        if(hit instanceof EntityHitResult entityHit&&entityHit.getEntity() instanceof LivingEntity target){
            for(int i=0;i<8;i++){
                Vec3 candidate=target.position().subtract(new Vec3(0,0,1.5).yRot((float)-Math.toRadians(target.getYRot()+45*i))).add(0,1,0);
                if(level.noCollision(caster,caster.getBoundingBox().move(candidate.subtract(caster.position())))){
                    destination=candidate;lookTarget=target;break;
                }
            }
        }
        if(destination==null)destination=TeleportSpell.findTeleportLocation(level, caster, distance(spellLevel, caster));
        if (destination == null || !level.noCollision(caster, caster.getBoundingBox().move(destination.subtract(caster.position())))) return;
        HazenParticles.ring(level, caster.position(), .5, .15);
        if (caster.isPassenger()) caster.stopRiding();
        Utils.handleSpellTeleport(this, caster, destination);
        if(lookTarget!=null)caster.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,lookTarget.getEyePosition().subtract(0,.15,0));
        if (caster.hasEffect(HazenCombat.CHAOS_STATE.get())) {
            var damageType=level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(
                    net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,HazenEquipment.id("hazen/corrupt_magic")));
            caster.hurt(new net.minecraft.world.damagesource.DamageSource(damageType), Math.max(1, caster.getMaxHealth() * .5f));
        }
        boolean harmonious = caster.getArmorSlots().iterator().hasNext() && HazenCombat.hasHarmoniousEquipment(caster);
        caster.addEffect(new net.minecraft.world.effect.MobEffectInstance(harmonious ? HazenCombat.HARMONIZED.get() : HazenCombat.CHAOS_STATE.get(),
                harmonious ? (int)(getSpellPower(spellLevel, caster)*20) : 120, harmonious ? Math.max(0, spellLevel/2-1) : 1));
        caster.resetFallDistance();
        HazenParticles.ring(level, destination, 1.2, -.25);
    }
    @Override public int getRecastCount(int level, LivingEntity caster) { return kind.equals("call_forth_terraprisma") ? 2 : 0; }
    @Override public ICastDataSerializable getEmptyCastData() {
        return kind.equals("call_forth_terraprisma") ? new SummonedEntitiesCastData() : super.getEmptyCastData();
    }
    @Override public void onRecastFinished(ServerPlayer player, RecastInstance recast, RecastResult result, ICastDataSerializable data) {
        if (!kind.equals("call_forth_terraprisma") || SummonManager.recastFinishedHelper(player, recast, result, data))
            super.onRecastFinished(player, recast, result, data);
    }
    private void summon(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
        if (data.getPlayerRecasts().hasRecastForSpell(this)) return;
        var castData = new SummonedEntitiesCastData();
        for (int i = 0; i < Math.min(spellLevel, 7); i++) {
            var sword = HazenEntities.SUMMON.get().create(level);
            if (sword == null) continue;
            // The supplied source overwrote 25/2 base stats with zero at level one.
            // Treat its explicitly named health/damage bonuses as additions to those bases.
            sword.getAttribute(Attributes.MAX_HEALTH).setBaseValue(25 + (getSpellPower(spellLevel,caster)-1)*.1);
            sword.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(2 + (getSpellPower(spellLevel,caster)-1)*.05);
            sword.setHealth(sword.getMaxHealth()); sword.setPos(caster.position());
            var event = new SpellSummonEvent<LivingEntity>(caster, sword, resource, spellLevel);
            MinecraftForge.EVENT_BUS.post(event);
            LivingEntity creature = event.getCreature();
            if (creature != null && level.addFreshEntity(creature)) SummonManager.initSummon(caster, creature, 12000, castData);
        }
        data.getPlayerRecasts().addRecast(new RecastInstance(getSpellId(), spellLevel, 2, 12000, source, castData), data);
    }
}
