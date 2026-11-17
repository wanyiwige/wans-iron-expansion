package cn.wanyiwige.alexcavesspellsbook.traveller;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import java.util.List;

public final class MapTravelSpell extends AbstractSpell {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("kubejs", "map_travel");
    private final DefaultConfig config = new DefaultConfig().setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(TravellerSchools.RESOURCE).setMaxLevel(1).setCooldownSeconds(120).build();

    public MapTravelSpell() {
        baseManaCost = 150;
        manaCostPerLevel = 0;
        castTime = 100;
    }

    @Override public ResourceLocation getSpellResource() { return ID; }
    @Override public DefaultConfig getDefaultConfig() { return config; }
    @Override public CastType getCastType() { return CastType.LONG; }
    @Override public boolean requiresLearning() { return false; }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("spell.kubejs.map_travel.info"));
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
        return caster instanceof Player && super.checkPreCastConditions(level, spellLevel, caster, data);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
        if (caster instanceof ServerPlayer player) {
            MapTravelServer.grant(player);
        }
        super.onCast(level, spellLevel, caster, source, data);
    }
}
