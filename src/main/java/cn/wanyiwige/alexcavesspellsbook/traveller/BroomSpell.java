package cn.wanyiwige.alexcavesspellsbook.traveller;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/** The former KubeJS broom spell, retaining its spell ID and cast parameters. */
public final class BroomSpell extends AbstractSpell {
    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(TravellerAttributes.NAMESPACE, "broom");
    private static final ResourceLocation BROOM_ENTITY =
            ResourceLocation.fromNamespaceAndPath("majobroom", "broom");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(TravellerSchools.RESOURCE)
            .setMaxLevel(2)
            .setCooldownSeconds(600)
            .build();

    public BroomSpell() {
        baseManaCost = 0;
        manaCostPerLevel = 100;
        castTime = 60;
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
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource castSource, MagicData magicData) {
        super.onCast(level, spellLevel, caster, castSource, magicData);
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(BROOM_ENTITY);
        if (type == null) {
            return;
        }
        Entity broom = type.create(serverLevel);
        if (broom == null) {
            return;
        }
        Vec3 position = caster.position().add(caster.getLookAngle().scale(1.0D));
        broom.moveTo(position.x, position.y, position.z, caster.getYRot(), 0.0F);
        serverLevel.addFreshEntity(broom);
    }
}
